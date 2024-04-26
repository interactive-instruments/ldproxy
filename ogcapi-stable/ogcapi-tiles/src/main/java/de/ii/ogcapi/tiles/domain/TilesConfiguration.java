/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.DATASET_TILES;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.features.core.domain.SfFlatConfiguration;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.tiles.domain.TileAccess;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock TILES
 * @langEn ### Prerequisites
 *     <p>The building block *Tile Matrix Sets* must be enabled. If that building block is not
 *     configured, it is automatically enabled if *Tiles* is enabled.
 *     <p>### Storage
 *     <p>The tile cache is located in the [Store](../../application/20-configuration/10-store-new.md) as resource with path
 *       `tiles/{apiId}`.
 *     <p>If the data for an API or tile configuration has been changed, then
 *     the cache directory for the API should be deleted so that the cache is rebuilt with the
 *     updated data or rules.
 * @langDe ### Voraussetzungen
 *     <p>Das Modul *Tile Matrix Sets* muss aktiviert sein. Wenn dieses Modul nicht konfiguriert
 *     ist, wird es automatisch aktiviert, wenn *Tiles* aktiviert ist.
 *     <p>### Storage
 *     <p>Der Tile-Cache liegt im [Store](../../application/20-configuration/10-store-new.md)
 *       als Ressource mit dem Pfad `tiles/{apiId}`.
 *     <p>Wenn die Daten zu einer API oder Kachelkonfiguration geändert wurden, dann sollte das
 *     Cache-Verzeichnis für die API gelöscht werden, damit der Cache mit den aktualisierten Daten
 *     oder Regeln neu aufgebaut wird.
 * @examplesEn Example of the specifications in the configuration file from the API for [Vineyards
 *     in Rhineland-Palatinate](https://demo.ldproxy.net/vineyards).
 *     <p>At API level (since there is only a single feature type, the dataset tileset is the same
 *     as the tileset of the single collection):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProviderTileset: vineyards
 * ```
 *     </code>
 *     <p>The tile provider, includes configuration for caches (two caches,
 *     an immutable cache up to level 12 and an unseeded dynamic cache for the other levels)
 *     adjacent features are aggregated up to zoom level 9:
 *     <p><code>
 * ```yaml
 * ---
 * id: vineyards-tiles
 * providerType: TILE
 * providerSubType: FEATURES
 * caches:
 * - type: IMMUTABLE
 *   storage: MBTILES
 *   levels:
 *     WebMercatorQuad:
 *       min: 5
 *       max: 12
 * - type: DYNAMIC
 *   storage: MBTILES
 *   seeded: false
 *   levels:
 *     WebMercatorQuad:
 *       min: 13
 *       max: 18
 * tilesets:
 *   vineyards:
 *     id: vineyards
 *     levels:
 *       WebMercatorQuad:
 *         min: 5
 *         max: 18
 *     transformations:
 *       WebMercatorQuad:
 *       - min: 5
 *         max: 7
 *         merge: true
 *         groupBy:
 *         - region
 *       - min: 8
 *         max: 8
 *         merge: true
 *         groupBy:
 *         - region
 *         - subregion
 *       - min: 9
 *         max: 9
 *         merge: true
 *         groupBy:
 *         - region
 *         - subregion
 *         - cluster
 * ```
 *     </code>
 *     <p>Seeding example (no seeding at startup, rebuilding the cache every hour) in a Features
 *     tile provider:
 *     <p><code>
 * ```yaml
 * providerType: TILE
 * providerSubType: FEATURES
 * seeding:
 *   runOnStartup: false
 *   runPeriodic: '0 * * * *'
 *   purge: true
 * ```
 *     </code>
 *     <p>Example of using four threads for seeding:
 *     <p><code>
 * ```yaml
 * providerType: TILE
 * providerSubType: FEATURES
 * seeding:
 *   maxThreads: 4
 * ```
 *     </code>
 *     <p>For this, at least 4 threads must be configured for background processes in the global
 *     configuration (`cfg.yml`), for example:
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
 *     <p>So, for example, setting `seeding.maxThreads` to 2 with the specified `cfg.yml`
 *     will split the seeding into 2 parts if at least 2 of the 4 threads are available. If 3
 *     threads are used by other services, it will not be split. And if all 4 threads are busy, it
 *     will wait until at least 1 thread becomes available.
 *     <p>Example of the specifications in the configuration file from the API [Earth at
 *     Night](https://demo.ldproxy.net/earthatnight), which has an MBTiles tile provider..
 *     <p>At API level, only the TILES building block needs to be enabled and the tileset in the
 *     tile provider is referenced:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProviderTileset: earthatnight
 * ```
 *     </code>
 *     <p>The tile provider defines a single tileset and references the MBTiles file:
 *     <p><code>
 * ```yaml
 * ---
 * id: earthatnight-tiles
 * providerType: TILE
 * providerSubType: MBTILES
 * tilesets:
 *   earthatnight:
 *     id: earthatnight
 *     source: earthatnight/dnb_land_ocean_ice.2012.54000x27000_geo.mbtiles
 * ```
 *     </code>
 * @examplesDe Beispiel für die Angaben in der Konfigurationsdatei aus der API für [Weinlagen in
 *     Rheinland-Pfalz](https://demo.ldproxy.net/vineyards).
 *     <p>Auf API-Ebene (da es nur eine einzige Objektart gibt, ist das Tileset des Datensatzes
 *     dasselbe wie das Tileset der einzigen Collection):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProviderTileset: vineyards
 * ```
 *     </code>
 *     <p>Der Tile Provider, mit der Konfiguration für Caches (zwei Caches, ein unveränderlicher
 *     Cache bis zur Ebene 12 und ein dynamischer Cache ohne Seeding für die anderen Ebenen).
 *     Angrenzende Features werden bis zur Zoomstufe 9 zusammengefasst:
 *     <p><code>
 * ```yaml
 * ---
 * id: vineyards-tiles
 * providerType: TILE
 * providerSubType: FEATURES
 * caches:
 * - type: IMMUTABLE
 *   storage: MBTILES
 *   levels:
 *     WebMercatorQuad:
 *       min: 5
 *       max: 12
 * - type: DYNAMIC
 *   storage: MBTILES
 *   seeded: false
 *   levels:
 *     WebMercatorQuad:
 *       min: 13
 *       max: 18
 * tilesets:
 *   vineyards:
 *     id: vineyards
 *     levels:
 *       WebMercatorQuad:
 *         min: 5
 *         max: 18
 *     transformations:
 *       WebMercatorQuad:
 *       - min: 5
 *         max: 7
 *         merge: true
 *         groupBy:
 *         - region
 *       - min: 8
 *         max: 8
 *         merge: true
 *         groupBy:
 *         - region
 *         - subregion
 *       - min: 9
 *         max: 9
 *         merge: true
 *         groupBy:
 *         - region
 *         - subregion
 *         - cluster
 * ```
 *     </code>
 *     <p>Seeding-Beispiel (kein Seeding beim Start, Neuaufbau des Cache zu jeder Stunde) im
 *     Features-Tile-Provider:
 *     <p><code>
 * ```yaml
 * providerType: TILE
 * providerSubType: FEATURES
 * seeding:
 *   runOnStartup: false
 *   runPeriodic: '0 * * * *'
 *   purge: true
 * ```
 *     </code>
 *     <p>Beispiel für die Verwendung von mehreren Threads für das Seeding:
 *     <p><code>
 * ```yaml
 * providerType: TILE
 * providerSubType: FEATURES
 * seeding:
 *   maxThreads: 4
 * ```
 *     </code>
 *     <p>Hierfür müssen in der globalen Konfiguration (`cfg.yml`) mindestens 4 Threads für Hintergrundprozesse
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
 *     beginnt, begrenzt durch `seeding.maxThreads`.
 *     <p>Wenn man also zum Beispiel `seedingOptions.maxThreads` mit der angegebenen `cfg.yml` auf 2
 *     setzt, wird das Seeding in 2 Teile aufgeteilt, wenn mindestens 2 der 4 Threads verfügbar
 *     sind. Wenn 3 Threads von anderen Diensten benutzt werden, wird es nicht aufgeteilt. Und wenn
 *     alle 4 Threads belegt sind, wird gewartet, bis mindestens 1 Thread frei wird.
 *     <p>Beispielkonfiguration (from the API [Earth at
 *     Night](https://demo.ldproxy.net/earthatnight)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProvider:
 *     type: MBTILES
 *     filename: dnb_land_ocean_ice.2012.54000x27000_geo.mbtiles
 * ```
 *     <p>Beispielkonfiguration für die API [Earth at Night](https://demo.ldproxy.net/earthatnight),
 *     die einen MBTiles-Tile-Provider hat.
 *     <p>In der API muss das TILES-Modul aktiviert werden und das Tileset im Provider referenziert
 *     werden:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProviderTileset: earthatnight
 * ```
 *     </code>
 *     <p>Der Tile-Provider definiert ein einziges Tileset und referenziert die MBTiles-Datei:
 *     <p><code>
 * ```yaml
 * ---
 * id: earthatnight-tiles
 * providerType: TILE
 * providerSubType: MBTILES
 * tilesets:
 *   earthatnight:
 *     id: earthatnight
 *     source: earthatnight/dnb_land_ocean_ice.2012.54000x27000_geo.mbtiles
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "TILES")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public interface TilesConfiguration extends SfFlatConfiguration, CachingConfiguration {

  /**
   * @langEn Specifies the data source for the tiles, see [Tile
   *     Providers](../../providers/tile/README.md).
   * @langDe Spezifiziert die Datenquelle für die Kacheln, siehe
   *     [Tile-Provider](../../providers/tile/README.md).
   * @default null
   * @since v4.0
   */
  @JsonAlias("tileProviderId")
  @Nullable
  String getTileProvider();

  /**
   * @langEn Specifies the tileset from the tile provider that should be used. The default is
   *     `__all__` for dataset tiles and `{collectionId}` for collection tiles.
   * @langDe Spezifiziert das Tileset vom Tile-Provider das verwendet werden soll. Der Default ist
   *     `__all__` für Dataset Tiles und `{collectionId}` für Collection Tiles.
   * @default __all__ \| {collectionId}
   * @since v3.3
   */
  @Nullable
  String getTileProviderTileset();

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
   *     `DEFAULT` the `defaultStyle` from [module HTML](html.md) is used. If the map client is
   *     MapLibre, the style must be available in the Mapbox format. If the style is set to `NONE`,
   *     a simple wireframe style will be used with OpenStreetMap as a basemap. If the map client is
   *     Open Layers, the setting is ignored.
   * @langDe Ein Style im Style-Repository, der standardmäßig in Karten mit den Tiles verwendet
   *     werden soll. Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet.
   *     Handelt es sich bei dem Kartenclient um MapLibre, muss der Style im Mapbox-Format verfügbar
   *     sein. Wenn der Style auf `NONE` gesetzt ist, wird ein einfacher Wireframe Style mit
   *     OpenStreetMap als Basiskarte verwendet. Handelt es sich bei dem Kartenclient um Open
   *     Layers, wird die Angabe ignoriert.
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

  default boolean hasCollectionTiles(
      TilesProviders providers, OgcApiDataV2 apiData, String collectionId) {
    return Objects.nonNull(providers)
        && hasTiles(
            providers.getTileProvider(apiData, apiData.getCollectionData(collectionId)),
            collectionId,
            (tileset, tileAccess) -> true);
  }

  default boolean hasCollectionVectorTiles(
      TilesProviders providers, OgcApiDataV2 apiData, String collectionId) {
    return Objects.nonNull(providers)
        && hasTiles(
            providers.getTileProvider(apiData, apiData.getCollectionData(collectionId)),
            collectionId,
            (tileset, tileAccess) -> tileAccess.tilesetHasVectorTiles(tileset));
  }

  default boolean hasCollectionMapTiles(
      TilesProviders providers, OgcApiDataV2 apiData, String collectionId) {
    return Objects.nonNull(providers)
        && hasTiles(
            providers.getTileProvider(apiData, apiData.getCollectionData(collectionId)),
            collectionId,
            (tileset, tileAccess) -> tileAccess.tilesetHasMapTiles(tileset));
  }

  default boolean hasDatasetTiles(TilesProviders providers, OgcApiDataV2 apiData) {
    return Objects.nonNull(providers)
        && hasTiles(
            providers.getTileProvider(apiData), DATASET_TILES, (tileset, tileAccess) -> true);
  }

  default boolean hasDatasetVectorTiles(TilesProviders providers, OgcApiDataV2 apiData) {
    return Objects.nonNull(providers)
        && hasTiles(
            providers.getTileProvider(apiData),
            DATASET_TILES,
            (tileset, tileAccess) -> tileAccess.tilesetHasVectorTiles(tileset));
  }

  default boolean hasDatasetMapTiles(TilesProviders providers, OgcApiDataV2 apiData) {
    return Objects.nonNull(providers)
        && hasTiles(
            providers.getTileProvider(apiData),
            DATASET_TILES,
            (tileset, tileAccess) -> tileAccess.tilesetHasMapTiles(tileset));
  }

  default boolean hasTiles(
      Optional<TileProvider> provider,
      String tilesetDefault,
      BiFunction<String, TileAccess, Boolean> testForTileType) {
    String tileset = Objects.requireNonNullElse(getTileProviderTileset(), tilesetDefault);
    return provider
        .filter(tileProvider -> tileProvider.getData().getTilesets().containsKey(tileset))
        .filter(
            tileProvider ->
                tileProvider instanceof TileAccess
                    && testForTileType.apply(tileset, (TileAccess) tileProvider))
        .isPresent();
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableTilesConfiguration.Builder();
  }

  @Override
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

    return builder.build();
  }

  static Optional<FeatureTypeConfigurationOgcApi> getCollectionData(
      OgcApiDataV2 apiData, String tileset) {
    Optional<FeatureTypeConfigurationOgcApi> collectionData =
        apiData.getCollections().values().stream()
            .filter(
                collection ->
                    collection
                        .getExtension(TilesConfiguration.class)
                        .map(TilesConfiguration::getTileProviderTileset)
                        .filter(tileset::equals)
                        .isPresent())
            .findFirst();

    if (collectionData.isEmpty()) {
      collectionData =
          apiData.getCollections().values().stream()
              .filter(collection -> collection.getId().equals(tileset))
              .findFirst();
    }

    return collectionData;
  }
}
