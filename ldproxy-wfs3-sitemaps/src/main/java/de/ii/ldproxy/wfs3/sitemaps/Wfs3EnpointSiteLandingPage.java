/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.dropwizard.api.XtraPlatform;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class Wfs3EnpointSiteLandingPage implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EnpointSiteLandingPage.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("sitemap_landingPage.xml")
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .main(MediaType.TEXT_HTML_TYPE)
                    .build()
    );

    @Requires
    private XtraPlatform xtraPlatform;

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset) {
        return API_MEDIA_TYPES;
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return isExtensionEnabled(datasetData, SitemapsConfiguration.class);
    }

    @GET
    public Response getLandingPageSitemap(@Auth Optional<User> optionalUser, @Context OgcApiDataset service,
                                          @Context OgcApiRequestContext wfs3Request) {

        List<Site> sites = new ArrayList<>();
        sites.add(new Site(String.format("%s/%s?f=html", xtraPlatform.getServicesUri(), service.getId())));
        Sitemap sitemap = new Sitemap(sites);

        return Response.ok()
                       .entity(sitemap)
                       .build();
    }

}
