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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointSiteIndex implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointSiteIndex.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("sitemap_index.xml")
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
    public Response getDatasetSiteIndex(@Auth Optional<User> optionalUser, @Context OgcApiDataset service,
                                        @Context OgcApiRequestContext wfs3Request) {

        Set<String> collectionIds = service.getData()
                                           .getFeatureTypes()
                                           .keySet();

        Map<String, Long> featureCounts = SitemapComputation.getFeatureCounts(collectionIds, service.getFeatureProvider());
        long totalFeatureCount = featureCounts.values()
                                              .stream()
                                              .mapToLong(i -> i)
                                              .sum();


        long limit = 2250000000L;
        if (totalFeatureCount > limit) {
            LOGGER.error("Warning: Limit for maximum features reached");
        }

        List<Site> sitemaps = new ArrayList<>();
        String landingPageUrl = String.format("%s/%s/sitemap_landingPage.xml", xtraPlatform.getServicesUri(), service.getId(), service.getId());
        sitemaps.add(new Site(landingPageUrl));

        //TODO duration with big blocks is too long, therefore the block length is dynamically generated


        Map<String, Long> blockLengths = SitemapComputation.getDynamicRanges(collectionIds, featureCounts);

        SitemapComputation.getCollectionIdStream(service.getData())
                          .forEach(collectionId -> {
                              String baseUrl = String.format("%s/%s/collections/%s", xtraPlatform.getServicesUri(), service.getId(), collectionId);

                              List<Site> sitemapsBlock = SitemapComputation.getSitemaps(baseUrl, featureCounts.get(collectionId), blockLengths.get(collectionId));

                              sitemaps.addAll(sitemapsBlock);
                          });

        SitemapIndex sitemapIndex = new SitemapIndex(SitemapComputation.truncateToUpperLimit(sitemaps));

        return Response.ok()
                       .entity(sitemapIndex)
                       .build();
    }


}
