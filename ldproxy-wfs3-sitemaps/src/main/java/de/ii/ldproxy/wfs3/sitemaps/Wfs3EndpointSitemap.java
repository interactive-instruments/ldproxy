/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.wfs3.core.Wfs3Core;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.dropwizard.api.XtraPlatform;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointSitemap implements OgcApiEndpointExtension {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^\\/?(?:\\/\\w+\\/sitemap[_0-9]+\\.xml)$")
            .build();
    private static final ImmutableSet<OgcApiMediaType> API_MEDIA_TYPES = ImmutableSet.of(
            new ImmutableOgcApiMediaType.Builder()
                    .main(MediaType.TEXT_HTML_TYPE)
                    .build()
    );

    @Requires
    private XtraPlatform xtraPlatform;

    @Requires
    private Wfs3Core wfs3Core;

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

    @Path("/{id}/sitemap_{from}_{to}.xml")
    @GET
    public Response getCollectionSitemap(@Auth Optional<User> optionalUser, @PathParam("id") String id,
                                         @PathParam("from") Long from, @PathParam("to") Long to,
                                         @Context OgcApiDataset service, @Context OgcApiRequestContext wfs3Request) {
        wfs3Core.checkCollectionName(service.getData(), id);

        List<Site> sites = new ArrayList<>();

        String baseUrlItems = String.format("%s/%s/collections/%s/items?f=html", xtraPlatform.getServicesUri(), service.getId(), id);
        List<Site> itemsSites = SitemapComputation.getSites(baseUrlItems, from, to);
        sites.addAll(itemsSites);

        String baseUrlItem = String.format("%s/%s/collections/%s/items", xtraPlatform.getServicesUri(), service.getId(), id);
        ItemSitesReader itemSitesReader = new ItemSitesReader(baseUrlItem);
        FeatureQuery featureQuery = ImmutableFeatureQuery.builder()
                                                         .type(id)
                                                         .offset(from.intValue())
                                                         .limit(to.intValue() - from.intValue() + 1)
                                                         .fields(ImmutableList.of("ID")) //TODO only get id field
                                                         .build();
        FeatureStream<FeatureTransformer> featureStream = service.getFeatureProvider()
                                                                 .getFeatureTransformStream(featureQuery);
        featureStream.apply(itemSitesReader, null)
                     .toCompletableFuture()
                     .join();

        sites.addAll(itemSitesReader.getSites());

        Sitemap sitemap = new Sitemap(SitemapComputation.truncateToUpperLimit(sites));

        return Response.ok()
                       .entity(sitemap)
                       .build();
    }
}
