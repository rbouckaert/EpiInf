/*
* Copyright (C) 2013 Tim Vaughan <tgvaughan@gmail.com>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package epiinf.distribs;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.math.Binomial;
import beast.math.GammaFunction;
import beast.util.Randomizer;
import epiinf.*;
import epiinf.models.EpidemicModel;
import epiinf.util.ReplacementSampler;

import java.util.List;
import java.util.Random;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Use SMC to estimate density of tree conditional on model "
    + "parameters.")
public class SMCTreeDensity extends Distribution {

    public Input<EpidemicModel> modelInput = new Input<>(
            "model", "Epidemic model.", Validate.REQUIRED);

    public Input<TreeEventList> treeEventListInput = new Input<>(
            "treeEventList", "Tree event list.", Validate.REQUIRED);

    public Input<Integer> nParticlesInput = new Input<>(
            "nParticles", "Number of particles to use in SMC calculation.",
            Validate.REQUIRED);

    EpidemicModel model;
    TreeEventList treeEventList;
    int nParticles;

    // DEBUG
//    PrintStream debugOut;

    public SMCTreeDensity() {
    }

    @Override
    public void initAndValidate() throws Exception {
        model = modelInput.get();
        treeEventList = treeEventListInput.get();
        nParticles = nParticlesInput.get();
    }

    @Override
    public double calculateLogP() throws Exception {

        logP = 0.0;

        if (treeEventList.getEventList().get(0).time < 0) {
            logP = Double.NEGATIVE_INFINITY;
            return logP;
        }

        double[] particleWeights = new double[nParticles];
        EpidemicState[] particleStates = new EpidemicState[nParticles];
        EpidemicState[] particleStatesNew = new EpidemicState[nParticles];

        // Initialize particles
        for (int p = 0; p < nParticles; p++)
            particleStates[p] = model.getInitialState();

        int k = 1;
        for (TreeEvent treeEvent : treeEventList.getEventList()) {

            // Update particles
            double sumOfWeights = 0.0;
            for (int p = 0; p < nParticles; p++) {

                double newWeight = updateParticle(particleStates[p], k, treeEvent);

                particleWeights[p] = newWeight;
                sumOfWeights += newWeight;
            }

            // Update marginal likelihood estimate
            logP += Math.log(sumOfWeights / nParticles);

            if (!(sumOfWeights > 0.0))
                return Double.NEGATIVE_INFINITY;

            // Sample particle with replacement
            for (int i=0; i<nParticles; i++)
                particleWeights[i] = particleWeights[i]/sumOfWeights;

            ReplacementSampler replacementSampler = new ReplacementSampler(particleWeights);
            for (int p=0; p<nParticles; p++)
                particleStatesNew[p] = particleStates[replacementSampler.next()].copy();

            // Switch particleStates and particleStatesNew
            EpidemicState[] temp = particleStates;
            particleStates = particleStatesNew;
            particleStatesNew = temp;

            // Update lineage counter
            if (treeEvent.type == TreeEvent.Type.COALESCENCE)
                k += 1;
            else
                k -= treeEvent.multiplicity;
        }

        return logP;
    }

    /**
     * Updates weight and state of particle.
     *
     * @param particleState State of particle
     * @param lineages number of tree lineages in interval
     * @param finalTreeEvent tree event which terminates interval
     *
     * @return conditional prob of tree interval under trajectory
     */
    private double updateParticle(EpidemicState particleState,
                                  int lineages, TreeEvent finalTreeEvent) {
        double conditionalP = 1.0;

        while (true) {
            model.calculatePropensities(particleState);

            double infectionProp = model.propensities[EpidemicEvent.INFECTION];
            double allowedRecovProp, forbiddenRecovProp;
            if (particleState.I > lineages) {
                allowedRecovProp = model.propensities[EpidemicEvent.RECOVERY];
                forbiddenRecovProp = 0.0;
            } else {
                allowedRecovProp = 0.0;
                forbiddenRecovProp = model.propensities[EpidemicEvent.RECOVERY];
            }
            double allowedEventProp = infectionProp + allowedRecovProp;

            // Determine size of time increment

            double dt;
            if (allowedEventProp > 0.0)
                dt = Randomizer.nextExponential(allowedEventProp);
            else
                dt = Double.POSITIVE_INFINITY;

            double nextModelEventTime = model.getNextModelEventTime(particleState);

            // Condition against psi-sampling and illegal recovery within interval
            double trueDt = Math.min(dt, Math.min(nextModelEventTime, finalTreeEvent.time) - particleState.time);
            conditionalP *= Math.exp(-trueDt*(model.propensities[EpidemicEvent.PSI_SAMPLE_REMOVE]
                    + model.propensities[EpidemicEvent.PSI_SAMPLE_NOREMOVE]
                    + forbiddenRecovProp));

            // Increment time
            particleState.time += dt;

            // Deal with model events (rho sampling and rate shifts)
            if (nextModelEventTime < finalTreeEvent.time && particleState.time > nextModelEventTime) {

                ModelEvent nextModelEvent = model.getNextModelEvent(particleState);
                if (nextModelEvent.type == ModelEvent.Type.RHO_SAMPLING)
                    return 0.0;
                else {
                    particleState.time = nextModelEventTime;
                    particleState.intervalIdx += 1;
                    continue;
                }
            }

            // Stop here if we're past the end of the tree interval
            if (particleState.time > finalTreeEvent.time)
                break;

            EpidemicEvent event = new EpidemicEvent();
            if (allowedEventProp*Randomizer.nextDouble() < infectionProp) {
                event.type = EpidemicEvent.INFECTION;
            } else
                event.type = EpidemicEvent.RECOVERY;


            // Condition against infection events that produce coalescences not
            // observed in tree.
            if (event.type == EpidemicEvent.INFECTION)
                conditionalP *= 1.0 - lineages * (lineages - 1) / particleState.I / (particleState.I + 1);

            model.incrementState(particleState, event);

            // Early exit if invalid state:
            if (conditionalP == 0)
                return 0.0;

        }

        particleState.time = finalTreeEvent.time;

        // Include probability of tree event
        if (finalTreeEvent.type == TreeEvent.Type.COALESCENCE) {
            model.calculatePropensities(particleState);
            model.incrementState(particleState, EpidemicEvent.Infection);
            conditionalP *= 2.0 / particleState.I / (particleState.I - 1)
                    * model.propensities[EpidemicEvent.INFECTION];
        } else {

            double sampleProb;
            if (model.timesEqual(finalTreeEvent.time, model.getNextModelEventTime(particleState))
                    && model.getNextModelEvent(particleState).type == ModelEvent.Type.RHO_SAMPLING) {

                sampleProb = 0.0;

                // If model contains a rho sampling event at this time, calculate the probability
                // of sampling the number of samples in finalTreeEvent given the current
                // state.
                for (int i = 0; i < model.rhoSamplingProbInput.get().getDimension(); i++) {
                    double rhoProb = model.rhoSamplingProbInput.get().getValue(i);
                    double rhoTime = model.rhoSamplingTimeInput.get().getValue(i);

                    if (Math.abs(rhoTime - finalTreeEvent.time) < model.getTolerance()) {
                        int I = (int) Math.round(particleState.I);
                        int k = finalTreeEvent.multiplicity;
                        sampleProb += Binomial.choose(I, k)
                                * Math.pow(rhoProb, k) * Math.pow(1.0 - rhoProb, I - k);
                    }
                }

                model.incrementState(particleState,
                        EpidemicEvent.MultipleRhoSamples(finalTreeEvent.multiplicity));

            } else {
                if (model.psiSamplingRateInput.get() != null && finalTreeEvent.multiplicity == 1) {
                    model.calculatePropensities(particleState);
                    if (finalTreeEvent.type == TreeEvent.Type.LEAF) {
                        sampleProb = model.propensities[EpidemicEvent.PSI_SAMPLE_REMOVE];
                        model.incrementState(particleState, EpidemicEvent.PsiSampleRemove);
                    } else {
                        // Sampled ancestor
                        sampleProb = model.propensities[EpidemicEvent.PSI_SAMPLE_NOREMOVE];
//                        model.incrementState(particleState, EpidemicEvent.PsiSampleNoRemove);
                    }
                } else {
                    // No explicit sampling process
                    sampleProb = 1.0;
                    model.incrementState(particleState,
                            EpidemicEvent.MultipleOtherSamples(finalTreeEvent.multiplicity));
                }

            }

            conditionalP *= sampleProb * Math.exp(GammaFunction.lnGamma(1 + finalTreeEvent.multiplicity));
        }

        if (!particleState.isValid())
            return 0.0;
        else
            return conditionalP;
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(State state, Random random) {
    }

    @Override
    public boolean isStochastic() {
        return true;
    }
}
