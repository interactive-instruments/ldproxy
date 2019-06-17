/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.io.Resources;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.xtraplatform.server.CoreServerConfig;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceListingProvider;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
                                .ensureTrailingSlash()
                                .ensureLastPathSegment("___static___")
                                .getPath();
        }

        return "";
    }

    @Override
    public Response getServiceListing(List<ServiceData> services, URI uri) {
        URICustomizer uriCustomizer = new URICustomizer(uri);
        String urlPrefix = getStaticUrlPrefix(uriCustomizer);
        customizeUri(uriCustomizer);
        try {
            uri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            // ignore
        }
        //TODO: map in caller
        return Response.ok()
                       .entity(new ServiceOverviewView(uri, services.stream().sorted(Comparator.comparingLong(ServiceData::getCreatedAt).reversed()).collect(Collectors.toList()), urlPrefix, htmlConfig))
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

    //TODO: if still needed, move to feature provider
    private Response getResponseForParams(Collection<Service> services, UriInfo uriInfo) {
        /*if (uriInfo.getQueryParameters().containsKey(PARAM_WFS_URL)) {
            try {
                URI wfsUri = WFSAdapter.parseAndCleanWfsUrl(uriInfo.getQueryParameters().getFirst(PARAM_WFS_URL));
                for (Service service : services) {
                    URI serviceWfsUri = ((LdProxyService) service).getWfsAdapter().getUrls().get("default").get(WFS.METHOD.GET);
                    if (wfsUri.equals(serviceWfsUri)) {
                        URI serviceUri = new URIBuilder().setPath(getPath(uriInfo) + service.getId() + "/").build();
                        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", serviceUri.toString()).build();
                    }
                }
            } catch (URISyntaxException e) {
                //ignore
            }
            return Response.status(Response.Status.NOT_FOUND).build();
        }*/
        return null;
    }
}
