/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ogcapi.json.domain.JsonConfiguration;
import io.swagger.v3.oas.models.media.ObjectSchema;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Singleton
@AutoBind
public class QueryablesFormatJson implements QueryablesFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "schema+json"))
            .label("JSON")
            .parameter("json")
            .build();

    @Inject
    QueryablesFormatJson() {
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(getBuildingBlockConfigurationType())
                      .map(cfg -> cfg.isEnabled())
                      .orElse(false) &&
                apiData.getExtension(JsonConfiguration.class)
                       .map(cfg -> cfg.isEnabled())
                       .orElse(true);
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
                       .getExtension(JsonConfiguration.class)
                       .map(cfg -> cfg.isEnabled())
                       .orElse(true);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                // TODO with OpenAPI 3.1 change to a link to a propert schema .schemaRef("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/schemas/v3.0/schema.json#/definitions/Schema")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public Object getEntity(JsonSchemaObject schemaQueryables, List<Link> links, String collectionId, OgcApi api, ApiRequestContext requestContext) {
        return schemaQueryables;
    }
}
