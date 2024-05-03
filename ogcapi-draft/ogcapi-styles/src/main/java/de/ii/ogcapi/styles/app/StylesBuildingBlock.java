/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration.Builder;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Styles
 * @langEn Publish styles.
 * @langDe Veröffentlichung von Styles.
 * @scopeEn Clients can discover, access, use, and update styles in the following stylesheet
 *     encodings:
 *     <p><code>
 * - Mapbox/MapLibre Style
 * - OGC SLD 1.0
 * - OGC SLD 1.1
 * - QGIS QML
 * - ArcGIS Desktop (lyr)
 * - ArcGIS Pro (lyrx)
 * - 3D Tiles Styling
 *     </code>
 *     <p>Styles available as Mapbox/MapLibre Style can be used by ldproxy to render features and
 *     vector tiles where MapLibre is used as the map client. Styles available as 3D Tiles Styling
 *     can be used by ldproxy to render features and vector tiles where Cesium is used as the map
 *     client. See `defaultStyle` in [HTML](html.md), and `style` in [Features
 *     HTML](features_-_html.md) and [Tiles](tiles.md).
 * @scopeDe Clients können Styles in den folgenden Style-Formaten entdecken, darauf zugreifen, sie
 *     verwenden und aktualisieren:
 *     <p><code>
 * - Mapbox/MapLibre Style
 * - OGC SLD 1.0
 * - OGC SLD 1.1
 * - QGIS QML
 * - ArcGIS Desktop (lyr)
 * - ArcGIS Pro (lyrx)
 * - 3D Tiles Styling
 *     </code>
 *     <p>Styles, die als Mapbox/MapLibre Style verfügbar sind, können von ldproxy verwendet werden,
 *     um Features und Vektorkacheln zu rendern, wenn MapLibre als Kartenclient verwendet wird.
 *     Styles, die als 3D Tiles Styling verfügbar sind, können von ldproxy verwendet werden, um
 *     Features und 3D Tiles zu rendern, wenn Cesium als Kartenclient verwendet wird. Siehe
 *     `defaultStyle` in [HTML](html.md), und `style` in [Features HTML](features_-_html.md) und
 *     [Tiles](tiles.md).
 * @conformanceEn This building block implements requirements of the conformance classes *Core*,
 *     *Manage Styles*, *Validation of styles*, *Resources*, *Manage resources*, *Mapbox Style*,
 *     *OGC SLD 1.0*, *OGC SLD 1.1*, *HTML* and *Style information* from the draft specification
 *     [OGC API - Styles](https://docs.ogc.org/DRAFTS/20-009.html). The implementation is subject to
 *     change in the course of the development and approval process of the draft.
 * @conformanceDe Der Baustein basiert auf den Vorgaben der Konformitätsklassen *Core*, *Manage
 *     styles*, *Validation of styles*, *Mapbox Style*, *OGC SLD 1.0* und *OGC SLD 1.1* aus dem
 *     [Entwurf von OGC API - Styles](https://docs.ogc.org/DRAFTS/20-009.html). Die Implementierung
 *     wird sich im Zuge der weiteren Standardisierung der Spezifikation noch ändern.
 * @limitationsEn All encodings of a style must be created and maintained separately. The on-the-fly
 *     derivation of a stylesheet in another stylesheet encoding is not supported.
 *     <p>The following limitations apply for the stylesheet encodings:
 *     <p><code>
 * - Mapbox/MapLibre Style: The stylesheets are parsed into an internal Java object. Not all structures are
 *     supported or validated:
 *   - Only a single sprite is supported.
 *   - Terrain is not supported.
 *   - Text strings (e.g., color values) are not validated.
 *   - `filter`, `layout` and `paint` values are not validated.
 *   - Interpolate expressions are only supported in `layout` and `paint` values.
 *   - Sources: `volatile` is not supported.
 *   - Vector sources: `promoteId` is not supported.
 * - 3D Tiles Styling: The stylesheets are parsed into an internal Java object. Text strings in
 *     the JSON are not validated. The `defines` key is not supported.
 * - OGC SLD 1.0/1.1: The content of these stylesheets is validated against the XML Schema.
 * - QGIS QML, ArcGIS Pro (lyrx), ArcGIS Desktop (lyr): The content of these stylesheets is not validated.
 *     </code>
 * @limitationsDe Alle Formate eines Styles müssen separat erstellt und gepflegt werden. Die
 *     automatische Ableitung eines Stylesheets in einem anderen Format wird nicht unterstützt.
 *     <p>Die folgenden Einschränkungen gelten für die Style-Formate:
 *     <p><code>
 * - Mapbox/MapLibre Style: Die Stylesheets werden in ein internes Java-Objekt geparst. Nicht alle Strukturen werden unterstützt oder validiert:
 *   - Nur ein einzelnes Sprite wird unterstützt.
 *   - Terrain wird nicht unterstützt.
 *   - Textstrings (z.B. Farbwerte) werden nicht validiert.
 *   - `filter`, `layout` und `paint` Werte werden nicht validiert.
 *   - Interpolationsausdrücke werden nur in `layout` und `paint` Werten unterstützt.
 *   - Sources: `volatile` wird nicht unterstützt.
 *   - Vector-Sources: `promoteId` wird nicht unterstützt.
 * - 3D Tiles Styling: Die Stylesheets werden in ein internes Java-Objekt geparst. Textstrings in JSON werden nicht validiert. Der JSON-Schlüssel `defines` wird nicht unterstützt.
 * - OGC SLD 1.0/1.1: Der Inhalt dieser Stylesheets wird anhand des XML-Schemas validiert.
 * - QGIS QML, ArcGIS Pro (lyrx), ArcGIS Desktop (lyr): Der Inhalt dieser Stylesheets wird nicht validiert.
 *     </code>
 * @ref:cfg {@link de.ii.ogcapi.styles.domain.StylesConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.styles.infra.EndpointStyles}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyle}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleMetadata}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStylesCollection}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleCollection}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleMetadataCollection}, {@link
 *     de.ii.ogcapi.styles.infra.manager.EndpointStylesManager}, {@link
 *     de.ii.ogcapi.styles.infra.manager.EndpointStylesManagerCollection}
 * @ref:pathParameters {@link de.ii.ogcapi.styles.domain.PathParameterCollectionIdStyles}, {@link
 *     de.ii.ogcapi.styles.domain.PathParameterStyleId}
 * @ref:queryParameters {@link de.ii.ogcapi.styles.domain.QueryParameterFStyles}, {@link
 *     de.ii.ogcapi.styles.domain.QueryParameterFStyle}, {@link
 *     de.ii.ogcapi.styles.app.manager.QueryParameterDryRunStylesManager}
 */
@Singleton
@AutoBind
public class StylesBuildingBlock implements ApiBuildingBlock {

  public static final Optional<SpecificationMaturity> MATURITY =
      Optional.of(SpecificationMaturity.DRAFT_OGC);
  public static final Optional<ExternalDocumentation> SPEC =
      Optional.of(
          ExternalDocumentation.of(
              "https://docs.ogc.org/DRAFTS/20-009.html", "OGC API - Styles (DRAFT)"));
  public static final String STORE_RESOURCE_TYPE = "other-styles";

  private final ExtensionRegistry extensionRegistry;

  @Inject
  public StylesBuildingBlock(ExtensionRegistry extensionRegistry) {
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder()
        .enabled(false)
        .managerEnabled(false)
        .validationEnabled(false)
        .useIdFromStylesheet(false)
        .styleEncodings(
            extensionRegistry.getExtensionsForType(StyleFormatExtension.class).stream()
                .filter(FormatExtension::isEnabledByDefault)
                .map(format -> format.getMediaType().label())
                .sorted()
                .collect(ImmutableList.toImmutableList()))
        .deriveCollectionStyles(false)
        .webmapWithPopup(true)
        .webmapWithLayerControl(false)
        .layerControlAllLayers(false)
        .build();
  }
}
