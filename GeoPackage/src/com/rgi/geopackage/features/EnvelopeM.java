/* The MIT License (MIT)
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

package com.rgi.geopackage.features;

/**
 * @author Luke Lambert
 */
public final class EnvelopeM extends Envelope
{
    public EnvelopeM(final double minimumX,
                     final double maximumX,
                     final double minimumY,
                     final double maximumY,
                     final double minimumM,
                     final double maximumM)
    {
        super(minimumX,
              maximumX,
              minimumY,
              maximumY);

        this.minimumM = minimumM;
        this.maximumM = maximumM;
    }

    @Override
    public double[] toArray()
    {
        return new double[]{ this.getMinimumX(),
                             this.getMaximumX(),
                             this.getMinimumY(),
                             this.getMaximumY(),
                             this.minimumM,
                             this.maximumM
                           };
    }

    public double getMinimumM()
    {
        return this.minimumM;
    }

    public double getMaximumM()
    {
        return this.maximumM;
    }

    @Override
    public boolean hasM()
    {
        return true;
    }

    @Override
    public boolean isEmpty()
    {
        return super.isEmpty()             &&
               Double.isNaN(this.minimumM) &&
               Double.isNaN(this.maximumM);
    }

    @Override
    public EnvelopeContentsIndicator getContentsIndicator()
    {
        return this.isEmpty() ? EnvelopeContentsIndicator.NoEnvelope
                              : EnvelopeContentsIndicator.Xym;
    }

    public static EnvelopeM combine(final EnvelopeM first,
                                    final EnvelopeM second)
    {
        return new EnvelopeM(nanMinimum(first.getMinimumX(), second.getMinimumX()),
                             nanMaximum(first.getMaximumX(), second.getMaximumX()),
                             nanMinimum(first.getMinimumY(), second.getMinimumY()),
                             nanMaximum(first.getMaximumY(), second.getMaximumY()),
                             nanMinimum(first.minimumM,      second.minimumM),
                             nanMaximum(first.maximumM,      second.maximumM));
    }

    public static final EnvelopeM Empty = new EnvelopeM(Double.NaN,
                                                        Double.NaN,
                                                        Double.NaN,
                                                        Double.NaN,
                                                        Double.NaN,
                                                        Double.NaN);

    private final double minimumM;
    private final double maximumM;
}
