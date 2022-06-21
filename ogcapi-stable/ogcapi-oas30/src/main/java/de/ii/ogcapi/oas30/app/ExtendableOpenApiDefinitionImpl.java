/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.oas30.domain.OpenApiExtension;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AuthConfig;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.io.IOException;
import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class ExtendableOpenApiDefinitionImpl implements ExtendableOpenApiDefinition {

    private static Logger LOGGER = LoggerFactory.getLogger(ExtendableOpenApiDefinitionImpl.class);

    private final AuthConfig authConfig;
    private final ExtensionRegistry extensionRegistry;
    private final ClassSchemaCache classSchemaCache;

    @Inject
    public ExtendableOpenApiDefinitionImpl(AppContext appContext, ExtensionRegistry extensionRegistry, ClassSchemaCache classSchemaCache) {
        this.authConfig = appContext.getConfiguration().auth;
        this.extensionRegistry = extensionRegistry;
        this.classSchemaCache = classSchemaCache;
    }

    @Override
    public Response getOpenApi(String type, URICustomizer requestUriCustomizer, OgcApiDataV2 apiData) {

        boolean pretty = true;

        try {
            OpenAPI openAPI = Json.mapper()
                    .readerFor(OpenAPI.class)
                    .readValue(Resources.asByteSource(Resources.getResource(ExtendableOpenApiDefinitionImpl.class, "/openapi.json"))
                            .openBufferedStream());

            if (apiData.getSecured() && authConfig.isJwt()) {
                openAPI.getComponents()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT"));
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
                    ApiMetadata md = apiData.getMetadata().get();
                    openAPI.getInfo()
                            .version(md.getVersion().orElse("1.0.0"));
                    if (md.getContactName().isPresent() || md.getContactUrl().isPresent() || md.getContactEmail().isPresent()) {
                        Contact contact = new Contact();
                        md.getContactName().ifPresent(v -> contact.name(v));
                        md.getContactUrl().ifPresent(v -> contact.url(v));
                        md.getContactEmail().ifPresent(v -> contact.email(v));
                        openAPI.getInfo().contact(contact);
                    }
                    if (md.getLicenseName().isPresent()) {
                        // license name is required
                        License license = new License().name(md.getLicenseName().get());
                        md.getLicenseUrl().ifPresent(v -> license.url(v));
                        openAPI.getInfo().license(license);
                    }
                } else {
                    // version is required
                    openAPI.getInfo()
                            .version("1.0.0");
                }

                if (apiData.getExternalDocs().isPresent()) {
                    ExternalDocumentation externalDocs = apiData.getExternalDocs().get();
                    io.swagger.v3.oas.models.ExternalDocumentation docs = new io.swagger.v3.oas.models.ExternalDocumentation().url(externalDocs.getUrl());
                    if (externalDocs.getDescription().isPresent())
                        docs.description(externalDocs.getDescription().get());
                    openAPI.externalDocs(docs);
                }
            }

            // TODO update with examples and details (f enums, lang enums, etc.)

            extensionRegistry.getExtensionsForType(OpenApiExtension.class)
                .stream()
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
            throw new RuntimeException("OpenAPI document could not be created", e);
        }
    }
}
