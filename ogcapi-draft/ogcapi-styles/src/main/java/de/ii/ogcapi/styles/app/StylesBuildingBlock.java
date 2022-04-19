/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration.Builder;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.styles.infra.*;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * # Styles (STYLES)
 *
 * @lang_en Adds support for publishing and managing styles (*Mapbox Style* or *SLD*) and related
 * resources (symbols and sprites).
 *
 * ## Scope
 *
 * ### Conformance classes
 *
 * This module implements requirements of the conformance classes *Core*, *Manage Styles*, *Validation
 * of styles*, *Resources*, *Manage resources*, *Mapbox Style*, *OGC SLD 1.0*, *OGC SLD 1.1*,
 * *HTML* and *Style information* from the draft specification
 * [OGC API - Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html).
 * The implementation is subject to change in the course of the development and approval
 * process of the draft.
 *
 * ### Storage
 *
 * The stylesheets, style metadata and style information all reside as files in the data
 * directory:
 *
 * * Stylesheets reside under the relative path `styles/{apiId}/{styleId}.{ext}`, where
 * `{ext}` is either `mbs` (Mapbox), `sld10` (SLD 1.0) or `sld11` (SLD 1.1). The URIs
 * (Sprites, Glyphs, Source.url, Source.tiles) used in  Mapbox styles links might contain `{serviceUrl}`.
 * * Style metadata reside under the relative path `styles/{apiId}/{styleId}.metadata`.
 * Links might be templates (by setting `templated` to `true`) containing `{serviceUrl}`.
 * * Style information reside under the relative path `style-infos/{apiId}/{collectionId}.json`.
 * Links might be templates (by setting `templated` to `true`) containing `{serviceUrl}` and `{collectionId}`.
 * @lang_de Das Modul "Styles" kann für jede über ldproxy bereitgestellte API aktiviert werden.
 * Es ergänzt verschiedene Ressourcen für die Bereitstellung und Verwaltung von Styles.
 * (Mapbox Style, SLD).
 *
 * Das Modul basiert auf den Vorgaben der Konformitätsklassen *Core*, *Manage styles*,
 * *Validation of styles*, *Mapbox Style*, *OGC SLD 1.0* und *OGC SLD 1.1* aus dem
 * [Entwurf von OGC API - Styles](https://docs.ogc.org/DRAFTS/20-009.html).
 * Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.
 * Unterstützte Style-Formate sind:
 *
 * - Mapbox Style
 * - OGC SLD 1.0
 * - OGC SLD 1.1
 * - QGIS QML
 * - ArcGIS Desktop (lyr)
 * - ArcGIS Pro (lyrx)
 *
 * Style-Collections werden unter den folgenden `{baseResource}` zur Verfügung gestellt:
 *
 * - `{apiId}`
 * - `{apiId}/collection/{collectionId}`
 *
 * Erlaubte Zeichen für `{styleId}` sind alle Zeichen bis auf den Querstrich ("/").
 *
 * Die Stylesheets, die Style-Metadaten und die Style-Informationen liegen als Dateien
 * im ldproxy-Datenverzeichnis:
 *
 * - Die Stylesheets müssen unter dem relativen Pfad `api-resources/styles/{apiId}/{styleId}.{ext}` liegen.
 * Die URIs (Sprites, Glyphs, Source.url, Source.tiles) bei den Mapbox-Styles Links können dabei als Parameter
 * `{serviceUrl}` enthalten. Die Dateikennung `{ext}` muss den folgenden Wert in Abhängigkeit des Style-Formats haben:
 *   - Mapbox Style: "mbs"
 *   - OGC SLD 1.0: "sld10"
 *   - OGC SLD 1.1: "sld11"
 *   - QGIS QML: "qml"
 *   - ArcGIS Desktop: "lyr"
 *   - ArcGIS Pro: "lyrx
 * - Die Style-Metadaten müssen unter dem relativen Pfad
 * `api-resources/styles/{apiId}/{styleId}.metadata` liegen.
 * Links können dabei Templates sein (d.h. `templated` ist `true`) und als Parameter
 * `{serviceUrl}` enthalten.
 * @see StylesConfiguration
 * @see EndpointStyle
 * @see EndpointStyles
 * @see EndpointStyleMetadata
 * @see EndpointStyleMetadataCollection
 * @see EndpointStyleCollection
 * @see EndpointStylesCollection
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
        return new Builder().enabled(false)
                                                         .managerEnabled(false)
                                                         .validationEnabled(false)
                                                         .useIdFromStylesheet(false)
                                                         .resourcesEnabled(false)
                                                         .resourceManagerEnabled(false)
                                                         .styleEncodings(extensionRegistry.getExtensionsForType(
                                                                 StyleFormatExtension.class)
                                                                                     .stream()
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
