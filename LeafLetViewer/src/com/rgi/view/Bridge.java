package com.rgi.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import com.rgi.common.BoundingBox;
import com.rgi.common.coordinate.Coordinate;
import com.rgi.common.coordinate.CrsCoordinate;
import com.rgi.common.coordinate.referencesystem.profile.CrsProfile;
import com.rgi.common.coordinate.referencesystem.profile.CrsProfileFactory;
import com.rgi.common.tile.TileOrigin;
import com.rgi.common.tile.scheme.TileScheme;
import com.rgi.common.tile.scheme.ZoomTimesTwo;
import com.rgi.common.util.ImageUtility;
import com.rgi.store.tiles.TileStoreException;
import com.rgi.store.tiles.TileStoreReader;

public class Bridge
{
    private final        TileStoreReader selectedReader;
    private final        Integer         minimumZoomLevel;
    private final        Integer         maximumZoomLevel;
    private       static CrsProfile      crsProfile;
    private final static TileOrigin      leafletOrigin = TileOrigin.LowerLeft;
    private final static TileScheme      leafletTileScheme = new ZoomTimesTwo(0, 31, 1, 1);

    public Bridge(final Collection<TileStoreReader> tileStoreReaders) throws TileStoreException
    {
        //this selects first reader selected this will change to get the selected reader
        this.selectedReader = (TileStoreReader) tileStoreReaders.toArray()[0];

        crsProfile = CrsProfileFactory.create(this.selectedReader.getCoordinateReferenceSystem());

        this.minimumZoomLevel = this.selectedReader.getZoomLevels().stream().min(Integer::compare).orElse(-1);
        this.maximumZoomLevel = this.selectedReader.getZoomLevels().stream().max(Integer::compare).orElse(-1);

    }

    public byte[] getTile(final int z, final int x, final int y) throws TileStoreException, IOException
    {
        Coordinate<Integer> transformedCoordinate = leafletOrigin.transform(this.selectedReader.getTileOrigin(), x, y, leafletTileScheme.dimensions(z));

        CrsCoordinate crsCoordinate = crsProfile.tileToCrsCoordinate(transformedCoordinate.getX(),
                                                                     transformedCoordinate.getY(),
                                                                     this.selectedReader.getBounds(),
                                                                     leafletTileScheme.dimensions(z),
                                                                     this.selectedReader.getTileOrigin());

       BufferedImage tile = this.selectedReader.getTile(crsCoordinate, z);

        return ImageUtility.bufferedImageToBytes(tile, this.selectedReader.getImageType());
    }

    public BoundingBox getBounds() throws TileStoreException
    {
        return this.selectedReader.getBounds();
    }

    public Set<Integer> getZooms() throws TileStoreException
    {
        return this.selectedReader.getZoomLevels();
    }

    public int getMinZoom()
    {
        return this.minimumZoomLevel;
    }

    public int getMaxZoom()
    {
        return this.maximumZoomLevel;
    }
}
