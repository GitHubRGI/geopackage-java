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

/**
 * @author Luke Lambert
 * @author Duff Means
 *
 */
public enum TileOrigin
{
    UpperLeft(1, 1),
    LowerLeft(1, -1),
    UpperRight(-1, 1),
    LowerRight(-1, -1);

    TileOrigin(final int horizontal, final int vertical)
    {
        this.horizontal = horizontal;
        this.vertical   = vertical;
    }

    private final int horizontal;
    private final int vertical;

    public int getDeltaX()
    {
        return this.horizontal;
    }

    public int getDeltaY()
    {
        return this.vertical;
    }
}
