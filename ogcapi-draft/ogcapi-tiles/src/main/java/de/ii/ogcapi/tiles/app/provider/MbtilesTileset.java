/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.app.provider.ImmutableMbtilesMetadata.Builder;
import de.ii.ogcapi.tiles.domain.provider.TileCoordinates;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.xtraplatform.base.domain.LogContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MbtilesTileset {

  private static final Logger LOGGER = LoggerFactory.getLogger(MbtilesTileset.class);
  private static final int EMPTY_TILE_ID = 1;
  private Connection connection = null;
  private final Path tilesetPath;
  private final Semaphore mutex = new Semaphore(1);
  private final MbtilesMetadata metadata;

  public MbtilesTileset(Path tilesetPath) {
    if (!Files.exists(tilesetPath)) {
      throw new RuntimeException(String.format("Mbtiles file does not exist: %s", tilesetPath));
    }
    this.tilesetPath = tilesetPath;
    try {
      this.metadata = getMetadata();
    } catch (SQLException | IOException e) {
      throw new RuntimeException(
          String.format("Could not read from Mbtiles file: %s", tilesetPath), e);
    }
  }

  public MbtilesTileset(Path tilesetPath, MbtilesMetadata metadata) throws IOException {
    if (Files.exists(tilesetPath)) {
      throw new FileAlreadyExistsException(tilesetPath.toString());
    }
    this.tilesetPath = tilesetPath;
    this.metadata = metadata;

    // create and init MBTiles DB
    releaseConnection(getConnection(true));
  }

  private void initMbtilesDb(MbtilesMetadata metadata, Connection connection) {
    try {
      // create tables and views
      SqlHelper.execute(connection, "BEGIN TRANSACTION IMMEDIATE");
      SqlHelper.execute(connection, "CREATE TABLE metadata (name text, value text)");
      SqlHelper.execute(
          connection,
          "CREATE TABLE tile_map (zoom_level integer, tile_column integer, tile_row integer, tile_id integer)");
      SqlHelper.execute(
          connection,
          "CREATE UNIQUE INDEX tile_index on tile_map (zoom_level, tile_column, tile_row)");
      SqlHelper.execute(
          connection, "CREATE TABLE tile_blobs (tile_id integer primary key, tile_data blob)");
      SqlHelper.execute(
          connection,
          "CREATE VIEW tiles AS SELECT zoom_level, tile_column, tile_row, tile_data FROM tile_map INNER JOIN tile_blobs ON tile_map.tile_id = tile_blobs.tile_id");

      // populate metadata
      SqlHelper.addMetadata(connection, "name", metadata.getName());
      SqlHelper.addMetadata(connection, "format", metadata.getFormat());
      if (metadata.getBounds().size() == 4)
        SqlHelper.addMetadata(
            connection,
            "bounds",
            String.join(
                ",",
                metadata.getBounds().stream()
                    .map(v -> String.format(Locale.US, "%f", v))
                    .collect(Collectors.toUnmodifiableList())));
      if (metadata.getCenter().size() == 3)
        SqlHelper.addMetadata(
            connection,
            "center",
            String.join(
                ",",
                metadata.getCenter().stream()
                    .map(v -> String.format(Locale.US, "%s", v))
                    .collect(Collectors.toUnmodifiableList())));
      metadata.getMinzoom().ifPresent(v -> SqlHelper.addMetadata(connection, "minzoom", v));
      metadata.getMaxzoom().ifPresent(v -> SqlHelper.addMetadata(connection, "maxzoom", v));
      metadata.getAttribution().ifPresent(v -> SqlHelper.addMetadata(connection, "attribution", v));
      metadata.getDescription().ifPresent(v -> SqlHelper.addMetadata(connection, "description", v));
      metadata.getType().ifPresent(v -> SqlHelper.addMetadata(connection, "type", v));
      metadata.getVersion().ifPresent(v -> SqlHelper.addMetadata(connection, "version", v));
      if (metadata.getFormat() == MbtilesMetadata.MbtilesFormat.pbf) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        try {
          SqlHelper.addMetadata(
              connection,
              "json",
              mapper.writeValueAsString(
                  ImmutableMap.of("vector_layers", metadata.getVectorLayers())));
        } catch (JsonProcessingException e) {
          LOGGER.error(
              String.format("Could not write 'json' metadata entry. Reason: %s", e.getMessage()));
          if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
            LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
          }
          SqlHelper.addMetadata(
              connection,
              "json",
              mapper.writeValueAsString(ImmutableMap.of("vector_layers", ImmutableList.of())));
        }
      }

      // create empty MVT tile with rowid=1
      if (metadata.getFormat().equals(MbtilesMetadata.MbtilesFormat.pbf)) {
        PreparedStatement statement =
            connection.prepareStatement("INSERT INTO tile_blobs (tile_id,tile_data) VALUES(?,?)");
        statement.setInt(1, EMPTY_TILE_ID);
        ByteArrayOutputStream mvt = new ByteArrayOutputStream(0);
        GZIPOutputStream gzipStream = new GZIPOutputStream(mvt);
        gzipStream.close();
        statement.setBytes(2, mvt.toByteArray());
        statement.execute();
        statement.close();
      }

      SqlHelper.execute(connection, "COMMIT");
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Could not create new Mbtiles file: %s", tilesetPath), e);
    }
  }

  private Connection getConnection(boolean aquireMutexOnCreate) throws IOException {
    // we use a single connection per database to avoid multi-threading conflicts

    // check, if the file exists
    if (!Files.exists(tilesetPath)) {
      // aquire the mutex, if necessary (for write operations we already have it)
      boolean aquired = false;
      try {
        aquired = aquireMutexOnCreate && mutex.tryAcquire(5, TimeUnit.SECONDS);
        if (aquireMutexOnCreate)
          LOGGER.trace("getConnection: Trying to aquite mutex: '{}'.", aquired);
        if (aquireMutexOnCreate && !aquired)
          throw new RuntimeException(
              String.format("Could not aquire mutex to create MBTiles file: %s", tilesetPath));
        // now that we have the mutex, check again, if the file exists, it may have been
        // created by a parallel request
        if (!Files.exists(tilesetPath)) {
          // recreate an empty MBTiles container
          LOGGER.trace("Creating MBTiles file '{}'.", tilesetPath);
          Files.createDirectories(tilesetPath.getParent());
          connection = SqlHelper.getConnection(tilesetPath.toFile());
          initMbtilesDb(metadata, connection);
        } else if (Objects.isNull(connection)) {
          connection = SqlHelper.getConnection(tilesetPath.toFile());
        }
      } catch (InterruptedException e) {
        LOGGER.debug("getConnection: Thread has been interrupted.");
      } finally {
        if (aquired) {
          LOGGER.trace("getConnection: Releasing mutex.");
          mutex.release();
        }
      }
    } else if (Objects.isNull(connection)) {
      connection = SqlHelper.getConnection(tilesetPath.toFile());
    }

    return connection;
  }

  private void releaseConnection(@Nullable Connection connection) {
    // nothing to do
  }

  public MbtilesMetadata getMetadata() throws SQLException, IOException {
    Builder builder = ImmutableMbtilesMetadata.builder();
    Connection connection = getConnection(true);
    ResultSet rs = SqlHelper.executeQuery(connection, "SELECT name, value FROM metadata");
    while (rs.next()) {
      final String name = rs.getString("name");
      final String value = rs.getString("value");
      if (Objects.nonNull(value)) {
        switch (name) {
          case "name":
            builder.name(value);
            break;
          case "format":
            MbtilesMetadata.MbtilesFormat format = MbtilesMetadata.MbtilesFormat.of(value);
            if (Objects.isNull(format))
              throw new IllegalArgumentException(
                  String.format(
                      "The metadata entry '%s' in an Mbtiles container has an invalid value '%s'",
                      name, value));
            builder.format(format);
            break;
          case "bounds":
            List<Double> bounds =
                Splitter.on(',')
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToStream(value)
                    .map(Double::parseDouble)
                    .collect(Collectors.toUnmodifiableList());
            if (bounds.size() != 4)
              throw new IllegalArgumentException(
                  String.format(
                      "The metadata entry '%s' in an Mbtiles container has an invalid value '%s'",
                      name, value));
            builder.bounds(bounds);
            break;
          case "center":
            List<Double> center =
                Splitter.on(',')
                    .trimResults()
                    .omitEmptyStrings()
                    .splitToStream(value)
                    .map(Double::parseDouble)
                    .collect(Collectors.toUnmodifiableList());
            if (center.size() != 3)
              throw new IllegalArgumentException(
                  String.format(
                      "The metadata entry '%s' in an Mbtiles container has an invalid value '%s'",
                      name, value));
            builder.center(center);
            break;
          case "minzoom":
            builder.minzoom(Integer.parseInt(value));
            break;
          case "maxzoom":
            builder.maxzoom(Integer.parseInt(value));
            break;
          case "description":
            builder.description(value);
            break;
          case "attribution":
            builder.attribution(value);
            break;
          case "type":
            MbtilesMetadata.MbtilesType type = MbtilesMetadata.MbtilesType.of(value);
            if (Objects.isNull(type))
              throw new IllegalArgumentException(
                  String.format(
                      "The metadata entry '%s' in an Mbtiles container has an invalid value '%s'",
                      name, value));
            builder.type(type);
            break;
          case "version":
            try {
              int v = Integer.parseInt(value);
              builder.version(v);
            } catch (NumberFormatException e) {
              builder.version(Float.parseFloat(value));
            }
            break;
          case "vector_layers":
            // TODO vector_layers
            break;
        }
      }
    }
    releaseConnection(connection);
    return builder.build();
  }

  public Optional<InputStream> getTile(TileQuery tile) throws SQLException, IOException {
    Optional<InputStream> result = Optional.empty();
    int level = tile.getLevel();
    int row = tile.getTileMatrixSet().getTmsRow(level, tile.getRow());
    int col = tile.getCol();
    boolean gzip = Objects.equals(tile.getMediaType(), FeatureEncoderMVT.FORMAT);
    Connection connection = getConnection(true);
    String sql =
        String.format(
            "SELECT tile_data FROM tiles WHERE zoom_level=%d AND tile_row=%d AND tile_column=%d",
            level, row, col);
    ResultSet rs = SqlHelper.executeQuery(connection, sql);
    if (rs.next()) {
      result =
          Optional.of(
              gzip
                  ? new GZIPInputStream(rs.getBinaryStream("tile_data"))
                  : rs.getBinaryStream("tile_data"));
    }
    releaseConnection(connection);
    return result;
  }

  public Optional<Boolean> tileIsEmpty(TileCoordinates tile) throws SQLException, IOException {
    Optional<Boolean> result = Optional.empty();
    Connection connection = getConnection(true);
    int level = tile.getLevel();
    int row = tile.getTileMatrixSet().getTmsRow(level, tile.getRow());
    int col = tile.getCol();
    String sql =
        String.format(
            "SELECT tile_id FROM tile_map WHERE zoom_level=%d AND tile_row=%d AND tile_column=%d",
            level, row, col);
    ResultSet rs = SqlHelper.executeQuery(connection, sql);
    if (rs.next()) {
      result = Optional.of(rs.getInt("tile_id") == EMPTY_TILE_ID);
    }
    releaseConnection(connection);
    return result;
  }

  public boolean tileExists(TileCoordinates tile) throws SQLException, IOException {
    Connection connection = getConnection(true);
    int level = tile.getLevel();
    int row = tile.getTileMatrixSet().getTmsRow(level, tile.getRow());
    int col = tile.getCol();
    String sql =
        String.format(
            "SELECT tile_data FROM tiles WHERE zoom_level=%d AND tile_row=%d AND tile_column=%d",
            level, row, col);
    boolean exists = SqlHelper.executeQuery(connection, sql).next();
    releaseConnection(connection);
    return exists;
  }

  public void writeTile(TileQuery tile, byte[] content) throws SQLException, IOException {
    int level = tile.getLevel();
    int row = tile.getTileMatrixSet().getTmsRow(level, tile.getRow());
    int col = tile.getCol();
    boolean gzip = Objects.equals(tile.getMediaType(), FeatureEncoderMVT.FORMAT);
    boolean supportsEmtpyTile = Objects.equals(tile.getMediaType(), FeatureEncoderMVT.FORMAT);
    LOGGER.trace(
        "Write tile {}/{}/{}/{} to MBTiles cache {}.",
        tile.getTileMatrixSet().getId(),
        level,
        tile.getRow(),
        col,
        tilesetPath);
    Connection connection = null;
    boolean aquired = false;
    try {
      aquired = mutex.tryAcquire(5, TimeUnit.SECONDS);
      LOGGER.trace("writeTile: Trying to aquite mutex: '{}'.", aquired);
      if (!aquired)
        throw new RuntimeException(
            String.format("Could not aquire mutex to create MBTiles file: %s", tilesetPath));
      connection = getConnection(false);
      // do we have an old blob?
      boolean exists = false;
      Integer old_tile_id = null;
      String sql =
          String.format(
              "SELECT tile_id FROM tile_map WHERE zoom_level=%d AND tile_row=%d AND tile_column=%d",
              level, row, col);
      ResultSet rs = SqlHelper.executeQuery(connection, sql);
      if (rs.next()) {
        exists = true;
        old_tile_id = rs.getInt(1);
      }
      // add the new tile
      int tile_id = EMPTY_TILE_ID;
      if (content.length > 0 || !supportsEmtpyTile) {
        PreparedStatement statement =
            connection.prepareStatement("INSERT INTO tile_blobs (tile_data) VALUES(?)");
        ByteArrayOutputStream mvt = new ByteArrayOutputStream(content.length);
        if (gzip) {
          GZIPOutputStream gzipStream = new GZIPOutputStream(mvt);
          gzipStream.write(content);
          gzipStream.close();
        } else {
          mvt.write(content);
        }
        statement.setBytes(1, mvt.toByteArray());
        statement.execute();
        statement.close();
        rs = SqlHelper.executeQuery(connection, "SELECT last_insert_rowid()");
        tile_id = rs.getInt(1);
      }
      PreparedStatement statement =
          exists
              ? connection.prepareStatement(
                  "UPDATE tile_map SET tile_id=? WHERE zoom_level=? AND tile_row=? AND tile_column=?")
              : connection.prepareStatement(
                  "INSERT INTO tile_map (tile_id,zoom_level,tile_row,tile_column) VALUES(?,?,?,?)");
      statement.setInt(1, tile_id);
      statement.setInt(2, level);
      statement.setInt(3, row);
      statement.setInt(4, col);
      statement.execute();
      statement.close();
      // finally remove any old blob
      if (Objects.nonNull(old_tile_id) && (old_tile_id != EMPTY_TILE_ID || !supportsEmtpyTile)) {
        SqlHelper.execute(
            connection, String.format("DELETE FROM tile_map WHERE tile_id = %d", old_tile_id));
      }
    } catch (InterruptedException e) {
      LOGGER.debug("writeTile: Thread has been interrupted.");
    } finally {
      releaseConnection(connection);
      if (aquired) {
        LOGGER.trace("writeTile: Releasing mutex.");
        mutex.release();
      }
    }
  }

  public void deleteTile(TileQuery tile) throws SQLException, IOException {
    boolean supportsEmtpyTile = Objects.equals(tile.getMediaType(), FeatureEncoderMVT.FORMAT);
    int level = tile.getLevel();
    int row = tile.getTileMatrixSet().getTmsRow(level, tile.getRow());
    int col = tile.getCol();
    LOGGER.trace(
        "Delete tile {}/{}/{}/{} from MBTiles cache {}.",
        tile.getTileMatrixSet().getId(),
        level,
        tile.getRow(),
        col,
        tilesetPath);
    Connection connection = null;
    boolean aquired = false;
    try {
      aquired = mutex.tryAcquire(5, TimeUnit.SECONDS);
      LOGGER.trace("deleteTile: Trying to aquite mutex: '{}'.", aquired);
      if (!aquired)
        throw new RuntimeException(
            String.format("Could not aquire mutex to create MBTiles file: %s", tilesetPath));
      connection = getConnection(false);
      String sql =
          String.format(
              "SELECT tile_id FROM tile_map WHERE zoom_level=%d AND tile_row=%d AND tile_column=%d",
              level, row, col);
      ResultSet rs = SqlHelper.executeQuery(connection, sql);
      if (rs.next()) {
        int tile_id = rs.getInt(1);
        SqlHelper.execute(
            connection,
            String.format(
                "DELETE FROM tile_map WHERE zoom_level=%d AND tile_row=%d AND tile_column=%d",
                level, row, col));
        if (tile_id != EMPTY_TILE_ID || !supportsEmtpyTile) {
          SqlHelper.execute(
              connection, String.format("DELETE FROM tile_blobs WHERE tile_id=%d", tile_id));
        }
      }
    } catch (InterruptedException e) {
      LOGGER.debug("deleteTile: Thread has been interrupted.");
    } finally {
      releaseConnection(connection);
      if (aquired) {
        LOGGER.trace("deleteTile: Releasing mutex.");
        mutex.release();
      }
    }
  }

  public void deleteTiles(TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits)
      throws SQLException, IOException {
    int level = Integer.parseInt(limits.getTileMatrix());
    LOGGER.trace(
        "Delete tiles {}/{}/*/* from MBTiles cache {}.", tileMatrixSet.getId(), level, tilesetPath);
    Connection connection = null;
    boolean aquired = false;
    try {
      aquired = mutex.tryAcquire(5, TimeUnit.SECONDS);
      LOGGER.trace("deleteTiles: Trying to aquite mutex: '{}'.", aquired);
      if (!aquired)
        throw new RuntimeException(
            String.format("Could not aquire mutex to create MBTiles file: %s", tilesetPath));
      connection = getConnection(false);
      String sqlFrom =
          String.format(
              "FROM tile_map WHERE zoom_level=%d AND tile_row>=%d AND tile_column>=%d AND tile_row<=%d AND tile_column<=%d",
              level,
              tileMatrixSet.getTmsRow(level, limits.getMaxTileRow()),
              limits.getMinTileCol(),
              tileMatrixSet.getTmsRow(level, limits.getMinTileRow()),
              limits.getMaxTileCol());
      ResultSet rs =
          SqlHelper.executeQuery(connection, String.format("SELECT DISTINCT tile_id %s", sqlFrom));
      while (rs.next()) {
        int tile_id = rs.getInt(1);
        if (tile_id != EMPTY_TILE_ID) {
          SqlHelper.execute(
              connection, String.format("DELETE FROM tile_blobs WHERE tile_id=%d", tile_id));
        }
      }
      SqlHelper.execute(connection, String.format("DELETE %s", sqlFrom));
    } catch (InterruptedException e) {
      LOGGER.debug("deleteTile: Thread has been interrupted.");
    } finally {
      releaseConnection(connection);
      if (aquired) {
        LOGGER.trace("deleteTiles: Releasing mutex.");
        mutex.release();
      }
    }
  }
}
