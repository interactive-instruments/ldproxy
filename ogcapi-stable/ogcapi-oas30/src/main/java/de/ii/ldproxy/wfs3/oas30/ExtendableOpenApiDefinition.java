/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.oas30;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.dropwizard.api.AuthConfig;
import de.ii.xtraplatform.dropwizard.api.XtraPlatform;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Component
@Provides(specifications = {ExtendableOpenApiDefinition.class})
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.wfs3.oas30.OpenApiExtension)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class ExtendableOpenApiDefinition {

    private static Logger LOGGER = LoggerFactory.getLogger(ExtendableOpenApiDefinition.class);

    @Context
    private BundleContext bundleContext;

    private final AuthConfig authConfig;
    private Set<OpenApiExtension> openApiExtensions;

    public ExtendableOpenApiDefinition(@Requires XtraPlatform xtraPlatform) {
        this.authConfig = xtraPlatform.getConfiguration().auth;
    }

    private Set<OpenApiExtension> getOpenApiExtensions() {
        if (Objects.isNull(openApiExtensions)) {
            this.openApiExtensions = new TreeSet<>(Comparator.comparingInt(OpenApiExtension::getSortPriority));
        }
        return openApiExtensions;
    }

    public Response getOpenApi(String type, URICustomizer requestUriCustomizer, OgcApiApiDataV2 apiData) {

        boolean pretty = true;

        try {
            OpenAPI openAPI = Json.mapper()
                    .readerFor(OpenAPI.class)
                    .readValue(Resources.asByteSource(Resources.getResource(ExtendableOpenApiDefinition.class, "/openapi.json"))
                            .openBufferedStream());

            // TODO
            if (apiData.getSecured() && authConfig.isJwt()) {
                openAPI.getComponents()
                        .addSecuritySchemes("JWT", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
                openAPI.addSecurityItem(new SecurityRequirement().addList("JWT"));
            }

            openAPI.servers(ImmutableList.of(new Server().url(requestUriCustomizer.copy()
                    .clearParameters()
                    .ensureNoTrailingSlash()
                    .removeLastPathSegment("api")
                    .toString())));

            if (apiData != null) {

                openAPI.getInfo()
                        .title(apiData.getLabel())
                        .description(apiData.getDescription()
                                .orElse(""));

                if (apiData.getMetadata().isPresent()) {
                    Metadata md = apiData.getMetadata().get();
                    openAPI.getInfo()
                            .version(md.getVersion().orElse("1.0.0"))
                            .contact(new Contact().name(md.getContactName()
                                    .orElse(null))
                                    .url(md.getContactUrl()
                                            .orElse(null))
                                    .email(md.getContactEmail()
                                            .orElse(null)))
                            .license(new License().name(md.getLicenseName()
                                    .orElse(null))
                                    .url(md.getLicenseUrl()
                                            .orElse(null)));
                } else {
                    // version is required
                    openAPI.getInfo()
                            .version("1.0.0");
                }

                if (apiData.getExternalDocs().isPresent()) {
                    OgcApiExternalDocumentation externalDocs = apiData.getExternalDocs().get();
                    ExternalDocumentation docs = new ExternalDocumentation().url(externalDocs.getUrl());
                    if (externalDocs.getDescription().isPresent())
                        docs.description(externalDocs.getDescription().get());
                    openAPI.externalDocs(docs);
                }
            }

            // TODO update with examples and details (f enums, lang enums, etc.)

            getOpenApiExtensions().stream()
                    .sorted(Comparator.comparing(OpenApiExtension::getSortPriority))
                    .forEachOrdered(openApiExtension -> openApiExtension.process(openAPI, apiData));


            if (StringUtils.isNotBlank(type) && type.trim()
                    .equalsIgnoreCase("yaml")) {
                return Response.status(Response.Status.OK)
                        .entity(pretty ? Yaml.pretty(openAPI) : Yaml.mapper()
                                .writeValueAsString(openAPI))
                        .type("application/vnd.oai.openapi;version=3.0")
                        .build();
            } else {
                return Response.status(Response.Status.OK)
                        .entity(pretty ? Json.pretty(openAPI) : Json.mapper()
                                .writeValueAsString(openAPI))
                        .type("application/vnd.oai.openapi+json;version=3.0")
                        .build();
            }
        } catch (IOException e) {
            LOGGER.error("OpenAPI document could not be created: " + e.getMessage());
            throw new ServerErrorException("OpenAPI document could not be created", 500);
        }
    }

    private synchronized void onArrival(ServiceReference<OpenApiExtension> ref) {
        final OpenApiExtension openApiExtension = bundleContext.getService(ref);

        getOpenApiExtensions().add(openApiExtension);
    }

    private synchronized void onDeparture(ServiceReference<OpenApiExtension> ref) {
        final OpenApiExtension openApiExtension = bundleContext.getService(ref);

        if (Objects.nonNull(openApiExtension)) {
            getOpenApiExtensions().remove(openApiExtension);
        }
    }


}
