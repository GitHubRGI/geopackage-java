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

package com.rgi.geopackage.features.geometry.m;

import com.rgi.geopackage.features.ByteOutputStream;
import com.rgi.geopackage.features.GeometryType;
import com.rgi.geopackage.features.WellKnownBinaryFormatException;
import com.rgi.geopackage.features.geometry.Geometry;
import com.rgi.geopackage.features.geometry.GeometryFactory;
import com.rgi.geopackage.features.geometry.xy.WkbPoint;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Luke Lambert
 */
@SuppressWarnings("JavaDoc")
public class WkbGeometryCollectionMTest
{
    @BeforeClass
    public static void setUp()
    {
        GEOMETRY_FACTORIES.put(GeometryType.Point.getCode(), WkbPoint::readWellKnownBinary);
        GEOMETRY_FACTORIES.put(WkbGeometryM.GeometryTypeDimensionalityBase + GeometryType.GeometryCollection.getCode(), (byteBuffer) -> WkbGeometryCollectionM.readWellKnownBinary(WkbGeometryCollectionMTest::createGeometry, byteBuffer));
    }

    /**
     * Test the ellipsis constructor
     */
    @Test
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    public void ellipsisConstructor()
    {
        new WkbGeometryCollectionM<>(new WkbPointM(1.0, 1.0, 1.0),
                                     new WkbLineStringM());
    }

    /**
     * Test the collection constructor
     */
    @Test
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    public void collectionConstructor()
    {
        new WkbGeometryCollectionM<>(Arrays.asList(new WkbPointM(1.0, 1.0, 1.0),
                                                   new WkbLineStringM()));
    }

    /**
     * Test the collection constructor with a null collection
     */
    @Test(expected = IllegalArgumentException.class)
    public void collectionConstructorWithNull()
    {
        //noinspection CastToConcreteClass
        new WkbGeometryCollectionM<>((Collection<WkbGeometryM>)null);
    }

    /**
     * Test the collection constructor with a null geometry
     */
    @Test(expected = IllegalArgumentException.class)
    public void collectionConstructorWithNullGeometry()
    {
        //noinspection CastToConcreteClass
        new WkbGeometryCollectionM<>(Arrays.asList((WkbPointM)null));
    }

    /**
     * Test equals with the same object
     */
    @Test
    public void testEqualsSameObject()
    {
        final WkbGeometryCollectionM<WkbPointM> collection = new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0));

        //noinspection EqualsWithItself,SimplifiableJUnitAssertion
        assertTrue("equals returned false for testing an object against itself",
                   collection.equals(collection));
    }

    /**
     * Test equals with null
     */
    @Test
    public void testEqualsWithNull()
    {
        final WkbGeometryCollectionM<WkbPointM> collection = new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0));


        //noinspection SimplifiableJUnitAssertion,ObjectEqualsNull
        assertFalse("equals returned true for testing against null",
                    collection.equals(null));
    }

    /**
     * Test equals with a different type
     */
    @Test
    public void testEqualsWithDifferentType()
    {
        final WkbGeometryCollectionM<WkbPointM> collection = new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0));

        //noinspection UnnecessaryBoxing,EqualsBetweenInconvertibleTypes
        assertFalse("equals returned true for testing with an inconvertible type",
                    collection.equals(Integer.valueOf(0)));
    }

    /**
     * Test equals
     */
    @Test
    public void testEquals()
    {
        final WkbGeometryCollectionM<WkbPointM> collection1 = new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                                                           new WkbPointM(0.0, 0.0, 0.0),
                                                                                           new WkbPointM(0.0, 0.0, 0.0));

        final WkbGeometryCollectionM<WkbPointM> collection2 = new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                                                           new WkbPointM(0.0, 0.0, 0.0),
                                                                                           new WkbPointM(0.0, 0.0, 0.0));

        assertEquals("equals returned false for testing equivalent objects",
                     collection1,
                     collection2);
    }

    /**
     * Test hashCode()
     */
    @Test
    public void testHashCode()
    {
        assertEquals("hashCode returned the wrong value",
                     29791,
                     new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                  new WkbPointM(0.0, 0.0, 0.0),
                                                  new WkbPointM(0.0, 0.0, 0.0)).hashCode());
    }

    /**
     * Test getTypeCode()
     */
    @Test
    public void getTypeCode()
    {
        assertEquals("getTypeCode() returned the wrong value",
                     WkbGeometryM.GeometryTypeDimensionalityBase + GeometryType.GeometryCollection.getCode(),
                     new WkbGeometryCollectionM<>().getTypeCode());
    }

    /**
     * Test getGeometryTypeName()
     */
    @Test
    public void getGeometryTypeName()
    {
        assertEquals("getGeometryTypeName() returned the wrong value",
                     GeometryType.GeometryCollection.toString(),
                     new WkbGeometryCollectionM<>().getGeometryTypeName());
    }

    /**
     * Test isEmpty()
     */
    @Test
    public void testIsEmpty()
    {
        assertTrue("isEmpty() should have returned true",
                   new WkbGeometryCollectionM<>().isEmpty());

        assertFalse("isEmpty() should have returned false",
                    new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                 new WkbPointM(0.0, 0.0, 0.0),
                                                 new WkbPointM(0.0, 0.0, 0.0)).isEmpty());
    }

    /**
     * Test the serialization of write/read well known binary
     */
    @Test
    public void writeReadWellKnownBinary() throws WellKnownBinaryFormatException
    {
        final WkbGeometryCollectionM<WkbPointM> collection = new WkbGeometryCollectionM<>(new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0),
                                                                                          new WkbPointM(0.0, 0.0, 0.0));

        try(final ByteOutputStream output = new ByteOutputStream())
        {
            collection.writeWellKnownBinary(output);

            final WkbGeometryCollectionM<WkbGeometryM> foundCollection = WkbGeometryCollectionM.readWellKnownBinary(WkbPointM::readWellKnownBinary,
                                                                                                                    ByteBuffer.wrap(output.array()));

            assertEquals("error in well known binary (WKB) reading/writing",
                         collection,
                         foundCollection);
        }
    }

    /**
     * Test the serialization of write/read well known binary with a geometry with a geometry with incorrect
     * dimensionality
     */
    @Test(expected = WellKnownBinaryFormatException.class)
    public void writeReadWellKnownBinaryWithBadGeometry() throws WellKnownBinaryFormatException
    {
        try(final ByteOutputStream output = new ByteOutputStream())
        {
            //noinspection NumericCastThatLosesPrecision
            output.write((byte)(output.getByteOrder().equals(ByteOrder.BIG_ENDIAN) ? 0 : 1));
            //noinspection NumericCastThatLosesPrecision
            output.write((int)(new WkbGeometryCollectionM<>().getTypeCode()));

            output.write(1);    // number of contained geometries

            new WkbPoint(0.0, 0.0).writeWellKnownBinary(output);    // Geometry with the wrong dimensionality

            createGeometry(ByteBuffer.wrap(output.array()));
        }
    }

    /**
     * Test createEnvelope()
     */
    @Test
    public void createEnvelope()
    {
        @SuppressWarnings("CastToConcreteClass")
        final EnvelopeM emptyEnvelope = (EnvelopeM)new WkbGeometryCollectionM<>().createEnvelope();

        assertTrue("envelope minimum x value is wrong",
                   Double.isNaN(emptyEnvelope.getMinimumX()));

        assertTrue("envelope minimum y value is wrong",
                   Double.isNaN(emptyEnvelope.getMinimumY()));

        assertTrue("envelope minimum z value is wrong",
                   Double.isNaN(emptyEnvelope.getMinimumM()));

        assertTrue("envelope maximum x value is wrong",
                   Double.isNaN(emptyEnvelope.getMaximumX()));

        assertTrue("envelope maximum y value is wrong",
                   Double.isNaN(emptyEnvelope.getMaximumY()));

        assertTrue("envelope maximum z value is wrong",
                   Double.isNaN(emptyEnvelope.getMaximumM()));

        final double min = 0.0;
        final double max = 1.0;

        final EnvelopeM envelope = new WkbGeometryCollectionM<>(new WkbPointM(min, min, min),
                                                                new WkbPointM(max, max, max)).createEnvelopeM();

        assertEquals("combine() picked the wrong minimum x value",
                     min,
                     envelope.getMinimumX(),
                     0.0);

        assertEquals("combine() picked the wrong minimum y value",
                     min,
                     envelope.getMinimumY(),
                     0.0);

        assertEquals("combine() picked the wrong minimum z value",
                     min,
                     envelope.getMinimumM(),
                     0.0);

        assertEquals("combine() picked the wrong maximum x value",
                     max,
                     envelope.getMaximumX(),
                     0.0);

        assertEquals("combine() picked the wrong maximum y value",
                     max,
                     envelope.getMaximumY(),
                     0.0);

        assertEquals("combine() picked the wrong maximum z value",
                     max,
                     envelope.getMaximumM(),
                     0.0);
    }

    private static Geometry createGeometry(final ByteBuffer wkbByteBuffer) throws WellKnownBinaryFormatException
    {
        try
        {
            wkbByteBuffer.mark(); // Save the buffer position (.mark()) before we read the well known binary header.

            final ByteOrder byteOrder = wkbByteBuffer.get() == 0 ? ByteOrder.BIG_ENDIAN
                                                                 : ByteOrder.LITTLE_ENDIAN;

            wkbByteBuffer.order(byteOrder);

            // Read 4 bytes as an /unsigned/ int
            final long geometryType = Integer.toUnsignedLong(wkbByteBuffer.getInt());

            wkbByteBuffer.reset(); // This will reset the position to before the well known binary header.

            return GEOMETRY_FACTORIES.get(geometryType)
                                     .create(wkbByteBuffer);
        }
        catch(final BufferUnderflowException bufferUnderflowException)
        {
            throw new WellKnownBinaryFormatException(bufferUnderflowException);
        }
    }

    private static final Map<Long, GeometryFactory> GEOMETRY_FACTORIES = new HashMap<>();
}
