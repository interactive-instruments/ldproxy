/**
 * Copyright 2020 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.server.CoreServerConfig;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.ServerErrorException;
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
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?$")
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .build()
    );


    private final CoreServerConfig coreServerConfig;
    private final OgcApiFeatureCoreProviders providers;

    public Wfs3EndpointSiteIndex(@Requires CoreServerConfig coreServerConfig,
                                 @Requires OgcApiFeatureCoreProviders providers) {
        this.coreServerConfig = coreServerConfig;
        this.providers = providers;
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/?$"))
            return API_MEDIA_TYPES;

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, SitemapsConfiguration.class);
    }

    @GET
    public Response getDatasetSiteIndex(@Auth Optional<User> optionalUser, @Context OgcApiApi service,
                                        @Context OgcApiRequestContext wfs3Request) {

        Set<String> collectionIds = service.getData()
                                           .getCollections()
                                           .keySet();

        Map<String, Long> featureCounts = SitemapComputation.getFeatureCounts(collectionIds, providers.getFeatureProvider(service.getData()));
        long totalFeatureCount = featureCounts.values()
                                              .stream()
                                              .mapToLong(i -> i)
                                              .sum();


        long limit = 2250000000L;
        if (totalFeatureCount > limit) {
            LOGGER.error("Warning: Limit for maximum features reached");
        }

        List<Site> sitemaps = new ArrayList<>();
        String landingPageUrl = String.format("%s/%s/sitemap_landingPage.xml", coreServerConfig.getExternalUrl(), service.getId(), service.getId());
        sitemaps.add(new Site(landingPageUrl));

        //TODO duration with big blocks is too long, therefore the block length is dynamically generated


        Map<String, Long> blockLengths = SitemapComputation.getDynamicRanges(collectionIds, featureCounts);

        SitemapComputation.getCollectionIdStream(service.getData())
                          .forEach(collectionId -> {
                              String baseUrl = String.format("%s/%s/collections/%s", coreServerConfig.getExternalUrl(), service.getId(), collectionId);

                              List<Site> sitemapsBlock = SitemapComputation.getSitemaps(baseUrl, featureCounts.get(collectionId), blockLengths.get(collectionId));

                              sitemaps.addAll(sitemapsBlock);
                          });

        SitemapIndex sitemapIndex = new SitemapIndex(SitemapComputation.truncateToUpperLimit(sitemaps));

        return Response.ok()
                       .entity(sitemapIndex)
                       .build();
    }

}
