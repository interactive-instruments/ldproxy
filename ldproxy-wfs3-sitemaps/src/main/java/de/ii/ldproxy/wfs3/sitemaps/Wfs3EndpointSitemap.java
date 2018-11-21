/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.core.Wfs3Core;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Path("/{id}/sitemap_{from}_{to}.xml")
    @GET
    public Response getCollectionSitemap(@Auth Optional<User> optionalUser, @PathParam("id") String id, @PathParam("from") Long from, @PathParam("to") Long to, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3ServiceData serviceData = ((Wfs3Service) service).getData();

        wfs3Core.checkCollectionName(serviceData, id);

        List<Site> sites = new ArrayList<>();

        String baseUrl = String.format("%s/%s/collections/%s/items?f=html", coreServerConfig.getExternalUrl(), serviceData.getId(), id);

        //TODO split from-to into blocks of 10, add items site for each block

        sites.add(new Site(String.format("%s&limit=%d&offset=%d", baseUrl, to - from + 1, from)));

        //TODO add item page for each feature, FeatureQuery with type=collectionId, offset=from, limit=to-from+1, fields=id featureProvider.getFeatureTransformStream with featureQuery, apply with itemSitesReader

        ItemSitesReader itemSitesReader = new ItemSitesReader(baseUrl);

        sites.addAll(itemSitesReader.getSites());

        Sitemap sitemap = new Sitemap(sites);

        return Response.ok()
                       .entity(sitemap)
                       .build();
    }
}
