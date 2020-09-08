/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.app;

import com.google.common.io.Resources;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableHtmlConfiguration;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class ApiCatalogProviderHtml extends ApiCatalogProvider {

    public ApiCatalogProviderHtml(@Context BundleContext bundleContext, @Requires XtraPlatform xtraPlatform, @Requires I18n i18n, @Requires EntityDataDefaultsStore defaultsStore, @Requires ExtensionRegistry extensionRegistry) {
        super(bundleContext, xtraPlatform, i18n, defaultsStore, extensionRegistry);
    }

    private HtmlConfiguration getHtmlConfig() {
        //TODO: encapsulate in entities/defaults layer
        EntityDataBuilder<?> builder = defaultsStore.getBuilder(Identifier.from(EntityDataDefaultsStore.EVENT_TYPE, Service.TYPE, OgcApiDataV2.SERVICE_TYPE.toLowerCase()));
        if (builder instanceof ImmutableOgcApiDataV2.Builder) {
            ImmutableOgcApiDataV2 defaults = ((ImmutableOgcApiDataV2.Builder) builder).id("NOT_SET")
                                                                                      .build();
            return defaults.getExtension(HtmlConfiguration.class)
                           .orElse(new ImmutableHtmlConfiguration.Builder().build());
        }
        return new ImmutableHtmlConfiguration.Builder().build();
    }

    // TODO: move externalUri handling to XtraplatformRequestContext in ServicesResource
    // TODO: derive Wfs3Request from injected XtraplatformRequest

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_HTML_TYPE;
    }

    // TODO: add locale parameter in ServiceListing.getServiceListing() in xtraplatform
    @Override
    public Response getServiceListing(List<ServiceData> apis, URI uri, Optional<Locale> language) throws URISyntaxException {
        ApiCatalog apiCatalog = getCatalog(apis, uri, language);

        // TODO: map in caller
        return Response.ok()
                       .entity(new ServiceOverviewView(uri, apiCatalog, getHtmlConfig(), i18n, language))
                       .build();
    }

    @Override
    public Response getStaticAsset(String path) {

            final URL url = path.endsWith("favicon.ico") ? bundleContext.getBundle()
                                                                        .getResource("img/favicon.ico") : bundleContext.getBundle()
                                                                                                                       .getResource(path);

            MediaType mediaType = path.endsWith(".css") ? new MediaType("text", "css", "utf-8") : path.endsWith(".js") ? new MediaType("application", "javascript", "utf-8") : new MediaType("image", "x-icon");

            return Response.ok((StreamingOutput) output -> Resources.asByteSource(url)
                                                                    .copyTo(output))
                           .type(mediaType)
                           .build();

    }
}
