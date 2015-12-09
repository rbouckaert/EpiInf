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

package epiinf.models;

import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import epiinf.EpidemicEvent;
import epiinf.EpidemicState;

/**
 * General birth-death model of an epidemic.  Suitable only for the
 * exponential growth period of the epidemic.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class BirthDeathModel extends EpidemicModel {
    
    @Override
    public EpidemicState getInitialState() {
        return new EpidemicState(0, 1, 0);
    }

    @Override
    protected double calculateInfectionPropensity(EpidemicState state) {
        return rateCache.get(state.intervalIdx)[EpidemicEvent.INFECTION]*state.I;
    }

    @Override
    protected double calculateRecoveryPropensity(EpidemicState state) {
        return rateCache.get(state.intervalIdx)[EpidemicEvent.RECOVERY]*state.I;
    }

    @Override
    public void incrementState(EpidemicState state, EpidemicEvent event) {
        switch(event.type) {
            case EpidemicEvent.INFECTION:
                state.I += event.multiplicity;
                break;
            case EpidemicEvent.RECOVERY:
            case EpidemicEvent.RHO_SAMPLE:
            case EpidemicEvent.PSI_SAMPLE_REMOVE:
            case EpidemicEvent.OTHER_SAMPLE:
                state.I -= event.multiplicity;
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isCritical(EpidemicState state, double alpha, double tau) {
        double nRemove = propensities[EpidemicEvent.RECOVERY]
                + propensities[EpidemicEvent.PSI_SAMPLE_NOREMOVE]
                + propensities[EpidemicEvent.PSI_SAMPLE_REMOVE];

        return nRemove > state.I;
    }
}
