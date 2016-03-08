/**
 * Copyright 2016 interactive instruments GmbH
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ldproxy.rest;

import de.ii.ldproxy.output.html.ServiceOverviewView;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xsf.core.api.rest.ServiceResourceFactory;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import io.dropwizard.views.View;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.http.client.utils.URIBuilder;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
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

public class LdProxyServiceResourceFactory implements ServiceResourceFactory {

    public static final String PARAM_WFS_URL = "wfsUrl";

    @Override
    public Class getServiceResourceClass() {
        return LdProxyServiceResource.class;
    }

    @Override
    public View getServicesView(Collection<Service> collection, URI uri) {
        return new ServiceOverviewView(uri, collection);
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
