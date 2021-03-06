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

package com.rgi.geopackage;

import com.rgi.common.BoundingBox;
import com.rgi.common.coordinate.Coordinate;
import com.rgi.common.coordinate.CoordinateReferenceSystem;
import com.rgi.common.coordinate.CrsCoordinate;
import com.rgi.common.coordinate.referencesystem.profile.CrsProfile;
import com.rgi.common.coordinate.referencesystem.profile.CrsProfileFactory;
import com.rgi.common.coordinate.referencesystem.profile.EllipsoidalMercatorCrsProfile;
import com.rgi.common.coordinate.referencesystem.profile.GlobalGeodeticCrsProfile;
import com.rgi.common.coordinate.referencesystem.profile.SphericalMercatorCrsProfile;
import com.rgi.common.tile.scheme.TileMatrixDimensions;
import com.rgi.common.tile.scheme.TileScheme;
import com.rgi.common.tile.scheme.ZoomTimesTwo;
import com.rgi.common.util.ImageUtility;
import com.rgi.geopackage.core.SpatialReferenceSystem;
import com.rgi.geopackage.tiles.Tile;
import com.rgi.geopackage.tiles.TileMatrix;
import com.rgi.geopackage.tiles.TileMatrixSet;
import com.rgi.geopackage.tiles.TileSet;
import com.rgi.geopackage.verification.ConformanceException;
import com.rgi.geopackage.verification.VerificationLevel;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Jenifer Cochran
 * @uahtor Luke Lambert
 */
@SuppressWarnings("JavaDoc")
public class GeoPackageTilesAPITest
{
    @BeforeClass
    public static void setUp() throws ClassNotFoundException
    {
        Class.forName("org.sqlite.JDBC"); // Register the driver
    }

    /**
     * Tests if a GeoPackage will maintain the conversions
     * from tile coordinate to crs coordinate back to tile coordinate
     *
     * this test originated from tiling a GeoPackage that had
     * a tile matrix set that lied on the grid exactly
     */
    @Test
    public void tileCoordinateToCrsBackToTileCoordinate() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final BoundingBox bBox = new BoundingBox(10018754.1713946, -10018754.1713946, 20037508.3427892, 0.0);//data from a GeoPackage where the bounding box lied on the grid
        final CrsProfile spherMerc = new SphericalMercatorCrsProfile();

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final SpatialReferenceSystem srs = gpkg.core()
                                             .addSpatialReferenceSystem(spherMerc.getName(),
                                                                        spherMerc.getCoordinateReferenceSystem().getAuthority(),
                                                                        spherMerc.getCoordinateReferenceSystem().getIdentifier(),
                                                                        "definition", spherMerc.getDescription());


            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("pyramid",
                                                    "title",
                                                    "tiles",
                                                    bBox,
                                                    srs);

            final TileScheme scheme = new ZoomTimesTwo(2, 9, 1, 1);
            //populate the TileMatrices
            for(final int zoom: scheme.getZoomLevels())
            {
                final TileMatrixDimensions dimensions = scheme.dimensions(zoom);
                final int matrixWidth = dimensions.getWidth();
                final int matrixHeight = dimensions.getHeight();
                final int tileWidth = 256;
                final int tileHeight = 256;

                final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

                gpkg.tiles().addTileMatrix(tileMatrixSet,
                                           zoom,
                                           matrixWidth,
                                           matrixHeight,
                                           tileWidth,
                                           tileHeight);
            }
            //Test that the conversion from tileCoordinate-> CrsCoordinate -> tileCoordinate is as expected(original tileCoordinate)
            for(final int zoomLevel: scheme.getZoomLevels())
            {
                for(int row = 0; row < scheme.dimensions(zoomLevel).getHeight(); row++)
                {
                    for(int column = 0;  column < scheme.dimensions(zoomLevel).getWidth(); column++)
                    {
                        final CrsCoordinate crsCoordinate = gpkg.tiles().tileToCrsCoordinate(tileSet, column, row, zoomLevel);
                        final Coordinate<Integer> tileCoordinate = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoordinate, spherMerc.getPrecision(), zoomLevel);
                        final Coordinate<Integer> expectedTileCoordinate = new Coordinate<>(column, row);
                        assertEquals("Tile coordinates do not match",
                                     expectedTileCoordinate,
                                     tileCoordinate);
                    }
                }
            }
        }
    }

    /**
     * This tests if a GeoPackage can add a tile set successfully without throwing errors.
     */
    @Test
    public void addTileSet() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
           final TileSet tileSet = gpkg.tiles()
                                       .addTileSet("pyramid",
                                                   "title",
                                                   "tiles",
                                                   new BoundingBox(0.0, 0.0, 50.0, 60.0),
                                                   gpkg.core().getSpatialReferenceSystem("EPSG", 4326));


           final int matrixHeight = 2;
           final int matrixWidth = 4;
           final int tileHeight = 512;
           final int tileWidth = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       0,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);
        }

        final String query = "SELECT table_name FROM gpkg_tile_matrix_set WHERE table_name = 'pyramid';";

        try(final Connection con       = TestUtility.getConnection(testFile);
            final Statement  stmt      = con.createStatement();
            final ResultSet  tileName  = stmt.executeQuery(query))
        {
            assertTrue("The GeoPackage did not set the table_name into the gpkg_tile_matrix_set when adding a new set of tiles.", tileName.next());
            final String tableName = tileName.getString("table_name");
            assertEquals("The GeoPackage did not insert the correct table name into the gpkg_tile_matrix_set when adding a new set of tiles.", "pyramid", tableName);
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when given a null value for tileSetEntry
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithNullTileSetEntry() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles()
                .addTile(null,
                         gpkg.tiles()
                             .getTileMatrix(tileMatrixSet, 0),
                         0,
                         0,
                         GeoPackageTilesAPITest.createImageBytes());

            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a null value for tileSetEntry.");
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when given
     * a tile set entry with a null value for the bounding box

     * */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithNullBoundingBox() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .addTileSet("tableName",
                            "ident",
                            "desc",
                            null,
                            gpkg.core().getSpatialReferenceSystem(4236));


            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a null value for BoundingBox.");
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException
     * If it gives tries to create a TileSet with a null SRS value
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithNullSRS() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .addTileSet("name",
                            "ident",
                            "desc",
                            new BoundingBox(0.0,0.0,0.0,0.0),
                            null);

            fail("GeoPackage should have thrown an IllegalArgumentException when TileEntrySet is null.");
        }
    }

    /**
     * Test if the GeoPackage will add a Tile set with a new Spatial Reference System (one created by user).
     */
    @Test
    public void addTileSetWithNewSpatialReferenceSystem() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

             gpkg.core().addSpatialReferenceSystem("scaled world mercator",
                                                   "org",
                                                   9804,
                                                   "definition",
                                                   "description");
        }

        final String query = "SELECT srs_name FROM gpkg_spatial_ref_sys "+
                             "WHERE srs_name     = 'scaled world mercator' AND "+
                                   "organization = 'org'                   AND "+
                                   "definition   = 'definition'            AND "+
                                   "description  = 'description';";

        try(final Connection con     = TestUtility.getConnection(testFile);
            final Statement  stmt    = con.createStatement();
            final ResultSet  srsInfo = stmt.executeQuery(query))
        {
            assertTrue("The Spatial Reference System added to the GeoPackage by the user did not contain the same information given.", srsInfo.next());
        }
     }


    /**
     * Tests if given a GeoPackage with tiles already inside it can add another Tile Set without throwing an error and verify that it entered the correct information.
     */
    @Test
    public void addTileSetToExistingGpkgWithTilesInside() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        final int matrixHeight = 2;
        final int matrixWidth  = 2;
        final int tileHeight   = 256;
        final int tileWidth    = 256;

        // Create a GeoPackage with tiles inside
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetONE",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 60.0, 60.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            gpkg.tiles().addTileMatrix(gpkg.tiles().getTileMatrixSet(tileSet),
                                       0,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);
        }

        //open a file with tiles inside and add more tiles
        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Open))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("newTileSetTWO",
                                                    "title2",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 70.0, 50.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            gpkg.tiles().addTileMatrix(gpkg.tiles().getTileMatrixSet(tileSet),
                                       0,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);
        }

        //make sure the information was added to contents table and tile matrix set table
        final String query = "SELECT cnts.table_name FROM gpkg_contents        AS cnts WHERE cnts.table_name"+
                             " IN(SELECT tms.table_name  FROM gpkg_tile_matrix_set AS tms  WHERE cnts.table_name = tms.table_name);";

        try(final Connection con            = TestUtility.getConnection(testFile);
            final Statement  stmt           = con.createStatement();
            final ResultSet  tileTableNames = stmt.executeQuery(query))
        {
            if(!tileTableNames.next())
            {
                fail("The two tiles tables where not successfully added to both the gpkg_contents table and the gpkg_tile_matrix_set.");
            }
            while(tileTableNames.next())
            {
                final String tilesTableName = tileTableNames.getString("table_name");
                assertTrue("The tiles table names did not match what was being added to the GeoPackage",
                            tilesTableName.equals("newTileSetTWO") || tilesTableName.equals("tileSetONE"));
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an error when adding a tileset with the same name as another tileset in the GeoPackage.
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithRepeatedTileSetName() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .addTileSet("repeated_name",
                            "title",
                            "tiles",
                            new BoundingBox(0.0, 0.0, 0.0, 0.0),
                            gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            gpkg.tiles()
                .addTileSet("repeated_name",
                            "title2",
                            "tiles",
                            new BoundingBox(0.0, 0.0, 0.0, 0.0),
                            gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            fail("The GeoPackage should throw an IllegalArgumentException when a user gives a Tile Set Name that already exists.");
        }
    }

    /**
     * Tests if a GeoPackage can add 2 Tile Matrix entries with
     * the two different tile pyramids can be entered into one gpkg
     */
    @Test
    public void addTileSetToExistingTilesTable() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "tiles",
                                                    "desc",
                                                    new BoundingBox(0.0, 0.0, 70.0, 70.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));


            final Collection<TileSet> tileSetContnentEntries = new ArrayList<>();

            tileSetContnentEntries.add(tileSet);

            final int matrixHeight = 2;
            final int matrixWidth = 2;
            final int tileHeight = 256;
            final int tileWidth = 256;

            gpkg.tiles().addTileMatrix(gpkg.tiles().getTileMatrixSet(tileSet),
                                       0,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);

            final int matrixHeight2 = 4;
            final int matrixWidth2 = 4;
            final int tileHeight2 = 256;
            final int tileWidth2 = 256;

            gpkg.tiles().addTileMatrix(gpkg.tiles().getTileMatrixSet(tileSet),
                                       1,
                                       matrixWidth2,
                                       matrixHeight2,
                                       tileWidth2,
                                       tileHeight2);

            for(final TileSet gpkgEntry : gpkg.tiles().getTileSets())
            {
                assertTrue("The tile entry's information in the GeoPackage does not match what was originally given to a GeoPackage",
                           tileSetContnentEntries.stream()
                                                 .anyMatch(tileEntry -> tileEntry.getMinimumX()   .equals(gpkgEntry.getMinimumX())    &&
                                                                        tileEntry.getMinimumY()   .equals(gpkgEntry.getMinimumY())    &&
                                                                        tileEntry.getMaximumX()   .equals(gpkgEntry.getMaximumX())    &&
                                                                        tileEntry.getMaximumY()   .equals(gpkgEntry.getMaximumY())    &&
                                                                        tileEntry.getDataType()   .equals(gpkgEntry.getDataType())    &&
                                                                        tileEntry.getDescription().equals(gpkgEntry.getDescription()) &&
                                                                        tileEntry.getIdentifier() .equals(gpkgEntry.getIdentifier())  &&
                                                                        tileEntry.getTableName()  .equals(gpkgEntry.getTableName())   &&
                                                                        tileEntry.getSpatialReferenceSystemIdentifier().equals(gpkgEntry.getSpatialReferenceSystemIdentifier())));
            }
        }
    }

    /**
     * This ensures that when a user tries to add the same tileSet two times
     * that the TileSet object that is returned is the one that already exists
     * in the GeoPackage and verifies its contents
     */
    @Test
    public void addSameTileSetTwice() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final String      tableName   = "tableName";
            final String      identifier  = "identifier";
            final String      description = "description";
            final BoundingBox boundingBox = new BoundingBox(2.0,1.0,4.0,3.0);

            final SpatialReferenceSystem srs = gpkg.core().getSpatialReferenceSystem(0);

            final TileSet tileSet = gpkg.tiles().addTileSet(tableName,
                                                            identifier,
                                                            description,
                                                            boundingBox,
                                                            srs);

            final TileSet sameTileSet = gpkg.tiles().addTileSet(tableName,
                                                                identifier,
                                                                description,
                                                                boundingBox,
                                                                srs);

            assertTrue("The GeoPackage did not return the same tile set when trying to add the same tile set twice.",
                       sameTileSet.equals(tileSet.getTableName(),
                                          tileSet.getDataType(),
                                          tileSet.getIdentifier(),
                                          tileSet.getDescription(),
                                          tileSet.getMinimumX(),
                                          tileSet.getMinimumY(),
                                          tileSet.getMaximumX(),
                                          tileSet.getMaximumY(),
                                          tileSet.getSpatialReferenceSystemIdentifier()));
        }
    }

    /**
     * Expects GeoPackage to throw an IllegalArgumentException when giving
     * addTileSet a parameter with a null value for bounding box
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetBadTableName() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .addTileSet("TableName",
                            "identifier",
                            "definition",
                            null,
                            gpkg.core().getSpatialReferenceSystem(-1));

            fail("Expected an IllegalArgumentException when giving a null value for bounding box for addTileSet");
        }
    }

    /**
     * Expects GeoPackage to throw an IllegalArgumentException when giving
     * addTileSet a parameter with a null value for bounding box
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetBadSRS() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .addTileSet("TableName",
                            "identifier",
                            "definition",
                            new BoundingBox(0.0,0.0,0.0,0.0),
                            null);
            fail("Expected an IllegalArgumentException when giving a null value for bounding box for addTileSet");
        }
    }

    /**
     * Expects GeoPackage to throw an IllegalArgumentException when giving
     * addTileSet a parameter with a null value for bounding box
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetBadBoundingBox() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .addTileSet("TableName",
                            "identifier",
                            "definition",
                            new BoundingBox(0.0,0.0,0.0,0.0),
                            null);
            fail("Expected an IllegalArgumentException when giving a null value for bounding box for addTileSet");
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when The table name is an empty string
     * for TileSet.
     */
     @Test(expected = IllegalArgumentException.class)
     public void addTileSetContentEntryInvalidTableName() throws ClassNotFoundException, SQLException, ConformanceException, IOException
     {
         final File testFile = TestUtility.getRandomFile();
         try(GeoPackage gpkg = new GeoPackage(testFile))
         {
             gpkg.tiles()
                 .addTileSet("",
                             "ident",
                             "desc",
                             new BoundingBox(0.0,0.0,0.0,0.0),
                             gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

             fail("Expected the GeoPackage to throw an IllegalArgumentException when given a TileSet with an empty string for the table name.");
         }
     }

    /**
     * Tests if GeoPackageTiles throws an IllegalArgumentException when giving a
     * table name with symbols
     */
     @Test(expected = IllegalArgumentException.class)
     public void addTileIllegalArgumentException() throws SQLException, ClassNotFoundException, ConformanceException, IOException
     {
         final File testFile = TestUtility.getRandomFile();

         try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
         {
             gpkg.tiles().addTileSet("badTableName^", "identifier", "description", new BoundingBox(0.0,0.0,2.0,2.0), gpkg.core().getSpatialReferenceSystem(0));
             fail("Expected to get an IllegalArgumentException for giving an illegal tablename (with symbols not allowed by GeoPackage)");
         }
     }

    /**
     * Tests if GeoPackageTiles throws an IllegalArgumentException when giving a
     * table name starting with gpkg
     */
     @Test(expected = IllegalArgumentException.class)
     public void addTileIllegalArgumentException2() throws SQLException, ClassNotFoundException, ConformanceException, IOException
     {
         final File testFile = TestUtility.getRandomFile();

         try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
         {
             gpkg.tiles().addTileSet("gpkg_bad_tablename", "identifier", "description", new BoundingBox(0.0,0.0,2.0,2.0), gpkg.core().getSpatialReferenceSystem(0));
             fail("Expected to get an IllegalArgumentException for giving an illegal tablename (starting with gpkg_ which is not allowed by GeoPackage)");
         }
     }

    /**
     * Tests if GeoPackageTiles throws an IllegalArgumentException when giving a
     * table name with a null value
     */
     @Test(expected = IllegalArgumentException.class)
     public void addTileIllegalArgumentException3() throws SQLException, ClassNotFoundException, ConformanceException, IOException
     {
         final File testFile = TestUtility.getRandomFile();

         try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
         {
             gpkg.tiles().addTileSet(null, "identifier", "description", new BoundingBox(0.0,0.0,2.0,2.0), gpkg.core().getSpatialReferenceSystem(0));
             fail("Expected to get an IllegalArgumentException for giving an illegal tablename (a null value)");
         }
     }


    /**
     * Tests if a GeoPackage will return the same tileSets that was given to the GeoPackage when adding tileSets.
     */
    @Test
    public void getTileSetsFromGpkg() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "tiles",
                                                    "desc",
                                                    new BoundingBox(0.0, 0.0, 90.0, 50.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final TileSet tileSet2 = gpkg.tiles()
                                         .addTileSet("SecondTileSet",
                                                     "ident",
                                                     "descrip",
                                                     new BoundingBox(1.0,1.0,122.0,111.0),
                                                     gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final Collection<TileSet> tileSetContnentEntries = new ArrayList<>();

            tileSetContnentEntries.add(tileSet);
            tileSetContnentEntries.add(tileSet2);

            final int matrixHeight = 2;
            final int matrixWidth = 2;
            final int tileHeight = 256;
            final int tileWidth = 256;


            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       0,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);


            final int matrixHeight2 = 4;
            final int matrixWidth2 = 4;

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       1,
                                       matrixWidth2,
                                       matrixHeight2,
                                       tileWidth,
                                       tileHeight);

            final Collection<TileSet> tileSetsFromGpkg = gpkg.tiles().getTileSets();

            assertEquals("The number of tileSets added to a GeoPackage do not match with how many is retrieved from a GeoPacakage.", tileSetContnentEntries.size(), tileSetsFromGpkg.size());

            for(final TileSet gpkgEntry : tileSetsFromGpkg)
            {
                assertTrue("The tile entry's information in the GeoPackage does not match what was originally given to a GeoPackage",
                           tileSetContnentEntries.stream()
                                                 .anyMatch(tileEntry -> tileEntry.getMinimumX()   .equals(gpkgEntry.getMinimumX())    &&
                                                                        tileEntry.getMaximumX()   .equals(gpkgEntry.getMaximumX())    &&
                                                                        tileEntry.getMinimumY()   .equals(gpkgEntry.getMinimumY())    &&
                                                                        tileEntry.getMaximumY()   .equals(gpkgEntry.getMaximumY())    &&
                                                                        tileEntry.getDataType()   .equals(gpkgEntry.getDataType())    &&
                                                                        tileEntry.getDescription().equals(gpkgEntry.getDescription()) &&
                                                                        tileEntry.getIdentifier() .equals(gpkgEntry.getIdentifier())  &&
                                                                        tileEntry.getTableName()  .equals(gpkgEntry.getTableName())   &&
                                                                        tileEntry.getSpatialReferenceSystemIdentifier().equals(gpkgEntry.getSpatialReferenceSystemIdentifier())));
            }
        }
    }

    /**
     * Tests if a GeoPackage will find no tile Sets when searching with an SRS that is not in the GeoPackage.
     */
    @Test
    public void getTileSetWithNewSRS() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
          final Collection<TileSet> gpkgTileSets = gpkg.tiles().getTileSets(gpkg.core().addSpatialReferenceSystem("name", "org", 123, "def", "desc"));
            assertEquals("Should not have found any tile sets because there weren't any in "
                                        + "GeoPackage that matched the SpatialReferenceSystem given.", 0, gpkgTileSets.size());

        }
    }

    /**
     * Tests if the getTileSet returns null when the tile table does not exist
     */
    @Test
    public void getTileSetVerifyReturnNull()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().getTileSet("table_not_here");

            assertNull("GeoPackage expected to return null when the tile set does not exist in GeoPackage", tileSet);
        }
    }

    /**
     * Tests if the getTileSet returns the expected values.
     */
    @Test
    public void getTileSetVerifyReturnCorrectTileSet()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet         = gpkg.tiles().addTileSet("ttable","identifier", "Desc", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final TileSet returnedTileSet = gpkg.tiles().getTileSet("ttable");

            assertTrue("GeoPackage did not return the same values given to tile set",
                              tileSet.getMinimumX()   .equals(returnedTileSet.getMinimumX())    &&
                              tileSet.getMaximumX()   .equals(returnedTileSet.getMaximumX())    &&
                              tileSet.getMinimumY()   .equals(returnedTileSet.getMinimumY())    &&
                              tileSet.getMaximumY()   .equals(returnedTileSet.getMaximumY())    &&
                              tileSet.getDescription().equals(returnedTileSet.getDescription()) &&
                              tileSet.getDataType()   .equals(returnedTileSet.getDataType())    &&
                              tileSet.getIdentifier() .equals(returnedTileSet.getIdentifier())  &&
                              tileSet.getLastChange() .equals(returnedTileSet.getLastChange())  &&
                              tileSet.getTableName()  .equals(returnedTileSet.getTableName())   &&
                              tileSet.getSpatialReferenceSystemIdentifier().equals(returnedTileSet.getSpatialReferenceSystemIdentifier()));
        }
    }


    /**
     * Tests if the GeoPackage can detect there are zoom levels for
     * a tile that is not represented in the Tile Matrix Table.
     * Should throw a IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                     new BoundingBox(0.0,0.0,0.0,0.0),
                                                     gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet, 18, 20, 20, 2,2);

            gpkg.tiles().addTile(tileSet, tileMatrix, 0, 0, new byte[] {(byte)1, (byte)2, (byte)3, (byte)4});

            fail("GeoPackage should throw a IllegalArgumentExceptionException when Tile Matrix Table "
               + "does not contain a record for the zoom level of a tile in the Pyramid User Data Table.");

        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_row
     * is larger than the matrix_height -1. Which is a violation
     * of the GeoPackage Specifications. Requirement 55.
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException2() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 2, 2, 2, 2);

            gpkg.tiles().addTile(tileSet, tileMatrix, 0, 10, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});

            fail("GeoPackage should throw a IllegalArgumentException when tile_row is larger than matrix_height - 1 when zoom levels are equal.");
        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_row
     * is less than 0. Which is a violation
     * of the GeoPackage Specifications. Requirement 55.
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException3() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File      testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 2, 2, 2, 2);

            gpkg.tiles().addTile(tileSet, tileMatrix, 0, -1, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});

            fail("GeoPackage should throw a IllegalArgumentException when tile_row is less than 0.");
        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_column
     * is larger than matrix_width -1. Which is a violation
     * of the GeoPackage Specifications. Requirement 54.
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException4() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 2, 2, 2, 2);

            gpkg.tiles().addTile(tileSet, tileMatrix, 10, 0, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});

            fail("GeoPackage should throw a IllegalArgumentException when tile_column "
               + "is larger than matrix_width -1.");

        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_column
     * is less than 0. Which is a violation
     * of the GeoPackage Specifications. Requirement 54.
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException5() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 2, 2, 2, 2);

            gpkg.tiles().addTile(tileSet, tileMatrix, -1, 0, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});

            fail("GeoPackage should throw a IllegalArgumentException when tile_column "
               + "is less than 0.");
        }
    }

    /**
     * GeoPackage throws an SQLException when opening a GeoPackage since it does not contain the default tables
     * inside after bypassing the verifier.
     *
     */
    @Test(expected = SQLException.class)
    public void addTilesToGpkgAndAddTilesAndSetVerifyToFalse() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        testFile.createNewFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, VerificationLevel.None, GeoPackage.OpenMode.Open))
        {
            gpkg.tiles()
                .addTileSet("diff_tile_set",
                            "tile",
                            "desc",
                            new BoundingBox(1.0, 1.0, 1.0, 1.0),
                            gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            fail("The GeoPackage was expected to throw an IOException due to the file being empty.");

        }
        catch(final IOException ignored)
        {
            final String query = "SELECT table_name FROM gpkg_contents WHERE table_name = 'diff_tile_set';";

            try(final Connection con           = TestUtility.getConnection(testFile);
                final Statement  stmt          = con.createStatement();
                final ResultSet  tileTableName = stmt.executeQuery(query))
            {
                assertNull("The data should not be in the contents table since it throws an SQLException", tileTableName.getString("table_name"));
            }
        }
    }

    /**
     * This adds a tile to a GeoPackage and verifies that the Tile object added
     * into the GeoPackage is the same Tile object returned.
     */
    @Test
    public void addTileMethodByCrsTileCoordinate() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(-180.0, -80.0, 180.0, 80.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int zoomLevel = 2;
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int tileWidth = 256;
            final int tileHeight = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet,
                                                                     zoomLevel,
                                                                     matrixWidth,
                                                                     matrixHeight,
                                                                     tileWidth,
                                                                     tileHeight);

            final CoordinateReferenceSystem coordinateReferenceSystem = new CoordinateReferenceSystem("EPSG", 4326);

            final CrsProfile crsProfile = CrsProfileFactory.create(coordinateReferenceSystem);

            final CrsCoordinate crsCoordinate = new CrsCoordinate(0.0, -60.0, coordinateReferenceSystem);

            final Tile tileAdded = gpkg.tiles().addTile(tileSet, tileMatrix, crsCoordinate, crsProfile.getPrecision(), GeoPackageTilesAPITest.createImageBytes());

            final Tile tileFound = gpkg.tiles().getTile(tileSet, crsCoordinate, crsProfile.getPrecision(), zoomLevel);

            assertTrue("The GeoPackage did not return the tile Expected.",
                       tileAdded.getColumn()     == tileFound.getColumn()     &&
                       tileAdded.getIdentifier() == tileFound.getIdentifier() &&
                       tileAdded.getRow()        == tileFound.getRow()        &&
                       tileAdded.getZoomLevel()  == tileFound.getZoomLevel()  &&
                       Arrays.equals(tileAdded.getImageData(), tileFound.getImageData()));

        }
    }

    /**
     * Test if the GeoPackage can successfully add non empty tiles to a GeoPackage  without throwing an error.
     *
     */
    @Test
    public void addNonEmptyTile() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 20.0, 50.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final int matrixHeight = 2;
            final int matrixWidth = 2;
            final int tileHeight = 256;
            final int tileWidth = 256;


            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet,
                                                                     2,
                                                                     matrixWidth,
                                                                     matrixHeight,
                                                                     tileWidth,
                                                                     tileHeight);

            gpkg.tiles().addTile(tileSet, tileMatrix, 0, 0, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});
        }

        //use a query to test if the tile was inserted into database and to correct if the image is the same
        final String query = "SELECT tile_data FROM tileSetName WHERE zoom_level = 2 AND tile_column = 0 AND tile_row =0;";

        try(final Connection con      = TestUtility.getConnection(testFile);
            final Statement  stmt     = con.createStatement();
            final ResultSet  tileData = stmt.executeQuery(query))
        {
            // assert the image was input into the file
            assertTrue("The GeoPackage did not successfully write the tile_data into the GeoPackage", tileData.next());
            final byte[] bytes = tileData.getBytes("tile_data");

            // compare images
            assertTrue("The GeoPackage tile_data does not match the tile_data of the one given", Arrays.equals(bytes, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4}));
        }
    }

    /**
     * Tests if the GeoPackage will throw an SQLException when adding a
     * duplicate tile to the GeoPackage.
     */
    @Test(expected = SQLException.class)
    public void addDuplicateTiles() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "description",
                                                    new BoundingBox(1.1, 1.1, 100.1, 100.1),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final int matrixHeight = 2;
            final int matrixWidth  = 2;
            final int tileHeight   = 256;
            final int tileWidth    = 256;

            final TileMatrix tileMatrix = gpkg.tiles()
                                              .addTileMatrix(gpkg.tiles().getTileMatrixSet(tileSet),
                                                             1,
                                                             matrixWidth,
                                                             matrixHeight,
                                                             tileWidth,
                                                             tileHeight                      );

            final int column = 1;
            final int row    = 0;
            final byte[] imageData = {(byte) 1, (byte) 2, (byte) 3, (byte) 4};
            //add tile twice
            gpkg.tiles().addTile(tileSet, tileMatrix, column, row, imageData);
            gpkg.tiles().addTile(tileSet, tileMatrix, column, row, imageData);//see if it will add the same tile twice

            fail("Expected GeoPackage to throw an SQLException due to a unique constraint violation (zoom level, tile column, and tile row)."
               + " Was able to add a duplicate tile.");
        }
    }

    /**
     * Tests if the GeoPackage throws an IllegalArgumentException when trying to
     * add a tile with a parameter that is null (image data)
     */
    @Test(expected = IllegalArgumentException.class)
    public void addBadTile()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            // Add tile to gpkg
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet, 4, 10, 10, 1, 1);

            gpkg.tiles().addTile(tileSet, tileMatrix, 4, 0, null);

            fail("Expected the GeoPackage to throw an IllegalArgumentException when adding a null parameter to a Tile object (image data)");
        }
    }

    /**
     * Tests if the GeoPackage throws an IllegalArgumentException when trying to
     * add a tile with a parameter that is empty (image data)
     */
    @Test(expected = IllegalArgumentException.class)
    public void addBadTile2()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            // Add tile to gpkg
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet, 4, 10, 10, 1, 1);
            gpkg.tiles().addTile(tileSet,tileMatrix, 4, 0, new byte[0]);

            fail("Expected the GeoPackage to throw an IllegalArgumentException when adding an empty parameter to Tile (image data)");
        }
    }

    /**
     * Tests if the GeoPackage throws an IllegalArgumentException when trying to
     * add a tile with a parameter that is null (tileMatrix)
     */
    @Test(expected = IllegalArgumentException.class)
    public void addBadTile4()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            //add tile to gpkg
            gpkg.tiles().addTile(tileSet, null, 4, 0, new byte[]{(byte) 1, (byte) 2, (byte) 3, (byte) 4});

            fail("Expected the GeoPackage to throw an IllegalArgumentException when adding a null parameter to a addTile method (tileMatrix)");
        }
    }

    /**
     * Tests if the GeoPackage get tile will retrieve the correct tile with get tile method.
     */
    @Test
    public void getTile() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        //create tiles and file
        final File testFile = TestUtility.getRandomFile();
        final byte[] originalTile1 = {(byte) 1, (byte) 2, (byte) 3, (byte) 4};
        final byte[] originalTile2 = {(byte) 1, (byte) 2, (byte) 3, (byte) 4};

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 90.0, 80.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final int zoom1 = 4;

            //add tile to gpkg
            final int matrixHeight = 2;
            final int matrixWidth = 4;
            final int tileHeight = 512;
            final int tileWidth = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix1 = gpkg.tiles()
                                               .addTileMatrix(tileMatrixSet,
                                                              zoom1,
                                                              matrixWidth,
                                                              matrixHeight,
                                                              tileWidth,
                                                              tileHeight);

            final int matrixHeight2 = 4;
            final int matrixWidth2 = 8;
            final int tileHeight2 = 512;
            final int tileWidth2 = 256;

            final int zoom2 = 8;
            final TileMatrix tileMatrix2 = gpkg.tiles()
                                               .addTileMatrix(tileMatrixSet,
                                                              zoom2,
                                                              matrixWidth2,
                                                              matrixHeight2,
                                                              tileWidth2,
                                                              tileHeight2
            );

            final Coordinate<Integer> tile1 = new Coordinate<>(3, 0);
            final Coordinate<Integer> tile2 = new Coordinate<>(7, 0);

            gpkg.tiles().addTile(tileSet, tileMatrix1, tile1.getX(), tile1.getY(), originalTile1);
            gpkg.tiles().addTile(tileSet, tileMatrix2, tile2.getX(), tile2.getY(), originalTile2);

            //Retrieve tile from gpkg
            final Tile gpkgTile1 = gpkg.tiles().getTile(tileSet, tile1.getX(), tile1.getY(), zoom1);
            final Tile gpkgTile2 = gpkg.tiles().getTile(tileSet, tile2.getX(), tile2.getY(), zoom2);

            assertTrue("GeoPackage did not return the image expected when using getTile method.",
                       Arrays.equals(gpkgTile1.getImageData(), originalTile1));
            assertTrue("GeoPackage did not return the image expected when using getTile method.",
                       Arrays.equals(gpkgTile2.getImageData(), originalTile2));
        }
    }

    /**
     * Tests if the GeoPackage get tile will retrieve the correct tile with get tile method.
     */
    @Test
    public void getTile2() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            //Retrieve tile from gpkg
            final Tile gpkgTile1 = gpkg.tiles().getTile(tileSet, 4, 0, 4);

            assertNull("GeoPackage did not null when the tile doesn't exist in the getTile method.", gpkgTile1);
        }
    }

    /**
     * Tests if the GeoPackage get tile will retrieve the correct tile with get tile method.
     */
    @Test
    public void getTile3() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 80.0, 50.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final int matrixHeight = 2;
            final int matrixWidth = 3;
            final int tileHeight = 512;
            final int tileWidth = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet,
                                                                     0,
                                                                     matrixWidth,
                                                                     matrixHeight,
                                                                     tileWidth,
                                                                     tileHeight);

            // Tile coordinates

            final Coordinate<Integer> coord1 = new Coordinate<>(2, 1);
            final byte[] imageData = {(byte) 1, (byte) 2, (byte) 3, (byte) 4};

            // Retrieve tile from gpkg

            final Tile gpkgTileAdded    = gpkg.tiles().addTile(tileSet, tileMatrix, coord1.getX(), coord1.getY(), imageData);
            final int zoom = 0;
            final Tile gpkgTileRecieved = gpkg.tiles().getTile(tileSet, coord1.getX(), coord1.getY(), zoom);

            assertTrue("GeoPackage did not return the same tile added to the gpkg.",
                       gpkgTileAdded.getColumn()               ==  gpkgTileRecieved.getColumn()      &&
                       gpkgTileAdded.getIdentifier()           == (gpkgTileRecieved.getIdentifier()) &&
                       gpkgTileAdded.getRow()                  ==  gpkgTileRecieved.getRow()         &&
                       gpkgTileAdded.getZoomLevel()            ==  gpkgTileRecieved.getZoomLevel()   &&
                       Arrays.equals(gpkgTileAdded.getImageData(), gpkgTileRecieved.getImageData()));
        }
    }

    /**
     * Tests if a GeoPackage will return null when the tile being searched for does not exist.
     *
     */
    @Test
    public void getTileThatIsNotInGpkg() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    null,
                                                    null,
                                                    new BoundingBox(0.0, 0.0, 80.0, 80.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final int matrixWidth = 3;
            final int matrixHeight = 6;
            final int tileWidth = 256;
            final int tileHeight = 256;
            // add tile to gpkg
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       2,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);

            assertNull("GeoPackage should have returned null for a missing tile.", gpkg.tiles().getTile(tileSet, 0, 0, 0));
        }
    }

    /**
     * Tests if GeoPackage will throw an IllegalArgumentException when using getTile method with null value for table name.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileWithNullTileEntrySet() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTile(null, 2, 2, 0);
            fail("GeoPackage did not throw an IllegalArgumentException when giving a null value to table name (using getTile method)");
        }
    }

    /**
     * This adds a tile to a GeoPackage and verifies that the Tile object added
     * into the GeoPackage is the same Tile object returned.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileRelativeTileCoordinateNonExistent() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(-180.0, -80.0, 180.0, 80.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int zoomLevel = 2;

            final CoordinateReferenceSystem coordinateReferenceSystem = new CoordinateReferenceSystem("EPSG", 4326);

            final CrsCoordinate crsCoordinate = new CrsCoordinate(0.0, -60.0, coordinateReferenceSystem);

            gpkg.tiles().getTile(tileSet, crsCoordinate, CrsProfileFactory.create(coordinateReferenceSystem).getPrecision(), zoomLevel);
        }
    }

    /**
     * Tests if the GeoPackage will return the all and the correct zoom levels in a GeoPackage
     *
     */
    @Test
    public void getZoomLevels() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                          .addTileSet("tableName",
                                                      "ident",
                                                      "desc",
                                                      new BoundingBox(5.0,5.0,50.0,50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
           // Add tile matrices that represent zoom levels 0 and 12
            final int matrixHeight = 2;
            final int matrixWidth = 2;
            final int tileHeight = 256;
            final int tileWidth = 256;

           final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       0,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);

           gpkg.tiles().addTileMatrix(tileMatrixSet,
                                      12,
                                      matrixWidth,
                                      matrixHeight,
                                      tileWidth,
                                      tileHeight);

           final Set<Integer> zooms  = gpkg.tiles().getTileZoomLevels(tileSet);

           final Collection<Integer> expectedZooms = new ArrayList<>();

           expectedZooms.add(12);
           expectedZooms.add(0);

           for(final Integer zoom : zooms)
           {
               assertTrue("The GeoPackage's get zoom levels method did not return expected values.",
                          expectedZooms.stream()
                                       .anyMatch(currentZoom -> currentZoom.equals(zoom)));
           }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when given a
     * TileSet null for getZoomLevels()
     */
    @Test(expected = IllegalArgumentException.class)
    public void getZoomLevelsNullTileSetContentEntry()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileZoomLevels(null);
            fail("Expected the GeoPackage to throw an IllegalArgumentException when givinga null parameter to getTileZoomLevels");
        }
    }

     /**
      * Tests if GeoPackage will throw an IllegalArgumentException
      * when giving a null parameter to getRowCount
       */
    @Test(expected = IllegalArgumentException.class)
    public void getRowCountNullContentEntry() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.core().getRowCount(null);
            fail("GeoPackage should have thrown an IllegalArgumentException.");
        }
    }

    /**
     * Verifies that the GeoPackage counts the correct number of rows
     * with the method getRowCount
     */
    @Test
    public void getRowCountVerify() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,80.0,50.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            //create two TileMatrices to represent the tiles
            final int matrixHeight = 2;
            final int matrixWidth = 2;
            final int tileHeight = 256;
            final int tileWidth = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix2 = gpkg.tiles()
                                               .addTileMatrix(tileMatrixSet,
                                                              1,
                                                              matrixWidth,
                                                              matrixHeight,
                                                              tileWidth,
                                                              tileHeight);

            final int matrixHeight2 = 4;
            final int matrixWidth2 = 4;

            final TileMatrix tileMatrix1 = gpkg.tiles()
                                               .addTileMatrix(tileMatrixSet,
                                                              0,
                                                              matrixWidth2,
                                                              matrixHeight2,
                                                              tileWidth,
                                                              tileHeight);
            // Add two tiles
            gpkg.tiles().addTile(tileSet, tileMatrix2, 0, 0, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});
            gpkg.tiles().addTile(tileSet, tileMatrix1, 0, 0, new byte[] {(byte) 1, (byte) 2, (byte) 3, (byte) 4});

            final long count = gpkg.core().getRowCount(tileSet);

            assertEquals(String.format("Expected a different value from GeoPackage on getRowCount. expected: 2 actual: %d", count), 2, count);
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a null parameter to the method getTileMatrixSetEntry();
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileMatrixSetEntryNullTileSetContentEntry()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileMatrixSet(null);

            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a null parameter for TileSet");
        }
    }

    /**
     * Tests if GeoPackage returns the expected tileMatrices using the
     * getTileMatrices(TileSet tileSet) method
     */
    @Test
    public void getTileMatricesVerify() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tables", "identifier", "description", new BoundingBox(0.0,0.0,80.0,80.0), gpkg.core().getSpatialReferenceSystem(-1));

            final int matrixHeight = 2;
            final int matrixWidth = 4;
            final int tileHeight = 512;
            final int tileWidth = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileMatrixSet,
                                                                     0,
                                                                     matrixWidth,
                                                                     matrixHeight,
                                                                     tileWidth,
                                                                     tileHeight);

            final int matrixHeight2 = 4;
            final int matrixWidth2 = 8;
            final int tileHeight2 = 512;
            final int tileWidth2 = 256;

            final TileMatrix tileMatrix1 = gpkg.tiles().addTileMatrix(tileMatrixSet,
                                                                      3,
                                                                      matrixWidth2,
                                                                      matrixHeight2,
                                                                      tileWidth2,
                                                                      tileHeight2);

            final Collection<TileMatrix> expectedTileMatrix = new ArrayList<>(2);

            expectedTileMatrix.add(tileMatrix);
            expectedTileMatrix.add(tileMatrix1);

            final List<TileMatrix> gpkgTileMatrices = gpkg.tiles().getTileMatrices(tileSet);
            assertEquals("Expected the GeoPackage to return two Tile Matrices.", 2, gpkgTileMatrices.size());

            for(final TileMatrix gpkgTileMatrix : gpkg.tiles().getTileMatrices(tileSet))
            {
                //noinspection FloatingPointEquality
                assertTrue("The tile entry's information in the GeoPackage does not match what was originally given to a GeoPackage",
                           expectedTileMatrix.stream()
                                             .anyMatch(expectedTM -> expectedTM.getTableName()    .equals(gpkgTileMatrix.getTableName())   &&
                                                                     expectedTM.getMatrixHeight() ==      gpkgTileMatrix.getMatrixHeight() &&
                                                                     expectedTM.getMatrixWidth()  ==      gpkgTileMatrix.getMatrixWidth()  &&
                                                                     expectedTM.getPixelXSize()   ==      gpkgTileMatrix.getPixelXSize()   &&
                                                                     expectedTM.getPixelYSize()   ==      gpkgTileMatrix.getPixelYSize()   &&
                                                                     expectedTM.getZoomLevel()    ==      gpkgTileMatrix.getZoomLevel()));
            }
        }
    }

    /**
     * Tests if the GeoPackage will return null if no TileMatrix Entries are
     * found in the GeoPackage that matches the TileSet given.
     */
    @Test
    public void getTileMatricesNonExistant() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
           final TileSet tileSet = gpkg.tiles()
                                       .addTileSet("tables",
                                                   "identifier",
                                                   "description",
                                                   new BoundingBox(0.0,0.0,0.0,0.0),
                                                   gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            assertEquals("Expected the GeoPackage to return null when no tile Matrices are found", 0, gpkg.tiles().getTileMatrices(tileSet).size());
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a TileMatrix with a matrix width that is <=0
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 0, 5, 6, 7);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a matrix width that is <= 0");
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a TileMatrix with a matrix height that is <=0
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException2()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("name",
                                                    "identifier",
                                                    "description",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(-1));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 4, 0, 6, 7);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a matrix height that is <= 0");
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a TileMatrix with a tile width that is <=0
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException3()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0, 0.0, 0.0, 0.0), gpkg.core().getSpatialReferenceSystem(-1));
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 4, 5, 0, 7);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a tile width that is <= 0");

        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a TileMatrix with a tile height that is <=0
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException4()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 4, 5, 6, 0);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a tile height that is <= 0");
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a TileMatrix with a pixel X size that is <= 0
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException5()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 4, 5, 6, 7);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a pixelXsize that is <= 0");

        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a TileMatrix with a pixelYSize that is <=0
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException6()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 4, 5, 6, 7);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a pixelYSize that is <= 0");
        }
    }

    /**
     * Tests if a GeoPackage Tiles would throw an IllegalArgumentException when
     * attempting to add a Tile Matrix corresponding to the same tile set and
     * zoom level but have differing other fields
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatrixSameZoomDifferentOtherFields()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 2, 3, 4, 5);
            gpkg.tiles().addTileMatrix(tileMatrixSet, 0, 3, 2, 5, 4);

            fail("Expected GeoPackage Tiles to throw an IllegalArgumentException when addint a Tile Matrix with the same tile set and zoom level information but differing other fields");
        }
    }

    /**
     * Tests if the GeoPackage returns the same TileMatrix when trying to add
     * the same TileMatrix twice (verifies the values are the same)
     */
    @Test
    public void addTileMatrixTwiceVerify()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,90.0,90.0), gpkg.core().getSpatialReferenceSystem(-1));
            final int matrixHeight = 2;
            final int matrixWidth = 2;
            final int tileHeight = 256;
            final int tileWidth = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix2 = gpkg.tiles()
                                               .addTileMatrix(tileMatrixSet,
                                                              0,
                                                              matrixWidth,
                                                              matrixHeight,
                                                              tileWidth,
                                                              tileHeight);

            final TileMatrix tileMatrix1 = gpkg.tiles()
                                               .addTileMatrix(tileMatrixSet,
                                                              0,
                                                              matrixWidth,
                                                              matrixHeight,
                                                              tileWidth,
                                                              tileHeight);

            assertTrue("Expected the GeoPackage to return the existing Tile Matrix.",
                       tileMatrix1.equals(tileMatrix2.getTableName(),
                                          tileMatrix2.getZoomLevel(),
                                          tileMatrix2.getMatrixWidth(),
                                          tileMatrix2.getMatrixHeight(),
                                          tileMatrix2.getTileWidth(),
                                          tileMatrix2.getTileHeight(),
                                          tileMatrix2.getPixelXSize(),
                                          tileMatrix2.getPixelYSize()));
        }
    }

    /**
     * Tests if the GeoPackage returns the same TileMatrix when trying to add
     * the same TileMatrix twice (verifies the values are the same)
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatrixNullTileSet()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().addTileMatrix(null, 0, 2, 3, 4, 5);
            fail("Expected the GeoPackage to throw an IllegalArgumentException when giving a null parameter TileSet to addTileMatrix");
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when a user
     * tries to add a negative value for zoom level (when adding a tile Matrix
     * entry)
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatrixWithNegativeZoomLevel()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(2.0,1.0,4.0,3.0),
                                                      gpkg.core().getSpatialReferenceSystem(0));
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet, -1, 2, 4, 6, 8);
        }
    }

    /**
     * Tests if given a non empty tile Matrix Metadata information can be added without throwing an error.
     */
    @Test
    public void addNonEmptyTileMatrix() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            //add information to gpkg
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 80.0, 80.0),
                                                    gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final int matrixWidth = 4;
            final int matrixHeight = 8;
            final int tileWidth = 256;
            final int tileHeight = 512;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       1,
                                       matrixWidth,
                                       matrixHeight,
                                       tileWidth,
                                       tileHeight);
        }
        //test if information added is accurate
        final int matrixWidth = 4;
        final int matrixHeight = 8;
        final int tileWidth = 256;
        final int tileHeight = 512;

        final String query = String.format("SELECT table_name FROM gpkg_tile_matrix "
                                           + "WHERE zoom_level    = %d AND "
                                           + "      matrix_height = %d AND "
                                           + "       matrix_width = %d AND "
                                           + "        tile_height = %d AND "
                                           + "         tile_width = %d;",
                                           1,
                                           matrixHeight,
                                           matrixWidth,
                                           tileHeight,
                                           tileWidth);

        //noinspection JDBCExecuteWithNonConstantString
        try(final Connection con      = TestUtility.getConnection(testFile);
            final Statement stmt      = con.createStatement();
            final ResultSet tableName = stmt.executeQuery(query))
        {
            assertEquals("The GeoPackage did not enter the correct record into the gpkg_tile_matrix table", "tileSetName", tableName.getString("table_name"));
        }
     }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when giving
     * a null parameter to getTileMatrices
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileMatricesNullParameter() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileMatrices(null);
            fail("Expected the GeoPackage to throw an IllegalArgumentException when giving getTileMatrices a TileSet that is null.");
        }
    }

    /**
     * Tests if the GeoPackage getTIleMatrix can retrieve the correct TileMatrix
     * from the GeoPackage.
     */
    @Test
    public void getTileMatrixVerify()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "identifier",
                                                    "description",
                                                    new BoundingBox(0.0,0.0,100.0,100.0),
                                                    gpkg.core().getSpatialReferenceSystem(-1));
            final int matrixHeight = 2;
            final int matrixWidth = 6;
            final int tileHeight = 512;
            final int tileWidth = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final TileMatrix tileMatrix = gpkg.tiles()
                                              .addTileMatrix(tileMatrixSet,
                                                             0,
                                                             matrixWidth,
                                                             matrixHeight,
                                                             tileWidth,
                                                             tileHeight);

            final int matrixHeight2 = 1;
            final int matrixWidth2 = 3;
            final int tileHeight2 = 512;
            final int tileWidth2 = 256;

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       1,
                                       matrixWidth2,
                                       matrixHeight2,
                                       tileWidth2,
                                       tileHeight2
            );

            final TileMatrix returnedTileMatrix = gpkg.tiles().getTileMatrix(tileMatrixSet, 0);

            //noinspection FloatingPointEquality
            assertTrue("GeoPackage did not return the TileMatrix expected",
                       tileMatrix.getMatrixHeight() ==      returnedTileMatrix.getMatrixHeight() &&
                       tileMatrix.getMatrixWidth()  ==      returnedTileMatrix.getMatrixWidth()  &&
                       tileMatrix.getPixelXSize()   ==      returnedTileMatrix.getPixelXSize()   &&
                       tileMatrix.getPixelYSize()   ==      returnedTileMatrix.getPixelYSize()   &&
                       tileMatrix.getTableName()    .equals(returnedTileMatrix.getTableName())   &&
                       tileMatrix.getTileHeight()   ==      returnedTileMatrix.getTileHeight()   &&
                       tileMatrix.getTileWidth()    ==      returnedTileMatrix.getTileWidth()    &&
                       tileMatrix.getZoomLevel()    ==      returnedTileMatrix.getZoomLevel());
        }
    }

    /**
     * Tests if the GeoPackage returns null if the TileMatrix entry does not
     * exist in the GeoPackage file.
     */
    @Test
    public void getTileMatrixNonExistant()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("TableName",
                                                    "identifier",
                                                    "description",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(-1));

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            assertNull("GeoPackage was supposed to return null when there is a nonexistant TileMatrix entry at that zoom level and TileSet", gpkg.tiles().getTileMatrix(tileMatrixSet, 0));
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when
     * giving a null parameter to getTileMatrix.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileMatrixNullParameter()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileMatrix(null, 8);
            fail("GeoPackage should have thrown an IllegalArgumentException when giving a null parameter for TileSet in the method getTileMatrix");
        }
    }

    /**
     * Tests if getTileMatrixSet retrieves the values that is expected
     */
    @Test
    public void getTileMatrixSetVerify()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            //values for tileMatrixSet
            final String                 tableName   = "tableName";
            final String                 identifier  = "identifier";
            final String                 description = "description";
            final BoundingBox            bBox        = new BoundingBox(2.0, 1.0, 4.0, 3.0);
            final SpatialReferenceSystem srs         = gpkg.core().getSpatialReferenceSystem("EPSG", 4326);

            //add tileSet and tileMatrixSet to gpkg
            final TileSet       tileSet       = gpkg.tiles().addTileSet(tableName, identifier, description, bBox, srs);
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            assertTrue("Expected different values from getTileMatrixSet for SpatialReferenceSystem or BoundingBox or TableName.",
                                               tileMatrixSet.getBoundingBox()           .equals(bBox) &&
                                               tileMatrixSet.getSpatialReferenceSystem().equals(srs) &&
                                               tileMatrixSet.getTableName()             .equals(tableName));
        }
    }

    /**
     * Tests if the GeoPackage will throw a GeoPackage Conformance Exception
     * when given a GeoPackage that violates a requirement with a severity equal
     * to Error
     */
    @Test(expected = ConformanceException.class)
    @SuppressWarnings("ExpectedExceptionNeverThrown") // Intellij bug?
    public void geoPackageConformanceException() throws IOException, ConformanceException, SQLException, ClassNotFoundException
    {
        final File testFile = TestUtility.getRandomFile();
        testFile.createNewFile();
        try(final GeoPackage ignored = new GeoPackage(testFile, GeoPackage.OpenMode.Open))
        {
            fail("GeoPackage did not throw a geoPackageConformanceException as expected.");
        }
    }

    /**
     * Tests if the GeoPackage can convert an Geodetic crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateUpperRightGeodetic() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(-45.234567, 45.213192, geodeticRefSys);//upper right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(-180.0, 0.0, 0.0, 85.0511287798066),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 1;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 0, Expected Column: 1. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 0 && relativeCoord.getX() == 1);

        }
    }

    /**
     * Tests if the GeoPackage can convert an Geodetic crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateUpperLeftGeodetic() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(-180, 85, geodeticRefSys);//upper left tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                            "identifier",
                                                            "description",
                                                            new BoundingBox(-180.0, 0.0, 0.0, 85.0511287798066),
                                                            gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 1;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 0, Expected Column: 0. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 0 && relativeCoord.getX() == 0);

        }
    }

    /**
     * Tests if the GeoPackage can convert an Geodetic crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateLowerLeftGeodetic() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(-90, 41, geodeticRefSys);//lower left tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(-180.0, 0.0, 0.0, 85.0511287798066),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 1;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 1, Expected Column: 0. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 1 && relativeCoord.getX() == 1);

        }
    }

    /**
     * Tests if the GeoPackage can convert an Geodetic crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateLowerRightGeodetic() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(-0.000001, 12, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(-180.0, 0.0, 0.0, 85.0511287798066),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 1;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 1, Expected Column: 1. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 1 && relativeCoord.getX() == 1);
        }
    }

    /**
     * Tests if the GeoPackage can convert an Global Mercator crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateUpperLeftGlobalMercator() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final EllipsoidalMercatorCrsProfile mercator = new EllipsoidalMercatorCrsProfile();

        final CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
        final Coordinate<Double>        coordInMeters    = mercator.fromGlobalGeodetic(new Coordinate<>(-45.0, 5.0));
        final CrsCoordinate             crsMercatorCoord = new CrsCoordinate(coordInMeters.getX(), coordInMeters.getY(), globalMercator);

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final Coordinate<Double> minBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>(-90.0, -60.0));
            final Coordinate<Double> maxBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>( 5.0,   10.0));

            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(minBoundingBoxCoord.getX(), minBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY()),
                                                      gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator",
                                                                                            "EPSG",
                                                                                            3395,
                                                                                            "definition",
                                                                                            "description"));

            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 6;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord = gpkg.tiles().crsToTileCoordinate(tileSet, crsMercatorCoord, CrsProfileFactory.create(globalMercator).getPrecision(), zoomLevel);

            assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordinate.  "
                                    + "    \nExpected Row: 0, Expected Column: 0.\nActual Row: %d, Actual Column: %d.",
                                    relativeCoord.getY(),
                                    relativeCoord.getX()),
                        relativeCoord.getY() == 0 && relativeCoord.getX() == 0);

        }

    }

    /**
     * Tests if the GeoPackage can convert an Global Mercator crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateUpperRightGlobalMercator() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final EllipsoidalMercatorCrsProfile mercator = new EllipsoidalMercatorCrsProfile();

        final CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
        final Coordinate<Double>        coordInMeters    = mercator.fromGlobalGeodetic(new Coordinate<>(-42.0, 5.0));
        final CrsCoordinate             crsMercatorCoord = new CrsCoordinate(coordInMeters.getX(), coordInMeters.getY(), globalMercator);

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final Coordinate<Double> minBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>(-90.0, -60.0));
            final Coordinate<Double> maxBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>( 5.0,   10.0));

            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(minBoundingBoxCoord.getX(), minBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY()),
                                                      gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator",
                                                                                            "EPSG",
                                                                                            3395,
                                                                                            "definition",
                                                                                            "description"));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 6;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord = gpkg.tiles().crsToTileCoordinate(tileSet, crsMercatorCoord, CrsProfileFactory.create(globalMercator).getPrecision(), zoomLevel);

            assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordinate.  "
                                    + "    \nExpected Row: 0, Expected Column: 1.\nActual Row: %d, Actual Column: %d.",
                                    relativeCoord.getY(),
                                    relativeCoord.getX()),
                        relativeCoord.getY() == 0 && relativeCoord.getX() == 1);

        }
    }

    /**
     * Tests if the GeoPackage can convert an Global Mercator crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateLowerLeftGlobalMercator() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final EllipsoidalMercatorCrsProfile mercator = new EllipsoidalMercatorCrsProfile();

        final CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
        final Coordinate<Double>        coordInMeters    = mercator.fromGlobalGeodetic(new Coordinate<>(-47.0, -45.0));
        final CrsCoordinate             crsMercatorCoord = new CrsCoordinate(coordInMeters.getX(), coordInMeters.getY(), globalMercator);

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final Coordinate<Double> minBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>(-90.0, -60.0));
            final Coordinate<Double> maxBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>( 5.0,   10.0));

            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(minBoundingBoxCoord.getX(), minBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY()),
                                                      gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator",
                                                                                            "EPSG",
                                                                                            3395,
                                                                                            "definition",
                                                                                            "description"));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 6;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord = gpkg.tiles().crsToTileCoordinate(tileSet, crsMercatorCoord, CrsProfileFactory.create(globalMercator).getPrecision(), zoomLevel);

            assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordinate.  "
                                    + "    \nExpected Row: 1, Expected Column: 0.\nActual Row: %d, Actual Column: %d.",
                                    relativeCoord.getY(),
                                    relativeCoord.getX()),
                        relativeCoord.getY() == 1 && relativeCoord.getX() == 0);

        }
    }

    /**
     * Tests if the GeoPackage can convert an Global Mercator crsCoordinate to a
     * relative tile coordinate
     */
    @Test
    public void crsToRelativeTileCoordinateLowerRightGlobalMercator() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final EllipsoidalMercatorCrsProfile mercator = new EllipsoidalMercatorCrsProfile();

        final CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
        final Coordinate<Double>        coordInMeters    = mercator.fromGlobalGeodetic(new Coordinate<>(4.999, -55.0));
        final CrsCoordinate             crsMercatorCoord = new CrsCoordinate(coordInMeters.getX(), coordInMeters.getY(), globalMercator);

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final Coordinate<Double> minBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>(-90.0, -60.0));
            final Coordinate<Double> maxBoundingBoxCoord = mercator.fromGlobalGeodetic(new Coordinate<>(  5.0,  10.0));

            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                            "identifier",
                                                            "description",
                                                            new BoundingBox(minBoundingBoxCoord.getX(), minBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY()),
                                                            gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator",
                                                                                                  "EPSG",
                                                                                                  3395,
                                                                                                  "definition",
                                                                                                  "description"));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 6;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord = gpkg.tiles().crsToTileCoordinate(tileSet, crsMercatorCoord, CrsProfileFactory.create(globalMercator).getPrecision(), zoomLevel);

            assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordinate.  "
                                    + "    \nExpected Row: 1, Expected Column: 1.\nActual Row: %d, Actual Column: %d.",
                                    relativeCoord.getY(),
                                    relativeCoord.getX()),
                        relativeCoord.getY() == 1 && relativeCoord.getX() == 1);

        }
    }

    /**
     * Tests if a GeoPackage can translate a crs to a relative tile coordinate
     * when there are multiple zoom levels and when there are more tiles at the
     * higher zoom
     */
    @Test
    public void crsToRelativeTileCoordinateMultipleZoomLevels() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(-27.5, -1.25, geodeticRefSys);

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(-100.0, -60.0, 100.0, 60.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            final int matrixWidth1  = 16;
            final int matrixHeight1 = 24;
            final int pixelXSize   = 256;
            final int pixelYSize   = 512;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 5;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth1,
                                       matrixHeight1,
                                       pixelXSize,
                                       pixelYSize
            );

            final int matrixWidth2  = 4;
            final int matrixHeight2 = 6;
            final int zoomLevel2 = 3;

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel2,
                                       matrixWidth2,
                                       matrixHeight2,
                                       pixelXSize,
                                       pixelYSize
            );
            final int matrixWidth3  = 8;
            final int matrixHeight3 = 12;
            final int zoomLevel3 = 4;

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel3,
                                       matrixWidth3,
                                       matrixHeight3,
                                       pixelXSize,
                                       pixelYSize
            );


            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 12, Expected Column: 5. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 12 && relativeCoord.getX() == 5);

        }
    }

    /**
     * This tests the validity of the transformation of crs to relative tile
     * coordinate when the crs coordinate lies in the middle of four tiles.
     */
    @Test
    public void crsToRelativeTileCoordEdgeCase() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(76.4875, 36.45, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(-180.0, 0.0, 90.0, 85.05),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 20;
            final int matrixHeight = 7;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 4, Expected Column: 18. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 4 && relativeCoord.getX() == 18);
        }
    }

    /**
     * This tests the validity of the transformation of crs to relative tile
     * coordinate when the crs coordinate lies between two tiles on top of each
     * other
     */
    @Test
    public void crsToRelativeTileCoordEdgeCase2() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(10, 25, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 0, Expected Column: 0. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 1 && relativeCoord.getX() == 0);
        }
    }

    /**
     * This tests the validity of the transformation of crs to relative tile
     * coordinate when the crs coordinate lies on the left border
     */
    @Test
    public void crsToRelativeTileCoordEdgeCase3() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(0, 40, geodeticRefSys);//upper Left tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                            "identifier",
                                                            "description",
                                                            new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                            gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                                + "\nExpected Row: 0, Expected Column: 0. \nActual Row: %d, Actual Column: %d",
                                            relativeCoord.getY(),
                                            relativeCoord.getX()),
                             relativeCoord.getY() == 0 && relativeCoord.getX() == 0);
        }
    }

    /**
     * This tests the validity of the transformation of crs to relative tile
     * coordinate when the crs coordinate lies on the right border
     */
    @Test
    public void crsToRelativeTileCoordEdgeCase4() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(29.9, 30, geodeticRefSys);//upper right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 0, Expected Column: 1. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 0 && relativeCoord.getX() == 1);
        }
    }

    /**
     * This tests the validity of the transformation of crs to relative tile
     * coordinate when the crs coordinate lies on the top border
     */
    @Test
    public void crsToRelativeTileCoordEdgeCase5() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(20, 50, geodeticRefSys);//upper right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 0, Expected Column: 1. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 0 && relativeCoord.getX() == 1);
        }
    }

    /**
     * This tests the validity of the transformation of crs to relative tile
     * coordinate when the crs coordinate lies on the bottom border
     */
    @Test
    public void crsToRelativeTileCoordEdgeCase6() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(20, 0.01, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                            "identifier",
                                                            "description",
                                                            new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                            gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 1, Expected Column: 1. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 1 && relativeCoord.getX() == 1);
        }
    }

    /**
     * Test if a crsCoordinate can be translated to a tile coordinate
     */

    @Test
    public void crsToRelativeTileCoordinateEdgeCase7()throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CrsCoordinate        coordinate = new CrsCoordinate((GlobalGeodeticCrsProfile.Bounds.getMinimumX()+(2*(GlobalGeodeticCrsProfile.Bounds.getWidth()))  / 8),
                (GlobalGeodeticCrsProfile.Bounds.getMaximumY()-(6*(GlobalGeodeticCrsProfile.Bounds.getHeight())) / 9),
                "epsg",
                4326);
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                            "identifier",
                                                            "description",
                                                            GlobalGeodeticCrsProfile.Bounds,
                                                            gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 8;
            final int matrixHeight = 9;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 0;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final Coordinate<Integer> relativeCoord  = gpkg.tiles().crsToTileCoordinate(tileSet, coordinate, CrsProfileFactory.create("EPSG", 4326).getPrecision(), zoomLevel);

            assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
                                       + "\nExpected Row: 6, Expected Column: 2. \nActual Row: %d, Actual Column: %d", relativeCoord.getY(), relativeCoord.getX()),
                       relativeCoord.getY() == 6 && relativeCoord.getX() == 2);
        }
    }

    /**
     * Tests if a GeoPackage will throw the appropriate exception when giving
     * the method a null value for crsCoordinate.
     */
    @Test(expected = IllegalArgumentException.class)
    public void crsToRelativeTileCoordException() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            gpkg.tiles().crsToTileCoordinate(tileSet, null, CrsProfileFactory.create("EPSG", 4326).getPrecision(), 0);

            fail("Expected the GeoPackage to throw an IllegalArgumentException when trying to input a crs tile coordinate that was null to the method crsToRelativeTileCoordinate.");
        }
    }

    /**
     * Tests if a GeoPackage will throw the appropriate exception when giving
     * the method a null value for crsCoordinate.
     */
    @Test(expected = IllegalArgumentException.class)
    public void crsToRelativeTileCoordException2() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final int zoomLevel = 1;
            final CoordinateReferenceSystem coordinateReferenceSystem = new CoordinateReferenceSystem("Police", 99);
            final CrsCoordinate           crsCoord                     = new CrsCoordinate(15, 20, coordinateReferenceSystem);

            gpkg.tiles().crsToTileCoordinate(null, crsCoord, 2, zoomLevel);

            fail("Expected the GeoPackage to throw an IllegalArgumentException when trying to input a tileSet that was null to the method crsToRelativeTileCoordinate.");
        }
    }

    /**
     * This tests that the appropriate exception is thrown when trying to find a
     * crs coordinate from a different SRS from the tiles.
     */
    @Test(expected = IllegalArgumentException.class)
    public void crsToRelativeTileCoordException3() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(20, 50, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem(-1));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            fail("Expected the GoePackage to throw an exception when the crs coordinate and the tiles are from two different projections.");
        }
    }

    /**
     * This tests that the appropriate exception is thrown when trying to find a
     * crs coordinate from with a zoom level that is not in the matrix table
     */
    @Test(expected = IllegalArgumentException.class)
    public void crsToRelativeTileCoordException4() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(20, 50, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;
            final int differentZoomLevel = 12;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       differentZoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            final int zoomLevel = 15;
            gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);
        }
    }

    /**
     * This tests that the appropriate exception is thrown when trying to find a
     * crs coordinate is not within bounds
     */
    @Test(expected = IllegalArgumentException.class)
    public void crsToRelativeTileCoordException5() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
        final CrsCoordinate crsCoord = new CrsCoordinate(20, -50, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);
        }
    }

    /**
     * This tests that the appropriate exception is thrown when trying to find a
     * crs coordinate from a different SRS from the tiles.
     */
    @Test(expected = IllegalArgumentException.class)
    public void crsToRelativeTileCoordException6() throws SQLException, ClassNotFoundException, ConformanceException, IOException
    {
        final CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG", 3857);
        final CrsCoordinate crsCoord = new CrsCoordinate(20, 50, geodeticRefSys);//lower right tile

        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, GeoPackage.OpenMode.Create))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                      "identifier",
                                                      "description",
                                                      new BoundingBox(0.0, 0.0, 30.0, 50.0),
                                                      gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            final int matrixWidth = 2;
            final int matrixHeight = 2;
            final int pixelXSize = 256;
            final int pixelYSize = 256;

            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            final int zoomLevel = 15;
            gpkg.tiles().addTileMatrix(tileMatrixSet,
                                       zoomLevel,
                                       matrixWidth,
                                       matrixHeight,
                                       pixelXSize,
                                       pixelYSize
            );

            gpkg.tiles().crsToTileCoordinate(tileSet, crsCoord, CrsProfileFactory.create(geodeticRefSys).getPrecision(), zoomLevel);

            fail("Expected the GoePackage to throw an exception when the crs coordinate and the tiles are from two different projections.");
        }
    }

    /**
     * Tests if a tileCoordinate can be converted to the correct CRS Coordinate
     */
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    public void tileToCrsCoordinate() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final BoundingBox bBox         = new BoundingBox(0, 0.0, 180.0, 90.0);
            final int         row          = 3;
            final int         column       = 5;
            final int         zoomLevel    = 4;
            final int         matrixWidth  = 6;
            final int         matrixHeight = 4;
            final TileMatrix  tileMatrix   = createTileSetAndTileMatrix(gpkg, bBox, zoomLevel, matrixWidth, matrixHeight);

            final CrsCoordinate crsCoordReturned = gpkg.tiles().tileToCrsCoordinate(gpkg.tiles().getTileSet(tileMatrix.getTableName()), column, row, zoomLevel);
            final CrsCoordinate crsCoordExpected = new CrsCoordinate(bBox.getMinimumX() + column * (bBox.getWidth()  / matrixWidth),
                                                                     bBox.getMaximumY() - row    * (bBox.getHeight() / matrixHeight),
                                                                     new GlobalGeodeticCrsProfile().getCoordinateReferenceSystem());

            GeoPackageTilesAPITest.assertCoordinatesEqual(crsCoordReturned, crsCoordExpected);
        }
    }

    /**
     * Tests if a tileCoordinate can be converted to the correct CRS Coordinate
     */
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    public void tileToCrsCoordinate2() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final CrsProfile spherMercator = new SphericalMercatorCrsProfile();

            final BoundingBox bBox = new BoundingBox(spherMercator.getBounds().getMinimumX()/2,
                                                     spherMercator.getBounds().getMinimumY()/3,
                                                     spherMercator.getBounds().getMaximumX(),
                                                     spherMercator.getBounds().getMaximumY()/2);
            final int row          = 5;
            final int column       = 1;
            final int zoomLevel    = 4;
            final int matrixWidth  = 13;
            final int matrixHeight = 8;

            final SpatialReferenceSystem srs = gpkg.core().addSpatialReferenceSystem(spherMercator.getName(),
                                                                                     spherMercator.getCoordinateReferenceSystem().getAuthority(),
                                                                                     spherMercator.getCoordinateReferenceSystem().getIdentifier(),
                                                                                     spherMercator.getWellKnownText(),
                                                                                     spherMercator.getDescription());

            final TileMatrix tileMatrix = createTileSetAndTileMatrix(gpkg, srs, bBox, zoomLevel, matrixWidth, matrixHeight, 256, 256, "tableName");

            final CrsCoordinate crsCoordExpected =  new CrsCoordinate(bBox.getMinimumX() + column * (bBox.getWidth()/matrixWidth),
                                                                      bBox.getMaximumY() - row    * (bBox.getHeight()/matrixHeight),
                                                                      spherMercator.getCoordinateReferenceSystem());

            final CrsCoordinate crsCoordReturned = gpkg.tiles().tileToCrsCoordinate(gpkg.tiles().getTileSet(tileMatrix.getTableName()), column, row, zoomLevel);


            GeoPackageTilesAPITest.assertCoordinatesEqual(crsCoordReturned, crsCoordExpected);
        }
    }

    /**
     * Tests if a tileCoordinate can be converted to the correct CRS Coordinate
     */
    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    public void tileToCrsCoordinate3() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final BoundingBox bBox         =  new BoundingBox(-22.1258, -15.325, 43.125, 78.248);
            final int         row          = 2;
            final int         column       = 7;
            final int         zoomLevel    = 4;
            final int         matrixWidth  = 13;
            final int         matrixHeight = 8;
            final TileMatrix  tileMatrix   = createTileSetAndTileMatrix(gpkg, bBox, zoomLevel, matrixWidth, matrixHeight);

            final CrsCoordinate crsCoordReturned = gpkg.tiles().tileToCrsCoordinate(gpkg.tiles().getTileSet(tileMatrix.getTableName()), column, row, zoomLevel);
            final CrsCoordinate crsCoordExpected = new CrsCoordinate(bBox.getMinimumX() + column*(bBox.getWidth()/matrixWidth),
                                                                     bBox.getMaximumY() - row*  (bBox.getHeight()/matrixHeight),
                                                                     new GlobalGeodeticCrsProfile().getCoordinateReferenceSystem());

            GeoPackageTilesAPITest.assertCoordinatesEqual(crsCoordReturned, crsCoordExpected);
        }
    }

    /**
     * Tests if an IllegalArgumentException when appropriate
     */
    @Test(expected = IllegalArgumentException.class)
    public void tileToCrsCoordinateIllegalArgumentException() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().tileToCrsCoordinate(null, 0, 0, 0);
            fail("Expected an IllegalArgumentException when giving a null value for tileSet");
        }
    }

    /**
     * Tests if an IllegalArgumentException when appropriate
     */
    @Test(expected = IllegalArgumentException.class)
    public void tileToCrsCoordinateIllegalArgumentException2() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
           final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                     "identifier",
                                                     "description",
                                                     new BoundingBox(0.0,0.0,0.0,0.0),
                                                     gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            gpkg.tiles().tileToCrsCoordinate(tileSet, -1, 0, 0);
            fail("Expected an IllegalArgumentException when giving a negative value for column");
        }
    }

    /**
     * Tests if an IllegalArgumentException when appropriate
     */
    @Test(expected = IllegalArgumentException.class)
    public void tileToCrsCoordinateIllegalArgumentException3() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
           final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                     "identifier",
                                                     "description",
                                                     new BoundingBox(0.0,0.0,0.0,0.0),
                                                     gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            gpkg.tiles().tileToCrsCoordinate(tileSet, 0, -1, 0);
            fail("Expected an IllegalArgumentException when giving a negative value for row");
        }
    }

    /**
     * Tests if an IllegalArgumentException when appropriate
     */
    @Test(expected = IllegalArgumentException.class)
    public void tileToCrsCoordinateIllegalArgumentException4() throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        final File testFile = TestUtility.getRandomFile();
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
           final TileSet tileSet = gpkg.tiles().addTileSet("tableName",
                                                     "identifier",
                                                     "description",
                                                     new BoundingBox(0.0,0.0,0.0,0.0),
                                                     gpkg.core().getSpatialReferenceSystem("EPSG", 4326));

            gpkg.tiles().tileToCrsCoordinate(tileSet, 0, 0, 0);
            fail("Expected an IllegalArgumentException when giving a zoom that does not have a Tile Matrix associated with it.");
        }
    }

    private static void assertCoordinatesEqual(final CrsCoordinate crsCoordReturned, final CrsCoordinate crsCoordExpected)
    {
        assertEquals(String.format("The coordinate returned was not the values expected.\n"
                                   + "Actual Coordinate: (%f, %f) Crs: %s %d\nReturned Coordinate: (%f, %f) Crs: %s %d",
                                   crsCoordReturned.getX(),
                                   crsCoordReturned.getY(),
                                   crsCoordReturned.getCoordinateReferenceSystem().getAuthority(),
                                   crsCoordReturned.getCoordinateReferenceSystem().getIdentifier(),
                                   crsCoordExpected.getX(),
                                   crsCoordReturned.getY(),
                                   crsCoordReturned.getCoordinateReferenceSystem().getAuthority(),
                                   crsCoordReturned.getCoordinateReferenceSystem().getIdentifier()),
                      crsCoordExpected,
                      crsCoordReturned);
    }

    private static TileMatrix createTileSetAndTileMatrix(final GeoPackage gpkg, final BoundingBox bBox, final int zoomLevel, final int matrixWidth, final int matrixHeight) throws SQLException
    {
        return createTileSetAndTileMatrix(gpkg, gpkg.core().getSpatialReferenceSystem("EPSG", 4326), bBox, zoomLevel, matrixWidth, matrixHeight, 256, 256, "tableName");
    }

    private static TileMatrix createTileSetAndTileMatrix(final GeoPackage             gpkg,
                                                         final SpatialReferenceSystem srs,
                                                         final BoundingBox            bBox,
                                                         final int                    zoomLevel,
                                                         final int                    matrixWidth,
                                                         final int                    matrixHeight,
                                                         final int                    tileWidth,
                                                         final int                    tileHeight,
                                                         final String                 identifierTableName) throws SQLException
    {
        // Create a tileSet
        final TileSet tileSet = gpkg.tiles()
                                    .addTileSet(identifierTableName,
                                                identifierTableName,
                                                "description",
                                                bBox,
                                                srs);
        // Create matrix set
        final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

        // Create matrix
        return gpkg.tiles().addTileMatrix(tileMatrixSet,
                                          zoomLevel,
                                          matrixWidth,
                                          matrixHeight,
                                          tileWidth,
                                          tileHeight);
    }

    private static byte[] createImageBytes() throws IOException
    {
        return ImageUtility.bufferedImageToBytes(new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB), "PNG");
    }
}
