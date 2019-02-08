/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.core.Wfs3Core;
import de.ii.ldproxy.wfs3.oas30.Oas30Configuration;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.ii.ldproxy.wfs3.sitemaps.SitemapsConfiguration.EXTENSION_KEY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointSitemap implements Wfs3EndpointExtension {

    @Requires
    private CoreServerConfig coreServerConfig;

    @Requires
    private Wfs3Core wfs3Core;

    @Override
    public String getPath() {
        return "collections";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/?(?:\\/\\w+\\/sitemap[_0-9]+\\.xml)$";
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData){
        if(!isExtensionEnabled(serviceData, EXTENSION_KEY)){
            throw new NotFoundException();
        }
        return true;
    }

    @Path("/{id}/sitemap_{from}_{to}.xml")
    @GET
    public Response getCollectionSitemap(@Auth Optional<User> optionalUser, @PathParam("id") String id, @PathParam("from") Long from, @PathParam("to") Long to, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3ServiceData serviceData = ((Wfs3Service) service).getData();

        wfs3Core.checkCollectionName(serviceData, id);

        List<Site> sites = new ArrayList<>();

        String baseUrlItems = String.format("%s/%s/collections/%s/items?f=html", coreServerConfig.getExternalUrl(), serviceData.getId(), id);
        List<Site> itemsSites = SitemapComputation.getSites(baseUrlItems,from,to);
        sites.addAll(itemsSites);

        String baseUrlItem = String.format("%s/%s/collections/%s/items", coreServerConfig.getExternalUrl(), serviceData.getId(), id);
        ItemSitesReader itemSitesReader = new ItemSitesReader(baseUrlItem);
        FeatureQuery featureQuery= ImmutableFeatureQuery.builder()
                .type(id)
                .offset(from.intValue())
                .limit(to.intValue() - from.intValue() + 1)
                .fields(ImmutableList.of("ID")) //TODO only get id field
                .build();
        FeatureStream <FeatureTransformer> featureStream = ((Wfs3Service) service).getFeatureProvider().getFeatureTransformStream(featureQuery);
        featureStream.apply(itemSitesReader).toCompletableFuture().join();

        sites.addAll(itemSitesReader.getSites());

        Sitemap sitemap = new Sitemap(SitemapComputation.truncateToUpperLimit(sites));

        return Response.ok()
                       .entity(sitemap)
                       .build();
    }
}
