/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>
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
package epiinf.util;

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.util.Binomial;
import beast.base.util.GammaFunction;
import beast.base.inference.distribution.Gamma;
import beast.base.util.Randomizer;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Some utility methods that don't fit anywhere else.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class EpiInfUtilityMethods {

    /**
     * Translate event list into file readable by expoTree's calc_likelihood.
     * 
     * @param tree TreeEventList for a tree.
     * @param origin Time of origin.
     * @param ps PrintStream  where output is sent.
     */
    public static void writeExpoTreeFile(Tree tree, double origin, PrintStream ps) {

        List<Node> nodeList = new ArrayList<>(Arrays.asList(tree.getNodesAsArray()));
        nodeList.sort((Node o1, Node o2) -> {
            if (o1.getHeight()<o2.getHeight())
                return -1;
            if (o1.getHeight()>o2.getHeight())
                return 1;
            return 0;
        });

        for (Node node : nodeList) {
            if (node.isLeaf())  {
                if (node.getHeight()>0.0)
                    ps.println(node.getHeight() + " 0");
            } else {
                ps.println(node.getHeight() + " 1");
            }
        }

        ps.println(origin + " 99");
    }

    /**
     * Basic inverse CDF sampler for binomial distribution.
     *
     * @param p success probability
     * @param n number of trials
     * @return number of successes
     */
    private static int nextBinomialICDF(double p, int n) {

        double u = Randomizer.nextDouble();

        int m = 0;
        double acc = 0;
        do {
            acc += Binomial.choose(n, m)*Math.pow(p, m)*Math.pow(1.0-p, n-m);
        } while (u > acc && ++m < n);

        return m;
    }

    /**
     * Sampler for binomial distribution that uses a normal approximation.
     * Need a reject step in here to make this exact - currently only approximate!
     *
     * @param p success probability
     * @param n number of trials
     * @return number of successes
     */
    private static int nextBinomialNormal(double p, int n) {
        return (int)Math.round(n*p + Randomizer.nextGaussian()*Math.sqrt(n*p*(1-p)));
    }

    /**
     * Sampler for binomial distribution.
     *
     * @param p success probability
     * @param n number of trials
     * @return number of successes
     */
    public static int nextBinomial(double p, int n) {
        if (p == 0 || n == 0)
            return 0;

        if (p == 1.0)
            return n;

        if (n*p > 10.0 && n*(1.0 - p) > 10.0)
            return nextBinomialNormal(p, n);

        return nextBinomialICDF(p, n);
    }

    public static void main(String[] args) throws FileNotFoundException {

        PrintStream outf = new PrintStream("out.txt");
        for (int i=0; i<10000; i++) {
            outf.println(nextBinomial(0.9, 1000));
        }
        outf.close();
    }

}
