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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.*;

import static de.ii.ldproxy.wfs3.sitemaps.SitemapsConfiguration.EXTENSION_KEY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointSiteIndex implements Wfs3EndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointSiteIndex.class);

    @Requires
    private CoreServerConfig coreServerConfig;

    @Override
    public String getPath() {
        return "sitemap_index.xml";
    }
    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData){
        if(!isExtensionEnabled(serviceData, EXTENSION_KEY)){
            throw new NotFoundException();
        }
        return true;
    }

    @GET
    public Response getDatasetSiteIndex(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {

        Wfs3ServiceData serviceData = ((Wfs3Service) service).getData();

        Set<String> collectionIds = serviceData.getFeatureTypes().keySet();

        Map<String, Long> featureCounts = SitemapComputation.getFeatureCounts(collectionIds,((Wfs3Service) service).getFeatureProvider());
        long totalFeatureCount = featureCounts.values()
                                              .stream()
                                              .mapToLong(i -> i)
                                              .sum();


        long limit = 2250000000L;
        if (totalFeatureCount > limit) {
            LOGGER.error("Warning: Limit for maximum features reached");
        }

        List<Site> sitemaps = new ArrayList<>();
        String landingPageUrl = String.format("%s/%s/sitemap_landingPage.xml", coreServerConfig.getExternalUrl(), serviceData.getId(), serviceData.getId());
        sitemaps.add(new Site(landingPageUrl));

        //TODO duration with big blocks is too long, therefore the block length is dynamically generated


        Map<String, Long> blockLengths = SitemapComputation.getDynamicRanges(collectionIds, featureCounts);

        SitemapComputation.getCollectionIdStream(serviceData)
                          .forEach(collectionId -> {
                              String baseUrl = String.format("%s/%s/collections/%s", coreServerConfig.getExternalUrl(), serviceData.getId(), collectionId);

                              List<Site> sitemapsBlock = SitemapComputation.getSitemaps(baseUrl, featureCounts.get(collectionId), blockLengths.get(collectionId));

                              sitemaps.addAll(sitemapsBlock);
                          });

        SitemapIndex sitemapIndex = new SitemapIndex(SitemapComputation.truncateToUpperLimit(sitemaps));

        return Response.ok()
                       .entity(sitemapIndex)
                       .build();
    }


}
