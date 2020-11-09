/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.store.domain.legacy.KeyValueStore;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
@Provides
@Instantiate
public class StyleFormatHtml implements StyleFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    @Requires
    private KeyValueStore keyValueStore;

    @Requires
    private XtraPlatform xtraPlatform;


    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFormatHtml.class);
    public static final String MEDIA_TYPE_STRING = "application/vnd.mapbox.style+json" ;
    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.TEXT_HTML_TYPE)
            .label("HTML")
            .parameter("html")
            .build();

    private final Schema schemaStyle;
    public final static String SCHEMA_REF_STYLE = "#/components/schemas/htmlSchema";

    public StyleFormatHtml() {
        schemaStyle = new StringSchema().example("<html>...</html>");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(StylesConfiguration.class)
                      .filter(config -> config.getStyleEncodings().contains(this.getMediaType().label()) &&
                                        config.getStyleEncodings().contains(StyleFormatMbStyle.MEDIA_TYPE.label()))
                      .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaStyle)
                .schemaRef(SCHEMA_REF_STYLE)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaType getMediaType() {
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
    public boolean getDerived() {
        return true;
    }

    @Override
    public Response getStyleResponse(String styleId, File stylesheet, List<Link> links, OgcApi api, ApiRequestContext requestContext) throws IOException {

        String styleUri = String.format("%s/%s/styles/%s?f=mbs", xtraPlatform.getServicesUri(), String.join("/", api.getData().getSubPath()), styleId);
        StyleView styleView = new StyleView(styleUri, api, styleId);

        return Response.ok()
                .entity(styleView)
                .type(MEDIA_TYPE.type())
                .links(links.isEmpty() ? null : links.stream().map(link -> link.getLink()).toArray(javax.ws.rs.core.Link[]::new))
                .build();
    }
}
