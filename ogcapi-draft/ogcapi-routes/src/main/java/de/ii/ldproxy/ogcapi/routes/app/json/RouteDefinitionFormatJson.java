/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionFormatExtension;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

@Component
@Provides
@Instantiate
public class RouteDefinitionFormatJson implements RouteDefinitionFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteDefinitionFormatJson.class);

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaRouteDefinition;
    public final static String SCHEMA_REF_ROUTE_DEFINITION = "#/components/schemas/RouteDefinition";

    public RouteDefinitionFormatJson(@Requires SchemaGenerator schemaGenerator,
                                     @Requires GeoJsonWriterRegistry geoJsonWriterRegistry) {
        this.schemaRouteDefinition = schemaGenerator.getSchema(RouteDefinition.class);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return getContent(apiData, path, HttpMethods.GET);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        if (path.equals("/routes/{routeId}/definition") && method.equals(HttpMethods.GET))
            return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaRouteDefinition)
                .schemaRef(SCHEMA_REF_ROUTE_DEFINITION)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
        return null;
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        if (path.equals("/routes") && method.equals(HttpMethods.POST))
            return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaRouteDefinition)
                .schemaRef(SCHEMA_REF_ROUTE_DEFINITION)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
        return null;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public byte[] getRouteDefinitionAsByteArray(RouteDefinition routeDefinition, OgcApiDataV2 apiData, ApiRequestContext requestContext) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        try {
            return mapper.writeValueAsBytes(routeDefinition);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write route definition.", e);
        }
    }
}
