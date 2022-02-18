/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojsonld.app;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojsonld.domain.GeoJsonLdConfiguration;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;

@Component
@Provides
@Instantiate
public class ContextFormatJsonLd implements ContextFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "ld+json"))
            .label("JSON-LD")
            .parameter("jsonld")
            .build();

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonLdConfiguration.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getCollections()
                      .values()
                      .stream()
                      .anyMatch(collectionData -> collectionData.getExtension(getBuildingBlockConfigurationType())
                                                                .map(cfg -> cfg.isEnabled())
                                                                .orElse(false) &&
                                                  collectionData.getExtension(GeoJsonConfiguration.class)
                                                                .map(cfg -> cfg.isEnabled())
                                                                .orElse(true));
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(getBuildingBlockConfigurationType())
                      .map(cfg -> cfg.isEnabled())
                      .orElse(false) &&
                apiData.getCollections()
                       .get(collectionId)
                       .getExtension(GeoJsonConfiguration.class)
                       .map(cfg -> cfg.isEnabled())
                       .orElse(true);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                // TODO propert JSON-LD context schema
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }
}
