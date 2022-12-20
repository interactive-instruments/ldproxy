/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.LIMIT_DEFAULT;
import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.MINIMUM_SIZE_IN_PIXEL;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.ogcapi.features.core.domain.SfFlatConfiguration;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax;
import de.ii.xtraplatform.tiles.domain.LevelFilter;
import de.ii.xtraplatform.tiles.domain.LevelTransformation;
import de.ii.xtraplatform.tiles.domain.MinMax;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock TILES
 * @langEn ### Prerequisites
 *     <p>The module *Tile Matrix Sets* must be enabled.
 *     <p>### Storage
 *     <p>The tile cache is located in the data directory under the relative path
 *     `cache/tiles/{apiId}`. If the data for an API or tile configuration has been changed, then
 *     the cache directory for the API should be deleted so that the cache is rebuilt with the
 *     updated data or rules.
 * @langDe ### Voraussetzungen
 *     <p>Das Modul *Tile Matrix Sets* muss aktiviert sein.
 *     <p>### Storage
 *     <p>Der Tile-Cache liegt im Datenverzeichnis unter dem relativen Pfad `cache/tiles/{apiId}`.
 *     Wenn die Daten zu einer API oder Kachelkonfiguration geändert wurden, dann sollte das
 *     Cache-Verzeichnis für die API gelöscht werden, damit der Cache mit den aktualisierten Daten
 *     oder Regeln neu aufgebaut wird.
 * @examplesEn Example of the specifications in the configuration file from the API for [Vineyards
 *     in Rhineland-Palatinate](https://demo.ldproxy.net/vineyards).
 *     <p>At API level:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   cache: MBTILES
 *   tileProvider:
 *     type: FEATURES
 *     multiCollectionEnabled: true
 *     zoomLevels:
 *       WebMercatorQuad:
 *         min: 5
 *         max: 16
 *     seeding:
 *       WebMercatorQuad:
 *         min: 5
 *         max: 11
 * ```
 *     </code>
 *     <p>For the vineyard objects (aggregation of adjacent objects up to zoom level 9):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   rules:
 *     WebMercatorQuad:
 *     - min: 5
 *       max: 7
 *       merge: true
 *       groupBy:
 *       - region
 *     - min: 8
 *       max: 8
 *       merge: true
 *       groupBy:
 *       - region
 *       - subregion
 *     - min: 9
 *       max: 9
 *       merge: true
 *       groupBy:
 *       - region
 *       - subregion
 *       - cluster
 * ```
 *     </code>
 *     <p>Example of a simple configuration (no seeding at startup, rebuilding the cache every
 *     hour):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   tileProvider:
 *     type: FEATURES
 *     seedingOptions:
 *       runOnStartup: false
 *       runPeriodic: '0 * * * *'
 *       purge: true
 * ```
 *     </code>
 *     <p>Example of using multiple threading for seeding:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   tileProvider:
 *     type: FEATURES
 *     seedingOptions:
 *       maxThreads: 2
 * ```
 *     </code>
 *     <p>For this, at least 4 threads must be configured for background processes in the global
 *     configuration, for example:
 *     <p><code>
 * ```yaml
 * backgroundTasks:
 *   maxThreads: 4
 * ```
 *     </code>
 *     <p>These settings make multiple background processes possible in the first place. So, even
 *     without changes to the seeding options, this would allow parallel execution of seeding for 4
 *     APIs.
 *     <p>If `maxThreads` in the seeding options is greater than 1, it means that the seeding will
 *     be split into n parts, where n is the number of threads available when the seeding starts,
 *     bounded by `seedingOptions.maxThreads`.
 *     <p>So, for example, setting `seedingOptions.maxThreads` to 2 with the specified `cfg.yml`
 *     will split the seeding into 2 parts if at least 2 of the 4 threads are available. If 3
 *     threads are used by other services, it will not be split. And if all 4 threads are busy, it
 *     will wait until at least 1 thread becomes available.
 *     <p>Example configuration (from the API [Low resolution satellite imagery (OpenMapTiles
 *     preview)](https://demo.ldproxy.net/openmaptiles)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProvider:
 *     type: MBTILES
 *     filename: satellite-lowres-v1.2-z0-z5.mbtiles
 * ```
 *     </code>
 *     <p>Example configuration:
 *     <p><code>
 * ```yaml
 * - buildingBlock: MAP_TILES
 *   enabled: true
 *   mapProvider:
 *     type: TILESERVER
 *     urlTemplate: 'https://www.example.com/tileserver/styles/topographic/{tileMatrix}/{tileCol}/{tileRow}@2x.{fileExtension}'
 *     tileEncodings:
 *       - WebP
 *       - PNG
 * ```
 *     </code>
 *     <p>Ein Beispiel für eine TileServer-GL-Konfiguration mit dem Style "topographic", der z.B.
 *     als Datenquelle die Vector Tiles der API verwenden kann:
 *     <p><code>
 * ```json
 * {
 *   "options": {},
 *   "styles": {
 *     "topographic": {
 *       "style": "topographic.json",
 *       "tilejson": {
 *         "type": "overlay",
 *         "bounds": [35.7550727, 32.3573507, 37.2052764, 33.2671397]
 *       }
 *     }
 *   },
 *   "data": {}
 * }
 * ```
 *     </code>
 * @examplesDe Beispiel für die Angaben in der Konfigurationsdatei aus der API für [Weinlagen in
 *     Rheinland-Pfalz](https://demo.ldproxy.net/vineyards).
 *     <p>Auf API-Ebene:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   cache: MBTILES
 *   tileProvider:
 *     type: FEATURES
 *     multiCollectionEnabled: true
 *     zoomLevels:
 *       WebMercatorQuad:
 *         min: 5
 *         max: 16
 *     seeding:
 *       WebMercatorQuad:
 *         min: 5
 *         max: 11
 * ```
 *     </code>
 *     <p>Für die Weinlagen-Objekte (Aggregation von aneinander angrenzenden Objekten bis Zoomstufe
 *     9):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   rules:
 *     WebMercatorQuad:
 *     - min: 5
 *       max: 7
 *       merge: true
 *       groupBy:
 *       - region
 *     - min: 8
 *       max: 8
 *       merge: true
 *       groupBy:
 *       - region
 *       - subregion
 *     - min: 9
 *       max: 9
 *       merge: true
 *       groupBy:
 *       - region
 *       - subregion
 *       - cluster
 * ```
 *     </code>
 *     <p>Beispiel für eine einfache Konfiguration (kein Seeding beim Start, Neuaufbau des Cache zu
 *     jeder Stunde):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   tileProvider:
 *     type: FEATURES
 *     seedingOptions:
 *       runOnStartup: false
 *       runPeriodic: '0 * * * *'
 *       purge: true
 * ```
 *     </code>
 *     <p>Beispiel für die Verwendung von mehreren Threads für das Seeding:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   tileProvider:
 *     type: FEATURES
 *     seedingOptions:
 *       maxThreads: 2
 * ```
 *     </code>
 *     <p>Hierfür müssen in der globalen Konfiguration mindestens 4 Threads für Hintergrundprozesse
 *     konfiguriert sein, zum Beispiel:
 *     <p><code>
 * ```yaml
 * backgroundTasks:
 *   maxThreads: 4
 * ```
 *     </code>
 *     <p>Durch diese Setzungen werden mehrere Hintergrundprozesse überhaupt erst ermöglicht. Selbst
 *     ohne Änderungen an den Seeding-Optionen würde dies also die parallele Ausführung des Seeding
 *     für 4 APIs ermöglichen.
 *     <p>Wenn `maxThreads` in den Seeding-Optionen größer als 1 ist, bedeutet das, dass das Seeding
 *     in n Teile geteilt wird, wobei n die Anzahl der verfügbaren Threads ist, wenn das Seeding
 *     beginnt, begrenzt durch `seedingOptions.maxThreads`.
 *     <p>Wenn man also zum Beispiel `seedingOptions.maxThreads` mit der angegebenen `cfg.yml` auf 2
 *     setzt, wird das Seeding in 2 Teile aufgeteilt, wenn mindestens 2 der 4 Threads verfügbar
 *     sind. Wenn 3 Threads von anderen Diensten benutzt werden, wird es nicht aufgeteilt. Und wenn
 *     alle 4 Threads belegt sind, wird gewartet, bis mindestens 1 Thread frei wird.
 *     <p>Beispielkonfiguration (aus der API[Satellitenbilder in niedriger Auflösung
 *     (OpenMapTiles-Preview)](https://demo.ldproxy.net/openmaptiles)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProvider:
 *     type: MBTILES
 *     filename: satellite-lowres-v1.2-z0-z5.mbtiles
 * ```
 *     </code>
 *     <p>Beispielkonfiguration:
 *     <p><code>
 * ```yaml
 * - buildingBlock: MAP_TILES
 *   enabled: true
 *   mapProvider:
 *     type: TILESERVER
 *     urlTemplate: 'https://www.example.com/tileserver/styles/topographic/{tileMatrix}/{tileCol}/{tileRow}@2x.{fileExtension}'
 *     tileEncodings:
 *       - WebP
 *       - PNG
 * ```
 *     </code>
 *     <p>Ein Beispiel für eine TileServer-GL-Konfiguration mit dem Style "topographic", der z.B.
 *     als Datenquelle die Vector Tiles der API verwenden kann:
 *     <p><code>
 * ```json
 * {
 *   "options": {},
 *   "styles": {
 *     "topographic": {
 *       "style": "topographic.json",
 *       "tilejson": {
 *         "type": "overlay",
 *         "bounds": [35.7550727, 32.3573507, 37.2052764, 33.2671397]
 *       }
 *     }
 *   },
 *   "data": {}
 * }
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public interface TilesConfiguration extends SfFlatConfiguration, CachingConfiguration {

  @Deprecated(since = "3.3")
  enum TileCacheType {
    FILES,
    MBTILES,
    NONE
  }

  /**
   * @langEn *Deprecated (from v4.0 on you have to use [Tile
   *     Provider](../../providers/tile/README.md) entities)* Specifies the data source for the
   *     tiles, see [Tile provider objects](#tile-provider).
   * @langDe *Deprecated (von v4.0 an müssen [Tile-Provider](../../providers/tile/README.md)
   *     Entities verwendet werden )* Spezifiziert die Datenquelle für die Kacheln, siehe
   *     [Tile-Provider-Objekte](#tile-provider).
   * @default { "type": "FEATURES", ... }
   */
  @Deprecated(since = "3.3")
  @Nullable
  TileProvider getTileProvider();

  /**
   * @langEn *Deprecated (will be renamed to `tileProvider` in v4.0)* Specifies the data source for
   *     the tiles, see [Tile Providers](../../providers/tile/README.md).
   * @langDe *Deprecated (wird in v4.0 zu `tileProvider` umbenannt)* Spezifiziert die Datenquelle
   *     für die Kacheln, siehe [Tile-Provider](../../providers/tile/README.md).
   * @default null
   * @since v3.3
   */
  @Deprecated(since = "3.3")
  @Nullable
  String getTileProviderId();

  /**
   * @langEn Specifies the tile provider layer.
   * @langDe Spezifiziert den Tile-Provider Layer.
   * @default {collectionId}
   * @since v3.3
   */
  @Nullable
  String getTileLayer();

  /**
   * @langEn Controls which formats are supported for the tileset resources. Available are [OGC
   *     TileSetMetadata](https://docs.ogc.org/DRAFTS/17-083r3.html#tsmd-json-encoding) ("JSON") and
   *     [TileJSON](https://github.com/mapbox/tilejson-spec) ("TileJSON").
   * @langDe Steuert, welche Formate für die Tileset-Ressourcen unterstützt werden sollen. Zur
   *     Verfügung stehen [OGC
   *     TileSetMetadata](https://docs.ogc.org/DRAFTS/17-083r3.html#tsmd-json-encoding) ("JSON") und
   *     [TileJSON](https://github.com/mapbox/tilejson-spec) ("TileJSON").
   * @default [ "JSON", "TileJSON" ]
   */
  List<String> getTileSetEncodings();

  /**
   * @langEn *Deprecated* `FILES` stores each tile as a file in the file system. `MBTILES` stores
   *     the tiles in an MBTiles file (one MBTiles file per tileset).
   * @langDe *Deprecated* `FILES` speichert jede Kachel als Datei im Dateisystem. `MBTILES`
   *     speichert die Kacheln in einer MBTiles-Datei (eine MBTiles-Datei pro Tileset).
   * @default FILES
   */
  @Deprecated(since = "3.3")
  @Nullable
  TileCacheType getCache();

  /**
   * @langEn Selection of the map client to be used in the HTML output. The default is MapLibre GL
   *     JS, only the "WebMercatorQuad" tiling scheme is supported. Alternatively 'OPEN_LAYERS' is
   *     supported as well (OpenLayers). The support of Open Layers only makes sense if other of the
   *     predefined tiling schemes should be supported in the HTML output. With `OPEN_LAYERS` no
   *     styles are supported.
   * @langDe Auswahl des zu verwendenden Map-Clients in der HTML-Ausgabe. Der Standard ist MapLibre
   *     GL JS, unterstützt wird nur das Kachelschema "WebMercatorQuad". Alternativ wird als auch
   *     `OPEN_LAYERS` unterstützt (OpenLayers). Die Unterstützung von Open Layers ist nur sinnvoll,
   *     wenn in der HTML Ausgabe auch andere der vordefinierten Kachelschemas unterstützt werden
   *     sollen. Bei `OPEN_LAYERS` werden keine Styles unterstützt.
   * @default MAP_LIBRE
   */
  @Nullable
  MapClient.Type getMapClientType();

  /**
   * @langEn A style in the style repository to be used in maps with tiles by default. With
   *     `DEFAULT` the `defaultStyle` from [module HTML](html.md) is used. With `NONE` a simple
   *     style with OpenStreetMap as base map is used. The style should cover all data and must be
   *     available in Mapbox Style format. A style with the name for the feature collection is
   *     searched for first; if none is found, a style with the name at the API level is searched
   *     for. If no style is found, 'NONE' is used.
   * @langDe Ein Style im Style-Repository, der standardmäßig in Karten mit den Tiles verwendet
   *     werden soll. Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet. Bei
   *     `NONE` wird ein einfacher Style mit OpenStreetMap als Basiskarte verwendet. Der Style
   *     sollte alle Daten abdecken und muss im Format Mapbox Style verfügbar sein. Es wird zuerst
   *     nach einem Style mit dem Namen für die Feature Collection gesucht; falls keiner gefunden
   *     wird, wird nach einem Style mit dem Namen auf der API-Ebene gesucht. Wird kein Style
   *     gefunden, wird `NONE` verwendet.
   * @default DEFAULT
   */
  @Nullable
  String getStyle();

  /**
   * @langEn If `true` is selected, the `minzoom` and `maxzoom` specifications for the layer objects
   *     are removed from the style specified in `style` so that the features are displayed at all
   *     zoom levels. This option should not be used if the style provides different presentations
   *     depending on the zoom level, otherwise all layers will be displayed at all zoom levels at
   *     the same time.
   * @langDe Bei `true` werden aus dem in `style` angegebenen Style die `minzoom`- und
   *     `maxzoom`-Angaben bei den Layer-Objekten entfernt, damit die Features in allen Zoomstufen
   *     angezeigt werden. Diese Option sollte nicht gewählt werden, wenn der Style unterschiedliche
   *     Präsentationen je nach Zoomstufe vorsieht, da ansonsten alle Layer auf allen Zoomstufen
   *     gleichzeitig angezeigt werden.
   * @default false
   */
  @Nullable
  Boolean getRemoveZoomLevelConstraints();

  /**
   * @langEn List of tile formats to be supported, in general `MVT` (Mapbox Vector Tiles), `PNG`,
   *     `WebP` and `JPEG` are allowed. The actually supported formats depend on the [Tile
   *     Provider](../../providers/tile/README.md).
   * @langDe Liste der zu unterstützenden Kachelformate, generell erlaubt sind `MVT` (Mapbox Vector
   *     Tiles), `PNG`, `WebP` und `JPEG`. Die konkret unterstützten Formate sind vom
   *     [Tile-Provider](../../providers/tile/README.md) abhängig.
   * @default [ "MVT" ]
   */
  List<String> getTileEncodings();

  // Note: Most configuration options have been moved to TileProviderFeatures and have been
  // deprecated here.
  // The getXyzDerived() methods support the deprecated configurations as well as the new style.

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default List<String> getTileEncodingsDerived() {
    return !getTileEncodings().isEmpty()
        ? getTileEncodings()
        : getTileProvider() instanceof TileProviderFeatures
            ? getTileProvider().getTileEncodings()
            : getTileProvider() instanceof TileProviderMbtiles
                    && Objects.nonNull(((TileProviderMbtiles) getTileProvider()).getTileEncoding())
                ? ImmutableList.of(((TileProviderMbtiles) getTileProvider()).getTileEncoding())
                : getTileProvider() instanceof TileProviderTileServer
                        && Objects.nonNull(
                            ((TileProviderTileServer) getTileProvider()).getTileEncodings())
                    ? getTileProvider().getTileEncodings()
                    : ImmutableList.of();
  }

  // TODO: always get from provider?
  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default `[ 0, 0 ]`
   */
  @Deprecated
  List<Double> getCenter();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default List<Double> getCenterDerived() {
    return !getCenter().isEmpty()
        ? getCenter()
        : getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getCenter()
            : getTileProvider() instanceof TileProviderMbtiles
                ? ((TileProviderMbtiles) getTileProvider()).getCenter()
                : ImmutableList.of();
  }

  /**
   * @langEn Controls the zoom levels available for each active tiling scheme as well as which zoom
   *     level to use as default.
   * @langDe Steuert die Zoomstufen, die für jedes aktive Kachelschema verfügbar sind sowie welche
   *     Zoomstufe als Default bei verwendet werden soll.
   * @default `{ "WebMercatorQuad" : { "min": 0, "max": 23 } }`
   */
  Map<String, MinMax> getZoomLevels();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Map<String, MinMax> getZoomLevelsDerived() {
    return !getZoomLevels().isEmpty()
        ? getZoomLevels()
        : getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getZoomLevels()
            : getTileProvider() instanceof TileProviderMbtiles
                ? ((TileProviderMbtiles) getTileProvider()).getZoomLevels()
                : ImmutableMap.of();
  }

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Set<String> getTileMatrixSets() {
    return getZoomLevelsDerived().keySet();
  }

  /**
   * @langEn Enable vector tiles for each *Feature Collection*. Every tile contains a layer with the
   *     feature from the collection.
   * @langDe Steuert, ob Vector Tiles für jede Feature Collection aktiviert werden sollen. Jede
   *     Kachel hat einen Layer mit den Features aus der Collection.
   * @default true
   * @since v3.3
   */
  @JsonAlias("singleCollectionEnabled")
  @Nullable
  Boolean getCollectionTiles();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default boolean hasCollectionTiles() {
    return Objects.equals(getCollectionTiles(), true)
        || (Objects.nonNull(getTileProvider()) && getTileProvider().isSingleCollectionEnabled());
  }

  /**
   * @langEn Enable vector tiles for the whole dataset. Every tile contains one layer per collection
   *     with the features of that collection.
   * @langDe Steuert, ob Vector Tiles für den Datensatz aktiviert werden sollen. Jede Kachel hat
   *     einen Layer pro Collection mit den Features aus der Collection.
   * @default true
   * @since v3.3
   */
  @JsonAlias("multiCollectionEnabled")
  @Nullable
  Boolean getDatasetTiles();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default boolean hasDatasetTiles() {
    return Objects.equals(getDatasetTiles(), true)
        || (Objects.nonNull(getTileProvider()) && getTileProvider().isMultiCollectionEnabled());
  }

  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default `{}`
   */
  @Deprecated
  Map<String, MinMax> getZoomLevelsCache();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Map<String, MinMax> getZoomLevelsCacheDerived() {
    return !getZoomLevelsCache().isEmpty()
        ? getZoomLevelsCache()
        : getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getZoomLevelsCache()
            : ImmutableMap.of();
  }

  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default `{}`
   */
  @Deprecated
  Map<String, MinMax> getSeeding();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Map<String, MinMax> getSeedingDerived() {
    return !getSeeding().isEmpty()
        ? getSeeding()
        : getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getSeeding()
            : ImmutableMap.of();
  }

  /**
   * @langEn *Deprecated (will be renamed to `seeding` in v4.0)* Controls how and when tiles are
   *     precomputed, see [Seeding options](#seeding-options).
   * @langDe *Deprecated (wird in v4.0 zu `seeding` umbenannt)* Steuert wie und wann Kacheln
   *     vorberechnet werden, siehe [Optionen für das * Seeding](#seeding-options).
   * @default {}
   */
  @Deprecated(since = "3.3")
  Optional<SeedingOptions> getSeedingOptions();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Optional<SeedingOptions> getSeedingOptionsDerived() {
    return getTileProvider() instanceof TileProviderFeatures
        ? ((TileProviderFeatures) getTileProvider()).getSeedingOptions().or(this::getSeedingOptions)
        : getSeedingOptions();
  }

  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default 100000
   */
  @Deprecated
  @Nullable
  Integer getLimit();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Integer getLimitDerived() {
    return Objects.nonNull(getLimit())
        ? getLimit()
        : Objects.requireNonNullElse(
            getTileProvider() instanceof TileProviderFeatures
                ? ((TileProviderFeatures) getTileProvider()).getLimit()
                : null,
            LIMIT_DEFAULT);
  }

  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default `false`
   */
  @Deprecated
  @Nullable
  Boolean getIgnoreInvalidGeometries();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default boolean isIgnoreInvalidGeometriesDerived() {
    return Objects.equals(getIgnoreInvalidGeometries(), true)
        || (getTileProvider() instanceof TileProviderFeatures
            && ((TileProviderFeatures) getTileProvider()).isIgnoreInvalidGeometries())
        || isEnabled();
  }

  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default `{}`
   */
  @Deprecated
  Map<String, List<LevelFilter>> getFilters();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Map<String, List<LevelFilter>> getFiltersDerived() {
    return !getFilters().isEmpty()
        ? getFilters()
        : getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getFilters()
            : ImmutableMap.of();
  }

  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default `{}`
   */
  @Deprecated
  Map<String, List<LevelTransformation>> getRules();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default Map<String, List<LevelTransformation>> getRulesDerived() {
    return !getRules().isEmpty()
        ? getRules()
        : getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getRules()
            : ImmutableMap.of();
  }

  /**
   * @langEn *Deprecated* See [Tile-Provider Features](#tile-provider-features).
   * @langDe *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
   * @default 0.5
   */
  @Deprecated
  @Nullable
  Double getMinimumSizeInPixel();

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default double getMinimumSizeInPixelDerived() {
    return Objects.requireNonNullElse(
        getMinimumSizeInPixel(),
        Objects.requireNonNullElse(
            getTileProvider() instanceof TileProviderFeatures
                ? ((TileProviderFeatures) getTileProvider()).getMinimumSizeInPixel()
                : null,
            MINIMUM_SIZE_IN_PIXEL));
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableTilesConfiguration.Builder();
  }

  @Override
  @SuppressWarnings("deprecation")
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableTilesConfiguration.Builder builder =
        ((ImmutableTilesConfiguration.Builder) source.getBuilder())
            .from(source)
            .from(this)
            .transformations(
                SfFlatConfiguration.super
                    .mergeInto((PropertyTransformations) source)
                    .getTransformations());

    TilesConfiguration src = (TilesConfiguration) source;

    if (Objects.nonNull(getTileProvider()) && Objects.nonNull(src.getTileProvider()))
      builder.tileProvider(getTileProvider().mergeInto(src.getTileProvider()));

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

    List<String> tileSetEncodings =
        Objects.nonNull(src.getTileSetEncodings())
            ? Lists.newArrayList(src.getTileSetEncodings())
            : Lists.newArrayList();
    getTileSetEncodings()
        .forEach(
            tileSetEncoding -> {
              if (!tileSetEncodings.contains(tileSetEncoding)) {
                tileSetEncodings.add(tileSetEncoding);
              }
            });
    builder.tileSetEncodings(tileSetEncodings);

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

    Map<String, MinMax> mergedZoomLevelsCache =
        Objects.nonNull(src.getZoomLevelsCache())
            ? Maps.newLinkedHashMap(src.getZoomLevelsCache())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getZoomLevelsCache()))
      getZoomLevelsCache().forEach(mergedZoomLevelsCache::put);
    builder.zoomLevelsCache(mergedZoomLevelsCache);

    Map<String, List<LevelTransformation>> mergedRules =
        Objects.nonNull(src.getRules())
            ? Maps.newLinkedHashMap(src.getRules())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getRules())) getRules().forEach(mergedRules::put);
    builder.rules(mergedRules);

    Map<String, List<LevelFilter>> mergedFilters =
        Objects.nonNull(src.getFilters())
            ? Maps.newLinkedHashMap(src.getFilters())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getFilters())) getFilters().forEach(mergedFilters::put);
    builder.filters(mergedFilters);

    if (!getCenter().isEmpty()) builder.center(getCenter());
    else if (!src.getCenter().isEmpty()) builder.center(src.getCenter());

    return builder.build();
  }

  /**
   * @return seeding map also considering the zoom level configuration (drops zoom levels outside of
   *     the range from seeding)
   */
  @JsonIgnore
  @Value.Lazy
  default Map<String, MinMax> getEffectiveSeeding() {
    Map<String, MinMax> baseSeeding = getSeedingDerived();
    if (baseSeeding.isEmpty()) return baseSeeding;

    Map<String, MinMax> zoomLevels = getZoomLevelsDerived();

    ImmutableMap.Builder<String, MinMax> responseBuilder = ImmutableMap.builder();
    for (Map.Entry<String, MinMax> entry : baseSeeding.entrySet()) {
      if (zoomLevels.containsKey(entry.getKey())) {
        MinMax minmax = zoomLevels.get(entry.getKey());
        int minSeeding = entry.getValue().getMin();
        int maxSeeding = entry.getValue().getMax();
        int minRange = minmax.getMin();
        int maxRange = minmax.getMax();
        if (maxSeeding >= minRange && minSeeding <= maxRange)
          responseBuilder.put(
              entry.getKey(),
              new ImmutableMinMax.Builder()
                  .min(Math.max(minSeeding, minRange))
                  .max(Math.min(maxSeeding, maxRange))
                  .build());
      }
    }

    return responseBuilder.build();
  }

  @Value.Check
  default TilesConfiguration alwaysFlatten() {
    Map<String, List<PropertyTransformation>> transformations = extendWithFlattenIfMissing();
    if (transformations.isEmpty()) {
      // a flatten transformation is already set
      return this;
    }

    return new ImmutableTilesConfiguration.Builder()
        .from(this)
        .transformations(transformations)
        .build();
  }
}
