/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rgi.geopackage.features.geometry.zm;

import com.rgi.geopackage.features.EnvelopeContentsIndicator;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Luke Lambert
 */
public final class EnvelopeZMTest
{
    /**
     * Test the constructor
     */
    @Test
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    public void constructor()
    {
        new EnvelopeZM(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }


    /**
     * Test toArray()
     */
    @Test
    public void toArray()
    {
        //noinspection ZeroLengthArrayAllocation
        assertArrayEquals("toArray returned an incorrect value for an empty envelope",
                          new double[] {},
                          new EnvelopeZM(Double.NaN,
                                         Double.NaN,
                                         Double.NaN,
                                         Double.NaN,
                                         Double.NaN,
                                         Double.NaN,
                                         Double.NaN,
                                         Double.NaN).toArray(),
                          0.0);

        final double minX = 0.0;
        final double minY = 1.0;
        final double minZ = 2.0;
        final double minM = 3.0;
        final double maxX = 4.0;
        final double maxY = 5.0;
        final double maxZ = 6.0;
        final double maxM = 7.0;

        final double[] array = { minX, maxX, minY, maxY, minZ, maxZ, minM, maxM };

        assertArrayEquals("toArray returned an incorrect value for a non-empty envelope",
                          array,
                          new EnvelopeZM(minX,
                                         minY,
                                         minZ,
                                         minM,
                                         maxX,
                                         maxY,
                                         maxZ,
                                         maxM).toArray(),
                          0.0);
    }

    /**
     * Test accessors
     */
    @Test
    public void accessors()
    {
        final double minX = 0.0;
        final double minY = 1.0;
        final double minZ = 2.0;
        final double minM = 3.0;
        final double maxX = 4.0;
        final double maxY = 5.0;
        final double maxZ = 6.0;
        final double maxM = 7.0;

        final EnvelopeZM envelope = new EnvelopeZM(minX,
                                                   minY,
                                                   minZ,
                                                   minM,
                                                   maxX,
                                                   maxY,
                                                   maxZ,
                                                   maxM);

        assertEquals("getMinimumZ returned the wrong value",
                     minZ,
                     envelope.getMinimumZ(),
                     0.0);

        assertEquals("getMaximumZ returned the wrong value",
                     maxZ,
                     envelope.getMaximumZ(),
                     0.0);

        assertEquals("getMinimumM returned the wrong value",
                     minM,
                     envelope.getMinimumM(),
                     0.0);

        assertEquals("getMaximumM returned the wrong value",
                     maxM,
                     envelope.getMaximumM(),
                     0.0);
    }

    /**
     * Test dimensionality
     */
    @Test
    public void dimensionality()
    {
        assertTrue("EnvelopeZM should support Z values",
                   new EnvelopeZM(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).hasZ());

        assertTrue("EnvelopeZM should support M values",
                   new EnvelopeZM(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).hasM());
    }

    /**
     * Test getContentsIndicator()
     */
    @Test
    public void getContentsIndicator()
    {
        assertSame("getContentsIndicator returned the wrong value",
                   EnvelopeContentsIndicator.NoEnvelope,
                   new EnvelopeZM(Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN,
                                  Double.NaN).getContentsIndicator());

        assertSame("getContentsIndicator returned the wrong value",
                   EnvelopeContentsIndicator.Xyzm,
                   new EnvelopeZM(0.0,
                                  1.0,
                                  2.0,
                                  3.0,
                                  4.0,
                                  5.0,
                                  6.0,
                                  7.0).getContentsIndicator());
    }

    /**
     * Test combine()
     */
    @Test
    public void combine()
    {
        final EnvelopeZM envelope1 = new EnvelopeZM(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        final EnvelopeZM envelope2 = new EnvelopeZM(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        final EnvelopeZM combined = EnvelopeZM.combine(envelope1, envelope2);

        assertEquals("combine() picked the wrong minimum x value",
                     Math.min(envelope1.getMinimumX(),
                              envelope2.getMinimumX()),
                     combined.getMinimumX(),
                     0.0);

        assertEquals("combine() picked the wrong minimum y value",
                     Math.min(envelope1.getMinimumY(),
                              envelope2.getMinimumY()),
                     combined.getMinimumY(),
                     0.0);

        assertEquals("combine() picked the wrong minimum z value",
                     Math.min(envelope1.getMinimumZ(),
                              envelope2.getMinimumZ()),
                     combined.getMinimumZ(),
                     0.0);

        assertEquals("combine() picked the wrong minimum m value",
                     Math.min(envelope1.getMinimumM(),
                              envelope2.getMinimumM()),
                     combined.getMinimumM(),
                     0.0);

        assertEquals("combine() picked the wrong maximum x value",
                     Math.max(envelope1.getMaximumX(),
                              envelope2.getMaximumX()),
                     combined.getMaximumX(),
                     0.0);

        assertEquals("combine() picked the wrong maximum y value",
                     Math.max(envelope1.getMaximumY(),
                              envelope2.getMaximumY()),
                     combined.getMaximumY(),
                     0.0);

        assertEquals("combine() picked the wrong maximum z value",
                     Math.max(envelope1.getMaximumZ(),
                              envelope2.getMaximumZ()),
                     combined.getMaximumZ(),
                     0.0);

        assertEquals("combine() picked the wrong maximum m value",
                     Math.max(envelope1.getMaximumM(),
                              envelope2.getMaximumM()),
                     combined.getMaximumM(),
                     0.0);
    }
}
