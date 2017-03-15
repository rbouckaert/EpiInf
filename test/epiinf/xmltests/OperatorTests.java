/*
 * Copyright (C) 2017 Tim Vaughan <tgvaughan@gmail.com>
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

package epiinf.xmltests;

import beast.util.Randomizer;
import beast.util.XMLParser;
import org.junit.Test;
import test.beast.beast2vs1.trace.Expectation;
import test.beast.beast2vs1.trace.LogAnalyser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class OperatorTests {

    @Test
    public void testScaleWithInt() throws Exception {
        Randomizer.setSeed(1);

        XMLParser parser = new XMLParser();
        beast.core.Runnable runnable = parser.parseFile(
                new File("test/epiinf/xmltests/ScaleWithIntTest.xml"));
        runnable.run();


        List<Expectation> expectations = new ArrayList<>();
        expectations.add(new Expectation("x1", 0.5, 0.01));
        expectations.add(new Expectation("y1", 0.5, 0.01));
        expectations.add(new Expectation("y2", 0.5, 0.01));
        expectations.add(new Expectation("y3", 0.5, 0.01));
        expectations.add(new Expectation("n", 50.5, 1.0));
        LogAnalyser logAnalyser = new LogAnalyser("ScaleWithIntTest.log", expectations);

        for (Expectation expectation : expectations) {
            assertTrue(expectation.isValid());
            assertTrue(expectation.isPassed());
        }

        Files.deleteIfExists(Paths.get("ScaleWithIntTest.xml.state"));
        Files.deleteIfExists(Paths.get("ScaleWithIntTest.log"));
    }
}
