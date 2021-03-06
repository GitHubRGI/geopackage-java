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

package com.rgi.geopackage.extensions.network;

import com.rgi.geopackage.GeoPackage;

/**
 * Representation of a <b>directional</b> edge from a {@link GeoPackage}
 * "SWAGD_network" network
 *
 * @author Luke Lambert
 *
 */
public class Edge
{
    /**
     * Constructor
     *
     * @param identifier
     *             Unique identifier
     * @param from
     *             The origin node of an edge
     * @param to
     *             The destination node of an edge
     */
    protected Edge(final int identifier,
                   final int from,
                   final int to)
    {
        this.identifier = identifier;
        this.from       = from;
        this.to         = to;
    }

    /**
     * @return the identifier
     */
    public int getIdentifier()
    {
        return this.identifier;
    }

    /**
     * @return the from
     */
    public int getFrom()
    {
        return this.from;
    }

    /**
     * @return the to
     */
    public int getTo()
    {
        return this.to;
    }

    private final int identifier;
    private final int from;
    private final int to;
}
