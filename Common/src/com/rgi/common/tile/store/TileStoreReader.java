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

package com.rgi.common.tile.store;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.stream.Stream;

import com.rgi.common.BoundingBox;
import com.rgi.common.Dimensions;
import com.rgi.common.coordinate.CoordinateReferenceSystem;
import com.rgi.common.coordinate.CrsCoordinate;
import com.rgi.common.tile.scheme.TileScheme;

/**
 * Interface for tile store reading
 *
 * @author Luke Lambert
 *
 */
public interface TileStoreReader
{
    /**
     * Gets the geographic bounds
     *
     * @return Returns a {@link BoundingBox} that represents the minimum
     *             bounding area of the data contained in this tile store, in
     *             the units of the store's coordinate reference system. This
     *             is not necessarily the same value as the bounds of the
     *             store's tile matrices.
     * @throws TileStoreException
     *             Wraps errors thrown by the tile store reader implementation
     */
    public BoundingBox getBounds() throws TileStoreException;

    /**
     * Count the number of tiles in this tile store.
     *
     * @return The number of tiles contained within this tile store.
     * @throws TileStoreException
     *             Wraps errors thrown by the tile store reader implementation
     */
    public long countTiles() throws TileStoreException;

    /**
     * Return the byte size of this tile store.
     *
     * @return The approximate size of this tile store in bytes.
     * @throws TileStoreException
     *             When an error occurs reading the from the file to calculate
     *             the file's total space due to a SecurityException (If a
     *             security manager has been installed and it denies
     *             RuntimePermission("getFileSystemAttributes") or its
     *             SecurityManager.checkRead(String) method denies read access
     *             to the file), a TileStoreException is thrown
     */
    public long getByteSize() throws TileStoreException;

    /**
     * Get a tile at a specified zoom, column (x) and row (y)
     *
     * @param column
     *             The 'x' portion of the coordinate. This value is relative to this tile store's tile scheme.
     * @param row
     *             The 'y' portion of the coordinate. This value is relative to this tile store's tile scheme.
     * @param zoomLevel
     *            The zoom level of the tile.
     * @return A buffered image, or null if the tile store has no tile data for the specified coordinate
     * @throws TileStoreException
     *             A TileStoreException occurs if an error occurs during tile retrieval.
     */
    public BufferedImage getTile(final int column, final int row, final int zoomLevel) throws TileStoreException;

    /**
     * Get a tile at a specified zoom, and geographic coordinate.
     *
     * @param coordinate
     *            Geographic coordinate that corresponds to the requested tile
     * @param zoomLevel
     *            The zoom level of the tile
     * @return A buffered image, or null if the tile store has no tile data for the specified coordinate
     * @throws TileStoreException
     *             A TileStoreException occurs if an error occurs during tile retrieval.
     */
    public BufferedImage getTile(final CrsCoordinate coordinate, final int zoomLevel) throws TileStoreException;

    /**
     * Ask the tile store for all the zoom levels that it contains.
     *
     * @return A list of integers that represent zoom levels this tile store
     *         contains.
     * @throws TileStoreException
     *             A TileStoreException is thrown when the list of zoom levels
     *             cannot be built.
     */
    public Set<Integer> getZoomLevels() throws TileStoreException;

    /**
     * Gets a stream of every tile in the tile store. Tile stores need not
     * contain the maximum number of tiles (rows * columns, per zoom level) so
     * missing entries will not be represented by this stream.
     *
     * @return Returns a {@link Stream} of {@link TileHandle}s
     * @throws TileStoreException
     *             A TileStoreException is thrown when unable to build a {@link Stream}
     *             of  {@link TileHandle}s based on the File Path
     */
    public Stream<TileHandle> stream() throws TileStoreException;


    /**
     * Gets a stream of every tile in the tile store for a given zoom level. The
     * zoom level need not contain the maximum number of tiles (rows * columns)
     * so missing entries will not be represented by this stream. If there are
     * no tiles at this zoom level, an empty stream will be returned.
     *
     * @param zoomLevel
     *            The zoom level of the requested tiles
     * @return Returns a {@link Stream} of {@link TileHandle}s
     * @throws TileStoreException
     *             A TileStoreException is thrown when unable to build a
     *             {@link Stream} of {@link TileHandle}s based on the given File
     *             Path
     */
    public Stream<TileHandle> stream(final int zoomLevel) throws TileStoreException;

    /**
     * @return returns the tile store's coordinate reference system
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * @return returns a human readable identifier for this tile store
     */
    public String getName();

    /**
     * @return Returns the best guess for the image type (MimeType subtype).
     *         Tile stores need not necessarily contain a single image type, so
     *         the store's implementation will return what it considers the most
     *         suitable. This function may return null if there are no tiles in
     *         the store.
     * @throws TileStoreException
     *             A TileStoreException is thrown when the method
     *             {@link #stream()} throws an Exception or if unable to
     *             retrieve the specified tile.
     */
    public String getImageType() throws TileStoreException;

    /**
     * @return Returns the best guess for the pixel dimensions of the tile
     *         store's images. Tile stores may contain images of differing
     *         sizes, so the store's implementation will return what it
     *         considers the most suitable. This function may return null if
     *         there are no tiles in the store.
     * @throws TileStoreException
     *             A TileStoreException is thrown if the method
     *             {@link #stream()} throws or if unable to get the specified
     *             tile.
     *
     */
    public Dimensions<Integer> getImageDimensions() throws TileStoreException;

    /**
     * @return the Tile Scheme which can calculate the number of tiles at a particular zoom level
     */
    public TileScheme getTileScheme();
}
