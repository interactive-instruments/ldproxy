/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app.json;

import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlForm;
import de.ii.ldproxy.ogcapi.routes.domain.Routes;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
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
public class RoutesFormatJson implements RoutesFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutesFormatJson.class);
    static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaRoutes;
    public final static String SCHEMA_REF_ROUTES = "#/components/schemas/Routes";
    private final I18n i18n;

    public RoutesFormatJson(@Requires SchemaGenerator schemaGenerator,
                            @Requires I18n i18n) {
        this.i18n = i18n;
        schemaRoutes = schemaGenerator.getSchema(Routes.class);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(RoutingConfiguration.class)
            .filter(RoutingConfiguration::getEnabled)
            .filter(RoutingConfiguration::getManageRoutes)
            .isPresent();
    }

    @Override
    public Object getRoutesEntity(Routes routes, OgcApi api, ApiRequestContext requestContext) {
        return routes;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schemaRoutes)
                .schemaRef(SCHEMA_REF_ROUTES)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }
}