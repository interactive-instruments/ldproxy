/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock STYLES
 * @langEn ### Storage
 *     <p>The stylesheets, style metadata and style information all reside as files in the data
 *     directory:
 *     <p><code>
 * - Stylesheets reside under the relative path `styles/{apiId}/{styleId}.{ext}`. The URIs (Sprites,
 *     Glyphs, Source.url, Source.tiles) used in Mapbox styles links might contain `{serviceUrl}`.
 *     The file extension `{ext}` must have the following value depending on the style encoding:
 *   - Mapbox Style: `mbs`
 *   - OGC SLD 1.0: `sld10`
 *   - OGC SLD 1.1: `sld11`
 *   - QGIS QML: `qml`
 *   - ArcGIS Desktop: `lyr`
 *   - ArcGIS Pro: `lyrx`
 *   - 3D Tiles Styling: `3dtiles`
 * - Style metadata reside under the relative path `styles/{apiId}/{styleId}.metadata`. Links
 *     might be templates (by setting `templated` to `true`) containing `{serviceUrl}`.
 * - Style information reside under the relative path `style-infos/{apiId}/{collectionId}.json`. Links
 *     might be templates (by setting `templated` to `true`) containing `{serviceUrl}` and
 *     `{collectionId}`.
 *     </code>
 * @langDe ### Storage
 *     <p>Die Stylesheets, die Style-Metadaten und die Style-Informationen liegen als Dateien im
 *     Datenverzeichnis:
 *     <p><code>
 * - Die Stylesheets müssen unter dem relativen Pfad
 *      `api-resources/styles/{apiId}/{styleId}.{ext}` liegen. Die URIs (Sprites, Glyphs, Source.url,
 *      Source.tiles) bei den Mapbox-Styles Links können dabei als Parameter `{serviceUrl}`
 *      enthalten. Die Dateikennung `{ext}` muss den folgenden Wert in Abhängigkeit des Style-Formats
 *      haben:
 *   - Mapbox Style: `mbs`
 *   - OGC SLD 1.0: `sld10`
 *   - OGC SLD 1.1: `sld11`
 *   - QGIS QML: `qml`
 *   - ArcGIS Desktop: `lyr`
 *   - ArcGIS Pro: `lyrx`
 *   - 3D Tiles Styling: `3dtiles`
 * - Die Style-Metadaten müssen unter dem relativen
 *     Pfad `api-resources/styles/{apiId}/{styleId}.metadata` liegen. Links können dabei Templates
 *     sein (d.h. `templated` ist `true`) und als Parameter `{serviceUrl}` enthalten.
 *     </code>
 * @examplesEn Example of the specifications in the configuration file:
 *     <p><code>
 * ```yaml
 * - buildingBlock: STYLES
 *   enabled: true
 *   styleEncodings:
 *   - Mapbox
 *   - HTML
 *   deriveCollectionStyles: true
 *   managerEnabled: false
 *   validationEnabled: false
 * ```
 *     </code>
 *     <p>Example Mapbox stylesheet
 *     <p><code>
 * ```json
 * {
 *   "bearing" : 0.0,
 *   "version" : 8,
 *   "pitch" : 0.0,
 *   "name" : "kitas",
 *   "center": [
 *     10.0,
 *     51.5
 *   ],
 *   "zoom": 12,
 *   "sources" : {
 *     "kindergarten" : {
 *       "type" : "vector",
 *       "tiles" : [ "{serviceUrl}/collections/governmentalservice/tiles/WebMercatorQuad/{z}/{y}/{x}?f=mvt" ],
 *       "maxzoom" : 16
 *     },
 *     "basemap" : {
 *       "type" : "raster",
 *       "tiles" : [ "https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png" ],
 *       "attribution" : "&copy; <a href=\"http://www.bkg.bund.de\" class=\"link0\" target=\"_new\">Bundesamt f&uuml;r Kartographie und Geod&auml;sie</a> 2017, <a href=\"http://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf\" class=\"link0\" target=\"_new\">Datenquellen</a>"
 *     }
 *   },
 *   "sprite" : "{serviceUrl}/resources/sprites-kitas",
 *   "glyphs": "https://go-spatial.github.io/carto-assets/fonts/{fontstack}/{range}.pbf",
 *   "layers" : [ {
 *       "id": "background",
 *       "type": "raster",
 *     "source" : "basemap"
 *   }, {
 *     "type" : "symbol",
 *     "source-layer" : "governmentalservice",
 *     "layout" : {
 *       "icon-image" : "kita",
 *       "icon-size" : 0.5
 *     },
 *     "paint" : {
 *       "icon-halo-width" : 2,
 *       "icon-opacity" : 1
 *     },
 *     "id" : "kita",
 *     "source" : "kitas"
 *   } ]
 * }
 * ```
 *     </code>
 *     <p>Example style information file
 *     <p><code>
 * ```json
 * {
 *   "id": "kitas",
 *   "title": "Kindertageseinrichtungen",
 *   "description": "(Hier steht eine Beschreibung des Styles...)",
 *   "keywords": [ ],
 *   "scope": "style",
 *   "version": "0.0.1",
 *   "stylesheets": [
 *     {
 *       "title": "Mapbox Style",
 *       "version": "8",
 *       "specification": "https://docs.mapbox.com/mapbox-gl-js/style-spec/",
 *       "native": true,
 *       "tilingScheme": "GoogleMapsCompatible",
 *       "link": {
 *         "href": "{serviceUrl}/styles/kitas?f=mbs",
 *         "rel": "stylesheet",
 *         "type": "application/vnd.mapbox.style+json",
 *         "templated": true
 *       }
 *     }
 *   ],
 *   "layers": [
 *     {
 *       "id": "governmentalservice",
 *       "type": "point",
 *       "sampleData": {
 *         "href": "{serviceUrl}/collections/governmentalservice/items?f=json",
 *         "rel": "data",
 *         "type": "application/geo+json",
 *         "templated": true
 *       }
 *     }
 *   ],
 *   "links": [
 *     {
 *       "href": "{serviceUrl}/resources/kitas-thumbnail.png",
 *       "rel": "preview",
 *       "type": "image/png",
 *       "title": "Thumbnail des Styles für Kindertagesstätten",
 *       "templated": true
 *     }
 *   ]
 * }
 * ```
 *     </code>
 * @examplesDe Beispiel für die Angaben in der Konfigurationsdatei:
 *     <p><code>
 * ```yaml
 * - buildingBlock: STYLES
 *   enabled: true
 *   styleEncodings:
 *   - Mapbox
 *   - HTML
 *   deriveCollectionStyles: true
 *   managerEnabled: false
 *   validationEnabled: false
 * ```
 *     </code>
 *     <p>Beispiel für ein Mapbox-Stylesheet:
 *     <p><code>
 * ```json
 * {
 *   "bearing" : 0.0,
 *   "version" : 8,
 *   "pitch" : 0.0,
 *   "name" : "kitas",
 *   "center": [
 *     10.0,
 *     51.5
 *   ],
 *   "zoom": 12,
 *   "sources" : {
 *     "kindergarten" : {
 *       "type" : "vector",
 *       "tiles" : [ "{serviceUrl}/collections/governmentalservice/tiles/WebMercatorQuad/{z}/{y}/{x}?f=mvt" ],
 *       "maxzoom" : 16
 *     },
 *     "basemap" : {
 *       "type" : "raster",
 *       "tiles" : [ "https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png" ],
 *       "attribution" : "&copy; <a href=\"http://www.bkg.bund.de\" class=\"link0\" target=\"_new\">Bundesamt f&uuml;r Kartographie und Geod&auml;sie</a> 2017, <a href=\"http://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf\" class=\"link0\" target=\"_new\">Datenquellen</a>"
 *     }
 *   },
 *   "sprite" : "{serviceUrl}/resources/sprites-kitas",
 *   "glyphs": "https://go-spatial.github.io/carto-assets/fonts/{fontstack}/{range}.pbf",
 *   "layers" : [ {
 *       "id": "background",
 *       "type": "raster",
 *     "source" : "basemap"
 *   }, {
 *     "type" : "symbol",
 *     "source-layer" : "governmentalservice",
 *     "layout" : {
 *       "icon-image" : "kita",
 *       "icon-size" : 0.5
 *     },
 *     "paint" : {
 *       "icon-halo-width" : 2,
 *       "icon-opacity" : 1
 *     },
 *     "id" : "kita",
 *     "source" : "kitas"
 *   } ]
 * }
 * ```
 *     </code>
 *     <p>Beispiel für eine Style-Metadaten-Datei:
 *     <p><code>
 * ```json
 * {
 *   "id": "kitas",
 *   "title": "Kindertageseinrichtungen",
 *   "description": "(Hier steht eine Beschreibung des Styles...)",
 *   "keywords": [ ],
 *   "scope": "style",
 *   "version": "0.0.1",
 *   "stylesheets": [
 *     {
 *       "title": "Mapbox Style",
 *       "version": "8",
 *       "specification": "https://docs.mapbox.com/mapbox-gl-js/style-spec/",
 *       "native": true,
 *       "tilingScheme": "GoogleMapsCompatible",
 *       "link": {
 *         "href": "{serviceUrl}/styles/kitas?f=mbs",
 *         "rel": "stylesheet",
 *         "type": "application/vnd.mapbox.style+json",
 *         "templated": true
 *       }
 *     }
 *   ],
 *   "layers": [
 *     {
 *       "id": "governmentalservice",
 *       "type": "point",
 *       "sampleData": {
 *         "href": "{serviceUrl}/collections/governmentalservice/items?f=json",
 *         "rel": "data",
 *         "type": "application/geo+json",
 *         "templated": true
 *       }
 *     }
 *   ],
 *   "links": [
 *     {
 *       "href": "{serviceUrl}/resources/kitas-thumbnail.png",
 *       "rel": "preview",
 *       "type": "image/png",
 *       "title": "Thumbnail des Styles für Kindertagesstätten",
 *       "templated": true
 *     }
 *   ]
 * }
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableStylesConfiguration.Builder.class)
public interface StylesConfiguration extends ExtensionConfiguration, CachingConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn List of enabled stylesheet encodings. Supported are Mapbox Style (`Mapbox`), OGC SLD
   *     1.0 (`SLD10`), OGC SLD 1.1 (`SLD11`), QGIS QML ("QML"), ArcGIS Layer ("lyr" und "lyrx"), 3D
   *     Tiles ("3D Tiles") and HTML (`HTML`). HTML is an output only encoding for web maps that
   *     requires a *Mapbox Style* stylesheet. For details see conformance classes *Mapbox Style*,
   *     *OGC SLD 1.0*, *OGC SLD 1.1* und *HTML*.
   * @langDe Steuert, welche Formate für Stylesheets unterstützt werden sollen. Zur Verfügung stehen
   *     Mapbox Style ("Mapbox"), OGC SLD 1.0 ("SLD10"), OGC SLD 1.1 ("SLD11"), QGIS QML ("QML"),
   *     ArcGIS Layer ("lyr" und "lyrx"), 3D Tiles ("3D Tiles") und HTML ("HTML"). HTML ist ein
   *     reines Ausgabeformat im Sinne einer Webmap und wird nur für Styles unterstützt, für die ein
   *     Stylesheet im Format Mapbox Style verfügbar ist. Siehe die Konformitätsklassen "Mapbox
   *     Style", "OGC SLD 1.0", "OGC SLD 1.1" und "HTML".
   * @default [ "Mapbox", "HTML" ]
   */
  List<String> getStyleEncodings();

  /**
   * @langEn Option to manage styles using POST, PUT and DELETE. If `styleInfosOnCollection` is
   *     enabled, style information may be created and updated using PATCH. Siehe die
   *     Konformitätsklasse "Manage styles".
   * @langDe Steuert, ob die Styles über POST, PUT und DELETE verwaltet werden können. Siehe die
   *     Konformitätsklasse "Manage styles".
   * @default false
   */
  @Nullable
  Boolean getManagerEnabled();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isManagerEnabled() {
    return Objects.equals(getManagerEnabled(), true);
  }

  /**
   * @langEn Option to validate styles when using POST and PUT by setting the query parameter
   *     `validate`. For details see conformance class *Validation of styles*.
   * @langDe Steuert, ob bei POST und PUT von Styles die Validierung der Styles über den
   *     Query-Parameter `validate` unterstützt werden soll. Siehe die Konformitätsklasse
   *     "Validation of styles".
   * @default false
   */
  @Nullable
  Boolean getValidationEnabled();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isValidationEnabled() {
    return Objects.equals(getValidationEnabled(), true);
  }

  @Nullable
  Boolean getUseIdFromStylesheet();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean shouldUseIdFromStylesheet() {
    return Objects.equals(getUseIdFromStylesheet(), true);
  }

  /**
   * @langEn *Deprecated* See `enabled` in [Modul Resources](resources.md).
   * @langDe *Deprecated* Siehe `enabled` in [Modul Resources](resources.md).
   * @default false
   */
  @Deprecated
  @Nullable
  Boolean getResourcesEnabled();

  @Deprecated
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isResourcesEnabled() {
    return Objects.equals(getResourcesEnabled(), true);
  }

  /**
   * @langEn *Deprecated* See `managerEnabled` in [Modul Resources](resources.md).
   * @langDe *Deprecated* Siehe `managerEnabled` in [Modul Resources](resources.md).
   * @default false
   */
  @Deprecated
  @Nullable
  Boolean getResourceManagerEnabled();

  @Deprecated
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isResourceManagerEnabled() {
    return Objects.equals(getResourceManagerEnabled(), true);
  }

  /**
   * @langEn *Deprecated* See `defaultStyle` in [Modul HTML](html.md).
   * @langDe *Deprecated* Siehe `defaultStyle` in [Modul HTML](html.md).
   * @default null
   */
  @Deprecated(since = "3.1.0")
  @Nullable
  String getDefaultStyle();

  /**
   * @langEn Only applies to styles in Mapbox Style format. Controls whether the styles at the
   *     collection level should be derived from the styles in the parent style collection. The
   *     prerequisite is that the name of the `source` in the stylesheet corresponds to `{apiId}`
   *     and the name of the `source-layer` corresponds to `{collectionId}`. If a style is to be
   *     used for displaying features in the FEATURES_HTML module, the option should be enabled.
   * @langDe Nur wirksam bei Styles im Format Mapbox Style. Steuert, ob die Styles auf der Ebene der
   *     Collections aus den Styles aus der übergeordneten Style-Collection abgeleitet werden
   *     sollen. Voraussetzung ist, dass der Name der `source` im Stylesheet der `{apiId}`
   *     entspricht und der Name der `source-layer` der `{collectionId}`. Sofern ein Style für die
   *     Darstellung von Features im Modul FEATURES_HTML verwendet werden soll, sollte die Option
   *     aktiviert sein.
   * @default false
   */
  @Nullable
  Boolean getDeriveCollectionStyles();

  /**
   * @langEn Option to support popups in web maps for *Mapbox Style* styles that show attributes for
   *     the top-most object.
   * @langDe Steuert, ob bei Webkarten zu Styles im Format Mapbox Style ein Popup mit den Attributen
   *     zum obersten Objekt angezeigt werden soll.
   * @default true
   */
  @Nullable
  Boolean getWebmapWithPopup();

  /**
   * @langEn Option to support layer controls in web maps for *Mapbox Style* styles. Allows to
   *     collectively enable and disable all layers for a certain feature collection.
   * @langDe Steuert, ob bei Webkarten zu Styles im Format Mapbox Style die Layer ein- und
   *     ausgeschaltet werden können. Ein- und ausgeschaltet werden können jeweils gebündelt alle
   *     Layer zu einer Feature Collection.
   * @default false
   */
  @Nullable
  Boolean getWebmapWithLayerControl();

  /**
   * @langEn Option to support layer controls for additional layers like background maps. Requires
   *     `webmapWithLayerControl: true`.
   * @langDe Nur wirksam bei `webmapWithLayerControl: true`. Steuert, ob auch Kartenlayer, die nicht
   *     aus den Vector Tiles dieser API, z.B. eine Hintergrundkarte, ein- und ausgeschaltet werden
   *     können.
   * @default false
   */
  @Nullable
  Boolean getLayerControlAllLayers();

  @Override
  default Builder getBuilder() {
    return new ImmutableStylesConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableStylesConfiguration.Builder builder =
        ((ImmutableStylesConfiguration.Builder) source.getBuilder()).from(source).from(this);

    List<String> styleEncodings =
        Lists.newArrayList(((StylesConfiguration) source).getStyleEncodings());
    getStyleEncodings()
        .forEach(
            styleEncoding -> {
              if (!styleEncodings.contains(styleEncoding)) {
                styleEncodings.add(styleEncoding);
              }
            });
    builder.styleEncodings(styleEncodings);

    return builder.build();
  }
}
