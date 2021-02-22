/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.app.json;

import de.ii.ldproxy.ogcapi.common.domain.CommonFormatExtension;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.common.domain.LandingPage;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {CommonFormatJson.class, CommonFormatExtension.class, FormatExtension.class, ApiExtension.class})
@Instantiate
public class CommonFormatJson implements CommonFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaLandingPage;
    public final static String SCHEMA_REF_LANDING_PAGE = "#/components/schemas/LandingPage";
    private final Schema schemaConformance;
    public final static String SCHEMA_REF_CONFORMANCE = "#/components/schemas/ConformanceDeclaration";

    public CommonFormatJson() {
        schemaLandingPage = schemaGenerator.getSchema(LandingPage.class);
        schemaConformance = schemaGenerator.getSchema(ConformanceDeclaration.class);
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

        // TODO add examples
        if (path.equals("/"))
            return new ImmutableApiMediaTypeContent.Builder()
                        .schema(schemaLandingPage)
                        .schemaRef(SCHEMA_REF_LANDING_PAGE)
                        .ogcApiMediaType(MEDIA_TYPE)
                        .build();
        else if (path.equals("/conformance"))
            return new ImmutableApiMediaTypeContent.Builder()
                        .schema(schemaConformance)
                        .schemaRef(SCHEMA_REF_CONFORMANCE)
                        .ogcApiMediaType(MEDIA_TYPE)
                        .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object getLandingPageEntity(LandingPage apiLandingPage, OgcApi api, ApiRequestContext requestContext) {
        return apiLandingPage;
    }

    @Override
    public Object getConformanceEntity(ConformanceDeclaration conformanceDeclaration,
                                       OgcApi api, ApiRequestContext requestContext) {
        return conformanceDeclaration;
    }
}
