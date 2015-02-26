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

package com.rgi.common.tile;

import com.rgi.common.coordinate.Coordinate;
import com.rgi.common.tile.scheme.TileMatrixDimensions;

/**
 * @author Luke Lambert
 *
 */
public enum TileOrigin
{
    /**
     * The TileOrigin of a tile is the UpperLeft corner
     */
    UpperLeft (0, 1),

    /**
     * The TileOrigin of a tile is the LowerLeft corner
     */
    LowerLeft (0, 0),

    /**
     * The TileOrigin of a tile is the UpperRight corner
     */
    UpperRight(1, 1),

    /**
     * The TileOrigin of a tile is the LowerRight corner
     */
    LowerRight(1, 0);

    TileOrigin(final int horizontal, final int vertical)
    {
        this.horizontal = horizontal;
        this.vertical   = vertical;
    }

    /**
     * @return vertical field of the TileOrigin
     */
    public int getVertical()
    {
        return this.vertical;
    }

    /**
     * @return horizontal field of the TileOrigin
     */
    public int getHorizontal()
    {
        return this.horizontal;
    }

    /**
     * Transforms a coordinate from one tile origin to its equivalent
     * coordinate in a different tile origin
     *
     * @param toOrigin
     *             The origin that the coordinate will be transformed to
     * @param tileX
     *             Tile column
     * @param tileY
     *             Tile row
     * @param matrixDimensions
     *             The dimensions of the tile matrix
     * @return Transformed tile coordinate
     */
    public Coordinate<Integer> transform(final TileOrigin toOrigin, final int tileX, final int tileY, final TileMatrixDimensions matrixDimensions)
    {
        if(toOrigin == null)
        {
            throw new IllegalArgumentException("Requested tile origin may not be null");
        }

        if(matrixDimensions == null)
        {
            throw new IllegalArgumentException("Tile matrix dimensions may not be null");
        }

        return new Coordinate<>(this.transformHorizontal(toOrigin, tileX, matrixDimensions.getWidth()),
                                this.transformVertical  (toOrigin, tileY, matrixDimensions.getHeight()));
    }

    private int transformHorizontal(final TileOrigin toOrigin, final int tileX, final int tileMatrixWidth)
    {
        return transform(this.getHorizontal(), toOrigin.getHorizontal(), tileX, tileMatrixWidth);
    }

    private int transformVertical(final TileOrigin toOrigin, final int tileY, final int tileMatrixHeight)
    {
        return transform(this.getVertical(), toOrigin.getVertical(), tileY, tileMatrixHeight);
    }

    private static int transform(final int fromDirection, final int toDirection, final int tileCoordinate, final int tileMatrixDimension)
    {
        final int maxTileCoordinate = tileMatrixDimension - 1;

        return tileCoordinate + (fromDirection ^ toDirection) * (maxTileCoordinate - 2*tileCoordinate);
    }

    private final int horizontal;
    private final int vertical;
}
