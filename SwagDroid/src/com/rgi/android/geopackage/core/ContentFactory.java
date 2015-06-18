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
package com.rgi.android.geopackage.core;

import com.rgi.android.common.BoundingBox;

/**
 * @author Luke Lambert
 *
 * @param <T> Extends {@link Content}
 */
public interface ContentFactory<T extends Content>
{
    /**
     * @param tableName the table name
     * @param dataType the data type
     * @param identifier the identifier
     * @param description the description
     * @param lastChange  the time of the last change
     * @param boundingBox the bounding box
     * @param spatialReferenceSystemIdentifier the spatial reference system version number (otherwise known as identifier)
     * @return a Content object with the following parameters
     */
    public T create(final String      tableName,
                    final String      dataType,
                    final String      identifier,
                    final String      description,
                    final String      lastChange,
                    final BoundingBox boundingBox,
                    final Integer     spatialReferenceSystemIdentifier);
}