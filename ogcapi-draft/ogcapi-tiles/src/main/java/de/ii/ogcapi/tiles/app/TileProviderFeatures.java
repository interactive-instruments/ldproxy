/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileEmpty;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileMultiLayer;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileSingleLayer;
import de.ii.ogcapi.tiles.domain.ImmutableTile;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.ogcapi.tiles.domain.PredefinedFilter;
import de.ii.ogcapi.tiles.domain.Rule;
import de.ii.ogcapi.tiles.domain.SeedingOptions;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TileFromFeatureQuery;
import de.ii.ogcapi.tiles.domain.TileProvider;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.ws.rs.NotAcceptableException;
import org.immutables.value.Value;

/**
 * # Tile-Provider FEATURES
 *
 * @langEn In this tile provider, the tiles in Mapbox Vector Tiles format are derived from the
 *     features provided by the API in the area of the tile.
 * @langDe Bei diesem Tile-Provider werden die Kacheln im Format Mapbox Vector Tiles aus den von der
 *     API bereitgestellten Features im Gebiet der Kachel abgeleitet.
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderFeatures.Builder.class)
public abstract class TileProviderFeatures extends TileProvider {

  /**
   * @langEn Fixed value, identifies the tile provider type.
   * @langDe Fester Wert, identifiziert die Tile-Provider-Art.
   * @default `FEATURES`
   */
  public final String getType() {
    return "FEATURES";
  }

  /**
   * @langEn Controls which formats should be supported for the tiles. Currently only Mapbox Vector
   *     Tiles ("MVT") is available.
   * @langDe Steuert, welche Formate für die Kacheln unterstützt werden sollen. Zur Verfügung steht
   *     derzeit nur Mapbox Vector Tiles ("MVT").
   * @default `[ "MVT" ]`
   */
  @Override
  public abstract List<String> getTileEncodings();

  /**
   * @langEn Controls the zoom levels available for each active tiling scheme as well as which zoom
   *     level to use as default.
   * @langDe Steuert die Zoomstufen, die für jedes aktive Kachelschema verfügbar sind sowie welche
   *     Zoomstufe als Default bei verwendet werden soll.
   * @default `{ "WebMercatorQuad" : { "min": 0, "max": 23 } }`
   */
  public abstract Map<String, MinMax> getZoomLevels();

  /**
   * @langEn Zoom levels for which tiles are cached.
   * @langDe Steuert die Zoomstufen, in denen erzeugte Kacheln gecacht werden.
   * @default `{}`
   */
  public abstract Map<String, MinMax> getZoomLevelsCache();

  /**
   * @langEn Controls how and when tiles are precomputed, see [Seeding options](#seeding-options).
   * @langDe Steuert wie und wann Kacheln vorberechnet werden, siehe [Optionen für das
   *     Seeding](#seeding-options).
   * @default
   */
  public abstract Optional<SeedingOptions> getSeedingOptions();

  /**
   * @langEn Zoom levels per enabled tile encoding for which the tile cache should be seeded on
   *     startup.
   * @langDe Steuert die Zoomstufen, die für jedes aktive Kachelschema beim Start vorberechnet
   *     werden.
   * @default `{}`
   */
  public abstract Map<String, MinMax> getSeeding();

  /**
   * @langEn Filters to select a subset of feature for certain zoom levels using a CQL filter
   *     expression, see example below.
   * @langDe Über Filter kann gesteuert werden, welche Features auf welchen Zoomstufen selektiert
   *     werden sollen. Dazu dient ein CQL-Filterausdruck, der in `filter` angegeben wird. Siehe das
   *     Beispiel unten.
   * @default `{}`
   */
  public abstract Map<String, List<PredefinedFilter>> getFilters();

  /**
   * @langEn Rules to postprocess the selected features for a certain zoom level. Supported
   *     operations are: selecting a subset of feature properties (`properties`), spatial merging of
   *     features that intersect (`merge`), with the option to restrict the operations to features
   *     with matching attributes (`groupBy`). See the example below. For `merge`, the resulting
   *     object will only obtain properties that are identical for all merged features.
   * @langDe Über Regeln können die selektierten Features in Abhängigkeit der Zoomstufe
   *     nachbearbeitet werden. Unterstützt wird eine Reduzierung der Attribute (`properties`), das
   *     geometrische Verschmelzen von Features, die sich geometrisch schneiden (`merge`), ggf.
   *     eingeschränkt auf Features mit bestimmten identischen Attributen (`groupBy`). Siehe das
   *     Beispiel unten. Beim Verschmelzen werden alle Attribute in das neue Objekt übernommen, die
   *     in den verschmolzenen Features identisch sind.
   * @default `{}`
   */
  public abstract Map<String, List<Rule>> getRules();

  /**
   * @langEn Longitude and latitude that a map with the tiles should be centered on by default.
   * @langDe Legt Länge und Breite fest, auf die standardmäßig eine Karte mit den Kacheln zentriert
   *     werden sollte.
   * @default `[ 0, 0 ]`
   */
  public abstract List<Double> getCenter();

  /**
   * @langEn Maximum number of features contained in a single tile per query.
   * @langDe Steuert die maximale Anzahl der Features, die pro Query für eine Kachel berücksichtigt
   *     werden.
   * @default 100000
   */
  @Nullable
  public abstract Integer getLimit();

  /**
   * @langEn Enable vector tiles for each *Feature Collection*. Every tile contains a layer with the
   *     feature from the collection.
   * @langDe Steuert, ob Vector Tiles für jede Feature Collection aktiviert werden sollen. Jede
   *     Kachel hat einen Layer mit den Features aus der Collection.
   * @default `true`
   */
  @Nullable
  public abstract Boolean getSingleCollectionEnabled();

  @Override
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  public boolean isSingleCollectionEnabled() {
    return Objects.equals(getSingleCollectionEnabled(), true);
  }

  /**
   * @langEn Enable vector tiles for the whole dataset. Every tile contains one layer per collection
   *     with the features of that collection.
   * @langDe Steuert, ob Vector Tiles für den Datensatz aktiviert werden sollen. Jede Kachel hat
   *     einen Layer pro Collection mit den Features aus der Collection.
   * @default `true`
   */
  @Nullable
  public abstract Boolean getMultiCollectionEnabled();

  @Override
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  public boolean isMultiCollectionEnabled() {
    return Objects.equals(getMultiCollectionEnabled(), true);
  }

  /**
   * @langEn Ignore features with invalid geometries. Before ignoring a feature, an attempt is made
   *     to transform the geometry to a valid geometry. The topology of geometries might be invalid
   *     in the data source or in some cases the quantization of coordinates to integers might
   *     render it invalid.
   * @langDe Steuert, ob Objekte mit ungültigen Objektgeometrien ignoriert werden. Bevor Objekte
   *     ignoriert werden, wird zuerst versucht, die Geometrie in eine gültige Geometrie zu
   *     transformieren. Nur wenn dies nicht gelingt, wird die Geometrie ignoriert. Die Topologie
   *     von Geometrien können entweder schon im Provider ungültig sein oder die Geometrie kann in
   *     seltenen Fällen als Folge der Quantisierung der Koordinaten zu Integern für die Speicherung
   *     in der Kachel ungültig werden.
   * @default `false`
   */
  @Nullable
  public abstract Boolean getIgnoreInvalidGeometries();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  public boolean isIgnoreInvalidGeometries() {
    return Objects.equals(getIgnoreInvalidGeometries(), true);
  }

  /**
   * @langEn *Deprecated, no longer used* Maximum allowed relative change of surface sizes when
   *     attempting to fix an invalid surface geometry. The fixed geometry is only used when the
   *     condition is met. The value `0.1` means 10%.
   * @langDe *Deprecated, wird nicht mehr benutzt* Steuert die maximal erlaubte relative Änderung
   *     der Flächengröße beim Versuch eine topologisch ungültige Polygongeometrie im
   *     Koordinatensystem der Kachel zu reparieren. Ist die Bedingung erfüllt, wird die reparierte
   *     Polygongeometrie verwendet. Der Wert 0.1 entspricht 10%.
   * @default 0.1
   */
  @Deprecated
  public abstract Optional<Double> getMaxRelativeAreaChangeInPolygonRepair();

  /**
   * @langEn *Deprecated, no longer used* Maximum allowed absolute change of surface sizes when
   *     attempting to fix an invalid surface geometry. The fixed geometry is only used when the
   *     condition is met. The value `1.0` corresponds to one "pixel" in the used coordinate
   *     reference system.
   * @langDe *Deprecated, wird nicht mehr benutzt* Steuert die maximal erlaubte absolute Änderung
   *     der Flächengröße beim Versuch eine topologisch ungültige Polygongeometrie im
   *     Koordinatensystem der Kachel zu reparieren. Ist die Bedingung erfüllt, wird die reparierte
   *     Polygongeometrie verwendet. Der Wert 1.0 entspricht einem "Pixel" im
   *     Kachelkoordinatensystem.
   * @default 1.0
   */
  @Deprecated
  public abstract Optional<Double> getMaxAbsoluteAreaChangeInPolygonRepair();

  /**
   * @langEn Features with line geometries shorter that the given value are excluded from tiles.
   *     Features with surface geometries smaller than the square of the given value are excluded
   *     from the tiles. The value `0.5` corresponds to half a "pixel" in the used coordinate
   *     reference system.
   * @langDe Objekte mit Liniengeometrien, die kürzer als der Wert sind, werden nicht in die Kachel
   *     aufgenommen. Objekte mit Flächengeometrien, die kleiner als das Quadrat des Werts sind,
   *     werden nicht in die Kachel aufgenommen. Der Wert 0.5 entspricht einem halben "Pixel" im
   *     Kachelkoordinatensystem.
   * @default 0.5
   */
  @Nullable
  public abstract Double getMinimumSizeInPixel();

  @Override
  @JsonIgnore
  @Value.Default
  public boolean tilesMayBeCached() {
    return true;
  }

  @Override
  @JsonIgnore
  @Value.Derived
  public QueryInput getQueryInput(
      OgcApiDataV2 apiData,
      URICustomizer uriCustomizer,
      Map<String, String> queryParameters,
      List<OgcApiQueryParameter> allowedParameters,
      QueryInput genericInput,
      Tile tile) {
    if (!tile.getFeatureProvider().map(FeatureProvider2::supportsQueries).orElse(false)) {
      throw new IllegalStateException(
          "Tile cannot be generated. The feature provider does not support feature queries.");
    }

    TileFormatExtension outputFormat = tile.getOutputFormat();
    List<String> collections = tile.getCollectionIds();

    if (collections.isEmpty()) {
      return new ImmutableQueryInputTileEmpty.Builder().from(genericInput).tile(tile).build();
    }

    if (!(outputFormat instanceof TileFormatWithQuerySupportExtension))
      throw new RuntimeException(
          String.format(
              "Unexpected tile format without query support. Found: %s",
              outputFormat.getClass().getSimpleName()));

    // first execute the information that is passed as processing parameters (e.g., "properties")
    Map<String, Object> processingParameters = new HashMap<>();
    for (OgcApiQueryParameter parameter : allowedParameters) {
      processingParameters =
          parameter.transformContext(null, processingParameters, queryParameters, apiData);
    }

    if (tile.isDatasetTile()) {
      if (!outputFormat.canMultiLayer() && collections.size() > 1)
        throw new NotAcceptableException(
            "The requested tile format supports only a single layer. Please select only a single collection.");

      Map<String, Tile> singleLayerTileMap =
          collections.stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      collectionId -> collectionId,
                      collectionId ->
                          new ImmutableTile.Builder()
                              .from(tile)
                              .collectionIds(ImmutableList.of(collectionId))
                              .isDatasetTile(false)
                              .build()));

      Map<String, FeatureQuery> queryMap =
          collections.stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      collectionId -> collectionId,
                      collectionId -> {
                        String featureTypeId =
                            apiData
                                .getCollections()
                                .get(collectionId)
                                .getExtension(FeaturesCoreConfiguration.class)
                                .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                .orElse(collectionId);
                        TilesConfiguration layerConfiguration =
                            apiData
                                .getExtension(TilesConfiguration.class, collectionId)
                                .orElseThrow();
                        FeatureQuery query =
                            ((TileFormatWithQuerySupportExtension) outputFormat)
                                .getQuery(
                                    singleLayerTileMap.get(collectionId),
                                    allowedParameters,
                                    queryParameters,
                                    layerConfiguration,
                                    uriCustomizer);
                        return ImmutableFeatureQuery.builder()
                            .from(query)
                            .type(featureTypeId)
                            .build();
                      }));

      FeaturesCoreConfiguration coreConfiguration =
          apiData.getExtension(FeaturesCoreConfiguration.class).orElseThrow();

      return new ImmutableQueryInputTileMultiLayer.Builder()
          .from(genericInput)
          .tile(tile)
          .singleLayerTileMap(singleLayerTileMap)
          .queryMap(queryMap)
          .processingParameters(processingParameters)
          .defaultCrs(
              apiData
                  .getExtension(FeaturesCoreConfiguration.class)
                  .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                  .orElseThrow())
          .build();
    } else {
      String collectionId = tile.getCollectionId();
      FeatureTypeConfigurationOgcApi featureType =
          apiData.getCollectionData(collectionId).orElseThrow();
      TilesConfiguration layerConfiguration =
          apiData.getExtension(TilesConfiguration.class, collectionId).orElseThrow();
      FeatureQuery query =
          ((TileFromFeatureQuery) outputFormat)
              .getQuery(
                  tile, allowedParameters, queryParameters, layerConfiguration, uriCustomizer);

      FeaturesCoreConfiguration coreConfiguration =
          featureType.getExtension(FeaturesCoreConfiguration.class).orElseThrow();

      return new ImmutableQueryInputTileSingleLayer.Builder()
          .from(genericInput)
          .tile(tile)
          .query(query)
          .processingParameters(processingParameters)
          .defaultCrs(
              featureType
                  .getExtension(FeaturesCoreConfiguration.class)
                  .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                  .orElseThrow())
          .build();
    }
  }

  @Override
  public TileProvider mergeInto(TileProvider source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderFeatures)) return this;

    ImmutableTileProviderFeatures.Builder builder =
        ImmutableTileProviderFeatures.builder().from((TileProviderFeatures) source).from(this);

    TileProviderFeatures src = (TileProviderFeatures) source;

    List<String> tileEncodings =
        Objects.nonNull(src.getTileEncodings())
            ? Lists.newArrayList(src.getTileEncodings())
            : Lists.newArrayList();
    getTileEncodings()
        .forEach(
            tileEncoding -> {
              if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
              }
            });
    builder.tileEncodings(tileEncodings);

    Map<String, MinMax> mergedSeeding =
        Objects.nonNull(src.getSeeding())
            ? Maps.newLinkedHashMap(src.getSeeding())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getSeeding())) getSeeding().forEach(mergedSeeding::put);
    builder.seeding(mergedSeeding);

    Map<String, MinMax> mergedZoomLevels =
        Objects.nonNull(src.getZoomLevels())
            ? Maps.newLinkedHashMap(src.getZoomLevels())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getZoomLevels())) getZoomLevels().forEach(mergedZoomLevels::put);
    builder.zoomLevels(mergedZoomLevels);

    if (!getCenter().isEmpty()) builder.center(getCenter());
    else if (!src.getCenter().isEmpty()) builder.center(src.getCenter());

    Map<String, MinMax> mergedZoomLevelsCache =
        Objects.nonNull(src.getZoomLevelsCache())
            ? Maps.newLinkedHashMap(src.getZoomLevelsCache())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getZoomLevelsCache()))
      getZoomLevelsCache().forEach(mergedZoomLevelsCache::put);
    builder.zoomLevelsCache(mergedZoomLevelsCache);

    Map<String, List<Rule>> mergedRules =
        Objects.nonNull(src.getRules())
            ? Maps.newLinkedHashMap(src.getRules())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getRules())) getRules().forEach(mergedRules::put);
    builder.rules(mergedRules);

    Map<String, List<PredefinedFilter>> mergedFilters =
        Objects.nonNull(src.getFilters())
            ? Maps.newLinkedHashMap(src.getFilters())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getFilters())) getFilters().forEach(mergedFilters::put);
    builder.filters(mergedFilters);

    if (Objects.nonNull(getCenter())) builder.center(getCenter());
    else if (Objects.nonNull(src.getCenter())) builder.center(src.getCenter());

    return builder.build();
  }
}
