/*  Copyright (C) 2014 Reinventing Geospatial, Inc
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>,
 *  or write to the Free Software Foundation, Inc., 59 Temple Place -
 *  Suite 330, Boston, MA 02111-1307, USA.
 */

package com.rgi.common.coordinate.referencesystem.profile;

import com.rgi.common.BoundingBox;
import com.rgi.common.Dimensions;
import com.rgi.common.coordinate.Coordinate;
import com.rgi.common.coordinate.CoordinateReferenceSystem;
import com.rgi.common.coordinate.CrsCoordinate;
import com.rgi.common.tile.TileOrigin;
import com.rgi.common.tile.scheme.TileMatrixDimensions;

/**
 * @author Luke Lambert
 *
 */
public class SphericalMercatorCrsProfile implements CrsProfile
{
    @Override
    public Coordinate<Integer> crsToTileCoordinate(final CrsCoordinate        coordinate,
                                                   final TileMatrixDimensions dimensions,
                                                   final TileOrigin           tileOrigin)
    {
        if(coordinate == null)
        {
            throw new IllegalArgumentException("Meter coordinate may not be null");
        }

        if(dimensions == null)
        {
            throw new IllegalArgumentException("Tile matrix dimensions may not be null");
        }

        if(tileOrigin == null)
        {
            throw new IllegalArgumentException("Origin may not be null");
        }

        if(!Utility.contains(Bounds, coordinate, tileOrigin))
        {
            throw new IllegalArgumentException("Coordinate is outside the bounds of this coordinate reference system");
        }

        final Coordinate<Double> tileCorner = Utility.tileCorner(Bounds, tileOrigin);

        final double tileHeightInSrs = Bounds.getHeight() / dimensions.getHeight();
        final double tileWidthInSrs  = Bounds.getWidth()  / dimensions.getWidth();

        final double normalizedSrsTileCoordinateY = Math.abs(coordinate.getY() - tileCorner.getY());
        final double normalizedSrsTileCoordinateX = Math.abs(coordinate.getX() - tileCorner.getX());

        final int tileY = (int)Math.floor(normalizedSrsTileCoordinateY / tileHeightInSrs);
        final int tileX = (int)Math.floor(normalizedSrsTileCoordinateX / tileWidthInSrs);

        return new Coordinate<>(tileY, tileX);
    }

    @Override
    public CrsCoordinate tileToCrsCoordinate(final int                  row,
                                             final int                  column,
                                             final TileMatrixDimensions dimensions,
                                             final TileOrigin           tileOrigin)
    {
        if(row < 0)
        {
            throw new IllegalArgumentException("Row must be at least 0");
        }

        if(column < 0)
        {
            throw new IllegalArgumentException("Column must be at least 0");
        }

        if(dimensions == null)
        {
            throw new IllegalArgumentException("Tile matrix dimensions may not be null");
        }

        if(tileOrigin == null)
        {
            throw new IllegalArgumentException("Origin may not be null");
        }

        final double tileHeight = EarthEquatorialCircumfrence / dimensions.getHeight();
        final double tileWidth  = EarthEquatorialCircumfrence / dimensions.getWidth();

        final double originShift = (EarthEquatorialCircumfrence / 2.0);

        final Coordinate<Integer> tileCoordinate = TileOrigin.LowerLeft.transform(tileOrigin,
                                                                                  row,
                                                                                  column,
                                                                                  dimensions);

        return new CrsCoordinate((tileCoordinate.getY() * tileHeight) - originShift,
                                 (tileCoordinate.getX() * tileWidth)  - originShift,
                                 this.getCoordinateReferenceSystem());
    }

    @Override
    public Dimensions getTileDimensions(final TileMatrixDimensions dimensions)
    {
        return new Dimensions(EarthEquatorialCircumfrence / dimensions.getHeight(),
                              EarthEquatorialCircumfrence / dimensions.getWidth());
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem()
    {
        return SphericalMercatorCrsProfile.CoordinateReferenceSystem;
    }

    @Override
    public Coordinate<Double> toGlobalGeodetic(final Coordinate<Double> coordinate)
    {
        // TODO algorithm documentation
        return new Coordinate<>(Math.toDegrees(2 * Math.atan(Math.exp(coordinate.getY() / EarthEquatorialRadius)) - Math.PI / 2),
                                Math.toDegrees(coordinate.getX() / EarthEquatorialRadius));
    }

    @Override
    public BoundingBox getBounds()
    {
        return Bounds;
    }

    /**
     * Datum's spheroid's semi-major axis (radius of earth) in meters
     */
    public static final double EarthEquatorialRadius = 6378137.0;

    public static final BoundingBox Bounds = new BoundingBox(-Math.PI * EarthEquatorialRadius,
                                                             -Math.PI * EarthEquatorialRadius,
                                                              Math.PI * EarthEquatorialRadius,
                                                              Math.PI * EarthEquatorialRadius);

    /**
     * Earth's equatorial circumference (based on the datum's spheroid's semi-major axis, radius) in meters
     */
    public static final double EarthEquatorialCircumfrence = 2.0 * Math.PI * EarthEquatorialRadius;

    private final static CoordinateReferenceSystem CoordinateReferenceSystem = new CoordinateReferenceSystem("EPSG", 3857);

}
