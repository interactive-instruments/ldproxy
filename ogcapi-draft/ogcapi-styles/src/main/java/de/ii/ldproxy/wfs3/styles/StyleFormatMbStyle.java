/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
@Provides
@Instantiate
public class StyleFormatMbStyle implements ConformanceClass, StyleFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFormatMbStyle.class);

    @Requires
    SchemaGenerator schemaGenerator;

    public static final String MEDIA_TYPE_STRING = "application/vnd.mapbox.style+json" ;
    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "vnd.mapbox.style+json"))
            .label("Mapbox Style")
            .parameter("mbs")
            .build();

    private final Schema schemaStyle;
    public final static String SCHEMA_REF_STYLE = "#/components/schemas/MbStyleStylesheet";

    public StyleFormatMbStyle() {
        schemaStyle = schemaGenerator.getSchema(MbStyleStylesheet.class);
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/t15/opf-styles-1/1.0/conf/mapbox-styles");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return getExtensionConfiguration(apiData, StylesConfiguration.class)
                .map(StylesConfiguration::getMbStyleEnabled)
                .orElse(false);
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schemaStyle)
                .schemaRef(SCHEMA_REF_STYLE)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public OgcApiMediaTypeContent getRequestContent(OgcApiApiDataV2 apiData, String path, OgcApiContext.HttpMethods method) {
        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schemaStyle)
                .schemaRef(SCHEMA_REF_STYLE)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "mbs";
    }

    @Override
    public String getSpecification() {
        return "https://docs.mapbox.com/mapbox-gl-js/style-spec/";
    }

    @Override
    public String getVersion() {
        return "8";
    }

    @Override
    public Response getStyleResponse(String styleId, File stylesheet, List<OgcApiLink> links, OgcApiApi api, OgcApiRequestContext requestContext) throws IOException {

        final byte[] content = java.nio.file.Files.readAllBytes(stylesheet.toPath());

        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MbStyleStylesheet parsedContent;
        try {
            // parse input
            parsedContent = mapper.readValue(content, MbStyleStylesheet.class);
        } catch (IOException e) {
            LOGGER.error("Stylesheet in the styles store is invalid: " + stylesheet.getAbsolutePath());
            throw new RuntimeException("An error occurred while processing style '" + stylesheet.getName() + "'.");
        }

        return Response.ok()
                .entity(parsedContent)
                .type(MEDIA_TYPE.type())
                .links(links.isEmpty() ? null : links.stream().map(link -> link.getLink()).toArray(Link[]::new))
                .build();
    }
}
