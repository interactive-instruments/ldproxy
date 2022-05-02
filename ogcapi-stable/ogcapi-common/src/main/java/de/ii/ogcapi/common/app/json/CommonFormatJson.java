/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.app.json;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ogcapi.common.domain.ImmutableLandingPage;
import de.ii.ogcapi.common.domain.LandingPage;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import io.swagger.v3.oas.models.media.Schema;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CommonFormatJson implements CommonFormatExtension, ConformanceClass {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema<?> schemaLandingPage;
    private final Map<String, Schema<?>> referencedSchemasLandingPage;
    private final Schema<?> schemaConformance;
    private final Map<String, Schema<?>> referencedSchemasConformance;

    @Inject
    public CommonFormatJson(ClassSchemaCache classSchemaCache) {
        schemaLandingPage = classSchemaCache.getSchema(LandingPage.class);
        referencedSchemasLandingPage = classSchemaCache.getReferencedSchemas(LandingPage.class);
        schemaConformance = classSchemaCache.getSchema(ConformanceDeclaration.class);
        referencedSchemasConformance = classSchemaCache.getReferencedSchemas(ConformanceDeclaration.class);
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/json");
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/")) {
            return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaLandingPage)
                .schemaRef(LandingPage.SCHEMA_REF)
                .referencedSchemas(referencedSchemasLandingPage)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
        } else if (path.equals("/conformance")) {
            return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaConformance)
                .schemaRef(ConformanceDeclaration.SCHEMA_REF)
                .referencedSchemas(referencedSchemasConformance)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
        }

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object getLandingPageEntity(LandingPage apiLandingPage, OgcApi api, ApiRequestContext requestContext) {
        return new ImmutableLandingPage.Builder()
                .from(apiLandingPage)
                .extensions(apiLandingPage.getExtensions()
                                          .entrySet()
                                          .stream()
                                          .filter(entry -> !entry.getKey().equals("datasetDownloadLinks"))
                                          .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }

    @Override
    public Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                       OgcApi api, ApiRequestContext requestContext) {
        return conformanceDeclaration;
    }
}
