/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @lang_en Example of the specifications in the configuration file for the entire API
 * (from the API for [Topographic Data in Daraa, Syria](https://demo.ldproxy.net/daraa)):
 * @lang_de Beispiel für die Angaben in der Konfigurationsdatei für die gesamte API
 * (aus der API für [Topographische Daten in Daraa, Syrien](https://demo.ldproxy.net/daraa)):
 * @example <code>
 * ```yaml
 * - buildingBlock: FEATURES_HTML
 *   enabled: true
 *   style: 'topographic-with-basemap'
 * ```
 * </code>
 */

/**
 * @lang_en Example of the specifications in the configuration file for a feature collection:
 * @lang_de Beispiel für die Angaben in der Konfigurationsdatei für eine Feature Collection:
 * @example <code>
 * ```yaml
 * - buildingBlock: FEATURES_HTML
 *   itemLabelFormat: '{{ZI005_FNA}}'
 *   transformations:
 *     F_CODE:
 *       codelist: f_code
 *     ZI001_SDV:
 *       dateFormat: MM/dd/yyyy[', 'HH:mm:ss[' 'z]]
 *     RTY:
 *       codelist: rty
 *     FCSUBTYPE:
 *       codelist: fcsubtype
 *     TRS:
 *       codelist: trs
 *     RIN_ROI:
 *       codelist: roi
 *     ZI016_WTC:
 *       codelist: wtc
 *     RLE:
 *       codelist: rle
 *     LOC:
 *       codelist: loc
 * ```
 * </code>
 */

/**
 * @lang_en Example of using CesiumJS for building data that is partially composed
 * of building components. The floor slab is used as a fallback:
 * @lang_de Beispiel für die Verwendung von CesiumJS für Gebäudedaten,
 * die teilweise aus Bauteilen zusammengesetzt sind. Als Fallback wird die Bodenplatte verwendet:
 * @example <code>
 * ```yaml
 * - buildingBlock: FEATURES_HTML
 *   mapClientType: CESIUM
 *   geometryProperties:
 *   - consistsOfBuildingPart.lod1Solid
 *   - lod1Solid
 *   - lod1GroundSurface
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableFeaturesHtmlConfiguration.Builder.class)
public interface FeaturesHtmlConfiguration extends ExtensionConfiguration, PropertyTransformations {

  abstract class Builder extends ExtensionConfiguration.Builder {

  }

  enum LAYOUT {CLASSIC, COMPLEX_OBJECTS}
  enum POSITION {AUTO, TOP, RIGHT}

  /**
   * @lang_en *Deprecated* Superseded by `mapPosition` and the [`flattern` transformation](../providers/transformations.md).
   * @lang_de *Deprecated* Wird abgelöst von `mapPosition` und der [`flatten`-Transformation](../providers/transformations.md).
   * @default `CLASSIC`
   */
  @Deprecated(since = "3.1.0")
  @Nullable
  LAYOUT getLayout();

  /**
   * @lang_en Can be `TOP`, `RIGHT` or `AUTO`. `AUTO` is the default, it chooses `TOP` when any nested
   * objects are found and `RIGHT` otherwise.
   * @lang_de Mögliche Werte sind `TOP`, `RIGHT` und `AUTO`. `AUTO` ist der Default, es wählt automatisch
   * `TOP` wenn verschachtelte Objekte gefunden werden und sonst `RIGHT`.
   * @default `AUTO`
   */
  @Nullable
  POSITION getMapPosition();

  /**
   * @lang_en Define how the feature label for HTML is formed. Default is the feature id. Property names in double
   * curly braces will be replaced with the corresponding value.
   * @lang_de Steuert, wie der Titel eines Features in der HTML-Ausgabe gebildet wird. Standardmäßig ist der
   * Titel der Identifikator. In der Angabe können über die Angabe des Attributnamens in
   * doppelt-geschweiften Klammern Ersetzungspunkte für die Attribute des Features verwendet werden.
   * Es können nur Attribute verwendet werden, die nur einmal pro Feature vorkommen können.
   * Neben einer direkten Ersetzung mit dem Attributwert können auch [Filter](general-rules.md#String-Template-Filter)
   * angewendet werden. Ist ein Attribut `null`, dann wird der Ersetzungspunkt durch einen leeren String
   * ersetzt.
   * @default `{{id}}`
   */
  @JsonAlias("itemLabelFormat")
  Optional<String> getFeatureTitleTemplate();

  /**
   * @lang_en Optional transformations for feature properties for HTML, see [transformations](general-rules.md#transformations).
   * @lang_de Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in der HTML-Ausgabe
   * [transformiert](general-rules.md#transformations) werden.
   * @default `{}`
   */
  @JsonSerialize(converter = IgnoreLinksWildcardSerializer.class)
  @Override
  Map<String, List<PropertyTransformation>> getTransformations();

  /**
   * @lang_en The map client library to use to display features in the HTML representation. The default is MapLibre
   * GL (`MAP_LIBRE`). WIP: Cesium (`CESIUM`) can be used for displaying 3D features on a globe.
   * @lang_de Auswahl des in den Ressourcen "Features" und "Feature" zu verwendenden Map-Clients.
   * Der Standard ist MapLibre GL JS. Alternativ wird als auch `CESIUM` unterstützt (CesiumJS).
   * Die Unterstützung von CesiumJS zielt vor allem auf die Darstellung von 3D-Daten ab und besitzt
   * in der aktuellen Version experimentellen Charakter, es werden keine Styles unterstützt.
   * @default `MAP_LIBRE`
   */
  @Nullable
  MapClient.Type getMapClientType();

  /**
   * @lang_en An optional Mapbox style in the style repository to use for the map in the HTML representation
   * of a feature or feature collection. If set to `DEFAULT`, the `defaultStyle` configured in the
   * [HTML configuration](html.md) is used. If set to `NONE`, a simple wireframe style will be used
   * with OpenStreetMap as a basemap. The value is ignored, if the map client is not MapLibre.
   * @lang_de Ein Style im Style-Repository, der standardmäßig in Karten mit den Features verwendet werden soll.
   * Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet. Bei `NONE` wird ein einfacher
   * Style mit OpenStreetMap als Basiskarte verwendet. Der Style sollte alle Daten abdecken und muss im
   * Format Mapbox Style verfügbar sein. Es wird zuerst nach einem Style mit dem Namen für
   * die Feature Collection gesucht; falls keiner gefunden wird, wird nach einem Style mit dem Namen auf
   * der API-Ebene gesucht. Wird kein Style gefunden, wird `NONE` verwendet.
   * @default `DEFAULT`
   */
  @Nullable
  String getStyle();

  /**
   * @lang_en If `true`, any `minzoom` or `maxzoom` members are removed from the GeoJSON layers.
   * The value is ignored, if the map client is not MapLibre or `style` is `NONE`.
   * @lang_de Bei `true` werden aus dem in `style` angegebenen Style die `minzoom`- und `maxzoom`-Angaben bei
   * den Layer-Objekten entfernt, damit die Features in allen Zoomstufen angezeigt werden. Diese Option
   * sollte nicht gewählt werden, wenn der Style unterschiedliche Präsentationen je nach Zoomstufe
   * vorsieht, da ansonsten alle Layer auf allen Zoomstufen gleichzeitig angezeigt werden.
   * @default `false`
   */
  @Nullable
  Boolean getRemoveZoomLevelConstraints();

  /**
   * @lang_en TThis option works only for CesiumJS as map client. By default, the geometry identified in
   * the provider as PRIMARY_GEOMETRY is used for representation on the map.
   * This option allows multiple geometry properties to be specified in a list.
   * The first geometry property set for a feature will be used.
   * @lang_de Diese Option wirkt nur für CesiumJS als Map-Client. Als Standard wird die im Provider als
   * PRIMARY_GEOMETRY identifizierte Geometrie für die Darstellung in der Karte verwendet.
   * Diese Option ermöglicht es, mehrere Geometrieeigenschaften anzugeben in einer Liste anzugeben.
   * Die erste Geometrieeigenschaft, die für ein Feature gesetzt ist, wird dabei verwendet.
   * @default `[]`
   */
  @Nullable
  List<String> getGeometryProperties();

  /**
   * @lang_en This option can be used to set a custom maximum value for the `limit` parameter for the HTML
   * output. If no value is specified, the value from the Features Core module applies. When using
   * CesiumJS as a map client, a value of 100 is recommended.
   * @lang_de Mit dieser Option kann für die HTML-Ausgabe ein eigener Maximalwert für den Parameter `limit`
   * gesetzt werden. Sofern kein Wert angegeben ist, so gilt der Wert aus dem Modul "Features Core".
   * Bei der Verwendung von CesiumJS als Map-Client wird ein Wert von 100 empfohlen.
   * @default `null`
   */
  @Nullable
  Integer getMaximumPageSize();

  @Value.Check
  default FeaturesHtmlConfiguration backwardsCompatibility() {
    if (getLayout() == LAYOUT.CLASSIC
      && (!hasTransformation(PropertyTransformations.WILDCARD, transformations ->
        transformations.getFlatten().isPresent()))) {
      Map<String, List<PropertyTransformation>> transformations = withTransformation(PropertyTransformations.WILDCARD,
          new ImmutablePropertyTransformation.Builder()
          .flatten(".")
          .build());

        return new ImmutableFeaturesHtmlConfiguration.Builder()
            .from(this)
            .mapPosition(POSITION.RIGHT)
            .transformations(transformations)
            .build();
    }

    if (getLayout() == LAYOUT.COMPLEX_OBJECTS && getMapPosition() != POSITION.TOP) {
      return new ImmutableFeaturesHtmlConfiguration.Builder()
          .from(this)
          .mapPosition(POSITION.TOP)
          .build();
    }

    return this;
  }

  String LINK_WILDCARD = "*{objectType=Link}";

  @Value.Check
  default FeaturesHtmlConfiguration transformLinks() {
    if (!hasTransformation(LINK_WILDCARD, transformation -> transformation.getReduceStringFormat().isPresent())) {

      Map<String, List<PropertyTransformation>> transformations = withTransformation(LINK_WILDCARD,
          new ImmutablePropertyTransformation.Builder()
          .reduceStringFormat("<a href=\"{{href}}\">{{title}}</a>")
          .build());

      return new ImmutableFeaturesHtmlConfiguration.Builder()
          .from(this)
          .transformations(transformations)
          .build();
    }

    return this;
  }

  class IgnoreLinksWildcardSerializer extends
      StdConverter<Map<String, List<PropertyTransformation>>, Map<String, List<PropertyTransformation>>> {

    @Override
    public Map<String, List<PropertyTransformation>> convert(
        Map<String, List<PropertyTransformation>> value) {
      if (value.containsKey(LINK_WILDCARD) && value.get(LINK_WILDCARD).stream().anyMatch(transformation -> transformation.getReduceStringFormat().isPresent())) {

        return value.entrySet().stream()
            .filter(entry -> !Objects.equals(entry.getKey(), LINK_WILDCARD)
                || entry.getValue().size() != 1
                || entry.getValue().get(0).getReduceStringFormat().isEmpty())
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
      }

      return value;
    }
  }

  @Override
  default Builder getBuilder() {
    return new ImmutableFeaturesHtmlConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    return new ImmutableFeaturesHtmlConfiguration.Builder()
        .from(source)
        .from(this)
        .transformations(PropertyTransformations.super.mergeInto((PropertyTransformations) source).getTransformations())
        .build();
  }
}
