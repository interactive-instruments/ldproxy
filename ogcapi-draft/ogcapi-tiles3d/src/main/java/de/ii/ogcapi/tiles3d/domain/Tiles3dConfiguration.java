/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.tiles.domain.SeedingOptions;
import de.ii.xtraplatform.docs.DocIgnore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock 3D TILES
 * @langEn ### Prerequisites
 *     <p>The module requires that the feature provider includes a type `building`. The requirements
 *     for the type are the same as in the configuration of the [CityJSON
 *     encoding](features_-_cityjson.html#configuration).
 * @langDe ### Voraussetzungen
 *     <p>Das Modul erfordert, dass der Feature Provider einen Typ "building" enthält. Die
 *     Anforderungen an den Typ sind dieselben wie in der Konfiguration der
 *     [CityJSON-Kodierung](features_-_cityjson.html#konfiguration).
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: TILES3D
 *   enabled: true
 *   maxLevel: 9
 *   firstLevelWithContent: 5
 *   contentFilters:
 *   - diameter3d(bbox)>200
 *   - diameter3d(bbox)<=200 and diameter3d(bbox)>100
 *   - diameter3d(bbox)<=100 and diameter3d(bbox)>40
 *   - diameter3d(bbox)<=40 and diameter3d(bbox)>18
 *   - diameter3d(bbox)<=18
 *   tileFilters:
 *   - true
 *   - diameter3d(bbox)<=200
 *   - diameter3d(bbox)<=100
 *   - diameter3d(bbox)<=40
 *   - diameter3d(bbox)<=18
 *   geometricErrorRoot: 8192
 *   clampToEllipsoid: true
 *   subtreeLevels: 3
 *   seeding:
 *     runOnStartup: true
 *     runPeriodic: false
 *     purge: false
 *     maxThreads: 4
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableTiles3dConfiguration.Builder.class)
public interface Tiles3dConfiguration extends ExtensionConfiguration {

  /**
   * @langEn The first level of the tileset which will contain buildings. The value will depend on
   *     the spatial extent of the dataset, i.e., at what level of the implicit tiling scheme large
   *     buildings can be displayed.
   * @langDe Die erste Ebene des Kachelsatzes, die Gebäude enthalten wird. Der Wert hängt von der
   *     räumlichen Ausdehnung des Datensatzes ab, d. h. davon, auf welcher Ebene des impliziten
   *     Kachelschemas große Gebäude dargestellt werden können.
   * @default 0
   * @since v3.4
   */
  @Nullable
  Integer getFirstLevelWithContent();

  /**
   * @langEn The last level of the tileset which will contain buildings. The value will depend on
   *     the spatial extent of the dataset, i.e., at what level of the implicit tiling scheme small
   *     buildings can be displayed in detail.
   * @langDe Die erste Ebene des Kachelsatzes, die Gebäude enthalten wird. Der Wert hängt von der
   *     räumlichen Ausdehnung des Datensatzes ab, d. h. davon, auf welcher Ebene des impliziten
   *     Kachelschemas große Gebäude dargestellt werden können.
   * @default 0
   * @since v3.4
   */
  @Nullable
  Integer getMaxLevel();

  /**
   * @langEn A CQL2 text filter expression for each level between the `firstLevelWithContent` and
   *     the `maxLevel` to select the buildings to include in the tile on that level. Since the
   *     [refinement strategy](https://docs.ogc.org/cs/22-025r4/22-025r4.html#toc19) is always
   *     `ADD`, specify disjoint filter expressions, so that each building will be included on
   *     exactly one level.
   * @langDe Ein CQL2-Text-Filterausdruck für jede Ebene zwischen `firstLevelWithContent` und
   *     `maxLevel` zur Auswahl der Gebäude, die in die Kachel auf dieser Ebene aufgenommen werden
   *     sollen. Da die
   *     [Verfeinerungsstrategie](https://docs.ogc.org/cs/22-025r4/22-025r4.html#toc19) immer `ADD`
   *     ist, geben Sie disjunkte Filterausdrücke an, sodass jedes Gebäude auf genau einer Ebene
   *     einbezogen wird.
   * @default []
   * @since v3.4
   */
  List<String> getContentFilters();

  /**
   * @langEn A CQL2 text filter expression for each level between the `firstLevelWithContent` and
   *     the `maxLevel` to select the buildings to include in the tile on that level or in any of
   *     the child tiles. This filter expression is the same as all the `contentFilters` on this or
   *     higher levels combined with an `OR`. This is also the default value. However, depending on
   *     the filter expressions, this may lead to inefficient tile filters and to improve
   *     performance the tile filters can also be specified explicitly.
   * @langDe Ein CQL2-Text-Filterausdruck für jede Ebene zwischen `firstLevelWithContent` und
   *     `maxLevel` zur Auswahl der Gebäude, die in die Kachel auf dieser Ebene aufgenommen werden
   *     sollen oder in eine Kachel auf den tieferen Ebenen. Dieser Filterausdruck ist derselbe wie
   *     alle `contentFilters` auf dieser oder tieferen Ebenen, kombiniert mit einem `OR`. Dies ist
   *     auch der Standardwert. Je nach den Filterausdrücken kann dies jedoch zu ineffizienten
   *     Kachelfiltern führen, und zur Verbesserung der Leistung können die Kachelfilter auch
   *     explizit angegeben werden.
   * @default [ ... ]
   * @since v3.4
   */
  @Value.Default
  default List<String> getTileFilters() {
    int levels = getContentFilters().size();
    return IntStream.range(0, levels)
        .mapToObj(
            i ->
                String.format(
                    "(%s)",
                    IntStream.range(i, levels)
                        .mapToObj(j -> getContentFilters().get(j))
                        .collect(Collectors.joining(") OR ("))))
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * @langEn The error, in meters, introduced if a tile at level 0 (root) is rendered and its
   *     children at level 1 are not. At runtime, the geometric error is used to compute screen
   *     space error (SSE), i.e., the error measured in pixels.
   * @langDe Der Fehler in Metern, der entsteht, wenn eine Kachel auf Ebene 0 (Root) gerendert wird,
   *     ihre Kinder auf Ebene 1 jedoch nicht. Zur Laufzeit wird der geometrische Fehler zur
   *     Berechnung des Bildschirmabstandsfehlers (SSE) verwendet, d. h. des in Pixeln gemessenen
   *     Fehlers.
   * @default 0
   * @since v3.4
   */
  Float getGeometricErrorRoot();

  /**
   * @langEn The number of levels in each Subtree.
   * @langDe Die Anzahl der Ebenen in jedem Subtree.
   * @default 3
   * @since v3.4
   */
  @Nullable
  Integer getSubtreeLevels();

  /**
   * @langEn Controls how and when tiles are precomputed, see [Seeding options in the Tiles building
   *     block](tiles.md#seeding-options).
   * @langDe Steuert wie und wann Kacheln vorberechnet werden, siehe [Optionen für das Seeding im
   *     Modul Tiles](tiles.md#seeding-options).
   * @default {}
   * @since v3.4
   */
  Optional<SeedingOptions> getSeeding();

  /**
   * @langEn If set to `true`, each building will be translated vertically so that the bottom of the
   *     building is on the WGS 84 ellipsoid. Use this option, if the data is intended to be
   *     rendered without a terrain model.
   * @langDe Bei der Einstellung `true` wird jedes Gebäude vertikal so verschoben, dass der Boden
   *     des Gebäudes auf dem WGS 84-Ellipsoid liegt. Verwenden Sie diese Option, wenn die Daten
   *     ohne ein Geländemodell gerendert werden sollen.
   * @default false
   * @since v3.4
   */
  @Nullable
  Boolean getClampToEllipsoid();

  @DocIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean shouldClampToEllipsoid() {
    return Boolean.TRUE.equals(getClampToEllipsoid());
  }

  /**
   * @langEn If the 3D Tiles should be rendered in the integrated Cesium client using the terrain
   *     model from Cesium Ion, specify the access token to use in requests.
   * @langDe Wenn die 3D-Kacheln im integrierten Cesium-Client unter Verwendung des Geländemodells
   *     von Cesium Ion gerendert werden sollen, geben Sie das Zugriffstoken an, das in Anfragen
   *     verwendet werden soll.
   * @default null
   * @since v3.4
   */
  Optional<String> getIonAccessToken();

  /**
   * @langEn If the 3D Tiles should be rendered in the integrated Cesium client using the terrain
   *     model from MapTiler, specify the api key to use in requests.
   * @langDe Wenn die 3D-Kacheln im integrierten Cesium-Client unter Verwendung des Geländemodells
   *     von MapTiler gerendert werden sollen, geben Sie den API-Schlüssel an, der in Anfragen
   *     verwendet werden soll.
   * @default null
   * @since v3.4
   */
  Optional<String> getMaptilerApiKey();

  /**
   * @langEn If the 3D Tiles should be rendered in the integrated Cesium client using an external
   *     Terrain Provider, specify the URI of the provider.
   * @langDe Wenn die 3D-Kacheln im integrierten Cesium-Client unter Verwendung des Geländemodells
   *     eines externen Terrain Anbieters gerendert werden sollen, geben Sie die URI des Providers
   *     an.
   * @default null
   * @since v3.4
   */
  Optional<String> getCustomTerrainProviderUri();

  /**
   * @langEn If the terrain does not match the height values in the data, this option can be used to
   *     translate the buildings vertically in the integrated Cesium client.
   * @langDe Wenn das Gelände nicht mit den Höhenwerten in den Daten übereinstimmt, kann diese
   *     Option verwendet werden, um die Gebäude im integrierten Caesium-Client vertikal zu
   *     verschieben.
   * @default 0
   * @since v3.4
   */
  Optional<Double> getTerrainHeightDifference();

  /**
   * @langEn A style in the style repository of the collection to be used in maps with 3D Tiles.
   *     With `DEFAULT` the `defaultStyle` from [module HTML](html.md) is used. With `NONE` the
   *     default Cesium style is used. The style must be available in the 3D Tiles Styling format.
   *     If no style is found, 'NONE' is used.
   * @langDe Ein Style im Style-Repository der Collection, der in Karten mit den 3D Tiles verwendet
   *     werden soll. Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet. Bei
   *     `NONE` wird der Standard-Style von Cesium verwendet. Der Style muss im Format 3D Tiles
   *     Styling verfügbar sein. Wird kein Style gefunden, wird `NONE` verwendet.
   * @default DEFAULT
   */
  @Nullable
  String getStyle();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableTiles3dConfiguration.Builder();
  }

  @Value.Check
  default void check() {
    Preconditions.checkState(
        Objects.requireNonNull(getMaxLevel()) <= 16,
        "The maximum level that is supported is 16. Found: %s.",
        getMaxLevel());
    //noinspection ConstantConditions
    Preconditions.checkState(
        getContentFilters().isEmpty()
            || getContentFilters().size() == getMaxLevel() - getFirstLevelWithContent() + 1,
        "The length of 'contentFilters' must be the same as the levels with content. Found: %s filters and %s levels with content.",
        getContentFilters().size(),
        getMaxLevel() - getFirstLevelWithContent() + 1);
  }
}
