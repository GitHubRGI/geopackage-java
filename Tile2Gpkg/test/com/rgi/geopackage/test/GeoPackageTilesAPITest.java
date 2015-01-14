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

package com.rgi.geopackage.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.rgi.common.BoundingBox;
import com.rgi.common.CoordinateReferenceSystem;
import com.rgi.common.LatLongConversions;
import com.rgi.common.coordinates.Coordinate;
import com.rgi.common.coordinates.CrsTileCoordinate;
import com.rgi.geopackage.GeoPackage;
import com.rgi.geopackage.GeoPackage.OpenMode;
import com.rgi.geopackage.core.SpatialReferenceSystem;
import com.rgi.geopackage.tiles.RelativeTileCoordinate;
import com.rgi.geopackage.tiles.Tile;
import com.rgi.geopackage.tiles.TileMatrix;
import com.rgi.geopackage.tiles.TileMatrixSet;
import com.rgi.geopackage.tiles.TileSet;
import com.rgi.geopackage.verification.ConformanceException;

/**
 * @author Jenifer Cochran
 */
@SuppressWarnings("static-method")
public class GeoPackageTilesAPITest
{
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private final Random randomGenerator = new Random();


    /**
     * This tests if a GeoPackage can add a tile set successfully without throwing errors.
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void addTileSet() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(5);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
           final TileSet tileSet = gpkg.tiles()
                                       .addTileSet("pyramid",
                                                   "title",
                                                   "tiles",
                                                   new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                   gpkg.core().getSpatialReferenceSystem(4326));

            gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 2, 2);
        }

        final String query = "SELECT table_name FROM gpkg_tile_matrix_set WHERE table_name = 'pyramid';";

        try(Connection con       = this.getConnection(testFile.getAbsolutePath());
            Statement  stmt      = con.createStatement();
            ResultSet  tileName  = stmt.executeQuery(query);)
        {
            assertTrue("The GeoPackage did not set the table_name into the gpkg_tile_matrix_set when adding a new set of tiles.", tileName.next());
            final String tableName = tileName.getString("table_name");
            assertTrue("The GeoPackage did not insert the correct table name into the gpkg_tile_matrix_set when adding a new set of tiles.", tableName.equals("pyramid"));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when given a null value for tileSetEntry
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithNullTileSetEntry() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(3);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
        	TileSet tileSet = gpkg.tiles().addTileSet("tableName", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(4326));
            gpkg.tiles().addTile(null, gpkg.tiles().getTileMatrix(tileSet, 0), new RelativeTileCoordinate(0, 0, 0), createImageBytes());
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a null value for tileSetEntry.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when
     * given a tilesetentry with a null value for the boundingbox
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithNullBoundingBox() throws Exception
    {
        final File testFile = this.getRandomFile(3);
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
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when
     * given a tilesetentry with a null value for the boundingbox
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithNullBoundingBoxParameters() throws Exception
    {
        final File testFile = this.getRandomFile(3);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .addTileSet("tableName",
                            "ident",
                            "desc",
                            new BoundingBox(0.0, 0.0, null, null),
                            gpkg.core().getSpatialReferenceSystem(4326));


            fail("The GeoPackage should have thrown an IllegalArgumentException when trying to add null values to the bounding box for the "
                    + " Tile Matrix Set table.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

   /**
     * Tests if the Geopackage will throw an IllegalArgumentException
     * If it gives tries to create a TileSet with a null SRS value
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithNullSRS() throws Exception
    {
        final File testFile = this.getRandomFile(8);

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
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Test if the GeoPackage will add a Tile set with a new Spatial Reference System (one created by user).
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void addTileSetWithNewSpatialReferenceSystem() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(5);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

             gpkg.core().addSpatialReferenceSystem("scaled world mercator",
                                                   9804,
                                                   "org",
                                                   9804,
                                                   "definition",
                                                   "description");
        }

        final String query = "SELECT srs_name FROM gpkg_spatial_ref_sys "+
                             "WHERE srs_name     = 'scaled world mercator' AND "+
                                   "srs_id       = 9804                    AND "+
                                   "organization = 'org'                   AND "+
                                   "definition   = 'definition'            AND "+
                                   "description  = 'description';";

        try(Connection con     = this.getConnection(testFile.getAbsolutePath());
            Statement  stmt    = con.createStatement();
            ResultSet  srsInfo = stmt.executeQuery(query);)
        {
            assertTrue("The Spatial Reference System added to the GeoPackage by the user did not contain the same information given.", srsInfo.next());
        }
        finally
        {

            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
     }


    /**
     * Tests if given a GeoPackage with tiles already inside it can add another Tile Set without throwing an error and verify that it entered the correct information.
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void addTileSetToExistingGpkgWithTilesInside() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(5);
        //create a geopackage with tiles inside
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 2, 2);

            //open a file with tiles inside and add more tiles
            try(GeoPackage gpkgWithTiles = new GeoPackage(testFile, OpenMode.Open))
            {
                final TileSet tileSetEntry2 = gpkgWithTiles.tiles()
                                                           .addTileSet("newTileSet",
                                                                       "title2",
                                                                       "tiles",
                                                                       new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                                       gpkgWithTiles.core().getSpatialReferenceSystem(4326));

                gpkgWithTiles.tiles().addTileMatrix(tileSetEntry2, 0, 2, 2, 2, 2, 2, 2);
            }
        }
        //make sure the information was added to contents table and tile matrix set table
        final String query = "SELECT cnts.table_name FROM gpkg_contents        AS cnts WHERE cnts.table_name"+
                             " IN(SELECT tms.table_name  FROM gpkg_tile_matrix_set AS tms  WHERE cnts.table_name = tms.table_name);";

        try(Connection con            = this.getConnection(testFile.getAbsolutePath());
            Statement  stmt           = con.createStatement();
            ResultSet  tileTableNames = stmt.executeQuery(query);)
        {
            if(!tileTableNames.next())
            {
                fail("The two tiles tables where not successfully added to both the gpkg_contents table and the gpkg_tile_matrix_set.");
            }
            while (tileTableNames.next())
            {
                final String tilesTableName = tileTableNames.getString("table_name");
                assertTrue("The tiles table names did not match what was being added to the GeoPackage",
                            tilesTableName.equals("newTileSet") || tilesTableName.equals("tileSetName"));
            }
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

   

    /**
     * Tests if a GeoPackage will throw an error when adding a tileset with the same name as another tileset in the GeoPackage.
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetWithRepeatedTileSetName() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(5);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("repeated_name",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 2, 2);

            final TileSet tileSetEntry2 = gpkg.tiles()
                                              .addTileSet("repeated_name",
                                                          "title2",
                                                          "tiles",
                                                          new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                          gpkg.core().getSpatialReferenceSystem(4326));

            gpkg.tiles().addTileMatrix(tileSetEntry2, 0, 2, 2, 2, 2, 2, 2);

            fail("The GeoPackage should throw an IllegalArgumentException when a user gives a Tile Set Name that already exists.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if a GeoPackage can add 2 Tile Matrix entries with
     * the two different tile pyramids can be entered into one gpkg
     */
    @Test
    public void addTileSetToExistingTilesTable() throws Exception
    {
        final File testFile = this.getRandomFile(9);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "tiles",
                                                    "desc",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));


            final ArrayList<TileSet> tileSetContnentEntries = new ArrayList<>();

            tileSetContnentEntries.add(tileSet);
            tileSetContnentEntries.add(tileSet);

            gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 2, 2);
            gpkg.tiles().addTileMatrix(tileSet, 1, 2, 2, 2, 2, 2, 2);

            for(final TileSet gpkgEntry : gpkg.tiles().getTileSets())
            {
                assertTrue("The tile entry's information in the GeoPackage does not match what was originally given to a GeoPackage",
                           tileSetContnentEntries.stream()
                                                 .anyMatch(tileEntry -> tileEntry.getBoundingBox()           .equals(gpkgEntry.getBoundingBox())             &&
                                                                        tileEntry.getDataType()              .equals(gpkgEntry.getDataType())                &&
                                                                        tileEntry.getSpatialReferenceSystem().equals(gpkgEntry.getSpatialReferenceSystem())  &&
                                                                        tileEntry.getDescription()           .equals(gpkgEntry.getDescription())             &&
                                                                        tileEntry.getIdentifier()            .equals(gpkgEntry.getIdentifier())              &&
                                                                        tileEntry.getTableName()             .equals(gpkgEntry.getTableName())));
            }
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delte testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * This ensures that when a user tries to add
     * the same tileSet two times that the TileSet object
     * that is returned is the one that already exists in
     * the GeoPackage and verifies its contents
     * 
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws FileNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     */
    @Test
    public void addSameTileSetTwice() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	final File testFile = this.getRandomFile(13);
    	try(GeoPackage gpkg = new GeoPackage(testFile))
    	{
    		String      tableName   = "tableName";
    		String      identifier  = "identifier";
    		String      description = "description";
    		BoundingBox boundingBox = new BoundingBox(1.0,2.0,3.0,4.0);
    		
    		SpatialReferenceSystem srs = gpkg.core().getSpatialReferenceSystem(0);
    		
    		TileSet tileSet = gpkg.tiles().addTileSet(tableName, 
    												  identifier, 
    												  description, 
    												  boundingBox, 
    												  srs);
    		
    		TileSet sameTileSet = gpkg.tiles().addTileSet(tableName, 
    				            						  identifier, 
    													  description, 
    													  boundingBox, 
    													  srs);
    		
    		assertTrue("The GeoPackage did not return the same tile set when trying to add the same tile set twice.",
    				   sameTileSet.equals(tileSet.getTableName(), 
    						   			  tileSet.getDataType(), 
    						   			  tileSet.getIdentifier(), 
    						   			  tileSet.getDescription(), 
    						   			  tileSet.getBoundingBox(), 
    						   			  tileSet.getSpatialReferenceSystem()));
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    	
    }

    /**
     * Expects GeoPackage to throw an IllegalArgumentException
     * when giving addTileSet a parameter with a null value
     * for bounding box
     * @throws SQLException
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetBadTableName() throws SQLException, FileAlreadyExistsException, ClassNotFoundException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(9);

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
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Expects GeoPackage to throw an IllegalArgumentException
     * when giving addTileSet a parameter with a null value
     * for bounding box
     * @throws SQLException
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetBadSRS() throws SQLException, FileAlreadyExistsException, ClassNotFoundException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(9);

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
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Expects GeoPackage to throw an IllegalArgumentException
     * when giving addTileSet a parameter with a null value
     * for bounding box
     * @throws SQLException
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileSetBadBoundingBox() throws SQLException, FileAlreadyExistsException, ClassNotFoundException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(9);

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
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when The table name is an empty string
     * for TileSet.
     * @throws SQLException
     * @throws Exception
     */
     @Test(expected = IllegalArgumentException.class)
     public void addTileSetContentEntryInvalidTableName() throws SQLException, Exception
     {
         final File testFile = this.getRandomFile(5);
         try(GeoPackage gpkg = new GeoPackage(testFile))
         {
             gpkg.tiles()
                 .addTileSet("",
                             "ident",
                             "desc",
                             new BoundingBox(0.0,0.0,0.0,0.0),
                             gpkg.core().getSpatialReferenceSystem(4326));

             fail("Expected the GeoPackage to throw an IllegalArgumentException when given a TileSet with an empty string for the table name.");
         }
         finally
         {
             if(testFile.exists())
             {
                 if(!testFile.delete())
                 {
                     throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                 }
             }
         }
     }
    

    /**
     * Tests if a GeoPackage will return the same tileSets that was given to the GeoPackage when adding tileSets.
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void getTileSetsFromGpkg() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(6);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "tiles",
                                                    "desc",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            final TileSet tileSet2 = gpkg.tiles()
                                         .addTileSet("SecondTileSet",
                                                     "ident",
                                                     "descrip",
                                                     new BoundingBox(1.0,1.0,1.0,1.0),
                                                     gpkg.core().getSpatialReferenceSystem(4326));

            final ArrayList<TileSet> tileSetContnentEntries = new ArrayList<>();

            tileSetContnentEntries.add(tileSet);
            tileSetContnentEntries.add(tileSet2);

            gpkg.tiles().addTileMatrix(tileSet,  0, 2, 2, 2, 2, 2, 2);
            gpkg.tiles().addTileMatrix(tileSet2, 1, 2, 2, 2, 2, 2, 2);

            final Collection<TileSet> tileSetsFromGpkg = gpkg.tiles().getTileSets();

            assertTrue("The number of tileSets added to a GeoPackage do not match with how many is retrieved from a GeoPacakage.",tileSetContnentEntries.size() == tileSetsFromGpkg.size());

            for(final TileSet gpkgEntry : tileSetsFromGpkg)
            {
                assertTrue("The tile entry's information in the GeoPackage does not match what was originally given to a GeoPackage",
                           tileSetContnentEntries.stream()
                                                 .anyMatch(tileEntry -> tileEntry.getBoundingBox()           .equals(gpkgEntry.getBoundingBox())             &&
                                                                        tileEntry.getDataType()              .equals(gpkgEntry.getDataType())                &&
                                                                        tileEntry.getSpatialReferenceSystem().equals(gpkgEntry.getSpatialReferenceSystem())  &&
                                                                        tileEntry.getDescription()           .equals(gpkgEntry.getDescription())             &&
                                                                        tileEntry.getIdentifier()            .equals(gpkgEntry.getIdentifier())              &&
                                                                        tileEntry.getTableName()             .equals(gpkgEntry.getTableName())));
            }
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will find no tile Sets when searching with an SRS that is not in the GeoPackage.
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void getTileSetWithNewSRS() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(7);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
          final Collection<TileSet> gpkgTileSets = gpkg.tiles().getTileSets(gpkg.core().addSpatialReferenceSystem("name", 123,"org", 123,"def","desc"));
          assertTrue("Should not have found any tile sets because there weren't any in "
                   + "GeoPackage that matched the SpatialReferenceSystem given.", gpkgTileSets.size() == 0);

        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the getTileSet returns null when the tile table
     * does not exist
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test
    public void getTileSetVerifyReturnNull() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(4);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().getTileSet("table_not_here");

            assertTrue("GeoPackage expected to return null when the tile set does not exist in GeoPackage",tileSet == null);
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the getTileSet returns the expected
     * values.
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test
    public void getTileSetVerifyReturnCorrectTileSet() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet         = gpkg.tiles().addTileSet("ttable","identifier", "Desc", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(4326));
            final TileSet returnedTileSet = gpkg.tiles().getTileSet("ttable");

            assertTrue("GeoPackage did not return the same values given to tile set",
                       tileSet.getBoundingBox().equals(returnedTileSet.getBoundingBox()) &&
                       tileSet.getDescription().equals(returnedTileSet.getDescription()) &&
                       tileSet.getDataType()   .equals(returnedTileSet.getDataType())    &&
                       tileSet.getIdentifier() .equals(returnedTileSet.getIdentifier())  &&
                       tileSet.getLastChange() .equals(returnedTileSet.getLastChange())  &&
                       tileSet.getTableName()  .equals(returnedTileSet.getTableName())   &&
                       tileSet.getSpatialReferenceSystem().equals(returnedTileSet.getSpatialReferenceSystem()));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }


    /**
     * Tests if the GeoPackage can detect there are zoom levels for
     * a tile that is not represented in the Tile Matrix Table.
     * Should throw a IllegalArgumentException.
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(4);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                     new BoundingBox(0.0,0.0,0.0,0.0),
                                                     gpkg.core().getSpatialReferenceSystem(4326));

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileSet, 18, 20, 20, 2,2, 1, 1);
            gpkg.tiles().addTile(tileSet, tileMatrix, new RelativeTileCoordinate(0, 0, 0), new byte[] {1, 2, 3, 4});

            fail("Geopackage should throw a IllegalArgumentExceptionException when Tile Matrix Table "
               + "does not contain a record for the zoom level of a tile in the Pyramid User Data Table.");

        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_row
     * is larger than the matrix_height -1. Which is a violation
     * of the GeoPackage Specifications. Requirement 55.
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException2() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(4);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 1, 1);
            gpkg.tiles().addTile(tileSet, tileMatrix, new RelativeTileCoordinate(10, 0, 0), new byte[] {1, 2, 3, 4});

            fail("Geopackage should throw a IllegalArgumentException when tile_row "
               + "is larger than matrix_height - 1 when zoom levels are equal.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_row
     * is less than 0. Which is a violation
     * of the GeoPackage Specifications. Requirement 55.
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException3() throws SQLException, Exception
    {
        final File      testFile = this.getRandomFile(4);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 1, 1);
            gpkg.tiles().addTile(tileSet, tileMatrix, new RelativeTileCoordinate(-1, 0, 0), new byte[] {1, 2, 3, 4});

            fail("Geopackage should throw a IllegalArgumentException when tile_row "
               + "is less than 0.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_column
     * is larger than matrix_width -1. Which is a violation
     * of the GeoPackage Specifications. Requirement 54.
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException4() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(4);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 1, 1);
            gpkg.tiles().addTile(tileSet,tileMatrix, new RelativeTileCoordinate(0, 10, 0), new byte[] {1, 2, 3, 4});

            fail("Geopackage should throw a IllegalArgumentException when tile_column "
               + "is larger than matrix_width -1.");

        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage can detect the tile_column
     * is less than 0. Which is a violation
     * of the GeoPackage Specifications. Requirement 54.
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTilesIllegalArgumentException5() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(4);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 2, 2, 2, 1, 1);
            gpkg.tiles().addTile(tileSet, tileMatrix, new RelativeTileCoordinate(0, -1, 0), new byte[] {1, 2, 3, 4});

            fail("Geopackage should throw a IllegalArgumentException when tile_column "
               + "is less than 0.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Geopackage throws an SQLException when opening a Geopackage since it does not contain the default tables
     * inside after bypassing the verifier.
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = SQLException.class)
    public void addTilesToGpkgAndAddTilesAndSetVerifyToFalse() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(37);
        testFile.createNewFile();

        try(GeoPackage gpkg = new GeoPackage(testFile, false, OpenMode.Open))
        {
            gpkg.tiles()
                .addTileSet("diff_tile_set",
                            "tile",
                            "desc",
                            new BoundingBox(1.0, 1.0, 1.0, 1.0),
                            gpkg.core().getSpatialReferenceSystem(4326));

            fail("The GeoPackage was expected to throw an SQLException due to no default tables inside the file.");

        }
        catch(final SQLException ex)
        {
            final String query = "SELECT table_name FROM gpkg_contents WHERE table_name = 'diff_tile_set';";

            try(Connection con           = this.getConnection(testFile.getAbsolutePath());
                Statement  stmt          = con.createStatement();
                ResultSet  tileTableName = stmt.executeQuery(query);)
            {
                assertTrue("The data should not be in the contents table since it throws an SQLException", tileTableName.getString("table_name") == null);
            }
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Test if the GeoPackage can successfully add non empty tiles to a GeoPackage  without throwing an error.
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void addNonEmptyTile() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileSet, 2, 2, 2, 2, 2, 2, 2);
            gpkg.tiles().addTile(tileSet, tileMatrix, new RelativeTileCoordinate(0, 0, 2), new byte[] {1, 2, 3, 4});
        }

        //use a query to test if the tile was inserted into database and to correct if the image is the same
        final String query = "SELECT tile_data FROM tileSetName WHERE zoom_level = 2 AND tile_column = 0 AND tile_row =0;";

        try(Connection con      = this.getConnection(testFile.getAbsolutePath());
            Statement  stmt     = con.createStatement();
            ResultSet  tileData = stmt.executeQuery(query);)
        {
            // assert the image was inputed into the file
            assertTrue("The GeoPackage did not successfully write the tile_data into the GeoPackage", tileData.next());
            final byte[] bytes = tileData.getBytes("tile_data");

            // compare images
            assertTrue("The GeoPackage tile_data does not match the tile_data of the one given", Arrays.equals(bytes, new byte[] {1, 2, 3, 4}));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage will throw an SQLException when adding
     * a duplicate tile to the GeoPackage.
     *
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws IllegalArgumentException
     * @throws FileNotFoundException
     */
    @Test(expected = SQLException.class)
    public void addDuplicateTiles() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, IllegalArgumentException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(13);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet    tileSet   = gpkg.tiles().addTileSet("tableName", "ident", "description", new BoundingBox(1.1,1.1,1.1,1.1), gpkg.core().getSpatialReferenceSystem(4326));
            final TileMatrix matrixSet = gpkg.tiles().addTileMatrix(tileSet, 1, 2, 2, 5, 5, 256, 256);
            //tile data
            final RelativeTileCoordinate coordinate = new RelativeTileCoordinate(0, 1, 1);
            final byte[] imageData = new byte[]{1, 2, 3, 4};
            //add tile twice
            gpkg.tiles().addTile(tileSet, matrixSet, coordinate, imageData);
            gpkg.tiles().addTile(tileSet, matrixSet, coordinate, imageData);//see if it will add the same tile twice

            fail("Expected GeoPackage to throw an SQLException due to a unique constraint violation (zoom level, tile column, and tile row)."
               + " Was able to add a duplicate tile.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage throws an IllegalArgumentException
     * when trying to add a tile with a parameter that is null (image data)
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addBadTile() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            //Tile coords
            final RelativeTileCoordinate coord1 = new RelativeTileCoordinate(0, 4, 4);
            //add tile to gpkg
            final TileMatrix tileMatrix1 = gpkg.tiles().addTileMatrix(tileSet, 4, 10, 10, 1, 1, 1.0, 1.0);
            gpkg.tiles().addTile(tileSet, tileMatrix1, coord1, null);

            fail("Expected the GeoPackage to throw an IllegalArgumentException when adding a null parameter to a Tile object (image data)");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage throws an IllegalArgumentException
     * when trying to add a tile with a parameter that is empty (image data)
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addBadTile2() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            //Tile coords
            final RelativeTileCoordinate coord1 = new RelativeTileCoordinate(0, 4, 4);
            //add tile to gpkg
            final TileMatrix tileMatrix1 = gpkg.tiles().addTileMatrix(tileSet, 4, 10, 10, 1, 1, 1.0, 1.0);
            gpkg.tiles().addTile(tileSet,tileMatrix1, coord1, new byte[]{});

            fail("Expected the GeoPackage to throw an IllegalArgumentException when adding an empty parameter to Tile (image data)");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage throws an IllegalArgumentException
     * when trying to add a tile with a parameter that is null (coordinate)
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addBadTile3() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
      //create tiles and file
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            //add tile to gpkg
            final TileMatrix tileMatrix1 = gpkg.tiles().addTileMatrix(tileSet, 4, 10, 10, 1, 1, 1.0, 1.0);
            gpkg.tiles().addTile(tileSet, tileMatrix1, (RelativeTileCoordinate)null, new byte[]{1,2,3,4});

            fail("Expected the GeoPackage to throw an IllegalArgumentException when adding a null parameter to a Tile object (coordinate)");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage throws an IllegalArgumentException
     * when trying to add a tile with a parameter that is null (tileMatrix)
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addBadTile4() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            //Tile coords
            final RelativeTileCoordinate coord1 = new RelativeTileCoordinate(0, 4, 4);
            //add tile to gpkg
            gpkg.tiles().addTile(tileSet, null, coord1, new byte[]{1,2,3,4});

            fail("Expected the GeoPackage to throw an IllegalArgumentException when adding a null parameter to a addTile method (tileMatrix)");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage get tile will retrieve the correct tile with get tile method.
     * @throws Exception
     */
    @Test
    public void getTile() throws Exception
    {
        //create tiles and file
        final File testFile = this.getRandomFile(6);
        final byte[] originalTile1 = new byte[] {1, 2, 3, 4};
        final byte[] originalTile2 = new byte[] {1, 2, 3, 4};

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {

            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            //Tile coords
            final RelativeTileCoordinate coord1 = new RelativeTileCoordinate(0, 4, 4);
            final RelativeTileCoordinate coord2 = new RelativeTileCoordinate(0, 8, 8);

            //add tile to gpkg
            final TileMatrix tileMatrix1 = gpkg.tiles().addTileMatrix(tileSet, 4, 10, 10, 1, 1, 1.0, 1.0);
            final TileMatrix tileMatrix2 = gpkg.tiles().addTileMatrix(tileSet, 8, 10, 10, 1, 1, 1.0, 1.0);

            gpkg.tiles().addTile(tileSet, tileMatrix1, coord1, originalTile1);
            gpkg.tiles().addTile(tileSet, tileMatrix2, coord2, originalTile2);

            //Retrieve tile from gpkg
            final Tile gpkgTile1 = gpkg.tiles().getTile(tileSet, coord1);
            final Tile gpkgTile2 = gpkg.tiles().getTile(tileSet, coord2);

            assertTrue("GeoPackage did not return the image expected when using getTile method.",
                       Arrays.equals(gpkgTile1.getImageData(), originalTile1));
            assertTrue("GeoPackage did not return the image expected when using getTile method.",
                       Arrays.equals(gpkgTile2.getImageData(), originalTile2));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage get tile will retrieve the correct tile with get tile method.
     * @throws Exception
     */
    @Test
    public void getTile2() throws Exception
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            //Tile coords
            final RelativeTileCoordinate coord1 = new RelativeTileCoordinate(0, 4, 4);

            //Retrieve tile from gpkg
            final Tile gpkgTile1 = gpkg.tiles().getTile(tileSet, coord1);

            assertTrue("GeoPackage did not null when the tile doesn't exist in the getTile method.",
                       gpkgTile1 == null);
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage get tile will retrieve the correct tile with get tile method.
     * @throws Exception
     */
    @Test
    public void getTile3() throws Exception
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            final TileMatrix tileMatrix = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 3, 4, 5, 6, 7);

            //Tile coords
            final RelativeTileCoordinate coord1 = new RelativeTileCoordinate(2, 1, 0);
            final byte[] imageData = new byte[]{1,2,3,4};

            //Retrieve tile from gpkg
            final Tile gpkgTileAdded    = gpkg.tiles().addTile(tileSet, tileMatrix, coord1, imageData);
            final Tile gpkgTileRecieved = gpkg.tiles().getTile(tileSet, coord1);

            assertTrue("GeoPackage did not return the same tile added to the gpkg.",
                       gpkgTileAdded.getColumn()                 == gpkgTileRecieved.getColumn()            &&
                       gpkgTileAdded.getRow()                    == gpkgTileRecieved.getRow()               &&
                       gpkgTileAdded.getIdentifier()             ==(gpkgTileRecieved.getIdentifier())       &&
                       gpkgTileAdded.getColumn()                 == gpkgTileRecieved.getColumn()            &&
                       gpkgTileAdded.getRow()                    == gpkgTileRecieved.getRow()               &&
                       gpkgTileAdded.getZoomLevel()              == gpkgTileRecieved.getZoomLevel()         &&
                       Arrays.equals(gpkgTileAdded.getImageData(), gpkgTileRecieved.getImageData()));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will return null when the tile being searched for does not exist.
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void getTileThatIsNotInGpkg() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(4);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    null,
                                                    null,
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            // add tile to gpkg
            gpkg.tiles().addTileMatrix(tileSet, 2, 2, 2, 2, 2, 2, 2);
            assertTrue("GeoPackage should have returned null for a missing tile.",
                       gpkg.tiles().getTile(tileSet, new RelativeTileCoordinate(0, 0, 0)) == null);
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
  
    /**
     * Tests if GeoPackage will throw an IllegalArgumentException when using getTile method with null value for table name.
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileWithNullTileEntrySet() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(5);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTile(null, new RelativeTileCoordinate(2, 2, 0));
            fail("GeoPackage did not throw an IllegalArgumentException when giving a null value to table name (using getTile method)");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an Illegal Argument Exception when given a null coordinate to getTile method.
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected= IllegalArgumentException.class)
    public void getTileWithRequestedTileNull() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(5);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles()
                .getTile(gpkg.tiles()
                             .addTileSet("name",
                                         "ident",
                                         "des",
                                         new BoundingBox(0.0,0.0,0.0,0.0),
                                         gpkg.core().getSpatialReferenceSystem(4326)),
                         (RelativeTileCoordinate)null);

            fail("GeoPackage did not throw an IllegalArgumentException when giving a null value to requested tile (using getTile method)");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException when a zoom level is out of range. (negative value)
     * @throws SQLException
     * @throws Exception
     */
    @Test (expected = IllegalArgumentException.class)
   public void getTileZoomLevelRangeIncorrect() throws SQLException, Exception
   {
       final File testFile = this.getRandomFile(5);

       try(GeoPackage gpkg = new GeoPackage(testFile))
       {
           gpkg.tiles()
               .getTile(gpkg.tiles()
                            .addTileSet("name",
                                        "ind",
                                        "des",
                                        new BoundingBox(0.0,0.0,0.0,0.0),
                                        gpkg.core().getSpatialReferenceSystem(4326)),
                        new RelativeTileCoordinate(2, 2, -3));

           fail("GeoPackage did not throw an IllegalArgumentException when giving a zoom level that is out of range (using getTile method)");
       }
       finally
       {
           if(testFile.exists())
           {
               if(!testFile.delete())
               {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
               }
           }
       }
   }
    
    /**
     * Tests if the GeoPackage will return the all and the correct zoom levels in a GeoPackage
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void getZoomLevels() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(6);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileEntry = gpkg.tiles()
                                          .addTileSet("tableName",
                                                      "ident",
                                                      "desc",
                                                      new BoundingBox(5.0,5.0,5.0,5.0),
                                                      gpkg.core().getSpatialReferenceSystem(4326));
           //add tile Matricies that represent zoom levels 0 and 12
           gpkg.tiles().addTileMatrix(tileEntry, 0, 10, 10, 1, 1, 1.0, 1.0);
           gpkg.tiles().addTileMatrix(tileEntry, 12, 5, 5, 6, 6, 7, 7);

           final Set<Integer> zooms  = gpkg.tiles().getTileZoomLevels(tileEntry);

           final ArrayList<Integer> expectedZooms = new ArrayList<>();

           expectedZooms.add(new Integer(12));
           expectedZooms.add(new Integer(0));

           for(final Integer zoom : zooms)
           {
               assertTrue("The GeoPackage's get zoom levels method did not return expected values.",
                          expectedZooms.stream()
                                       .anyMatch(currentZoom -> currentZoom.equals(zoom)));
           }
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw
     * an IllegalArgumentException when given
     * a TileSet null for getZoomLevels()
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void getZoomLevelsNullTileSetContentEntry() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(7);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileZoomLevels(null);
            fail("Expected the GeoPackage to throw an IllegalArgumentException when givinga null parameter to getTileZoomLevels");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

     /**
      * Tests if GeoPackage will throw an IllegalArgumentException
      * when giving a null parameter to getRowCount
      * @throws SQLException
      * @throws Exception
      */
    @Test(expected = IllegalArgumentException.class)
    public void getRowCountNullContentEntry() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(9);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.core().getRowCount(null);
            fail("GeoPackage should have thrown an IllegalArgumentException.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Verifies that the GeoPackage counts the correct number of rows
     * with the method getRowCount
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void getRowCountVerify() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(8);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "ident",
                                                    "desc",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));
            //create two TileMatricies to represent the tiles
            final TileMatrix tileMatrix1 = gpkg.tiles().addTileMatrix(tileSet, 1, 10, 10, 1, 1, 1.0, 1.0);
            final TileMatrix tileMatrix2 = gpkg.tiles().addTileMatrix(tileSet, 0, 10, 10, 1, 1, 1.0, 1.0);
            //add two tiles
            gpkg.tiles().addTile(tileSet, tileMatrix2, new RelativeTileCoordinate(0, 0, 0), new byte[] {1, 2, 3, 4});
            gpkg.tiles().addTile(tileSet, tileMatrix1, new RelativeTileCoordinate(0, 0, 1), new byte[] {1, 2, 3, 4});

            final long count = gpkg.core().getRowCount(tileSet);

            assertTrue(String.format("Expected a different value from GeoPackage on getRowCount. expected: 2 actual: %d", count),count == 2);
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException
     * when giving a null parameter to the method
     * getTileMatrixSetEntry();
     *
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileMatrixSetEntryNullTileSetContentEntry() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(7);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileMatrixSet(null);

            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a null parameter for TileSet");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if GeoPackage returns the expected tileMatrices using the getTIleMatrices(TileSet tileSet) method
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws IllegalArgumentException
     * @throws IOException
     */
    @Test
    public void getTileMatricesVerify() throws ClassNotFoundException, SQLException, ConformanceException, IllegalArgumentException, IOException
    {
        final File testFile = this.getRandomFile(5);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet    tileSet     = gpkg.tiles().addTileSet("tables", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            final TileMatrix tileMatrix  = gpkg.tiles().addTileMatrix(tileSet, 0, 3, 3, 4, 4, 5, 5);
            final TileMatrix tileMatrix2 = gpkg.tiles().addTileMatrix(tileSet, 3, 2, 2, 3, 3, 4, 4);

            gpkg.tiles().addTile(tileSet, tileMatrix, new RelativeTileCoordinate(0, 0, 0), createImageBytes());
            gpkg.tiles().addTile(tileSet, tileMatrix, new RelativeTileCoordinate(0, 1, 0), createImageBytes());

            final ArrayList<TileMatrix> expectedTileMatrix = new ArrayList<>();
            expectedTileMatrix.add(tileMatrix);
            expectedTileMatrix.add(tileMatrix2);

            final List<TileMatrix> gpkgTileMatrices = gpkg.tiles().getTileMatrices(tileSet);
            assertTrue("Expected the GeoPackage to return two Tile Matricies.",gpkgTileMatrices.size() == 2);

            for(final TileMatrix gpkgTileMatrix : gpkg.tiles().getTileMatrices(tileSet))
            {
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
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage will return null if no TileMatrix
     * Entries are found in the GeoPackage that matches the TileSet given.
     * @throws SQLException
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test
    public void getTileMatricesNonExistant() throws SQLException, FileAlreadyExistsException, ClassNotFoundException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(9);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
           final TileSet tileSet = gpkg.tiles()
                                       .addTileSet("tables",
                                                   "identifier",
                                                   "description",
                                                   new BoundingBox(0.0,0.0,0.0,0.0),
                                                   gpkg.core().getSpatialReferenceSystem(4326));

               assertTrue("Expected the GeoPackage to return null when no tile Matrices are found", gpkg.tiles().getTileMatrices(tileSet).size() == 0);
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when
     * giving a TileMatrix with a matrix width that is <=0
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            gpkg.tiles().addTileMatrix(tileSet, 0, 0, 5, 6, 7, 8, 9);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a matrix width that is <= 0");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when
     * giving a TileMatrix with a matrix height that is <=0
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException2() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("name",
                                                    "identifier",
                                                    "description",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(-1));

            gpkg.tiles().addTileMatrix(tileSet, 0, 4, 0, 6, 7, 8, 9);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a matrix height that is <= 0");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when
     * giving a TileMatrix with a tile width that is <=0
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException3() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0, 0.0, 0.0, 0.0), gpkg.core().getSpatialReferenceSystem(-1));
            gpkg.tiles().addTileMatrix(tileSet, 0, 4, 5, 0, 7, 8, 9);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a tile width that is <= 0");

        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when
     * giving a TileMatrix with a tile height that is <=0
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException4() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            gpkg.tiles().addTileMatrix(tileSet, 0, 4, 5, 6, 0, 8, 9);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a tile height that is <= 0");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when
     * giving a TileMatrix with a pixelXsize that is <=0
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException5() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            gpkg.tiles().addTileMatrix(tileSet, 0, 4, 5, 6, 7, 0, 9);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a pixelXsize that is <= 0");

        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException when
     * giving a TileMatrix with a pixelYSize that is <=0
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatricesIllegalArgumentException6() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));
            gpkg.tiles().addTileMatrix(tileSet, 0, 4, 5, 6, 7, 8, 0);
            fail("Expected GeoPackage to throw an IllegalArgumentException when giving a Tile Matrix a pixelYSize that is <= 0");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if a Geopackage Tiles would throw an IllegalArgumentException
     * when attempting to add a Tile Matrix corresponding to the same tile set and
     * zoom level but have differing other fields
     *
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatrixSameZoomDifferentOtherFields() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(13);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));

            gpkg.tiles().addTileMatrix(tileSet, 0, 2, 3, 4, 5, 6, 7);
            gpkg.tiles().addTileMatrix(tileSet, 0, 3, 2, 5, 4, 7, 6);
            fail("Expected GeoPackage Tiles to throw an IllegalArgumentException when addint a Tile Matrix with the same tile set and zoom level information but differing other fields");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage returns the same TileMatrix when trying
     * to add the same TileMatrix twice (verifies the values are the same)
     *
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test
    public void addTileMatrixTwiceVerify() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(13);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles().addTileSet("name", "identifier", "description", new BoundingBox(0.0,0.0,0.0,0.0), gpkg.core().getSpatialReferenceSystem(-1));

            final TileMatrix tileMatrix1 = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 3, 4, 5, 6, 7);
            final TileMatrix tileMatrix2 = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 3, 4, 5, 6, 7);

           assertTrue("Expected the GeoPackage to return the existing Tile Matrix.",tileMatrix1.equals(tileMatrix2.getTableName(),
                                                                                                       tileMatrix2.getZoomLevel(),
                                                                                                       tileMatrix2.getMatrixWidth(),
                                                                                                       tileMatrix2.getMatrixHeight(),
                                                                                                       tileMatrix2.getTileWidth(),
                                                                                                       tileMatrix2.getTileHeight(),
                                                                                                       tileMatrix2.getPixelXSize(),
                                                                                                       tileMatrix2.getPixelYSize()));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage returns the same TileMatrix when trying
     * to add the same TileMatrix twice (verifies the values are the same)
     *
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatrixNullTileSet() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(13);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().addTileMatrix(null, 0, 2, 3, 4, 5, 6, 7);
            fail("Expected the GeoPackage to throw an IllegalArgumentException when giving a null parameter TileSet to addTileMatrix");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException
     * when a user tries to add a negative value for zoom level 
     * (when adding a tile Matrix entry)
     * 
     * @throws SQLException
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws FileNotFoundException
     * @throws ConformanceException
     */
    @Test(expected = IllegalArgumentException.class)
    public void addTileMatrixWithNegativeZoomLevel() throws SQLException, FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, ConformanceException
    {
    	final File testFile = this.getRandomFile(12);
    	try(GeoPackage gpkg = new GeoPackage(testFile))
    	{
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(1.0,2.0,3.0,4.0), 
    												  gpkg.core().getSpatialReferenceSystem(0));
    		gpkg.tiles().addTileMatrix(tileSet, -1, 2, 4, 6, 8, 10, 12);
    	}
    	finally 
    	{
			if (testFile.exists()) 
			{
				if (!testFile.delete()) 
				{
					throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
				}
			}
		}
	}
    
    /**
     * Tests if given a non empty tile Matrix Metadata information can be added without throwing an error.
     *
     * @throws SQLException
     * @throws Exception
     */
    @Test
    public void addNonEmptyTileMatrix() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(5);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            //add information to gpkg
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tileSetName",
                                                    "title",
                                                    "tiles",
                                                    new BoundingBox(0.0, 0.0, 0.0, 0.0),
                                                    gpkg.core().getSpatialReferenceSystem(4326));

            gpkg.tiles().addTileMatrix(tileSet, 1, 1, 1, 1, 1, 1, 1);
        }
        //test if information added is accurate
        final String query = "SELECT table_name FROM gpkg_tile_matrix "
                             + "WHERE zoom_level = matrix_height = matrix_width = tile_width = tile_height = pixel_x_size = pixel_y_size = 1;";

        try(Connection con      = this.getConnection(testFile.getAbsolutePath());
            Statement stmt      = con.createStatement();
            ResultSet tableName = stmt.executeQuery(query);)
        {
            assertTrue("The GeoPackage did not enter the correct record into the gpkg_tile_matrix table", tableName.getString("table_name").equals("tileSetName"));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
     }

    /**
     * Tests if a GeoPackage will throw an IllegalArgumentException
     * when giving a null parameter to getTileMatrices
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileMatricesNullParameter() throws FileAlreadyExistsException, ClassNotFoundException
    , SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileMatrices(null);
            fail("Expected the GeoPackage to throw an IllegalArgumentException when giving getTileMatrices a TileSet that is null.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage getTIleMatrix can
     * retrieve the correct TileMatrix from the GeoPackage.
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test
    public void getTileMatrixVerify() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(6);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("tableName",
                                                    "identifier",
                                                    "description",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(-1));

            gpkg.tiles().addTileMatrix(tileSet, 1, 3, 5, 7, 9, 25, 27);
            final TileMatrix tileMatrix         = gpkg.tiles().addTileMatrix(tileSet, 0, 2, 4, 6, 8, 256, 512);
            final TileMatrix returnedTileMatrix = gpkg.tiles().getTileMatrix(tileSet, 0);

            assertTrue("GeoPackage did not return the TileMatrix expected", tileMatrix.getMatrixHeight() ==      returnedTileMatrix.getMatrixHeight() &&
                                                                            tileMatrix.getMatrixWidth()  ==      returnedTileMatrix.getMatrixWidth()  &&
                                                                            tileMatrix.getPixelXSize()   ==      returnedTileMatrix.getPixelXSize()   &&
                                                                            tileMatrix.getPixelYSize()   ==      returnedTileMatrix.getPixelYSize()   &&
                                                                            tileMatrix.getTableName()    .equals(returnedTileMatrix.getTableName())   &&
                                                                            tileMatrix.getTileHeight()   ==      returnedTileMatrix.getTileHeight()   &&
                                                                            tileMatrix.getTileWidth()    ==      returnedTileMatrix.getTileWidth()    &&
                                                                            tileMatrix.getZoomLevel()    ==      returnedTileMatrix.getZoomLevel());
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage returns null if the TileMatrix entry does not
     * exist in the GeoPackage file.
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test
    public void getTileMatrixNonExistant() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(8);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            final TileSet tileSet = gpkg.tiles()
                                        .addTileSet("TableName",
                                                    "identifier",
                                                    "description",
                                                    new BoundingBox(0.0,0.0,0.0,0.0),
                                                    gpkg.core().getSpatialReferenceSystem(-1));

            assertTrue("GeoPackage was supposed to return null when there is a nonexistant TileMatrix entry at that zoom level and TileSet",
                       null == gpkg.tiles().getTileMatrix(tileSet, 0));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }

    /**
     * Tests if the GeoPackage will throw an IllegalArgumentException
     * when giving a null parameter to getTileMatrix.
     *
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test(expected = IllegalArgumentException.class)
    public void getTileMatrixNullParameter() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(10);
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().getTileMatrix(null, 8);
            fail("GeoPackage should have thrown an IllegalArgumentException when giving a null parameter for TileSet in the method getTileMatrix");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if getTileMatrixSet retrieves the values that is expected
     *
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     * @throws FileNotFoundException
     */
    @Test
    public void getTileMatrixSetVerify() throws FileAlreadyExistsException, ClassNotFoundException, SQLException, ConformanceException, FileNotFoundException
    {
        final File testFile = this.getRandomFile(12);

        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            //values for tileMatrixSet
            final String                 tableName   = "tableName";
            final String                 identifier  = "identifier";
            final String                 description = "description";
            final BoundingBox            bBox        = new BoundingBox(1.0, 2.0, 3.0, 4.0);
            final SpatialReferenceSystem srs         = gpkg.core().getSpatialReferenceSystem(4326);
            //add tileSet and tileMatrixSet to gpkg
            final TileSet       tileSet       = gpkg.tiles().addTileSet(tableName, identifier, description, bBox, srs);
            final TileMatrixSet tileMatrixSet = gpkg.tiles().getTileMatrixSet(tileSet);

            assertTrue("Expected different values from getTileMatrixSet for SpatialReferenceSystem or BoundingBox or TableName.",
                                               tileMatrixSet.getBoundingBox()           .equals(bBox) &&
                                               tileMatrixSet.getSpatialReferenceSystem().equals(srs) &&
                                               tileMatrixSet.getTableName()             .equals(tableName));
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage will throw a GeoPackage Conformance Exception
     * when given a GeoPackage that violates a requirement with a severity equal
     * to Error
     * @throws SQLException
     * @throws Exception
     */
    @Test(expected = ConformanceException.class)
    public void geoPackageConformanceException() throws SQLException, Exception
    {
        final File testFile = this.getRandomFile(19);
        testFile.createNewFile();
        try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Open))
        {
            fail("GeoPackage did not throw a geoPackageConformanceException as expected.");
        }
        finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Geodetic crsCoordinate to a relative tile coordinate
     * 
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws FileNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     */
    @Test
    public void crsToRelativeTileCoordinateUpperRightGeodetic() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 1;
    	CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
    	CrsTileCoordinate crsCoord = new CrsTileCoordinate(32.423521, -45.234567, zoomLevel, geodeticRefSys);//upper right tile
    	
    	File testFile = this.getRandomFile(8);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(0.0, -180.0, 85.0511287798066, 0.0), 
    												  gpkg.core().getSpatialReferenceSystem(4326));
    		int matrixWidth = 2;
    		int matrixHeight = 2;
    		int pixelXSize = 256;
    		int pixelYSize = 256;
    		
    		gpkg.tiles().addTileMatrix(tileSet, 
					   zoomLevel, 
					   matrixWidth, 
					   matrixHeight, 
					   pixelXSize, 
					   pixelYSize, 
					   (tileSet.getBoundingBox().getWidth()/matrixWidth)/pixelXSize, 
					   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
    		
    		RelativeTileCoordinate relativeCoord  = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsCoord);
    		
    		assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
    								   + "\nExpected Row: 0, Expected Column: 1. \nActual Row: %d, Actual Column: %d", relativeCoord.getRow(), relativeCoord.getColumn()),
                       relativeCoord.getRow() == 0 && relativeCoord.getColumn() == 1);
    		
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Geodetic crsCoordinate to a relative tile coordinate
     * 
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws FileNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     */
    @Test
    public void crsToRelativeTileCoordinateUpperLeftGeodetic() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 1;
    	CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
    	CrsTileCoordinate crsCoord = new CrsTileCoordinate(0, -180, zoomLevel, geodeticRefSys);//upper left tile
    	
    	File testFile = this.getRandomFile(8);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(0.0, -180.0, 85.0511287798066, 0.0), 
    												  gpkg.core().getSpatialReferenceSystem(4326));
    		int matrixWidth = 2;
    		int matrixHeight = 2;
    		int pixelXSize = 256;
    		int pixelYSize = 256;
    		
    		gpkg.tiles().addTileMatrix(tileSet, 
					   zoomLevel, 
					   matrixWidth, 
					   matrixHeight, 
					   pixelXSize, 
					   pixelYSize, 
					   (tileSet.getBoundingBox().getWidth()/matrixWidth)/pixelXSize, 
					   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
    		
    		RelativeTileCoordinate relativeCoord  = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsCoord);
    		
    		assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
    								   + "\nExpected Row: 0, Expected Column: 0. \nActual Row: %d, Actual Column: %d", relativeCoord.getRow(), relativeCoord.getColumn()),
                       relativeCoord.getRow() == 0 && relativeCoord.getColumn() == 0);
    		
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Geodetic crsCoordinate to a relative tile coordinate
     * 
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws FileNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     */
    @Test
    public void crsToRelativeTileCoordinateLowerLeftGeodetic() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 1;
    	CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
    	CrsTileCoordinate crsCoord = new CrsTileCoordinate(49, -90, zoomLevel, geodeticRefSys);//lower left tile
    	
    	File testFile = this.getRandomFile(8);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(0.0, -180.0, 85.0511287798066, 0.0), 
    												  gpkg.core().getSpatialReferenceSystem(4326));
    		int matrixWidth = 2;
    		int matrixHeight = 2;
    		int pixelXSize = 256;
    		int pixelYSize = 256;
    		
    		gpkg.tiles().addTileMatrix(tileSet, 
									   zoomLevel, 
									   matrixWidth, 
									   matrixHeight, 
									   pixelXSize, 
									   pixelYSize, 
									   (tileSet.getBoundingBox().getWidth() /matrixWidth )/pixelXSize, 
									   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
    		
    		RelativeTileCoordinate relativeCoord  = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsCoord);
    		
    		assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
    								   + "\nExpected Row: 1, Expected Column: 0. \nActual Row: %d, Actual Column: %d", relativeCoord.getRow(), relativeCoord.getColumn()),
                       relativeCoord.getRow() == 1 && relativeCoord.getColumn() == 0);
    		
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Geodetic crsCoordinate to a relative tile coordinate
     * 
     * @throws FileAlreadyExistsException
     * @throws ClassNotFoundException
     * @throws FileNotFoundException
     * @throws SQLException
     * @throws ConformanceException
     */
    @Test
    public void crsToRelativeTileCoordinateLowerRightGeodetic() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 1;
    	CoordinateReferenceSystem geodeticRefSys = new CoordinateReferenceSystem("EPSG",4326);
    	CrsTileCoordinate crsCoord = new CrsTileCoordinate(80, 0, zoomLevel, geodeticRefSys);//lower right tile
    	
    	File testFile = this.getRandomFile(8);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(0.0, -180.0, 85.0511287798066, 0.0), 
    												  gpkg.core().getSpatialReferenceSystem(4326));
    		int matrixWidth = 2;
    		int matrixHeight = 2;
    		int pixelXSize = 256;
    		int pixelYSize = 256;
    		
    		gpkg.tiles().addTileMatrix(tileSet, 
    								   zoomLevel, 
    								   matrixWidth, 
    								   matrixHeight, 
    								   pixelXSize, 
    								   pixelYSize, 
    								   (tileSet.getBoundingBox().getWidth()/matrixWidth)/pixelXSize, 
    								   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
    		
    		RelativeTileCoordinate relativeCoord  = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsCoord);
    		
    		assertTrue(String.format("The crsToRelativeTileCoordinate did not return the expected values. "
    								   + "\nExpected Row: 1, Expected Column: 1. \nActual Row: %d, Actual Column: %d", relativeCoord.getRow(), relativeCoord.getColumn()),
                       relativeCoord.getRow() == 1 && relativeCoord.getColumn() == 1);
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Global Mercator crsCoordinate to a relative tile coordinate
     * 
     * @throws ConformanceException 
     * @throws SQLException 
     * @throws FileNotFoundException 
     * @throws ClassNotFoundException 
     * @throws FileAlreadyExistsException 
     * @throws IOException
     */
    @Test
    public void crsToRelativeTileCoordinateUpperLeftGlobalMercator() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 6;
    	
    	CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
    	Coordinate<Double>        coordInMeters    = LatLongConversions.latLongToMeters(-43, -45);
    	CrsTileCoordinate         crsMercatorCoord = new CrsTileCoordinate(coordInMeters.getY(), coordInMeters.getX(), zoomLevel, globalMercator);
    	
    	File testFile = this.getRandomFile(9);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		Coordinate<Double> minBoundingBoxCoord = LatLongConversions.latLongToMeters(-60.0, -90.0);
    		Coordinate<Double> maxBoundingBoxCoord = LatLongConversions.latLongToMeters( 10.0,   5.0);
    		
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(minBoundingBoxCoord.getY(), minBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX()), 
    												  gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator", 
    														  							    3395, 
    														  							    "EPSG", 
    														  							    3395, 
    														  							    "definition", 
    														  							    "description"));
    		
    		int matrixWidth = 2;
    		int matrixHeight = 2;
    		int pixelXSize = 256;
    		int pixelYSize = 256;
    		
    		gpkg.tiles().addTileMatrix(tileSet, 
    								   zoomLevel, 
    								   matrixWidth, 
    								   matrixHeight, 
    								   pixelXSize, 
    								   pixelYSize, 
    								   (tileSet.getBoundingBox().getWidth() /matrixWidth )/pixelXSize, 
    								   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
    		
    		RelativeTileCoordinate relativeCoord = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsMercatorCoord);
    		
    		assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordiante.  "
    								+ "	\nExpected Row: 0, Expected Column: 0.\nActual Row: %d, Actual Column: %d.",
    								relativeCoord.getRow(), 
    								relativeCoord.getColumn()),
    					relativeCoord.getRow() == 0 && relativeCoord.getColumn() == 0);
    		
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    	
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Global Mercator crsCoordinate to a relative tile coordinate
     * 
     * @throws ConformanceException 
     * @throws SQLException 
     * @throws FileNotFoundException 
     * @throws ClassNotFoundException 
     * @throws FileAlreadyExistsException 
     * @throws IOException
     */
    @Test
    public void crsToRelativeTileCoordinateUpperRightGlobalMercator() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 6;
    	
    	CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
    	Coordinate<Double>        coordInMeters    = LatLongConversions.latLongToMeters(-40, -42);
    	CrsTileCoordinate         crsMercatorCoord = new CrsTileCoordinate(coordInMeters.getY(), coordInMeters.getX(), zoomLevel, globalMercator);
    	
    	File testFile = this.getRandomFile(9);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		Coordinate<Double> minBoundingBoxCoord = LatLongConversions.latLongToMeters(-60.0, -90.0);
    		Coordinate<Double> maxBoundingBoxCoord = LatLongConversions.latLongToMeters( 10.0,   5.0);
    		
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(minBoundingBoxCoord.getY(), minBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX()), 
    												  gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator", 
    														  							    3395, 
    														  							    "EPSG", 
    														  							    3395, 
    														  							    "definition", 
    														  							    "description"));
			int matrixWidth = 2;
			int matrixHeight = 2;
			int pixelXSize = 256;
			int pixelYSize = 256;
			
			gpkg.tiles().addTileMatrix(tileSet, 
									   zoomLevel, 
									   matrixWidth, 
									   matrixHeight, 
									   pixelXSize, 
									   pixelYSize, 
									   (tileSet.getBoundingBox().getWidth()/matrixWidth)/pixelXSize, 
									   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
			
			RelativeTileCoordinate relativeCoord = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsMercatorCoord);
    		
    		assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordiante.  "
    								+ "	\nExpected Row: 0, Expected Column: 1.\nActual Row: %d, Actual Column: %d.",
    								relativeCoord.getRow(), 
    								relativeCoord.getColumn()),
    					relativeCoord.getRow() == 0 && relativeCoord.getColumn() == 1);
    		
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    	
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Global Mercator crsCoordinate to a relative tile coordinate
     * 
     * @throws ConformanceException 
     * @throws SQLException 
     * @throws FileNotFoundException 
     * @throws ClassNotFoundException 
     * @throws FileAlreadyExistsException 
     * @throws IOException
     */
    @Test
    public void crsToRelativeTileCoordinateLowerLeftGlobalMercator() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 6;
    	
    	CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
    	Coordinate<Double>        coordInMeters    = LatLongConversions.latLongToMeters(9, -47);
    	CrsTileCoordinate         crsMercatorCoord = new CrsTileCoordinate(coordInMeters.getY(), coordInMeters.getX(), zoomLevel, globalMercator);
    	
    	File testFile = this.getRandomFile(9);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		Coordinate<Double> minBoundingBoxCoord = LatLongConversions.latLongToMeters(-60.0, -90.0);
    		Coordinate<Double> maxBoundingBoxCoord = LatLongConversions.latLongToMeters( 10.0,   5.0);
    		
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(minBoundingBoxCoord.getY(), minBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX()), 
    												  gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator", 
    														  							    3395, 
    														  							    "EPSG", 
    														  							    3395, 
    														  							    "definition", 
    														  							    "description"));
			int matrixWidth = 2;
			int matrixHeight = 2;
			int pixelXSize = 256;
			int pixelYSize = 256;
			
			gpkg.tiles().addTileMatrix(tileSet, 
									   zoomLevel, 
									   matrixWidth, 
									   matrixHeight, 
									   pixelXSize, 
									   pixelYSize, 
									   (tileSet.getBoundingBox().getWidth()/matrixWidth)/pixelXSize, 
									   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
    		
    		RelativeTileCoordinate relativeCoord = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsMercatorCoord);
    		
    		assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordiante.  "
    								+ "	\nExpected Row: 1, Expected Column: 0.\nActual Row: %d, Actual Column: %d.",
    								relativeCoord.getRow(), 
    								relativeCoord.getColumn()),
    					relativeCoord.getRow() == 1 && relativeCoord.getColumn() == 0);
    		
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }
    
    /**
     * Tests if the GeoPackage can convert an
     * Global Mercator crsCoordinate to a relative tile coordinate
     * 
     * @throws ConformanceException 
     * @throws SQLException 
     * @throws FileNotFoundException 
     * @throws ClassNotFoundException 
     * @throws FileAlreadyExistsException 
     * @throws IOException
     */
    @Test
    public void crsToRelativeTileCoordinateLowerRightGlobalMercator() throws FileAlreadyExistsException, ClassNotFoundException, FileNotFoundException, SQLException, ConformanceException
    {
    	int zoomLevel = 6;
    	
    	CoordinateReferenceSystem globalMercator   = new CoordinateReferenceSystem("EPSG", 3395);
    	Coordinate<Double> 		  coordInMeters    = LatLongConversions.latLongToMeters(9,5);
    	CrsTileCoordinate 		  crsMercatorCoord = new CrsTileCoordinate(coordInMeters.getY(), coordInMeters.getX(), zoomLevel, globalMercator);
    	
    	File testFile = this.getRandomFile(9);
    	
    	try(GeoPackage gpkg = new GeoPackage(testFile, OpenMode.Create))
    	{
    		Coordinate<Double> minBoundingBoxCoord = LatLongConversions.latLongToMeters(-60.0, -90.0);
    		Coordinate<Double> maxBoundingBoxCoord = LatLongConversions.latLongToMeters( 10.0,   5.0);
    		
    		TileSet tileSet = gpkg.tiles().addTileSet("tableName", 
    												  "identifier", 
    												  "description", 
    												  new BoundingBox(minBoundingBoxCoord.getY(), minBoundingBoxCoord.getX(), maxBoundingBoxCoord.getY(), maxBoundingBoxCoord.getX()), 
    												  gpkg.core().addSpatialReferenceSystem("EPSG/World Mercator", 
    														  							    3395, 
    														  							    "EPSG", 
    														  							    3395, 
    														  							    "definition", 
    														  							    "description"));
			int matrixWidth = 2;
			int matrixHeight = 2;
			int pixelXSize = 256;
			int pixelYSize = 256;
			
			gpkg.tiles().addTileMatrix(tileSet, 
									   zoomLevel, 
									   matrixWidth, 
									   matrixHeight, 
									   pixelXSize, 
									   pixelYSize, 
									   (tileSet.getBoundingBox().getWidth()/matrixWidth)/pixelXSize, 
									   (tileSet.getBoundingBox().getHeight()/matrixHeight)/pixelYSize);
    		
    		RelativeTileCoordinate relativeCoord = gpkg.tiles().srsToRelativeTileCoordinate(tileSet, crsMercatorCoord);
    		
    		assertTrue(String.format("The GeoPackage did not return the expected row and column from the conversion crs to relative tile coordiante.  "
    								+ "	\nExpected Row: 1, Expected Column: 1.\nActual Row: %d, Actual Column: %d.",
    								relativeCoord.getRow(), 
    								relativeCoord.getColumn()),
    					relativeCoord.getRow() == 1 && relativeCoord.getColumn() == 1);
    		
    	}
    	finally
        {
            if(testFile.exists())
            {
                if(!testFile.delete())
                {
                    throw new RuntimeException(String.format("Unable to delete testFile. testFile: %s", testFile));
                }
            }
        }
    }


    private static byte[] createImageBytes() throws IOException
    {
        final BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            ImageIO.write(img, "PNG", outputStream);

            return outputStream.toByteArray();
        }
    }

    private String getRanString(final int length)
    {
        final String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(this.randomGenerator.nextInt(characters.length()));
        }
        return new String(text);
    }

    private File getRandomFile(final int length)
    {
        File testFile;

        do
        {
            testFile = new File(String.format(FileSystems.getDefault().getPath(this.getRanString(length)).toString() + ".gpkg"));
        }
        while (testFile.exists());

        return testFile;
    }
   
    private Connection getConnection(final String filePath) throws Exception
    {
        Class.forName("org.sqlite.JDBC"); // Register the driver

        return DriverManager.getConnection("jdbc:sqlite:" + filePath);
    }
}