/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import de.ii.ldproxy.output.html.ServiceOverviewView;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.core.api.rest.ServiceResourceFactory;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import io.dropwizard.views.View;
import org.apache.felix.ipojo.annotations.*;
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
        serviceResource.setOpenApiResource(openApiResource);

        return serviceResource;
    }

    @Override
    public View getServicesView(Collection<Service> collection, URI uri) {
        Collection<Service> collection2 = Collections2.filter(collection, new Predicate<Service>() {
            @Override
            public boolean apply(Service s) {
                return s.isStarted();
            }
        });
        return new ServiceOverviewView(uri, collection2);
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

    private String getPath(UriInfo uriInfo) {
        return uriInfo.getPath().endsWith("/") ? "" : uriInfo.getPath().substring(uriInfo.getPath().lastIndexOf("/") + 1) + "/";
    }
}
