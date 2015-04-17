package com.rgi.g2t;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.naming.OperationNotSupportedException;

import org.gdal.gdal.Dataset;

import utility.GdalUtility;
import utility.GdalUtility.GdalRasterParameters;

import com.rgi.common.BoundingBox;
import com.rgi.common.Dimensions;
import com.rgi.common.Range;
import com.rgi.common.coordinate.Coordinate;
import com.rgi.common.coordinate.CoordinateReferenceSystem;
import com.rgi.common.coordinate.CrsCoordinate;
import com.rgi.common.coordinate.referencesystem.profile.CrsProfile;
import com.rgi.common.coordinate.referencesystem.profile.CrsProfileFactory;
import com.rgi.common.tile.TileOrigin;
import com.rgi.common.tile.scheme.TileMatrixDimensions;
import com.rgi.common.tile.scheme.TileScheme;
import com.rgi.common.tile.scheme.ZoomTimesTwo;
import com.rgi.common.tile.store.TileHandle;
import com.rgi.common.tile.store.TileStoreException;
import com.rgi.common.tile.store.TileStoreReader;
import com.rgi.common.util.FileUtility;

/**
 * TileStoreReader implementation for GDAL-readable image files
 *
 * @author Steven D. Lander
 * @author Luke D. Lambert
 *
 */
public class RawImageTileReader implements TileStoreReader
{
    /**
     * Constructor
     *
     * @param rawImage
     *             A raster image
     * @param tileSize
     *             A {@link Dimensions} that describes what an individual tile
     *             looks like
     * @param noDataColor
     *             The {@link Color} of the NODATA fields within the raster image
     * @throws TileStoreException
     *             Thrown when GDAL could not get the correct
     *             {@link CoordinateReferenceSystem} of the input raster OR if
     *             the raw image could not be loaded as a {@link Dataset}
     */
    public RawImageTileReader(final File                rawImage,
                              final Dimensions<Integer> tileSize,
                              final Color               noDataColor) throws TileStoreException
    {
        this(rawImage,
             tileSize,
             noDataColor,
             null);
    }

    /**
     * Constructor
     *
     * @param rawImage
     *             A raster image {@link File}
     * @param tileSize
     *             A {@link Dimensions} that describes what an individual tile
     *             looks like
     * @param noDataColor
     *             The {@link Color} of the NODATA fields within the raster image
     * @param coordinateReferenceSystem
     *             The {@link CoordinateReferenceSystem} the tiles should be
     *             output in
     * @throws TileStoreException
     *             Thrown when GDAL could not get the correct
     *             {@link CoordinateReferenceSystem} of the input raster OR if
     *             the raw image could not be loaded as a {@link Dataset}
     */
    public RawImageTileReader(final File                      rawImage,
                              final Dimensions<Integer>       tileSize,
                              final Color                     noDataColor,
                              final CoordinateReferenceSystem coordinateReferenceSystem) throws TileStoreException
    {
        if(rawImage == null || !rawImage.canRead())
        {
            throw new IllegalArgumentException("Raw image may not be null, and must represent a valid file on disk.");
        }

        if(tileSize == null)
        {
            throw new IllegalArgumentException("Tile size may not be null.");
        }

        this.rawImage    = rawImage;
        this.tileSize    = tileSize;
        this.noDataColor = noDataColor;

        this.dataset = GdalUtility.open(rawImage, coordinateReferenceSystem);

        try
        {
            this.coordinateReferenceSystem = GdalUtility.getCoordinateReferenceSystem(GdalUtility.getSpatialReference(this.dataset));

            if(this.coordinateReferenceSystem == null)
            {
                throw new IllegalArgumentException("Image file is not in a recognized coordinate reference system");
            }

            this.profile = CrsProfileFactory.create(this.coordinateReferenceSystem);

            this.tileScheme = new ZoomTimesTwo(0, 31, 1, 1);    // Use absolute tile numbering

            final BoundingBox datasetBounds = GdalUtility.getBounds(this.dataset);

            this.tileRanges = GdalUtility.calculateTileRanges(this.tileScheme,
                                                              datasetBounds,
                                                              this.profile.getBounds(),
                                                              this.profile,
                                                              RawImageTileReader.Origin);

            final int minimumZoom = GdalUtility.getMinimalZoom(this.dataset, this.tileRanges, Origin, this.tileScheme, tileSize);
            final int maximumZoom = GdalUtility.getMaximalZoom(this.dataset, this.tileRanges, Origin, this.tileScheme, tileSize);

            // The bounds of the dataset is **almost never** the bounds of the
            // data.  The bounds of the dataset fit inside the bounds of the
            // data because the bounds of the data must align to the tile grid.
            // The minimum zoom level is selected such that the entire dataset
            // fits inside a single tile.  that single tile is the minimum data
            // bounds.

            final Coordinate<Integer> minimumTile = this.tileRanges.get(minimumZoom).getMinimum();  // The minimum and maximum for the range returned from tileRanges.get() should be identical (it's a single tile)

            this.dataBounds = this.getTileBoundingBox(minimumTile.getX(),
                                                      minimumTile.getY(),
                                                      this.tileScheme.dimensions(minimumZoom));

            this.zoomLevels = IntStream.rangeClosed(minimumZoom, maximumZoom)
                                       .boxed()
                                       .collect(Collectors.toSet());

            this.tileCount = IntStream.rangeClosed(minimumZoom, maximumZoom)
                                      .map(zoomLevel -> { final Range<Coordinate<Integer>> range = this.tileRanges.get(zoomLevel);

                                                          return (range.getMaximum().getX() - range.getMinimum().getX() + 1) *
                                                                 (range.getMinimum().getY() - range.getMaximum().getY() + 1);
                                                    })
                                      .sum();
        }
        catch(final DataFormatException dfe)
        {
            this.close();
            throw new TileStoreException(dfe);
        }
    }

    @Override
    public void close()
    {
        if(this.dataset != null)
        {
            this.dataset.delete();
        }
    }

    @Override
    public BoundingBox getBounds()
    {
        return this.dataBounds;
    }

    @Override
    public long countTiles()
    {
        return this.tileCount;
    }

    @Override
    public long getByteSize()
    {
        return this.rawImage.length();
    }

    @Override
    public BufferedImage getTile(final int column, final int row, final int zoomLevel) throws TileStoreException
    {
        throw new TileStoreException(new OperationNotSupportedException());
    }

    @Override
    public BufferedImage getTile(final CrsCoordinate coordinate, final int zoomLevel) throws TileStoreException
    {
        throw new TileStoreException(new OperationNotSupportedException());
    }

    @Override
    public Set<Integer> getZoomLevels()
    {
        return this.zoomLevels;
    }

    @Override
    public Stream<TileHandle> stream() throws TileStoreException
    {
        final List<TileHandle> tileHandles = new ArrayList<>();

        final Range<Integer> zoomRange = new Range<>(this.zoomLevels, Integer::compare);

        for(int zoomLevel = zoomRange.getMaximum(); zoomLevel >= zoomRange.getMinimum(); --zoomLevel) //for(final int zoomLevel : this.zoomLevels)
        {
            final Range<Coordinate<Integer>> zoomInfo = this.tileRanges.get(zoomLevel);

            // Get the coordinate information
            final Coordinate<Integer> topLeftCoordinate     = zoomInfo.getMinimum();
            final Coordinate<Integer> bottomRightCoordinate = zoomInfo.getMaximum();

            // Parse each coordinate into min/max tiles for X/Y
            final int zoomMinXTile =     topLeftCoordinate.getX();
            final int zoomMaxXTile = bottomRightCoordinate.getX();
            final int zoomMinYTile = bottomRightCoordinate.getY();
            final int zoomMaxYTile =     topLeftCoordinate.getY();

            for(int tileY = zoomMaxYTile; tileY >= zoomMinYTile; --tileY)
            {
                for(int tileX = zoomMinXTile; tileX <= zoomMaxXTile; ++tileX)
                {
                    tileHandles.add(new RawImageTileHandle(zoomLevel, tileX, tileY));
                }
            }
        }

        // Return the entire tile handle list as a stream
        return tileHandles.stream();
    }

    @Override
    public Stream<TileHandle> stream(final int zoomLevel)
    {
        final Range<Coordinate<Integer>> zoomInfo = this.tileRanges.get(zoomLevel);

        // Get the coordinate information
        final Coordinate<Integer> topLeftCoordinate     = zoomInfo.getMinimum();
        final Coordinate<Integer> bottomRightCoordinate = zoomInfo.getMaximum();

        // Parse each coordinate into min/max tiles for X/Y
        final int zoomMinXTile =     topLeftCoordinate.getX();
        final int zoomMaxXTile = bottomRightCoordinate.getX();
        final int zoomMinYTile = bottomRightCoordinate.getY();
        final int zoomMaxYTile =     topLeftCoordinate.getY();

        // Create a tile handle list that we can append to
        final List<TileHandle> tileHandles = new ArrayList<>();

        for(int tileY = zoomMaxYTile; tileY >= zoomMinYTile; --tileY)
        {
            for(int tileX = zoomMinXTile; tileX <= zoomMaxXTile; ++tileX)
            {
                tileHandles.add(new RawImageTileHandle(zoomLevel, tileX, tileY));
            }
        }
        // Return the entire tile handle list as a stream
        return tileHandles.stream();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem()
    {
        return this.coordinateReferenceSystem;
    }

    @Override
    public String getName()
    {
        return FileUtility.nameWithoutExtension(this.rawImage);
    }

    @Override
    public String getImageType() throws TileStoreException
    {
        try
        {
            final MimeType mimeType = new MimeType(Files.probeContentType(this.rawImage.toPath()));

            if(mimeType.getPrimaryType().toLowerCase().equals("image"))
            {
               return mimeType.getSubType();
            }

            return null;
        }
        catch(final MimeTypeParseException | IOException ex)
        {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public Dimensions<Integer> getImageDimensions() throws TileStoreException
    {
        return this.tileSize;
    }

    @Override
    public TileScheme getTileScheme() throws TileStoreException
    {
        return this.tileScheme;
    }

    @Override
    public TileOrigin getTileOrigin()
    {
        return Origin;
    }

    private class RawImageTileHandle implements TileHandle
    {
        private final TileMatrixDimensions matrix;

        private boolean gotImage = false;
        private BufferedImage image;

        private final int zoomLevel;
        private final int column;
        private final int row;

        RawImageTileHandle(final int zoom, final int column, final int row)
        {
            this.zoomLevel = zoom;
            this.column    = column;
            this.row       = row;
            this.matrix    = RawImageTileReader.this.tileScheme.dimensions(this.zoomLevel);
        }

        @Override
        public int getZoomLevel()
        {
            return this.zoomLevel;
        }

        @Override
        public int getColumn()
        {
            return this.column;
        }

        @Override
        public int getRow()
        {
            return this.row;
        }

        @Override
        public TileMatrixDimensions getMatrix() throws TileStoreException
        {
            return this.matrix;
        }

        @Override
        public CrsCoordinate getCrsCoordinate() throws TileStoreException
        {
            return RawImageTileReader.this.tileToCrsCoordinate(this.column,
                                                                this.row,
                                                                this.matrix,
                                                                RawImageTileReader.Origin);
        }

        @Override
        public CrsCoordinate getCrsCoordinate(final TileOrigin corner) throws TileStoreException
        {
            return RawImageTileReader.this.tileToCrsCoordinate(this.column,
                                                                this.row,
                                                                this.matrix,
                                                                corner);
        }

        @Override
        public BoundingBox getBounds() throws TileStoreException
        {
             return RawImageTileReader.this.getTileBoundingBox(this.column,
                                                                this.row,
                                                                this.matrix);
        }

        @Override
        public BufferedImage getImage() throws TileStoreException
        {
            if(this.gotImage == true)
            {
                return this.image;
            }

            // Build the parameters for GDAL read raster call
            final GdalRasterParameters params = GdalUtility.getGdalRasterParameters(RawImageTileReader.this.dataset.GetGeoTransform(),
                                                                                    this.getBounds(),
                                                                                    RawImageTileReader.this.tileSize,
                                                                                    RawImageTileReader.this.dataset);
            try
            {
                // Read image data directly from the raster
                final byte[] imageData = GdalUtility.readRaster(params, RawImageTileReader.this.dataset);

                // TODO: logic goes here in the case that the querysize == tile size (gdalconstConstants.GRA_NearestNeighbour) (write directly)
                final Dataset querySizeImageCanvas = GdalUtility.writeRaster(params, imageData, RawImageTileReader.this.dataset.GetRasterCount());

                try
                {
                    // Scale each band of tileDataInMemory down to the tile size (down from the query size)
                    final Dataset tileDataInMemory = GdalUtility.scaleQueryToTileSize(querySizeImageCanvas, RawImageTileReader.this.tileSize);

                    this.image = GdalUtility.convert(tileDataInMemory);
                    tileDataInMemory.delete();
                    this.gotImage = true;
                    return this.image;
                }
                catch(final TilingException ex)
                {
                    throw new TileStoreException(ex);
                }
                finally
                {
                    querySizeImageCanvas.delete();
                }

            }
            catch(final TilingException ex)
            {
                throw new TileStoreException(ex);
            }
        }
    }

    private CrsCoordinate tileToCrsCoordinate(final int column, final int row, final TileMatrixDimensions tileMatrixDimensions, final TileOrigin corner)
    {
        if(corner == null)
        {
            throw new IllegalArgumentException("Corner may not be null");
        }

        return this.profile.tileToCrsCoordinate(column + corner.getHorizontal(),
                                                row    + corner.getVertical(),
                                                this.profile.getBounds(),    // RawImageTileReader uses absolute tiling, which covers the whole globe
                                                tileMatrixDimensions,
                                                RawImageTileReader.Origin);
    }

    private BoundingBox getTileBoundingBox(final int column, final int row, final TileMatrixDimensions tileMatrixDimensions)
    {
        return this.profile.getTileBounds(column,
                                          row,
                                          this.profile.getBounds(),
                                          tileMatrixDimensions,
                                          RawImageTileReader.Origin);
    }

    private final File                                     rawImage;
    private final CoordinateReferenceSystem                coordinateReferenceSystem;
    private final Dimensions<Integer>                      tileSize;
    private final Color                                    noDataColor; // TODO implement no-data color handling
    private final Dataset                                  dataset;
    private final BoundingBox                              dataBounds;
    private final Set<Integer>                             zoomLevels;
    private final ZoomTimesTwo                             tileScheme;
    private final CrsProfile                               profile;
    private final int                                      tileCount;
    private final Map<Integer, Range<Coordinate<Integer>>> tileRanges;

    private static final TileOrigin Origin = TileOrigin.LowerLeft;
}
