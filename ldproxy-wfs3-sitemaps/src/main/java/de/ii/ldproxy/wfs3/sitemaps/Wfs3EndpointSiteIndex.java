package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointSiteIndex implements Wfs3EndpointExtension {

    @Requires
    private CoreServerConfig coreServerConfig;

    @Override
    public String getPath() {
        return "sitemap_index.xml";
    }

    @GET
    public Response getDatasetSiteIndex(@Auth Optional<User> optionalUser, @PathParam("id") String id, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3ServiceData serviceData = ((Wfs3Service) service).getData();


        // get feature count per collection
        Map<String, Long> featureCounts = getCollectionIdStream(serviceData)
                .map(collectionId -> {
                    //TODO get actual count: FeatureQuery with type=collectionId and hitsOnly=true, featureProvider.getFeatureStream with featureQuery, apply with FeatureCountReader
                    long count = 44999L;

                    return new AbstractMap.SimpleImmutableEntry<>(collectionId, count);
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        //get total feature count
        long totalFeatureCount = featureCounts.values()
                                .stream()
                                .mapToLong(i -> i)
                                .sum();

        //TODO: check if totalFeatureCount greater than ca. 2.250.000.000, log warning

        List<Site> sitemaps = new ArrayList<>();

        //add items pages to sitemaps
        getCollectionIdStream(serviceData).forEach(collectionId -> {

            long featureCount = featureCounts.get(collectionId);

            //TODO split featureCount into blocks of max 45000, add one site per block
            //example: if featureCount=123000, add 3 sites: sitemap_0_44999.xml, sitemap_45000_94999.xml and sitemap_950000_123000.xml

            long from = 0;
            long to = featureCount;
            String url = String.format("%s/%s/collections/%s/sitemap_%d_%d.xml", coreServerConfig.getExternalUrl(), serviceData.getId(), collectionId, from, to);

            sitemaps.add(new Site(url));

        });



        SitemapIndex sitemapIndex = new SitemapIndex(sitemaps);

        return Response.ok()
                       .entity(sitemapIndex)
                       .build();
    }

    private Stream<String> getCollectionIdStream(Wfs3ServiceData serviceData) {
        return serviceData.getFeatureTypes()
                                                       .values()
                                                       .stream()
                                                       //TODO
                                                       .filter(featureType -> serviceData.isFeatureTypeEnabled(featureType.getId()))
                                                       .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                                                       .map(FeatureTypeConfiguration::getId);
    }
}
