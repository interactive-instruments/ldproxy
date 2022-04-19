/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.MINIMUM_SIZE_IN_PIXEL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.tiles.app.TileProviderFeatures;
import de.ii.ogcapi.tiles.app.TileProviderMbtiles;
import de.ii.ogcapi.tiles.app.TileProviderTileServer;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @lang_en Example of the specifications in the configuration file from the API for
 * [Vineyards in Rhineland-Palatinate](https://demo.ldproxy.net/vineyards).
 *
 * At API level:
 * @lang_de Beispiel für die Angaben in der Konfigurationsdatei aus der API für
 * [Weinlagen in Rheinland-Pfalz](https://demo.ldproxy.net/vineyards).
 *
 * Auf API-Ebene:
 * @example <code>
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
 * </code>
 */

/**
 * @lang_en For the vineyard objects (aggregation of adjacent objects up to zoom level 9):
 * @lang_de Für die Weinlagen-Objekte (Aggregation von aneinander angrenzenden Objekten bis Zoomstufe 9):
 * @example <code>
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
 * </code>
 */

/**
 * @lang_en Example of a simple configuration (no seeding at startup, rebuilding the cache every hour):
 * @lang_de Beispiel für eine einfache Konfiguration (kein Seeding beim Start, Neuaufbau des Cache zu jeder Stunde):
 * @example <code>
 * ```yaml
 * - buildingBlock: TILES
 *   tileProvider:
 *     type: FEATURES
 *     seedingOptions:
 *       runOnStartup: false
 *       runPeriodic: '0 * * * *'
 *       purge: true
 * ```
 * </code>
 */

/**
 * @lang_en Example of using multiple threading for seeding:
 * @lang_de Beispiel für die Verwendung von mehreren Threads für das Seeding:
 * @example <code>
 * ```yaml
 * - buildingBlock: TILES
 *   tileProvider:
 *     type: FEATURES
 *     seedingOptions:
 *       maxThreads: 2
 * ```
 * </code>
 */

/**
 * @lang_en For this, at least 4 threads must be configured for background processes in the global configuration, for example:
 * @lang_de Hierfür müssen in der globalen Konfiguration mindestens 4 Threads für Hintergrundprozesse konfiguriert sein, zum Beispiel:
 * @example <code>
 * ```yaml
 * backgroundTasks:
 *   maxThreads: 4
 * ```
 * </code>
 * @lang_en These seedings make multiple background processes possible in the first place. So, even without
 * changes to the seeding options, this would allow parallel execution of seeding for 4 APIs.
 *
 * If `maxThreads` in the seeding options is greater than 1, it means that the seeding will be split
 * into n parts, where n is the number of threads available when the seeding starts, bounded by
 * `seedingOptions.maxThreads`.
 *
 * So, for example, setting `seedingOptions.maxThreads` to 2 with the specified `cfg.yml` will split
 * the seeding into 2 parts if at least 2 of the 4 threads are available. If 3 threads are used by
 * other services, it will not be split. And if all 4 threads are busy, it will wait until at least
 * 1 thread becomes available.
 * @lang_de Durch diese Setzungen werden mehrere Hintergrundprozesse überhaupt erst ermöglicht.
 * Selbst ohne Änderungen an den Seeding-Optionen würde dies also die parallele Ausführung
 * des Seeding für 4 APIs ermöglichen.
 *
 * Wenn `maxThreads` in den Seeding-Optionen größer als 1 ist, bedeutet das, dass das Seeding
 * in n Teile geteilt wird, wobei n die Anzahl der verfügbaren Threads ist, wenn das Seeding
 * beginnt, begrenzt durch `seedingOptions.maxThreads`.
 *
 * Wenn man also zum Beispiel `seedingOptions.maxThreads` mit der angegebenen `cfg.yml` auf 2 setzt,
 * wird das Seeding in 2 Teile aufgeteilt, wenn mindestens 2 der 4 Threads verfügbar sind. Wenn 3
 * Threads von anderen Diensten benutzt werden, wird es nicht aufgeteilt. Und wenn alle 4 Threads
 * belegt sind, wird gewartet, bis mindestens 1 Thread frei wird.
 */

/**
 * @lang_en Example configuration (from the API [Low resolution satellite imagery (OpenMapTiles preview)](https://demo.ldproxy.net/openmaptiles)):
 * @lang_de Beispielkonfiguration (aus der API[Satellitenbilder in niedriger Auflösung (OpenMapTiles-Preview)](https://demo.ldproxy.net/openmaptiles)):
 * @example <code>
 * ```yaml
 * - buildingBlock: TILES
 *   enabled: true
 *   tileProvider:
 *     type: MBTILES
 *     filename: satellite-lowres-v1.2-z0-z5.mbtiles
 * ```
 * </code>
 */

/**
 * @lang_en Example configuration:
 * @lang_de Beispielkonfiguration:
 * @example <code>
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
 * </code>
 */

/**
 * @lang_en Ein Beispiel für eine TileServer-GL-Konfiguration mit dem Style "topographic", der z.B.
 * als Datenquelle die Vector Tiles der API verwenden kann:
 * @lang_de Ein Beispiel für eine TileServer-GL-Konfiguration mit dem Style "topographic", der z.B.
 * als Datenquelle die Vector Tiles der API verwenden kann:
 * @example <code>
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
 * </code>
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTilesConfiguration.Builder.class)
public interface TilesConfiguration extends ExtensionConfiguration, PropertyTransformations, CachingConfiguration {

    enum TileCacheType { FILES, MBTILES, NONE }

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    /**
     * @lang_en Specifies the data source for the tiles, see [Tile provider objects](#tile-provider).
     * @lang_de Spezifiziert die Datenquelle für die Kacheln, siehe [Tile-Provider-Objekte](#tile-provider).
     * @default `{ "type": "FEATURES", ... }`
     */
    @Nullable
    TileProvider getTileProvider(); // TODO add TileServer support

    /**
     * @lang_en Controls which formats are supported for the tileset resources.
     * Available are [OGC TileSetMetadata](https://docs.ogc.org/DRAFTS/17-083r3.html#tsmd-json-encoding)
     * ("JSON") and [TileJSON](https://github.com/mapbox/tilejson-spec) ("TileJSON").
     * @lang_de Steuert, welche Formate für die Tileset-Ressourcen unterstützt werden sollen. Zur Verfügung
     * stehen [OGC TileSetMetadata](https://docs.ogc.org/DRAFTS/17-083r3.html#tsmd-json-encoding)
     * ("JSON") und [TileJSON](https://github.com/mapbox/tilejson-spec) ("TileJSON").
     * @default `[ "JSON", "TileJSON" ]`
     */
    List<String> getTileSetEncodings();

    /**
     * @lang_en `FILES` stores each tile as a file in the file system. `MBTILES` stores the tiles in an
     * MBTiles file (one MBTiles file per tileset). It is recommended to use `MBTILES`.
     * It is planned to change the default to `MBTILES` with version 4.0.
     * @lang_de `FILES` speichert jede Kachel als Datei im Dateisystem. `MBTILES` speichert die Kacheln
     * in einer MBTiles-Datei (eine MBTiles-Datei pro Tileset). Es wird die Verwendung von
     * `MBTILES` empfohlen. Es ist geplant, den Default mit der Version 4.0 auf `MBTILES` zuändern.
     * @default `FILES`
     */
    @Nullable
    TileCacheType getCache();

    /**
     * @lang_en Selection of the map client to be used in the HTML output.
     * The default is MapLibre GL JS, only the "WebMercatorQuad" tiling scheme is supported.
     * Alternatively 'OPEN_LAYERS' is supported as well (OpenLayers). The support of Open Layers
     * only makes sense if other of the predefined tiling schemes should be supported in the HTML
     * output. With `OPEN_LAYERS` no styles are supported.
     * @lang_de Auswahl des zu verwendenden Map-Clients in der HTML-Ausgabe. Der Standard ist MapLibre
     * GL JS, unterstützt wird nur das Kachelschema "WebMercatorQuad". Alternativ wird als auch
     * `OPEN_LAYERS` unterstützt (OpenLayers). Die Unterstützung von Open Layers ist nur
     * sinnvoll, wenn in der HTML Ausgabe auch andere der vordefinierten Kachelschemas unterstützt
     * werden sollen. Bei `OPEN_LAYERS` werden keine Styles unterstützt.
     * @default `MAP_LIBRE`
     */
    @Nullable
    MapClient.Type getMapClientType();

    /**
     * @lang_en A style in the style repository to be used in maps with tiles by default.
     * With `DEFAULT` the `defaultStyle` from [module HTML](html.md) is used. With `NONE` a
     * simple style with OpenStreetMap as base map is used. The style should cover all data and
     * must be available in Mapbox Style format. A style with the name for the feature
     * collection is searched for first; if none is found, a style with the name at the
     * API level is searched for. If no style is found, 'NONE' is used.
     * @lang_de Ein Style im Style-Repository, der standardmäßig in Karten mit den Tiles verwendet
     * werden soll. Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet.
     * Bei `NONE` wird ein einfacher Style mit OpenStreetMap als Basiskarte verwendet.
     * Der Style sollte alle Daten abdecken und muss im Format Mapbox Style verfügbar sein.
     * Es wird zuerst nach einem Style mit dem Namen für die Feature Collection gesucht; falls
     * keiner gefunden wird, wird nach einem Style mit dem Namen auf der API-Ebene gesucht.
     * Wird kein Style gefunden, wird `NONE` verwendet.
     * @default `DEFAULT`
     */
    @Nullable
    String getStyle();

    /**
     * @lang_en If `true` is selected, the `minzoom` and `maxzoom`
     * specifications for the layer objects are removed from the style specified
     * in `style` so that the features are displayed at all zoom levels. This option
     * should not be used if the style provides different presentations depending on
     * the zoom level, otherwise all layers will be displayed at all zoom levels at the same time.
     * @lang_de Bei `true` werden aus dem in `style` angegebenen Style die `minzoom`- und
     * `maxzoom`-Angaben bei den Layer-Objekten entfernt, damit die Features in allen
     * Zoomstufen angezeigt werden. Diese Option sollte nicht gewählt werden,
     * wenn der Style unterschiedliche Präsentationen je nach Zoomstufe vorsieht,
     * da ansonsten alle Layer auf allen Zoomstufen gleichzeitig angezeigt werden.
     * @default `false`
     */
    @Nullable
    Boolean getRemoveZoomLevelConstraints();

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `[ "MVT" ]`
     */
    @Deprecated
    List<String> getTileEncodings();

    // Note: Most configuration options have been moved to TileProviderFeatures and have been deprecated here.
    // The getXyzDerived() methods support the deprecated configurations as well as the new style.

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default List<String> getTileEncodingsDerived() {
        return !getTileEncodings().isEmpty() ?
                getTileEncodings() :
                getTileProvider() instanceof TileProviderFeatures ?
                        getTileProvider().getTileEncodings() :
                        getTileProvider() instanceof TileProviderMbtiles && Objects.nonNull(((TileProviderMbtiles) getTileProvider()).getTileEncoding()) ?
                                ImmutableList.of(((TileProviderMbtiles) getTileProvider()).getTileEncoding()) :
                                getTileProvider() instanceof TileProviderTileServer && Objects.nonNull(((TileProviderTileServer) getTileProvider()).getTileEncodings()) ?
                                    getTileProvider().getTileEncodings() :
                                    ImmutableList.of();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `[ 0, 0 ]`
     */
    @Deprecated
    List<Double> getCenter();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default List<Double> getCenterDerived() {
        return !getCenter().isEmpty() ?
                getCenter() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getCenter() :
                        getTileProvider() instanceof TileProviderMbtiles ?
                                ((TileProviderMbtiles) getTileProvider()).getCenter() :
                                ImmutableList.of();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `{ "WebMercatorQuad" : { "min": 0, "max": 23 } }`
     */
    @Deprecated
    Map<String, MinMax> getZoomLevels();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, MinMax> getZoomLevelsDerived() {
        return !getZoomLevels().isEmpty() ?
                getZoomLevels() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getZoomLevels() :
                        getTileProvider() instanceof TileProviderMbtiles ?
                                ((TileProviderMbtiles) getTileProvider()).getZoomLevels() :
                                ImmutableMap.of();
    }

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Set<String> getTileMatrixSets() {
        return getZoomLevelsDerived().keySet();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `true`
     */
    @Deprecated
    @Nullable
    Boolean getSingleCollectionEnabled();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default boolean isSingleCollectionEnabled() {
        return Objects.equals(getSingleCollectionEnabled(), true)
                || (Objects.nonNull(getTileProvider()) && getTileProvider().isSingleCollectionEnabled())
                || isEnabled();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `true`
     */
    @Deprecated
    @Nullable
    Boolean getMultiCollectionEnabled();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default boolean isMultiCollectionEnabled() {
        return Objects.equals(getMultiCollectionEnabled(), true)
            || (Objects.nonNull(getTileProvider()) && getTileProvider().isMultiCollectionEnabled())
            || isEnabled();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `{}`
     */
    @Deprecated
    Map<String, MinMax> getZoomLevelsCache();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, MinMax> getZoomLevelsCacheDerived() {
        return !getZoomLevelsCache().isEmpty() ?
                getZoomLevelsCache() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getZoomLevelsCache() :
                        ImmutableMap.of();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `{}`
     */
    @Deprecated
    Map<String, MinMax> getSeeding();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, MinMax> getSeedingDerived() {
        return !getSeeding().isEmpty() ?
                getSeeding() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getSeeding() :
                        ImmutableMap.of();
    }

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Optional<SeedingOptions> getSeedingOptions() {
        return getTileProvider() instanceof TileProviderFeatures
            ? ((TileProviderFeatures) getTileProvider()).getSeedingOptions()
            : Optional.empty();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default 100000
     */
    @Deprecated
    @Nullable
    Integer getLimit();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    @Nullable
    default Integer getLimitDerived() {
        return Objects.nonNull(getLimit()) ?
                getLimit() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getLimit() :
                        null;
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
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
            || (getTileProvider() instanceof TileProviderFeatures && ((TileProviderFeatures) getTileProvider()).isIgnoreInvalidGeometries())
            || isEnabled();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `{}`
     */
    @Deprecated
    Map<String, List<PredefinedFilter>> getFilters();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, List<PredefinedFilter>> getFiltersDerived() {
        return !getFilters().isEmpty() ?
                getFilters() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getFilters() :
                        ImmutableMap.of();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default `{}`
     */
    @Deprecated
    Map<String, List<Rule>> getRules();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default Map<String, List<Rule>> getRulesDerived() {
        return !getRules().isEmpty() ?
                getRules() :
                getTileProvider() instanceof TileProviderFeatures ?
                        ((TileProviderFeatures) getTileProvider()).getRules() :
                        ImmutableMap.of();
    }

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default 0.1
     */
    @Deprecated
    Optional<Double> getMaxRelativeAreaChangeInPolygonRepair();

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default 1.0
     */
    @Deprecated
    Optional<Double> getMaxAbsoluteAreaChangeInPolygonRepair();

    /**
     * @lang_en *Deprecated* See [Tile-Provider Features](#tile-provider-features).
     * @lang_de *Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
     * @default 0.5
     */
    @Deprecated
    @Nullable
    Double getMinimumSizeInPixel();

    @Value.Auxiliary
    @Value.Derived
    @JsonIgnore
    default double getMinimumSizeInPixelDerived() {
        return Objects.requireNonNullElse(getMinimumSizeInPixel(),
            Objects.requireNonNullElse(getTileProvider() instanceof TileProviderFeatures
                    ? ((TileProviderFeatures) getTileProvider()).getMinimumSizeInPixel()
                    : null,
                MINIMUM_SIZE_IN_PIXEL));
    }

    @Override
    default Builder getBuilder() {
        return new ImmutableTilesConfiguration.Builder();
    }

    @Override
    @SuppressWarnings("deprecation")
    default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
        ImmutableTilesConfiguration.Builder builder = ((ImmutableTilesConfiguration.Builder) source.getBuilder())
                .from(source)
                .from(this)
                .transformations(PropertyTransformations.super.mergeInto((PropertyTransformations) source).getTransformations());

        TilesConfiguration src = (TilesConfiguration) source;

        if (Objects.nonNull(getTileProvider()) && Objects.nonNull(src.getTileProvider()))
            builder.tileProvider(getTileProvider().mergeInto(src.getTileProvider()));

        List<String> tileEncodings = Objects.nonNull(src.getTileEncodings()) ? Lists.newArrayList(src.getTileEncodings()) : Lists.newArrayList();
        getTileEncodings().forEach(tileEncoding -> {
            if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
            }
        });
        builder.tileEncodings(tileEncodings);

        List<String> tileSetEncodings = Objects.nonNull(src.getTileSetEncodings()) ? Lists.newArrayList(src.getTileSetEncodings()) : Lists.newArrayList();
        getTileSetEncodings().forEach(tileSetEncoding -> {
            if (!tileSetEncodings.contains(tileSetEncoding)) {
                tileSetEncodings.add(tileSetEncoding);
            }
        });
        builder.tileSetEncodings(tileSetEncodings);

        Map<String, MinMax> mergedSeeding = Objects.nonNull(src.getSeeding()) ? Maps.newLinkedHashMap(src.getSeeding()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getSeeding()))
            getSeeding().forEach(mergedSeeding::put);
        builder.seeding(mergedSeeding);

        Map<String, MinMax> mergedZoomLevels = Objects.nonNull(src.getZoomLevels()) ? Maps.newLinkedHashMap(src.getZoomLevels()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevels()))
            getZoomLevels().forEach(mergedZoomLevels::put);
        builder.zoomLevels(mergedZoomLevels);

        Map<String, MinMax> mergedZoomLevelsCache = Objects.nonNull(src.getZoomLevelsCache()) ? Maps.newLinkedHashMap(src.getZoomLevelsCache()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevelsCache()))
            getZoomLevelsCache().forEach(mergedZoomLevelsCache::put);
        builder.zoomLevelsCache(mergedZoomLevelsCache);

        Map<String, List<Rule>> mergedRules = Objects.nonNull(src.getRules()) ? Maps.newLinkedHashMap(src.getRules()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getRules()))
            getRules().forEach(mergedRules::put);
        builder.rules(mergedRules);

        Map<String, List<PredefinedFilter>> mergedFilters = Objects.nonNull(src.getFilters()) ? Maps.newLinkedHashMap(src.getFilters()) : Maps.newLinkedHashMap();
        if (Objects.nonNull(getFilters()))
            getFilters().forEach(mergedFilters::put);
        builder.filters(mergedFilters);

        if (!getCenter().isEmpty())
            builder.center(getCenter());
        else if (!src.getCenter().isEmpty())
            builder.center(src.getCenter());

        return builder.build();
    }

    /**
     * @return seeding map also considering the zoom level configuration (drops zoom levels outside of the range from seeding)
     */
    @JsonIgnore
    @Value.Lazy
    default Map<String, MinMax> getEffectiveSeeding() {
        Map<String, MinMax> baseSeeding = getSeedingDerived();
        if (baseSeeding.isEmpty())
            return baseSeeding;

        Map<String, MinMax> zoomLevels = getZoomLevelsDerived();

        ImmutableMap.Builder<String, MinMax> responseBuilder = ImmutableMap.builder();
        for (Map.Entry<String, MinMax> entry : baseSeeding.entrySet()) {
            if (zoomLevels.containsKey(entry.getKey())) {
                MinMax minmax = zoomLevels.get(entry.getKey());
                int minSeeding = entry.getValue()
                                      .getMin();
                int maxSeeding = entry.getValue()
                                      .getMax();
                int minRange = minmax.getMin();
                int maxRange = minmax.getMax();
                if (maxSeeding >= minRange && minSeeding <= maxRange)
                    responseBuilder.put(entry.getKey(), new ImmutableMinMax.Builder()
                            .min(Math.max(minSeeding, minRange))
                            .max(Math.min(maxSeeding, maxRange))
                            .build());
            }
        }

        return responseBuilder.build();
    }

    @Value.Check
    default TilesConfiguration alwaysFlatten() {
        if (!hasTransformation(PropertyTransformations.WILDCARD, transformation -> transformation.getFlatten().isPresent())) {

            Map<String, List<PropertyTransformation>> transformations = withTransformation(PropertyTransformations.WILDCARD,
                new ImmutablePropertyTransformation.Builder()
                .flatten(".")
                .build());

            return new ImmutableTilesConfiguration.Builder()
                .from(this)
                .transformations(transformations)
                .build();
        }

        return this;
    }

}
