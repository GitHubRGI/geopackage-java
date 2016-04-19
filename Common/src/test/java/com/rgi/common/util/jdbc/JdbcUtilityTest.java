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

package com.rgi.common.util.jdbc;

import com.mockrunner.mock.jdbc.MockConnection;
import com.rgi.common.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.Consumer;
import static java.nio.file.StandardCopyOption.*;

import static org.junit.Assert.*;

/**
 *
 * Created by justin.rhee on 10/29/2015.
 */
public class JdbcUtilityTest
{

    private static final String TEST_TABLE_NAME = "tiles";
    private final File gpkgFile = new File(ClassLoader.getSystemResource("testNetwork.gpkg").getFile());
    private final Random randomGenerator = new Random();
    private List<ExtensionData> gpkgExtensionsDataAndColumnName;

    @Before
    public void setUp() throws IOException
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (final ClassNotFoundException exception)
        {
            // Could not register driver
            throw new IOException(exception);
        }
    }

    public static Connection getConnection(final File gpkgFile) throws IOException
    {
        try
        {
            return DriverManager.getConnection("jdbc:sqlite:" + gpkgFile.getAbsolutePath());
        }
        catch (final SQLException exception)
        {
            System.out.println("Could not get DB instance.");
            throw new IOException(exception);
        }
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the databaseConnection is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void selectOneNullConectionTest() throws SQLException
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        final boolean result = JdbcUtility.selectOne(null, str,
                                                     preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                     resultSet -> resultSet.getInt(1)) > 0;
        fail("selectOne should have thrown an IllegalArgumentException for a null Connection.");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the String str is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void selectOneNullStringTest() throws Exception
    {
        final Connection con = new MockConnection();
        final boolean result = JdbcUtility.selectOne(con,
                                                     null,
                                                     preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                     resultSet -> resultSet.getInt(1)) > 0;
        fail("selectOne should have thrown an IllegalArgumentException for a null or empty String.");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the preparedStatementConsumer is null
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalArgumentException.class)
    public void selectOneNullResultSetFunctionTest() throws Exception 
    {
        final Connection con = new MockConnection();
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        final boolean result = JdbcUtility.selectOne(con,
                                                     str,
                                                     preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                     null);
        fail("selectOne should have thrown an IllegalArgumentException for a null resultMapper.");
    }

    /**
     * Tests to only run if the string is
     * a sql statement
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void selectOneTryStatementTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "This is not a sql command";
        JdbcUtility.selectOne(con,
                              str,
                              preparedStatement -> preparedStatement.setString(1, "tiles"),
                              resultSet -> resultSet.getInt(1));
        fail("selectOne should have a sql command in the String str");
    }

    /**
     * Tests if a connection is made and a value is returned
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void selectOneTryStatementPassTest() throws Exception
    {
        final Connection con = getConnection(this.gpkgFile);
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ?;";
        final String result = JdbcUtility.selectOne(con,
                                                    str,
                                                    preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                    resultSet -> resultSet.getString(1));
        con.close();
        assertNotNull("Result returned null when it should have returned a value", result);
    }

    /**
     * Tests if a MockConnection will return a null value
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void selectOneTryStatementNullTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ?;";
        final String result = JdbcUtility.selectOne(con,
                                                    str,
                                                    preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                    resultSet -> resultSet.getString(1));
        assertNull("Result should return null", result);
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the Connection is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void selectNullConnectionTest() throws SQLException
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        JdbcUtility.select(null,
                           str,
                           preparedStatement -> preparedStatement.setString(1, "tiles"),
                           resultSet -> resultSet.getInt(1));
        fail("select should have thrown an IllegalArgumentException for a null Connection.");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * whhen the String is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void selectNullStringTest() throws Exception
    {
        final Connection con = new MockConnection();
        JdbcUtility.select(con,
                           null,
                           preparedStatement -> preparedStatement.setString(1, "tiles"),
                           resultSet -> resultSet.getInt(1));
        fail("select should have thrown an IllegalArgumentException for a null or empty String.");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the resultSetFunction is null
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalArgumentException.class)
    public void selectNullResultSetFunctionTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        JdbcUtility.select(con,
                           str,
                           preparedStatement -> preparedStatement.setString(1, "tiles"),
                           null);
        fail("select should have thrown an IllegalArgumentException for a null resultMapper.");
    }

    /**
     * Tests if select will return a list with the size of 1
     * when connected to the gpkgFile
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void selectTryStatementPassTest() throws Exception
    {
        final Connection con = getConnection(this.gpkgFile);
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ?;";
        final List result = JdbcUtility.select(con,
                                               str,
                                               preparedStatement -> preparedStatement.setString(1, "tiles"),
                                               resultSet -> resultSet.getString(1));
        con.close();
        assertEquals("result should return a size of 1", 1, result.size());
    }

    /**
     * Tests if select will return an empty list if
     * connected to a MockConnection
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void selectTryStatementNullTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ?;";
        final List result = JdbcUtility.select(con,
                                               str,
                                               preparedStatement -> preparedStatement.setString(1, "tiles"),
                                               resultSet -> resultSet.getString(1));
        assertTrue("result should return a size of 0", result.isEmpty());
    }

    /**
     * tests if an IllegalArgumentException is thrown
     * when the connection is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void forEachNullConnectionTest() throws Exception
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        JdbcUtility.forEach(null,
                            str,
                            preparedStatement -> preparedStatement.setString(1, "tiles"),
                            resultSet -> resultSet.getInt(1));
        fail("select should have thrown an IllegalArgumentException for a null Connection.");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the String is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void forEachNullStringTest() throws Exception
    {
        final Connection con = new MockConnection();
        JdbcUtility.forEach(con,
                            null,
                            preparedStatement -> preparedStatement.setString(1, "tiles"),
                            resultSet -> resultSet.getInt(1));
        fail("forEach should have thrown an IllegalArgumentException for a null String.");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when ResultSetConsumer is null
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalArgumentException.class)
    public void forEachNullResultSetConsumerFunctionTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        JdbcUtility.forEach(con,
                            str,
                            preparedStatement -> preparedStatement.setString(1, "tiles"),
                            null);
        fail("forEach should have thrown an IllegalArgumentException for a null resultMapper.");
    }

    /**
     * Tests when the forEach function passes and should
     * return a non-empty list
     * @throws Exception
     */
    @Test
    public void forEachTryStatementPassTest() throws Exception
    {
        final List<Integer> list = new ArrayList<>();
        final boolean result = runConsumer(list, list::add);
        assertTrue("Result List should have items in it after running the forEach method", result);
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the Connection is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void update1NullDatabaseConnectionTest() throws Exception
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        JdbcUtility.update(null, str);
        fail("update should have thrown an IllegalArgumentException for a null connection");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the String is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void update1NullStringTest() throws Exception
    {
        final Connection con = new MockConnection();
        JdbcUtility.update(con, null);
        fail("update should have thrown an IllegalArgumentException for a null string");
    }

    /**
     * Runs the function with a String that is not a sql statement
     * and should return an Assertion error
     * @throws Exception
     */
    @Test(expected = AssertionError.class)
    public void update1PreparedStatementCatchTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "Hello! what is up my mans.";
        JdbcUtility.update(con, str);
        fail("update should return a sql string");
    }

    /**
     * runs the update function with a string
     * that is a sql statement
     * @throws Exception
     */
    @Test
     public void update1tryStatementTest() throws Exception
     {
        final List<Integer> list = new ArrayList<>();
        final boolean result = runConsumerUpdate(list, list::add);
        assertFalse("Result List should have items in it after running the forEach method", result);
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the Connection is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void update2NullConnectionTest() throws Exception
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        JdbcUtility.update(null,
                           str,
                           preparedStatement -> preparedStatement.setString(1, "tiles"));
        fail("update should have thrown an IllegalArgumentException for a null connection");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the String is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void update2NullStringTest() throws Exception
    {
        final Connection con = new MockConnection();
        JdbcUtility.update(con,
                           null,
                           preparedStatement -> preparedStatement.setString(1, "tiles"));
        fail("update should have thrown an IllegalArgumentException for a null String");
    }

    /**
     * Tests if the function properly updates the File
     * and sets the preparedStatementConsumer values
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void update2TryStatementPassTest() throws Exception
    {
        final FileSystem system = FileSystems.getDefault();
        File toReplace = this.getRandomFile(8);
        File Original = new File(ClassLoader.getSystemResource("testNetwork_orig.gpkg").getFile());
        final Path file = toReplace.toPath();
        final Path originalFile = Original.toPath();
        Connection con = null;
        try
        {
            Files.copy(originalFile, file, REPLACE_EXISTING);
            con = getConnection(this.gpkgFile);
            // this was moved below setting the pragmas because is starts a transaction and causes setPragmaSynchronousOff to throw an exception
            con.setAutoCommit(false);
            final String insert = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                                                "alaska2",
                                                "from_node",
                                                "to_node");
            JdbcUtility.update(con,
                               insert,
                               preparedStatement ->
                               {
                                   preparedStatement.setInt(1, 1);
                                   preparedStatement.setInt(2, 2);
                               });

            assertNotNull("runs the function properly", file);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (con != null)
            {
                con.close();
            }
            Files.copy(originalFile, file, REPLACE_EXISTING);
            toReplace.delete();
        }
    }

    /**
     * This tests if an IllegalArgumentException is thrown
     * if the connection is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void update3NullConnectionTest() throws Exception
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        final boolean result = JdbcUtility.update(null,
                                                  str,
                                                  preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                  resultSet -> resultSet.getInt(1)) > 0;
        fail("update should have thrown an IllegalArgumentException for a null Connection");
    }

    /**
     * This tests if an IllegalArgumentException is thrown
     * if the String is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void update3NullStringTest() throws Exception
    {
        final Connection con = new MockConnection();
        final boolean result = JdbcUtility.update(con,
                                                  null,
                                                  preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                  resultSet -> resultSet.getInt(1)) > 0;
        fail("update should have thrown an IllegalArgumentException for a null Connection");
    }

    /**
     * This tests if an IllegalArgumentException is thrown
     * if ResultSetFunction is null
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void update3NullKeyMapperTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        final boolean result = JdbcUtility.update(con,
                                                  str,
                                                  preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                  null);
        fail("update should have thrown an illegalArgumentException for a null resultMapper");
    }

    /**
     * Tests if the function returns null when the connection
     * is connected to a MockConnection
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test(expected = SQLException.class)
    public void update3TryStatementNullTest() throws Exception
    {
        final Connection con = new MockConnection();
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ?;";
        final String result = JdbcUtility.update(con,
                                                 str,
                                                 preparedStatement -> preparedStatement.setString(1, "tiles"),
                                                 resultSet -> resultSet.getString(1));
        assertNull("Result should return null", result);
    }

    /**
     * Tests if the function runs with the proper connection
     * and the preparedStatement updates the database
     * @throws Exception
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void update3TryStatementPassTest() throws Exception
    {
        final FileSystem system = FileSystems.getDefault();
        File toReplace = this.getRandomFile(8);
        File Original = new File(ClassLoader.getSystemResource("testNetwork_orig.gpkg").getFile());
        final Path file = toReplace.toPath();
        final Path originalFile = Original.toPath();
        Connection con = null;
        try
        {
            Files.copy(originalFile, file, REPLACE_EXISTING);
            con = getConnection(this.gpkgFile);
            // this was moved below setting the pragmas because is starts a transaction and causes setPragmaSynchronousOff to throw an exception
            con.setAutoCommit(false);
            final String insert = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                                                "alaska2",
                                                "from_node",
                                                "to_node");
            final int identifier = JdbcUtility.update(con,
                                                      insert,
                                                      preparedStatement ->
                                                      {
                                                          preparedStatement.setInt(1, 1);
                                                          preparedStatement.setInt(2, 2);
                                                      },
                                                      resultSet -> resultSet.getInt(1));
            assertTrue("Result should be a non negative, non-zero integer", identifier > 0);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (con != null)
            {
                con.close();
            }
            Files.copy(originalFile, file, REPLACE_EXISTING);
            toReplace.delete();
        }
    }

    //this portion tests the fourth update function block
    /**
     * Tests if an IllegalArgumentException is thrown
     * when the connection is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void update4NullConnectionTest() throws SQLException
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        final Iterable<Pair<Integer, Integer>> edges;
        edges = Arrays.asList(new Pair<>(12, 23),
                              new Pair<>(12, 42),
                              new Pair<>(34, 56));
        JdbcUtility.update(null,
                           str,
                           edges,
                           (preparedStatement, edge) ->
                           {
                               preparedStatement.setInt(1, edge.getLeft());
                               preparedStatement.setInt(2, edge.getRight());
                           });
        fail("update should have thrown an illegalArgumentException for a null connection");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the String is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void update4NullStringTest() throws SQLException
    {
        final Connection con = new MockConnection();
        final Iterable<Pair<Integer, Integer>> edges;
        edges = Arrays.asList(new Pair<>(12, 23),
                              new Pair<>(12, 42),
                              new Pair<>(34, 56));
        JdbcUtility.update(con,
                           null,
                           edges,
                           (preparedStatement, edge) ->
                           {
                               preparedStatement.setInt(1, edge.getLeft());
                               preparedStatement.setInt(2, edge.getRight());
                           });
        fail("update should have thrown an illegalArgumentException for a null connection");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the Iterable is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void update4NullIterableTest() throws SQLException
    {
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;";
        final Connection con = new MockConnection();
        JdbcUtility.update(con,
                           str,
                           null,
                           (preparedStatement, edge) ->
                           {
                               preparedStatement.setInt(1, Integer.parseInt("tiles"));
                               preparedStatement.setInt(2, Integer.parseInt("tiles"));
                           });
        fail("update should have thrown an illegalArgumentException for a null Iterable");
    }

    /**
     * runs the update function with a string
     * that is a sql statement
     * @throws Exception
     */
    @Test
    public void update4tryStatementTest() throws Exception
    {
        final List<Integer> list = new ArrayList<>();
        final boolean result = runConsumerUpdate4(list, list::add);
        assertFalse("Result List should have items in it after running the forEach method", result);
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the resultSet is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void map2ResultSetNullTest() throws SQLException
    {
        final String str = "SELECT table_name FROM %s WHERE data_type = 'tiles';";
        JdbcUtility.map(null,
                        resultSet -> resultSet.getString("table_name"),
                        HashSet<String>::new);
        fail("map should have thrown an IllegalArgumentException for a null resultSet");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the ResultSetFunction is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void map2ResultSetFunctionNullTest() throws SQLException
    {
        final String str = "SELECT table_name FROM %s WHERE data_type = 'tiles';";
        final Connection con = new MockConnection();
        try (final Statement createStmt2 = con.createStatement();
             final ResultSet contentsPyramidTables = createStmt2.executeQuery(str))
        {
            JdbcUtility.map(contentsPyramidTables,
                            null,
                            HashSet<String>::new);
            fail("map should have thrown an IllegalArgumentException for a null resultSetFunction");
        }
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the CollectionFactory is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void map2NullCollectionFactoryTest() throws SQLException
    {
        final String str = "SELECT table_name FROM %s WHERE data_type = 'tiles';";
        final Connection con = new MockConnection();
        try (final Statement createStmt2 = con.createStatement();
             final ResultSet contentsPyramidTables = createStmt2.executeQuery(str))
        {
            JdbcUtility.map(contentsPyramidTables,
                            resultSet -> resultSet.getString("table_name"),
                            null);
            fail("map should have thrown an IllegalArgumentException for a null collectionFactory");
        }
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when the ResultSet is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void mapFilterNullResultSetTest() throws SQLException
    {
        final Connection con = new MockConnection();
        JdbcUtility.mapFilter(null,
                              resultSet -> resultSet.getString("table_name"),
                              pyramidName -> JdbcUtilityTest.tableOrViewExists(con, pyramidName),
                              HashSet<String>::new);
        fail("mapFilter should have thrown an IllegalArgumentException for a null resultSet");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when ResultSetFunction is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void mapFilterResultSetTest() throws SQLException
    {
        final String str = "SELECT DISTINCT table_name FROM %s;";
        final Connection con = new MockConnection();
        try (Statement createStmt3 = con.createStatement();
             ResultSet tileMatrixPyramidTables = createStmt3.executeQuery(str))
        {
            JdbcUtility.mapFilter(tileMatrixPyramidTables,
                                  null,
                                  pyramidName -> JdbcUtilityTest.tableOrViewExists(con, pyramidName),
                                  HashSet<String>::new);
            fail("mapFilter should have thrown an IllegalArgumentException for a null funciton");
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean tableOrViewExists(final Connection connection, final String name) throws SQLException
    {
        final Connection con = new MockConnection();
        JdbcUtilityTest.verify(connection);

        if (name == null || name.isEmpty())
        {
            throw new IllegalArgumentException("Table/view name cannot be null or empty");
        }
        return JdbcUtility.selectOne(connection,
                "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ? LIMIT 1;",
                preparedStatement -> preparedStatement.setString(1, name),
                resultSet -> resultSet.getInt(1)) > 0;
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when JdbcPredicate is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void mapFilterNullpredicateTest() throws SQLException
    {
        final String str = "SELECT DISTINCT table_name FROM %s;";
        final Connection con = new MockConnection();
        try (Statement createStmt3 = con.createStatement();
             ResultSet tileMatrixPyramidTables = createStmt3.executeQuery(str))
        {
            JdbcUtility.mapFilter(tileMatrixPyramidTables,
                                  resultSet -> resultSet.getString("table_name"),
                                  null,
                                  HashSet<String>::new);
            fail("mapFilter should have thrown an IllegalArgumentException for a null predicate");
        }
    }

    /**
     * Tests if an the function returns a collection after
     * executing with a proper string
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void mapFilterFunctionTest() throws SQLException
    {
        final String str = "SELECT DISTINCT table_name FROM %s;";
        final Connection con = new MockConnection();
        try (Statement createStmt3 = con.createStatement();
             ResultSet tileMatrixPyramidTables = createStmt3.executeQuery(str))
        {
            final Set<String> pyramidTablesInTileMatrix;
            pyramidTablesInTileMatrix = JdbcUtility.mapFilter(tileMatrixPyramidTables,
                                                              resultSet -> resultSet.getString("table_name"),
                                                              pyramidName -> JdbcUtilityTest.tableOrViewExists(con, pyramidName),
                                                              HashSet<String>::new);
            assertNotNull("mapFilter should have a set", pyramidTablesInTileMatrix);
        }
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when ResultSet is null
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void getObjectsNullResultSetTest() throws SQLException
    {
        final Connection con = new MockConnection();
        JdbcUtility.getObjects(null, 1, 3);
        fail("getObjects should have thrown an IllegalArgumentException for a null ResultSet");
    }

    /**
     * Tests if an IllegalArgumentException is thrown
     * when startColumnIndex is greater than EndColumnIndex
     * @throws SQLException
     */
    @Test(expected = IllegalArgumentException.class)
    public void getObjectsEndLessThanStartTest() throws SQLException
    {
        final String str = "SELECT tile_data FROM tileSetName WHERE zoom_level = 2 AND tile_column = 0 AND tile_row =0;";
        try (final Connection con = new MockConnection();
             final Statement stmt = con.createStatement();
             final ResultSet tileData = stmt.executeQuery(str))
        {
            JdbcUtility.getObjects(tileData, 3, 1);
            fail("getObjects should have thrown an IllegalArgumentException where endColumn is greater than startIndex");
        }
    }

    private Set<String> pyramidTablesInContents;

    private static final class ExtensionData
    {
        private ExtensionData(final String tableName,
                              final String columnName,
                              final String extensionName)
        {
            this.tableName = tableName;
            this.columnName = columnName;
            this.extensionName = extensionName;
        }

        private final String tableName;
        private final String columnName;
        private final String extensionName;
    }

    private static void verify(final Connection connection) throws SQLException
    {
        if (connection == null || connection.isClosed())
        {
            throw new IllegalArgumentException("The connection cannot be null or closed.");
        }
    }

    private File getRandomFile(final int length)
    {
        File testFile;
        do
        {
            testFile = new File(FileSystems.getDefault().getPath(this.getRandomString(length)) + ".gpkg");
        }
        while (testFile.exists());
        return testFile;
    }

    private String getRandomString(final int length)
    {
        final String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(this.randomGenerator.nextInt(characters.length()));
        }
        return new String(text);
    }

    private boolean runConsumer(List<Integer> list, Consumer<Integer> consumer) throws Exception
    {
        final Connection con = getConnection(this.gpkgFile);
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ?;";
        JdbcUtility.forEach(con,
                            str,
                            preparedStatement -> preparedStatement.setString(1, "tiles"),
                            resultSet -> consumer.accept(resultSet.getInt(1)));
        con.close();
        return !list.isEmpty();
    }

    private boolean runConsumerUpdate4(List<Integer> list, Consumer<Integer> consumer) throws Exception
    {
        final FileSystem system = FileSystems.getDefault();
        File toReplace = this.getRandomFile(8);
        File Original = new File(ClassLoader.getSystemResource("testNetwork_orig.gpkg").getFile());
        final Path file = toReplace.toPath();
        final Path originalFile = Original.toPath();
        final String str = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                                         "alaska2",
                                         "from_node",
                                         "to_node");
        final Connection con = getConnection(this.gpkgFile);
        con.setAutoCommit(false);
        final Iterable<Pair<Integer, Integer>> edges;
        edges = Arrays.asList(new Pair<>(12, 23),
                              new Pair<>(12, 42),
                              new Pair<>(34, 56));
        JdbcUtility.update(con,
                           str,
                           edges,
                           (preparedStatement, edge) ->
                           {
                               preparedStatement.setInt(1, edge.getLeft());
                               preparedStatement.setInt(2, edge.getRight());
                           });
        con.close();
        toReplace.delete();
        return !list.isEmpty();
    }

    private boolean runConsumerUpdate(List<Integer> list, Consumer<Integer> consumer) throws Exception
    {
        final Connection con = getConnection(this.gpkgFile);
        final String str = "SELECT COUNT(*) FROM sqlite_master WHERE (type = 'table' OR type = 'view') AND name = ?;";
        JdbcUtility.update(con, str);
        con.close();
        return !list.isEmpty();
    }
}