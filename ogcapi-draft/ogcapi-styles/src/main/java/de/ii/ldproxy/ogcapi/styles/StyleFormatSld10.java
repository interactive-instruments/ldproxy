/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import io.swagger.v3.oas.models.media.ObjectSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
@Provides
@Instantiate
public class StyleFormatSld10 implements ConformanceClass, StyleFormatExtension {

    public static final String MEDIA_TYPE_STRING = "application/vnd.ogc.sld+xml;version=1.0" ;
    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "vnd.ogc.sld+xml", ImmutableMap.of("version", "1.0")))
            .label("SLD 1.0")
            .parameter("sld10")
            .build();

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-styles-1/0.0/conf/sld-10");
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(new ObjectSchema())
                .schemaRef("#/components/schemas/anyObject")
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "sld10";
    }

    @Override
    public String getSpecification() {
        return "http://www.opengeospatial.org/standards/sld";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public Response getStyleResponse(String styleId, File stylesheet, List<Link> links, OgcApi api, ApiRequestContext requestContext) throws IOException {
        final byte[] content = java.nio.file.Files.readAllBytes(stylesheet.toPath());

        // TODO

        return Response.ok()
                .entity(content)
                .type(MEDIA_TYPE.type())
                .links(links.isEmpty() ? null : links.stream().map(link -> link.getLink()).toArray(javax.ws.rs.core.Link[]::new))
                .build();
    }
}
