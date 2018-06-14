/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest;

import com.google.common.collect.Collections2;
import de.ii.ldproxy.output.html.HtmlConfig;
import de.ii.ldproxy.output.html.ServiceOverviewView;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.ldproxy.target.html.WfsTargetHtml;
import de.ii.ldproxy.wfs3.URICustomizer;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.core.api.rest.ServiceResourceFactory;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.feature.transformer.api.AkkaStreamer;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import io.dropwizard.views.View;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.http.client.utils.URIBuilder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 *
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = ServiceResource.SERVICE_TYPE_KEY, type = "java.lang.String", value = LdProxyService.SERVICE_TYPE)
})
@Instantiate
//@Subresource
public class LdProxyServiceResourceFactory implements ServiceResourceFactory/*, SubresourceFactory*/ {

    public static final String PARAM_WFS_URL = "wfsUrl";

   @Requires
    private OpenApiResource openApiResource;

    @Requires
    private CoreServerConfig coreServerConfig;

    @Requires
    private HtmlConfig htmlConfig;

    @Requires
    private WfsTargetHtml wfsTargetHtml;

    private void customizeUri(final URICustomizer uriCustomizer) {
        try {
            final URI externalUri = new URI(coreServerConfig.getExternalUrl());

            uriCustomizer.setScheme(externalUri.getScheme());
            uriCustomizer.replaceInPath("/rest/services", externalUri.getPath());
            uriCustomizer.ensureTrailingSlash();
        } catch (URISyntaxException e) {
            // ignore
        }
    }

    private String getStaticUrlPrefix(final URICustomizer uriCustomizer) {
        try {
            final URI externalUri = new URI(coreServerConfig.getExternalUrl());
        return uriCustomizer.copy()
                            .cutPathAfterSegments("rest", "services")
                            .replaceInPath("/rest/services", externalUri
                                                                        .getPath())
                            .ensureTrailingSlash()
                            .ensureLastPathSegment("___static___")
                            .getPath();
        } catch (URISyntaxException e) {
            // ignore
        }
        return "";
    }

    //@Requires
    private AkkaStreamer akkaStreamer;

    @Requires
    private AkkaHttp akkaHttp;

    @Override
    public Class getServiceResourceClass() {
        return LdProxyServiceResource.class;
    }

    /*@Override
    public Class getSubresourceClass() {
        return LdProxyServiceResource.class;
    }*/

    @Override
    public ServiceResource getServiceResource() {
        LdProxyServiceResource serviceResource = new LdProxyServiceResource();
        serviceResource.inject(openApiResource, akkaStreamer, akkaHttp, coreServerConfig.getExternalUrl(), htmlConfig);

        return serviceResource;
    }

    @Override
    public View getServicesView(Collection<Service> services, URI uri) {
        Collection<Service> runningServices = Collections2.filter(services, Service::isStarted);
        URICustomizer uriCustomizer = new URICustomizer(uri);
        String urlPrefix = getStaticUrlPrefix(uriCustomizer);
        customizeUri(uriCustomizer);
        try {
            uri = uriCustomizer.build();
        } catch (URISyntaxException e) {
            // ignore
        }
        return new ServiceOverviewView(uri, runningServices, urlPrefix, htmlConfig);
    }

    @Override
    public Response getResponseForParams(Collection<Service> services, UriInfo uriInfo) {
        if (uriInfo.getQueryParameters().containsKey(PARAM_WFS_URL)) {
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
        }
        return null;
    }

    @Override
    public Response getFile(String file) {
        return wfsTargetHtml.getFile(file);
    }

    private String getPath(UriInfo uriInfo) {
        return uriInfo.getPath().endsWith("/") ? "" : uriInfo.getPath().substring(uriInfo.getPath().lastIndexOf("/") + 1) + "/";
    }
}
