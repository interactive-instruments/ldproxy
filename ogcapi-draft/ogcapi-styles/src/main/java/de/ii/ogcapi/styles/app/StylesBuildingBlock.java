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
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration.Builder;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Styles
 * @langEn Adds support for publishing and managing styles (*Mapbox Style* or *SLD*) and related
 *     resources (symbols and sprites).
 * @conformanceEn This module implements requirements of the conformance classes *Core*, *Manage
 *     Styles*, *Validation of styles*, *Resources*, *Manage resources*, *Mapbox Style*, *OGC SLD
 *     1.0*, *OGC SLD 1.1*, *HTML* and *Style information* from the draft specification [OGC API -
 *     Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html). The implementation is subject to
 *     change in the course of the development and approval process of the draft.
 * @storageEn The stylesheets, style metadata and style information all reside as files in the data
 *     directory:
 *     <p>* Stylesheets reside under the relative path `styles/{apiId}/{styleId}.{ext}`, where
 *     `{ext}` is either `mbs` (Mapbox), `sld10` (SLD 1.0) or `sld11` (SLD 1.1). The URIs (Sprites,
 *     Glyphs, Source.url, Source.tiles) used in Mapbox styles links might contain `{serviceUrl}`. *
 *     Style metadata reside under the relative path `styles/{apiId}/{styleId}.metadata`. Links
 *     might be templates (by setting `templated` to `true`) containing `{serviceUrl}`. * Style
 *     information reside under the relative path `style-infos/{apiId}/{collectionId}.json`. Links
 *     might be templates (by setting `templated` to `true`) containing `{serviceUrl}` and
 *     `{collectionId}`.
 * @langDe Das Modul *Styles* kann für jede über ldproxy bereitgestellte API aktiviert werden. Es
 *     ergänzt verschiedene Ressourcen für die Bereitstellung und Verwaltung von Styles. (Mapbox
 *     Style, SLD).
 * @conformanceDe Das Modul basiert auf den Vorgaben der Konformitätsklassen *Core*, *Manage
 *     styles*, *Validation of styles*, *Mapbox Style*, *OGC SLD 1.0* und *OGC SLD 1.1* aus dem
 *     [Entwurf von OGC API - Styles](https://docs.ogc.org/DRAFTS/20-009.html). Die Implementierung
 *     wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern. Unterstützte
 *     Style-Formate sind:
 *     <p>- Mapbox Style - OGC SLD 1.0 - OGC SLD 1.1 - QGIS QML - ArcGIS Desktop (lyr) - ArcGIS Pro
 *     (lyrx)
 *     <p>Style-Collections werden unter den folgenden `{baseResource}` zur Verfügung gestellt:
 *     <p>- `{apiId}` - `{apiId}/collection/{collectionId}`
 *     <p>Erlaubte Zeichen für `{styleId}` sind alle Zeichen bis auf den Querstrich ("/").
 *     <p>Die Stylesheets, die Style-Metadaten und die Style-Informationen liegen als Dateien im
 *     ldproxy-Datenverzeichnis:
 *     <p>- Die Stylesheets müssen unter dem relativen Pfad
 *     `api-resources/styles/{apiId}/{styleId}.{ext}` liegen. Die URIs (Sprites, Glyphs, Source.url,
 *     Source.tiles) bei den Mapbox-Styles Links können dabei als Parameter `{serviceUrl}`
 *     enthalten. Die Dateikennung `{ext}` muss den folgenden Wert in Abhängigkeit des Style-Formats
 *     haben: - Mapbox Style: "mbs" - OGC SLD 1.0: "sld10" - OGC SLD 1.1: "sld11" - QGIS QML: "qml"
 *     - ArcGIS Desktop: "lyr" - ArcGIS Pro: "lyrx - Die Style-Metadaten müssen unter dem relativen
 *     Pfad `api-resources/styles/{apiId}/{styleId}.metadata` liegen. Links können dabei Templates
 *     sein (d.h. `templated` ist `true`) und als Parameter `{serviceUrl}` enthalten.
 * @example {@link de.ii.ogcapi.styles.domain.StylesConfiguration}
 * @propertyTable {@link de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration}
 * @endpointTable {@link de.ii.ogcapi.styles.infra.EndpointStyle}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleCollection}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleMetadata}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleMetadataCollection}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyles}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStylesCollection}
 * @queryParameterTable {@link de.ii.ogcapi.styles.domain.QueryParameterFStyle}, {@link
 *     de.ii.ogcapi.styles.domain.QueryParameterFStyles}
 * @todo de.ii.ogcapi.resources.infra.EndpointResource
 * @todo de.ii.ogcapi.resources.infra.EndpointResources
 */
@Singleton
@AutoBind
public class StylesBuildingBlock implements ApiBuildingBlock {

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
        .resourcesEnabled(false)
        .resourceManagerEnabled(false)
        .styleEncodings(
            extensionRegistry.getExtensionsForType(StyleFormatExtension.class).stream()
                .filter(FormatExtension::isEnabledByDefault)
                .map(format -> format.getMediaType().label())
                .collect(ImmutableList.toImmutableList()))
        .deriveCollectionStyles(false)
        .webmapWithPopup(true)
        .webmapWithLayerControl(false)
        .layerControlAllLayers(false)
        .build();
  }
}
