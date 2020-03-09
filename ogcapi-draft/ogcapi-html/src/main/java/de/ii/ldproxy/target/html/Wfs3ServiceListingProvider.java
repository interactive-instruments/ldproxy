/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.io.Resources;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.server.CoreServerConfig;
import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceListingProvider;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3ServiceListingProvider implements ServiceListingProvider {

    @Context
    private BundleContext bundleContext;

    @Requires
    private HtmlConfig htmlConfig;

    @Requires
    private I18n i18n;

    // TODO: move externalUri handling to XtraplatformRequestContext in ServicesResource
    // TODO: derive Wfs3Request from injected XtraplatformRequest

    private Optional<URI> externalUri = Optional.empty();

    @Bind
    void setCore(CoreServerConfig coreServerConfig) {
        URI externalUri = null;
        try {
            externalUri = new URI(coreServerConfig.getExternalUrl());
        } catch (URISyntaxException e) {
            // ignore
        }

        this.externalUri = Optional.ofNullable(externalUri);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_HTML_TYPE;
    }

    private void customizeUri(final URICustomizer uriCustomizer) {
        if (externalUri.isPresent()) {
            uriCustomizer.setScheme(externalUri.get()
                                               .getScheme());
            uriCustomizer.replaceInPath("/rest/services", externalUri.get()
                                                                     .getPath());
            uriCustomizer.ensureTrailingSlash();
        }
    }

    private String getStaticUrlPrefix(final URICustomizer uriCustomizer) {
        if (externalUri.isPresent()) {
            return uriCustomizer.copy()
                                .cutPathAfterSegments("rest", "services")
                                .replaceInPath("/rest/services", externalUri.get()
                                                                            .getPath())
                                .ensureLastPathSegment("___static___")
                                .ensureTrailingSlash()
                                .getPath();
        }

        return "";
    }

    @Override
    public Response getServiceListing(List<ServiceData> services, URI uri) {
        return getServiceListing(services, uri, Optional.of(Locale.ENGLISH));
    }

    // TODO: add locale parameter in ServiceListing.getServiceListing() in xtraplatform
    public Response getServiceListing(List<ServiceData> services, URI uri, Optional<Locale> language) {
        URICustomizer uriCustomizer = new URICustomizer(uri);
        String urlPrefix = getStaticUrlPrefix(uriCustomizer);
        customizeUri(uriCustomizer);
        try {
            uri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            // ignore
        }
        // TODO: map in caller
        return Response.ok()
                .entity(new ServiceOverviewView(uri, services.stream().sorted(Comparator.comparingLong(ServiceData::getCreatedAt).reversed()).collect(Collectors.toList()), urlPrefix, htmlConfig, i18n, language))
                .build();
    }

    @Override
    public Response getStaticAsset(String path) {
        try {
            final URL url = path.endsWith("favicon.ico") ? bundleContext.getBundle()
                                                                        .getResource("img/favicon.ico") : bundleContext.getBundle()
                                                                                                                       .getResource(path);

            MediaType mediaType = path.endsWith(".css") ? new MediaType("text", "css", "utf-8") : path.endsWith(".js") ? new MediaType("application", "javascript", "utf-8") : new MediaType("image", "x-icon");

            return Response.ok((StreamingOutput) output -> Resources.asByteSource(url)
                                                                    .copyTo(output))
                           .type(mediaType)
                           .build();
        } catch (Throwable e) {
            throw new NotFoundException();
        }
    }
}
