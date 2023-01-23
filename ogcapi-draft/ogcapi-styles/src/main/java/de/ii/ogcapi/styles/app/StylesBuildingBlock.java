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
 * @langEn Publish styles.
 * @langDe Veröffentlichung von Styles.
 * @scopeEn Supported stylesheet encodings are:
 *     <p><code>
 *  - Mapbox Style
 *  - OGC SLD 1.0
 *  - OGC SLD 1.1
 *  - QGIS QML
 *  - ArcGIS Desktop (lyr)
 *  - ArcGIS Pro (lyrx)
 *     </code>
 * @scopeDe Unterstützte Style-Formate sind:
 *     <p><code>
 * - Mapbox Style
 * - OGC SLD 1.0
 * - OGC SLD 1.1
 * - QGIS QML
 * - ArcGIS Desktop (lyr)
 * - ArcGIS Pro (lyrx)
 *     </code>
 * @conformanceEn This module implements requirements of the conformance classes *Core*, *Manage
 *     Styles*, *Validation of styles*, *Resources*, *Manage resources*, *Mapbox Style*, *OGC SLD
 *     1.0*, *OGC SLD 1.1*, *HTML* and *Style information* from the draft specification [OGC API -
 *     Styles](https://docs.ogc.org/DRAFTS/20-009.html). The implementation is subject to change in
 *     the course of the development and approval process of the draft.
 * @conformanceDe Das Modul basiert auf den Vorgaben der Konformitätsklassen *Core*, *Manage
 *     styles*, *Validation of styles*, *Mapbox Style*, *OGC SLD 1.0* und *OGC SLD 1.1* aus dem
 *     [Entwurf von OGC API - Styles](https://docs.ogc.org/DRAFTS/20-009.html). Die Implementierung
 *     wird sich im Zuge der weiteren Standardisierung der Spezifikation noch ändern.
 * @ref:cfg {@link de.ii.ogcapi.styles.domain.StylesConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.styles.infra.EndpointStyles}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyle}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleMetadata}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStylesCollection}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleCollection}, {@link
 *     de.ii.ogcapi.styles.infra.EndpointStyleMetadataCollection}, {@link
 *     de.ii.ogcapi.styles.infra.manager.EndpointStylesManager}, {@link
 *     de.ii.ogcapi.styles.infra.manager.EndpointStyleMetadataManager}, {@link
 *     de.ii.ogcapi.styles.infra.manager.EndpointStylesManagerCollection}, {@link
 *     de.ii.ogcapi.styles.infra.manager.EndpointStyleMetadataManagerCollection}
 * @ref:pathParameters {@link de.ii.ogcapi.styles.domain.PathParameterCollectionIdStyles}, {@link
 *     de.ii.ogcapi.styles.domain.PathParameterStyleId}
 * @ref:queryParameters {@link de.ii.ogcapi.styles.domain.QueryParameterFStyles}, {@link
 *     de.ii.ogcapi.styles.domain.QueryParameterFStyle}, {@link
 *     de.ii.ogcapi.styles.app.manager.QueryParameterDryRunStylesManager}
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
