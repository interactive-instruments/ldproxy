/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Oas30Configuration;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.ii.ldproxy.wfs3.sitemaps.SitemapsConfiguration.EXTENSION_KEY;

@Component
@Provides
@Instantiate
public class Wfs3EnpointSiteLandingPage implements Wfs3EndpointExtension{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointSiteIndex.class);
    @Requires
    private CoreServerConfig coreServerConfig;

    @Override
    public String getPath() {
        return "sitemap_landingPage.xml";
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData){
        if(!isExtensionEnabled(serviceData, EXTENSION_KEY)){
            throw new NotFoundException();
        }
        return true;
    }

    @GET
    public Response getLandingPageSitemap(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3ServiceData serviceData = ((Wfs3Service) service).getData();

        List<Site> sites = new ArrayList<>();
        sites.add(new Site( String.format("%s/%s?f=html", coreServerConfig.getExternalUrl(), serviceData.getId())));
        Sitemap sitemap = new Sitemap(sites);

        return Response.ok()
                .entity(sitemap)
                .build();
    }

}
