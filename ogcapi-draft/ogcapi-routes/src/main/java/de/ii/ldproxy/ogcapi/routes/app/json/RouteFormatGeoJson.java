/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.routes.domain.Route;
import de.ii.ldproxy.ogcapi.routes.domain.RouteFormatExtension;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class RouteFormatGeoJson implements ConformanceClass, RouteFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteFormatGeoJson.class);

    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "geo+json"))
            .label("GeoJSON")
            .parameter("json")
            .build();

    private final GeoJsonWriterRegistry geoJsonWriterRegistry;
    private final Schema schemaRouteExchangeModel;
    public final static String SCHEMA_REF_REM = "#/components/schemas/RouteExchangeModel";

    @Inject
    public RouteFormatGeoJson(SchemaGenerator schemaGenerator,
                              GeoJsonWriterRegistry geoJsonWriterRegistry) {
        this.geoJsonWriterRegistry = geoJsonWriterRegistry;
        this.schemaRouteExchangeModel = schemaGenerator.getSchema(Route.class);
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/rem/0.0/conf/rem",
                                "http://www.opengis.net/spec/rem/0.0/conf/rem-overview",
                                "http://www.opengis.net/spec/rem/0.0/conf/rem-segment");
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return getContent(apiData, path, HttpMethods.GET);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        if ((path.equals("/routes") && method.equals(HttpMethods.POST)) ||
            (path.equals("/routes/{routeId}") && method.equals(HttpMethods.GET)))
            return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaRouteExchangeModel)
                .schemaRef(SCHEMA_REF_REM)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
        return null;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean canEncodeFeatures() {
        return true;
    }

    @Override
    public byte[] getRouteAsByteArray(Route route, OgcApiDataV2 apiData, ApiRequestContext requestContext) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        try {
            return mapper.writeValueAsBytes(route);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not write route.", e);
        }
    }
}
