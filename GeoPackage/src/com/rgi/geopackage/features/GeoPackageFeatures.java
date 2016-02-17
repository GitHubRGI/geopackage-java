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

package com.rgi.geopackage.features;

import com.rgi.common.BoundingBox;
import com.rgi.common.Pair;
import com.rgi.common.util.jdbc.JdbcUtility;
import com.rgi.geopackage.core.ContentFactory;
import com.rgi.geopackage.core.GeoPackageCore;
import com.rgi.geopackage.core.SpatialReferenceSystem;
import com.rgi.geopackage.features.geometry.Geometry;
import com.rgi.geopackage.features.geometry.GeometryFactory;
import com.rgi.geopackage.features.geometry.WktGeometryCollection;
import com.rgi.geopackage.features.geometry.WktLineString;
import com.rgi.geopackage.features.geometry.WktMultiLineString;
import com.rgi.geopackage.features.geometry.WktMultiPoint;
import com.rgi.geopackage.features.geometry.WktMultiPolygon;
import com.rgi.geopackage.features.geometry.WktPoint;
import com.rgi.geopackage.features.geometry.WktPolygon;
import com.rgi.geopackage.utility.DatabaseUtility;
import com.rgi.geopackage.verification.VerificationIssue;
import com.rgi.geopackage.verification.VerificationLevel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Luke Lambert
 *
 */
public class GeoPackageFeatures
{

    /**
     * Constructor
     *
     * @param databaseConnection
     *             The open connection to the database that contains a GeoPackage
     * @param core
     *             Access to GeoPackage's "core" methods
     */
    @SuppressWarnings("MagicNumber")
    public GeoPackageFeatures(final Connection databaseConnection, final GeoPackageCore core)
    {
        this.databaseConnection = databaseConnection;
        this.core               = core;

        this.geometryFactories.put(       GeometryType.Geometry       .getCode(), bytes -> { throw new WellKnownBinaryFormatException("Cannot instantiate abstract 'Geometry' type (geometry type code 0)"); } );      // type 0 xy
        this.geometryFactories.put(1000 + GeometryType.Geometry       .getCode(), bytes -> { throw new WellKnownBinaryFormatException("Cannot instantiate abstract 'Geometry' type (geometry type code 1000)"); } );   // type 0 xyz
        this.geometryFactories.put(2000 + GeometryType.Geometry       .getCode(), bytes -> { throw new WellKnownBinaryFormatException("Cannot instantiate abstract 'Geometry' type (geometry type code 2000)"); } );   // type 0 xym
        this.geometryFactories.put(3000 + GeometryType.Geometry       .getCode(), bytes -> { throw new WellKnownBinaryFormatException("Cannot instantiate abstract 'Geometry' type (geometry type code 3000)"); } );   // type 0 xyzm
        this.geometryFactories.put(GeometryType.Point          .getCode(), WktPoint::readWellKnownBinary);  // type 1 xy
        //this.geometryFactories.put(1000 + GeometryType.Point          .getCode(), WktPoint::readWellKnownBinary);  // type 1 xyz
        //this.geometryFactories.put(2000 + GeometryType.Point          .getCode(), WktPoint::readWellKnownBinary);  // type 1 xym
        //this.geometryFactories.put(3000 + GeometryType.Point          .getCode(), WktPoint::readWellKnownBinary);  // type 1 xyzm
        this.geometryFactories.put(GeometryType.LineString     .getCode(), WktLineString::readWellKnownBinary);  // type 2 xy
        //this.geometryFactories.put(1000 + GeometryType.LineString     .getCode(), WktLineString::readWellKnownBinary);  // type 2 xyz
        //this.geometryFactories.put(2000 + GeometryType.LineString     .getCode(), WktLineString::readWellKnownBinary);  // type 2 xym
        //this.geometryFactories.put(3000 + GeometryType.LineString     .getCode(), WktLineString::readWellKnownBinary);  // type 2 xyzm
        this.geometryFactories.put(GeometryType.Polygon        .getCode(), WktPolygon::readWellKnownBinary);  // type 3 xy
        //this.geometryFactories.put(1000 + GeometryType.Polygon        .getCode(), WktPolygon::readWellKnownBinary);  // type 3 xyz
        //this.geometryFactories.put(2000 + GeometryType.Polygon        .getCode(), WktPolygon::readWellKnownBinary);  // type 3 xym
        //this.geometryFactories.put(3000 + GeometryType.Polygon        .getCode(), WktPolygon::readWellKnownBinary);  // type 3 xyzm
        this.geometryFactories.put(GeometryType.MultiPoint     .getCode(), WktMultiPoint::readWellKnownBinary);  // type 4 xy
        //this.geometryFactories.put(1000 + GeometryType.MultiPoint     .getCode(), WktMultiPoint::readWellKnownBinary);  // type 4 xyz
        //this.geometryFactories.put(2000 + GeometryType.MultiPoint     .getCode(), WktMultiPoint::readWellKnownBinary);  // type 4 xym
        //this.geometryFactories.put(3000 + GeometryType.MultiPoint     .getCode(), WktMultiPoint::readWellKnownBinary);  // type 4 xyzm
        this.geometryFactories.put(GeometryType.MultiLineString.getCode(), WktMultiLineString::readWellKnownBinary);  // type 5 xy
        //this.geometryFactories.put(1000 + GeometryType.MultiLineString.getCode(), WktMultiLineString::readWellKnownBinary);  // type 5 xyz
        //this.geometryFactories.put(2000 + GeometryType.MultiLineString.getCode(), WktMultiLineString::readWellKnownBinary);  // type 5 xym
        //this.geometryFactories.put(3000 + GeometryType.MultiLineString.getCode(), WktMultiLineString::readWellKnownBinary);  // type 5 xyzm
        this.geometryFactories.put(GeometryType.MultiPolygon   .getCode(), WktMultiPolygon::readWellKnownBinary);  // type 6 xy
        //this.geometryFactories.put(1000 + GeometryType.MultiPolygon   .getCode(), WktMultiPolygon::readWellKnownBinary);  // type 6 xyz
        //this.geometryFactories.put(2000 + GeometryType.MultiPolygon   .getCode(), WktMultiPolygon::readWellKnownBinary);  // type 6 xym
        //this.geometryFactories.put(3000 + GeometryType.MultiPolygon   .getCode(), WktMultiPolygon::readWellKnownBinary);  // type 6 xyzm

        this.geometryFactories.put(GeometryType.GeometryCollection.getCode(), (byteBuffer) -> WktGeometryCollection.readWellKnownBinary(this::createGeometry, byteBuffer));  // type 7 xy
        //this.geometryFactories.put(1000 + GeometryType.GeometryCollection.getCode(), (byteBuffer) -> WktGeometryCollection.readWellKnownBinary(this::createGeometry, byteBuffer));  // type 7 xyz
        //this.geometryFactories.put(2000 + GeometryType.GeometryCollection.getCode(), (byteBuffer) -> WktGeometryCollection.readWellKnownBinary(this::createGeometry, byteBuffer));  // type 7 xym
        //this.geometryFactories.put(3000 + GeometryType.GeometryCollection.getCode(), (byteBuffer) -> WktGeometryCollection.readWellKnownBinary(this::createGeometry, byteBuffer));  // type 7 xyzm
    }

    /**
     * @param verificationLevel
     *             Controls the level of verification testing performed
     * @return the Feature GeoPackage requirements this GeoPackage fails to conform to
     */
    public Collection<VerificationIssue> getVerificationIssues(final VerificationLevel verificationLevel)
    {
        return new FeaturesVerifier(this.databaseConnection, verificationLevel).getVerificationIssues();
    }

    /**
     * Creates a user defined features table, and adds a corresponding entry to
     * the content table
     *
     * @param tableName
     *             The name of the features table. The table name must begin
     *             with a letter (A..Z, a..z) or an underscore (_) and may only
     *             be followed by letters, underscores, or numbers, and may not
     *             begin with the prefix "gpkg_"
     * @param identifier
     *             A human-readable identifier (e.g. short name) for the
     *             tableName content
     * @param description
     *             A human-readable description for the tableName content
     * @param boundingBox
     *             Bounding box for all content in tableName
     * @param spatialReferenceSystem
     *             Spatial Reference System (SRS)
     * @param geometryColumn
     *             Geometry column definition
     * @param columnDefinitions
     *             Definitions of non-geometry columns
     * @return Returns a newly created user defined features table
     * @throws SQLException
     *             throws if the method {@link #getFeatureSet(String)
     *             getFeatureSet} or the method {@link
     *             DatabaseUtility#tableOrViewExists(Connection, String)
     *             tableOrViewExists} or if the database cannot roll back the
     *             changes after a different exception throws will throw an
     *             SQLException
     */
    public FeatureSet addFeatureSet(final String                   tableName,
                                    final String                   identifier,
                                    final String                   description,
                                    final BoundingBox              boundingBox,
                                    final SpatialReferenceSystem   spatialReferenceSystem,
                                    final GeometryColumnDefinition geometryColumn,
                                    final ColumnDefinition...      columnDefinitions) throws SQLException
    {
        return this.addFeatureSet(tableName,
                                  identifier,
                                  description,
                                  boundingBox,
                                  spatialReferenceSystem,
                                  geometryColumn,
                                  Arrays.asList(columnDefinitions));
    }

    /**
     * Creates a user defined features table, and adds a corresponding entry to
     * the content table
     *
     * @param tableName
     *             The name of the features table. The table name must begin
     *             with a letter (A..Z, a..z) or an underscore (_) and may only
     *             be followed by letters, underscores, or numbers, and may not
     *             begin with the prefix "gpkg_"
     * @param identifier
     *             A human-readable identifier (e.g. short name) for the
     *             tableName content
     * @param description
     *             A human-readable description for the tableName content
     * @param boundingBox
     *             Bounding box for all content in tableName
     * @param spatialReferenceSystem
     *             Spatial Reference System (SRS)
     * @param geometryColumn
     *             Geometry column definition
     * @param columnDefinitions
     *             Definitions of non-geometry columns
     * @return Returns a newly created user defined features table
     * @throws SQLException
     *             throws if the method {@link #getFeatureSet(String)
     *             getFeatureSet} or the method {@link
     *             DatabaseUtility#tableOrViewExists(Connection, String)
     *             tableOrViewExists} or if the database cannot roll back the
     *             changes after a different exception throws will throw an
     *             SQLException
     */
    public FeatureSet addFeatureSet(final String                       tableName,
                                    final String                       identifier,
                                    final String                       description,
                                    final BoundingBox                  boundingBox,
                                    final SpatialReferenceSystem       spatialReferenceSystem,
                                    final GeometryColumnDefinition     geometryColumn,
                                    final Collection<ColumnDefinition> columnDefinitions) throws SQLException
    {
        GeoPackageCore.validateNewContentTableName(tableName);

        if(boundingBox == null)
        {
            throw new IllegalArgumentException("Bounding box cannot be mull.");
        }

        if(spatialReferenceSystem == null)
        {
            throw new IllegalArgumentException("Spatial reference system may not be null");
        }

        if(geometryColumn == null)
        {
            throw new IllegalArgumentException("Geometry column definition name may not be null");
        }

        if(columnDefinitions == null || Arrays.asList(columnDefinitions).stream().anyMatch(definition -> definition == null))
        {
            throw new IllegalArgumentException("Column definitions may not be null");
        }

        final FeatureSet existingContent = this.getFeatureSet(tableName);

        if(existingContent != null)
        {
            if(existingContent.equals(tableName,
                                      FeatureSet.FeatureContentType,
                                      identifier,
                                      description,
                                      boundingBox.getMinX(),
                                      boundingBox.getMaxX(),
                                      boundingBox.getMinY(),
                                      boundingBox.getMaxY(),
                                      spatialReferenceSystem.getIdentifier()))
            {
                return existingContent;
            }

            throw new IllegalArgumentException("An entry in the content table already exists with this table name, but has different values for its other fields");
        }

        if(DatabaseUtility.tableOrViewExists(this.databaseConnection, tableName))
        {
            throw new IllegalArgumentException("A table already exists with this feature set's table name");
        }

        try
        {
            // Create the feature set table
            this.addFeatureTableNoCommit(tableName,
                                         geometryColumn,
                                         columnDefinitions);

            // Add feature set to the content table
            this.core.addContent(tableName,
                                 FeatureSet.FeatureContentType,
                                 identifier,
                                 description,
                                 boundingBox,
                                 spatialReferenceSystem);

            this.addGeometryColumnNoCommit(tableName,
                                           geometryColumn,
                                           spatialReferenceSystem.getIdentifier());

            this.databaseConnection.commit();

            return this.getFeatureSet(tableName);
        }
        catch(final Exception ex)
        {
            this.databaseConnection.rollback();
            throw ex;
        }
    }

    /**
     * Gets a feature set object based on its table name
     *
     * @param featureSetTableName
     *            Name of a feature set table
     * @return Returns a {@link FeatureSet} or null if there isn't with the
     *            supplied table name
     * @throws SQLException
     *             throws if the method
     *             {@link GeoPackageCore#getContent(String, ContentFactory, SpatialReferenceSystem)}
     *             throws an SQLException
     */
    public FeatureSet getFeatureSet(final String featureSetTableName) throws SQLException
    {
        final String geometryColumnQuery = String.format("SELECT %s, %s, %s, %s, %s FROM %s WHERE %s = ?",
                                                         "column_name",
                                                         "geometry_type_name",
                                                         "srs_id",
                                                         "z",
                                                         "m",
                                                         GeoPackageFeatures.GeometryColumnsTableName,
                                                         "table_name");

        try(final PreparedStatement preparedStatement = this.databaseConnection.prepareStatement(geometryColumnQuery))
        {
            preparedStatement.setString(1, featureSetTableName);

            try(final ResultSet resultSet = preparedStatement.executeQuery())
            {
                resultSet.next();

                final String           geometryColumnName  = resultSet.getString(1);
                final String           geometryColumnType  = resultSet.getString(2);
                final int              geometryColumnSrsId = resultSet.getInt   (3);
                final ValueRequirement zValueRequirement   = ValueRequirement.fromInt(resultSet.getInt(4));
                final ValueRequirement mValueRequirement   = ValueRequirement.fromInt(resultSet.getInt(5));

                try(final Statement statement = GeoPackageFeatures.this.databaseConnection.createStatement())
                {
                    try(final ResultSet tableInfo = statement.executeQuery(String.format("PRAGMA table_info(%s)", featureSetTableName)))
                    {
                        final List<Column> columns = new ArrayList<>();

                        while(tableInfo.next())
                        {
                            final String name = tableInfo.getString("name");
                            final String type = tableInfo.getString("type");

                            final EnumSet<ColumnFlag> flags = EnumSet.noneOf(ColumnFlag.class);

                            if(tableInfo.getBoolean("notnull"))
                            {
                                flags.add(ColumnFlag.NotNull);
                            }

                            if(tableInfo.getBoolean("pk"))
                            {
                                flags.add(ColumnFlag.PrimaryKey);
                            }
                        }
                    }
                }
            }
        }

        return this.core.getContent(featureSetTableName,
                                    (tableName,
                                     dataType,
                                     identifier,
                                     description,
                                     lastChange,
                                     minimumX,
                                     maximumX,
                                     minimumY,
                                     maximumY,
                                     spatialReferenceSystem) -> new FeatureSet(tableName,
                                                                               identifier,
                                                                               description,
                                                                               lastChange,
                                                                               minimumX,
                                                                               maximumX,
                                                                               minimumY,
                                                                               maximumY,
                                                                               spatialReferenceSystem));
    }

    /**
     * Gets all entries in the GeoPackage's contents table with the "features"
     * data_type
     *
     * @return Returns a collection of {@link FeatureSet}s
     * @throws SQLException
     *             throws if the method
     *             {@link #getFeatureSets(SpatialReferenceSystem) getFeatureSets}
     *             throws
     */
    public Collection<FeatureSet> getFeatureSets() throws SQLException
    {
        return this.getFeatureSets(null);
    }

    /**
     * Gets all entries in the GeoPackage's contents table with the "features"
     * data_type that also match the supplied spatial reference system
     *
     * @param matchingSpatialReferenceSystem
     *            Spatial reference system that returned {@link FeatureSet}s
     *            much refer to
     * @return Returns a collection of {@link FeatureSet}s
     * @throws SQLException
     *             if there's an SQL error
     */
    public Collection<FeatureSet> getFeatureSets(final SpatialReferenceSystem matchingSpatialReferenceSystem) throws SQLException
    {
        return this.core.getContent(FeatureSet.FeatureContentType,
                                    (tableName,
                                     dataType,
                                     identifier,
                                     description,
                                     lastChange,
                                     minimumX,
                                     maximumX,
                                     minimumY,
                                     maximumY,
                                     spatialReferenceSystemIdentifier) -> new FeatureSet(tableName,
                                                                                         identifier,
                                                                                         description,
                                                                                         lastChange,
                                                                                         minimumX,
                                                                                         maximumX,
                                                                                         minimumY,
                                                                                         maximumY,
                                                                                         spatialReferenceSystemIdentifier),
                                    matchingSpatialReferenceSystem);
    }

    /**
     * Gets a {@link Feature} given a geometry column and feature identifier
     *
     * @param geometryColumn
     *             Geometry column definition
     * @param featureIdentifier
     *             Identifier for a feature
     * @return a {@link Feature}
     * @throws SQLException
     *             if there is a database error
     */
    public Feature getFeature(final GeometryColumn geometryColumn,
                              final int            featureIdentifier) throws SQLException, WellKnownBinaryFormatException
    {
        if(geometryColumn == null)
        {
            throw new IllegalArgumentException("Geometry column may not be null");
        }

        final List<String> schema = this.getSchema(geometryColumn); // Feature table's columns name with "id" listed first, the geometry column second, and the rest in the order returned by SQL.

        schema.remove(0);   // Remove "id", we've already got that value

        final String featureQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                                                  String.join(", ", schema),
                                                  GeoPackageFeatures.GeometryColumnsTableName,
                                                  "id");

        final Pair<Map<String, Object>, byte[]> feature = JdbcUtility.selectOne(this.databaseConnection,
                                                                                featureQuery,
                                                                                preparedStatement -> preparedStatement.setInt(1, featureIdentifier),
                                                                                resultSet -> { final Map<String, Object> attributes = new HashMap<>();

                                                                                               schema.remove(0); // Ignore the geometry column

                                                                                               for(final String columnName : schema)
                                                                                               {
                                                                                                   attributes.put(columnName, resultSet.getObject(columnName));
                                                                                               }

                                                                                               return Pair.of(attributes,
                                                                                                              resultSet.getBytes(geometryColumn.getColumnName()));
                                                                                             });
        if(feature == null)
        {
            throw new IllegalArgumentException(String.format("No feature exists for geometry column %s.%s and identifier %d",
                                                             geometryColumn.getTableName(),
                                                             geometryColumn.getColumnName(),
                                                             featureIdentifier));
        }


        return new Feature(featureIdentifier,
                           this.createGeometry(feature.getRight()),
                           feature.getLeft());
    }

    public List<Feature> getFeatures(final GeometryColumn geometryColumn) throws SQLException, WellKnownBinaryFormatException
    {
        if(geometryColumn == null)
        {
            throw new IllegalArgumentException("Geometry column may not be null");
        }

        final List<String> schema = this.getSchema(geometryColumn); // Feature table's columns name with "id" listed first, the geometry column second, and the rest in the order returned by SQL.

        final String featureQuery = String.format("SELECT %s FROM %s",
                                                  String.join(", ", schema),
                                                  GeoPackageFeatures.GeometryColumnsTableName);

        schema.remove(0);   // Remove, "id". We'll later refer to it by name.
        schema.remove(0);   // Remove the geometry column name. We'll also refer to it by name.

        try(final Statement statement = this.databaseConnection.createStatement())
        {
            try(final ResultSet resultSet = statement.executeQuery(featureQuery))
            {
                final List<Feature> results = new ArrayList<>();

                while(resultSet.next())
                {
                    final Map<String, Object> attributes = new HashMap<>();

                    for(final String columnName : schema)
                    {
                        attributes.put(columnName, resultSet.getObject(columnName));
                    }

                    results.add(new Feature(resultSet.getInt("id"),
                                            this.createGeometry(resultSet.getBytes(geometryColumn.getColumnName())),
                                            attributes));
                }

                return results;
            }
        }
    }

    public void visitFeatures(final GeometryColumn geometryColumn, final Consumer<Feature> featureConsumer) throws SQLException, WellKnownBinaryFormatException
    {
        if(geometryColumn == null)
        {
            throw new IllegalArgumentException("Geometry column may not be null");
        }

        if(featureConsumer == null)
        {
            throw new IllegalArgumentException("Feature consumer may not be null");
        }

        final List<String> schema = this.getSchema(geometryColumn); // Feature table's columns name with "id" listed first, the geometry column second, and the rest in the order returned by SQL.

        final String featureQuery = String.format("SELECT %s FROM %s",
                                                  String.join(", ", schema),
                                                  GeoPackageFeatures.GeometryColumnsTableName);

        schema.remove(0);   // Remove, "id". We'll later refer to it by name.
        schema.remove(0);   // Remove the geometry column name. We'll also refer to it by name.


        try(final PreparedStatement preparedStatement = this.databaseConnection.prepareStatement(featureQuery))
        {
            try(final ResultSet resultSet = preparedStatement.executeQuery())
            {
                while(resultSet.next())
                {
                    final Map<String, Object> attributes = new HashMap<>();

                    for(final String columnName : schema)
                    {
                        attributes.put(columnName, resultSet.getObject(columnName));
                    }

                    featureConsumer.accept(new Feature(resultSet.getInt("id"),
                                                       this.createGeometry(resultSet.getBytes(geometryColumn.getColumnName())),
                                                       attributes));
                }
            }
        }
    }

    public Feature addFeature(final GeometryColumn      geometryColumn,
                              final Geometry            geometry,
                              final Map<String, Object> attributes) throws SQLException
    {
        if(geometryColumn == null)
        {
            throw new IllegalArgumentException("Geometry column may not be null");
        }

        if(geometry == null)
        {
            throw new IllegalArgumentException("Geometry may not be null");
        }

        if(attributes == null)
        {
            throw new IllegalAccessError("Attributes may not be null");
        }

        if(!geometryColumn.getGeometryType()
                          .toUpperCase()
                          .equals(geometry.getGeometryTypeName()))
        {
            throw new IllegalArgumentException("Geometry column may only contain geometries of type " + geometryColumn.getGeometryType().toUpperCase());
        }

        verifyValueRequirements(geometryColumn, geometry);

        final List<String> columnNames = new LinkedList<>(attributes.keySet());

        columnNames.add(0, geometryColumn.getColumnName());

        final String insertFeatureSql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                                                      geometryColumn.getTableName(),
                                                      String.join(", ", columnNames),
                                                      String.join(", ", Collections.nCopies(columnNames.size(), "?")));

        final int identifier = JdbcUtility.update(this.databaseConnection,
                                                  insertFeatureSql,
                                                  preparedStatement -> { int parameterIndex = 1;

                                                                         try
                                                                         {
                                                                             final byte[] bytes = createBlob(geometry, geometryColumn.getSpatialReferenceSystemIdentifier());
                                                                             preparedStatement.setBytes(parameterIndex++, bytes);
                                                                         }
                                                                         catch(final IOException e)
                                                                         {
                                                                             throw new RuntimeException(e);
                                                                         }

                                                                         columnNames.remove(0);    // Skip the geometry column

                                                                         for(final String columnName : columnNames)
                                                                         {
                                                                             preparedStatement.setObject(parameterIndex++, attributes.get(columnName)); // TODO Map index instead of iterating over the KVPs due to order iteration concerns. Looking up values might be slow.
                                                                         }
                                                                        },
                                                  resultSet -> resultSet.getInt(1));    // New feature identifier

        this.databaseConnection.commit();

        return new Feature(identifier,
                           geometry,
                           attributes);
    }

    public void addFeatures(final GeometryColumn                         geometryColumn,
                            final Set<String>                            columns,
                            final Iterable<Pair<Geometry, List<Object>>> features) throws SQLException
    {
        if(geometryColumn == null)
        {
            throw new IllegalArgumentException("Geometry column may not be null");
        }

        if(columns == null)
        {
            throw new IllegalArgumentException("Columns may not be null");
        }

        if(features == null)
        {
            throw new IllegalArgumentException("Values may not be null");
        }

        features.forEach(feature -> { final Geometry geometry = feature.getLeft();

                                      if(!geometryColumn.getGeometryType()
                                                        .toUpperCase()
                                                        .equals(geometry.getGeometryTypeName()))
                                      {
                                          throw new IllegalArgumentException("Geometry column may only contain geometries of type " + geometryColumn.getGeometryType().toUpperCase());
                                      }

                                      verifyValueRequirements(geometryColumn, geometry);
                                    });

        final List<String> columnNames = new LinkedList<>(columns);

        columnNames.add(0, geometryColumn.getColumnName());

        final int columnCount = columnNames.size();

        final String insertFeatureSql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                                                      geometryColumn.getTableName(),
                                                      String.join(", ", columnNames),
                                                      String.join(", ", Collections.nCopies(columnNames.size(), "?")));

        JdbcUtility.update(this.databaseConnection,
                           insertFeatureSql,
                           features,
                           (preparedStatement, feature) -> { final Geometry     geometry   = feature.getLeft();
                                                             final List<Object> attributes = feature.getRight();

                                                             try
                                                             {
                                                                 preparedStatement.setBytes(1, createBlob(geometry, geometryColumn.getSpatialReferenceSystemIdentifier()));
                                                             }
                                                             catch(final IOException ex)
                                                             {
                                                                 throw new RuntimeException(ex);
                                                             }

                                                             for(int parameterIndex = 2; parameterIndex <= columnCount; ++parameterIndex)
                                                             {
                                                                 preparedStatement.setObject(parameterIndex, attributes.get(parameterIndex-1));
                                                             }
                                                           });

        this.databaseConnection.commit();
    }

    public void registerGeometryFactory(final long            geometryTypeCode,
                                        final GeometryFactory geometryFactory)
    {
        if(geometryTypeCode > maxUnsignedIntValue)
        {
            throw new IllegalArgumentException("Type code must be between 0 and 2^32 - 1 (range of a 32 bit unsigned integer)");
        }

        if(this.geometryFactories.containsKey(geometryTypeCode))    // TODO do we really want to prohibit this?
        {
            throw new IllegalArgumentException("A geometry factory already exists for this geometry type code");
        }

        if(geometryFactory == null)
        {
            throw new IllegalArgumentException("Geometry factory may not be null");
        }

        this.geometryFactories.put(geometryTypeCode, geometryFactory);
    }

    /**
     * Creates the Geometry Column table
     * <br>
     * <br>
     * <b>**WARNING**</b> this does not do a database commit. It is expected
     * that this transaction will always be paired with others that need to be
     * committed or roll back as a single transaction.
     *
     * @throws SQLException
     */
    protected void createGeometryColumnTableNoCommit() throws SQLException
    {
        // Create the geometry column table or view
        if(!DatabaseUtility.tableOrViewExists(this.databaseConnection, GeoPackageFeatures.GeometryColumnsTableName))
        {
            JdbcUtility.update(this.databaseConnection, getGeometryColumnsCreationSql());
        }
    }

    /**
     * Creates an entry in the gpkg_geometry_columns table
     * <br>
     * <br>
     * <b>**WARNING**</b> this does not do a database commit. It is expected
     * that this transaction will always be paired with others that need to be
     * committed or roll back as a single transaction.
     *
     * @param tableName
     *            The name of the features table
     * @param spatialReferenceSystemIdentifier
     *            Spatial Reference System (SRS)
     * @throws SQLException
     */
    protected void addGeometryColumnNoCommit(final String                   tableName,
                                             final GeometryColumnDefinition geometryColumn,
                                             final int                      spatialReferenceSystemIdentifier) throws SQLException
    {
        this.createGeometryColumnTableNoCommit(); // Create the feature metadata table

        final String insertTileMatrix = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?)",
                                                      GeoPackageFeatures.GeometryColumnsTableName,
                                                      "table_name",
                                                      "column_name",
                                                      "geometry_type_name",
                                                      "srs_id",
                                                      "z",
                                                      "m");

        JdbcUtility.update(this.databaseConnection,
                           insertTileMatrix,
                           preparedStatement -> { preparedStatement.setString(1, tableName);
                                                  preparedStatement.setString(2, geometryColumn.getName());
                                                  preparedStatement.setString(3, geometryColumn.getType());
                                                  preparedStatement.setInt   (4, spatialReferenceSystemIdentifier);
                                                  preparedStatement.setInt   (5, geometryColumn.getZRequirement().getValue());
                                                  preparedStatement.setInt   (6, geometryColumn.getMRequirement().getValue());
                                                });
    }

    protected static String getGeometryColumnsCreationSql()
    {
        // http://www.geopackage.org/spec/#gpkg_geometry_columns_cols
        // http://www.geopackage.org/spec/#gpkg_geometry_columns_sql
        return "CREATE TABLE " + GeoPackageFeatures.GeometryColumnsTableName  + '\n' +
               "(table_name         TEXT    NOT NULL, -- Name of the table containing the geometry column\n"                                                      +
               " column_name        TEXT    NOT NULL, -- Name of a column in the feature table that is a Geometry Column\n"                                       +
               " geometry_type_name TEXT    NOT NULL, -- Name from Geometry Type Codes (Core) or Geometry Type Codes (Extension) in Geometry Types (Normative)\n" +
               " srs_id             INTEGER NOT NULL, -- Spatial Reference System ID: gpkg_spatial_ref_sys.srs_id\n"                                              +
               " z                  TINYINT NOT NULL, -- 0: z values prohibited; 1: z values mandatory; 2: z values optional\n"                                   +
               " m                  TINYINT NOT NULL, -- 0: m values prohibited; 1: m values mandatory; 2: m values optional\n"                                   +
               " CONSTRAINT pk_geom_cols     PRIMARY KEY (table_name, column_name),"                                                                              +
               " CONSTRAINT uk_gc_table_name UNIQUE      (table_name),"                                                                                           +
               " CONSTRAINT fk_gc_tn         FOREIGN KEY (table_name) REFERENCES gpkg_contents        (table_name),"                                              +
               " CONSTRAINT fk_gc_srs        FOREIGN KEY (srs_id)     REFERENCES gpkg_spatial_ref_sys (srs_id));";
    }

    private Geometry createGeometry(final byte[] blob) throws WellKnownBinaryFormatException
    {
        final BinaryHeader binaryHeader = new BinaryHeader(blob);   // This will throw if the array length is too short to contain a header (or if it's not long enough to contain the envelope type specified)

        final int headerByteLength = binaryHeader.getByteSize();

        if(blob.length < headerByteLength + 5)    // one byte to indicate byte order, and another 4 (unsigned int) indicating the geometry type
        {
            throw new IllegalArgumentException("Well standard GeoPackage binary blob must contain at least 5 bytes beyond the header");
        }

        return this.createGeometry(ByteBuffer.wrap(blob, headerByteLength, blob.length - headerByteLength));
    }

    private Geometry createGeometry(final ByteBuffer byteBuffer) throws WellKnownBinaryFormatException
    {
        final ByteOrder byteOrder = byteBuffer.get() == 0 ? ByteOrder.BIG_ENDIAN
                                                          : ByteOrder.LITTLE_ENDIAN;

        byteBuffer.order(byteOrder);

        // Read 4 bytes as an /unsigned/ int
        final long geometryType = Integer.toUnsignedLong(byteBuffer.getInt());

        if(!this.geometryFactories.containsKey(geometryType))
        {
            throw new RuntimeException(String.format("Unrecognized geometry type code %d. Recognized geometry types are: %s. Additional types will require a GeoPackage extention to interact with.",
                                                     geometryType,
                                                     this.geometryFactories
                                                         .keySet()
                                                         .stream()
                                                         .map(Object::toString)
                                                         .collect(Collectors.joining(", "))));
        }

        byteBuffer.rewind();

        return this.geometryFactories
                   .get(geometryType)
                   .create(byteBuffer);
    }

    private static byte[] createBlob(final Geometry geometry, final int spatialReferenceSystemIdentifier) throws IOException
    {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(100); // TODO can the up-front

        // TODO HEADER USES OPTIONS:
        // FORCE ENVELOPE
        // FORCE ENDIANNESS

        BinaryHeader.writeBytes(byteBuffer,
                                geometry,
                                spatialReferenceSystemIdentifier);

        byteBuffer.order(ByteOrder.BIG_ENDIAN); // TODO make this an option (?)

        geometry.writeWellKnownBinary(byteBuffer);

        return byteBuffer.array();
    }

    private static void verifyValueRequirements(final GeometryColumn geometryColumn, final Geometry geometry)
    {
        final ValueRequirement zRequirement = geometryColumn.getZRequirement();
        final ValueRequirement mRequirement = geometryColumn.getMRequirement();

        if(geometry == null)
        {
            throw new IllegalArgumentException("Geometry may not be null");
        }

        final boolean hasZ = geometry.hasZ();
        final boolean hasM = geometry.hasM();

        if((zRequirement == ValueRequirement.Prohibited &&  hasZ) ||
           (zRequirement == ValueRequirement.Mandatory  && !hasZ) ||
           (mRequirement == ValueRequirement.Prohibited &&  hasM) ||
           (mRequirement == ValueRequirement.Mandatory  && !hasM))
        {
            throw new IllegalArgumentException(String.format("Geometry is incompatible with the requirements Z %s, M %s",
                                                             zRequirement.toString().toLowerCase(),
                                                             mRequirement.toString().toLowerCase()));
        }
    }

    /**
     * Returns the column names of a feature table, but reorders them so that
     * "id" is first, the geometry column name is second, followed by the rest
     * of the columns in the order returned by the query.
     *
     * @param geometryColumn
     *             {@link GeometryColumn}
     * @return The column names of a feature table, but reorders them so that
     *             "id" is first, the geometry column name is second, followed
     *             by the rest of the columns in the order returned by the
     *             query.
     * @throws SQLException
     *             if there is a database error
     */
    private List<String> getSchema(final GeometryColumn geometryColumn) throws SQLException
    {
        // In SQL (and by extension, SQLite) identifiers are case insensitive.
        // It's possible that a geometry column name could be specified in one
        // case and the table created in another case. We'll do everything in
        // uppercase to avoid failing to find either column.
        final List<String> columns = DatabaseUtility.getColumnNames(this.databaseConnection, geometryColumn.getTableName())
                                                    .stream()
                                                    .map(String::toUpperCase)
                                                    .collect(Collectors.toList());

        Collections.swap(columns, 0, columns.indexOf("id".toUpperCase()));                           // List "id" first
        Collections.swap(columns, 1, columns.indexOf(geometryColumn.getColumnName().toUpperCase())); // List geometry column second

        return columns;
    }

    private void addFeatureTableNoCommit(final String                       featureTableName,
                                         final GeometryColumnDefinition     geometryColumn,
                                         final Collection<ColumnDefinition> columnDefinitions) throws SQLException
    {
        // http://www.geopackage.org/spec/#feature_user_tables
        // http://www.geopackage.org/spec/#example_feature_table_sql

        final List<AbstractColumnDefinition> columns = new LinkedList<>(columnDefinitions);

        columns.add(0,
                    new ColumnDefinition("id",
                                         "INTEGER",
                                         EnumSet.of(ColumnFlag.PrimaryKey, ColumnFlag.AutoIncrement, ColumnFlag.NotNull),   // Specifying unique is unnecessary as primary keys are assumed to be unique
                                         null,
                                         null,
                                         "Autoincrement primary key"));
        columns.add(1, geometryColumn);

        final StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE ");
        createTableSql.append(featureTableName);
        createTableSql.append("\n(");

        for(int columnIndex = 0; columnIndex < columns.size(); ++columnIndex)
        {
            final AbstractColumnDefinition column = columns.get(columnIndex);

            final String comment = column.getComment();

            createTableSql.append("\n\t");

            createTableSql.append(column.getName());  // Verified by AbstractColumnDefinition to not contain SQL injection
            createTableSql.append(' ');
            createTableSql.append(column.getType());  // Verified by AbstractColumnDefinition to not contain SQL injection
            createTableSql.append(column.hasFlag(ColumnFlag.PrimaryKey)    ? " PRIMARY KEY"   : "");
            createTableSql.append(column.hasFlag(ColumnFlag.AutoIncrement) ? " AUTOINCREMENT" : "");
            createTableSql.append(column.hasFlag(ColumnFlag.NotNull)       ? " NOT NULL"      : "");
            createTableSql.append(column.hasFlag(ColumnFlag.Unique)        ? " UNIQUE"        : "");
            createTableSql.append(columnIndex == columns.size()-1          ? ""               : ",");
            createTableSql.append(comment     == null                      ? ""               : " -- " + comment);  // Verified by AbstractColumnDefinition to not contain newlines, which I think is the only way injection could work here
        }

        createTableSql.append("\n);");

        JdbcUtility.update(this.databaseConnection, createTableSql.toString());
    }

    public static final String GeometryColumnsTableName = "gpkg_geometry_columns";

    private final Connection     databaseConnection;
    private final GeoPackageCore core;

    private final Map<Long, GeometryFactory> geometryFactories = new HashMap<>();

    private static final long maxUnsignedIntValue = 4294967295L; // 2^31 - 1
}
