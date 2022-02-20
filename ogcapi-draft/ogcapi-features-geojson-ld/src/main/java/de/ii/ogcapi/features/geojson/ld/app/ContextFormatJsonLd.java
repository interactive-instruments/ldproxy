/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.ld.app;

import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.ld.domain.GeoJsonLdConfiguration;
import io.swagger.v3.oas.models.media.ObjectSchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class ContextFormatJsonLd implements ContextFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "ld+json"))
            .label("JSON-LD")
            .parameter("jsonld")
            .build();

    @Inject
    ContextFormatJsonLd() {
    }

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
