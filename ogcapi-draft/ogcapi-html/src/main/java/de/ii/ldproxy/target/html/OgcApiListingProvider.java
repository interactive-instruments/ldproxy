/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.io.Resources;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.dropwizard.api.XtraPlatform;
import de.ii.xtraplatform.event.store.EntityDataBuilder;
import de.ii.xtraplatform.event.store.EntityDataDefaultsStore;
import de.ii.xtraplatform.event.store.Identifier;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceListingProvider;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

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
public class OgcApiListingProvider implements ServiceListingProvider {


    private final BundleContext bundleContext;
    private final XtraPlatform xtraPlatform;
    private final I18n i18n;
    private final EntityDataDefaultsStore defaultsStore;

    public OgcApiListingProvider(@Context BundleContext bundleContext, @Requires XtraPlatform xtraPlatform, @Requires I18n i18n, @Requires EntityDataDefaultsStore defaultsStore) {
        this.bundleContext = bundleContext;
        this.xtraPlatform = xtraPlatform;
        this.i18n = i18n;
        this.defaultsStore = defaultsStore;
    }

    private HtmlConfiguration getHtmlConfig() {
        //TODO: encapsulate in entities/defaults layer
        EntityDataBuilder<?> builder = defaultsStore.getBuilder(Identifier.from(EntityDataDefaultsStore.EVENT_TYPE, Service.TYPE, OgcApiApiDataV2.SERVICE_TYPE.toLowerCase()));
        if (builder instanceof ImmutableOgcApiApiDataV2.Builder) {
            ImmutableOgcApiApiDataV2 defaults = ((ImmutableOgcApiApiDataV2.Builder) builder).id("NOT_SET")
                                                                                           .build();
            return defaults.getExtension(HtmlConfiguration.class)
                           .orElse(null);
        }
        return null;
    }

    // TODO: move externalUri handling to XtraplatformRequestContext in ServicesResource
    // TODO: derive Wfs3Request from injected XtraplatformRequest

    private Optional<URI> getExternalUri() {
        return Optional.ofNullable(xtraPlatform.getServicesUri());
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_HTML_TYPE;
    }

    private void customizeUri(final URICustomizer uriCustomizer) {
        if (getExternalUri().isPresent()) {
            uriCustomizer.setScheme(getExternalUri().get()
                                                    .getScheme());
            uriCustomizer.replaceInPath("/rest/services", getExternalUri().get()
                                                                          .getPath());
            uriCustomizer.ensureTrailingSlash();
        }
    }

    private String getStaticUrlPrefix(final URICustomizer uriCustomizer) {
        if (getExternalUri().isPresent()) {
            return uriCustomizer.copy()
                                .cutPathAfterSegments("rest", "services")
                                .replaceInPath("/rest/services", getExternalUri().get()
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
                       .entity(new ServiceOverviewView(uri, services.stream()
                                                                    .sorted(Comparator.comparingLong(ServiceData::getCreatedAt)
                                                                                      .reversed())
                                                                    .collect(Collectors.toList()), urlPrefix, getHtmlConfig(), i18n, language))
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
