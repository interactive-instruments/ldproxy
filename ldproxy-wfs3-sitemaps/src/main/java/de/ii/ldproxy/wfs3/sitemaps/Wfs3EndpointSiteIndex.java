package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.feature.query.api.*;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointSiteIndex implements Wfs3EndpointExtension {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointSiteIndex.class);
    @Requires
    private CoreServerConfig coreServerConfig;

    @Override
    public String getPath() {
        return "sitemap_index.xml";
    }
    @GET
    public Response getDatasetSiteIndex(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3ServiceData serviceData = ((Wfs3Service) service).getData();


        // get feature count per collection
        Map<String, Long> featureCounts = getCollectionIdStream(serviceData)
                .map(collectionId -> {

                    Wfs3Service wfs3Service= (Wfs3Service)service;
                    FeatureCountReader featureCountReader =new FeatureCountReader();
                    FeatureQuery featureQuery=ImmutableFeatureQuery.builder()
                            .type(collectionId)
                            .hitsOnly(true)
                            .limit(22500000) //TODO fix limit (without limit, the limit and count is 0)
                            .build();

                    FeatureProvider featureProvider=wfs3Service.getFeatureProvider();
                    FeatureStream<FeatureConsumer> featureStream = featureProvider.getFeatureStream(featureQuery);
                    featureStream.apply(featureCountReader).toCompletableFuture().join();


                    long count= featureCountReader.getFeatureCount().getAsLong();

                    return new AbstractMap.SimpleImmutableEntry<>(collectionId, count);
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        //get total feature count
        long totalFeatureCount = featureCounts.values()
                                .stream()
                                .mapToLong(i -> i)
                                .sum();

        //check if totalFeatureCount greater than ca. 2.250.000.000, log warning

        long limit =2250000000L;
        if(totalFeatureCount > limit){
            LOGGER.error("Warning: Limit for maximum features reached");
        }

        List<Site> sitemaps = new ArrayList<>();
        String landingPageUrl = String.format("%s/%s/sitemap_landingPage.xml", coreServerConfig.getExternalUrl(), serviceData.getId(),  serviceData.getId());
        sitemaps.add(new Site(landingPageUrl));

        //TODO duration with big blocks is too long, therefore the block length is dynamically generated
        AtomicLong siteMapsNumberCounter= new AtomicLong(1);
        Map<String, Long> blockLengths= new HashMap<>();
        Map<String, Long> collectionIdNumberOfSitemaps=new HashMap<>();
        getCollectionIdStream(serviceData).forEach(collectionId -> {
            //split in little blocks
            long featureCount = featureCounts.get(collectionId);
            long blockLength = featureCount;
            while (blockLength > 2000) {
                blockLength = blockLength >> 1;
            }
            if (blockLength == 0) {
                blockLength = 1;
            }

            long numberOfSitemaps = featureCount / blockLength;
            siteMapsNumberCounter.addAndGet(numberOfSitemaps);
            collectionIdNumberOfSitemaps.put(collectionId,numberOfSitemaps);
            blockLengths.put(collectionId,blockLength);
        });

            //check if the maximum number of sitemaps is reached
        while(siteMapsNumberCounter.longValue()>50000){
            // get collection id with highest number of sitemaps
            Map.Entry<String, Long> maxEntry = null;

            for (Map.Entry<String, Long> entry : collectionIdNumberOfSitemaps.entrySet())
            {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                {
                    maxEntry = entry;
                }
            }
            String collectionId=maxEntry.getKey();

            //shift the block length one back
            long blockLength=blockLengths.get(collectionId);
            blockLength=blockLength<<1;
            blockLengths.put(collectionId,blockLength);

            //update the number of Sitemaps for the collection
            long featureCount = featureCounts.get(collectionId);
            if(blockLength==0)
                blockLength=1;
            long numberOfSitemaps = featureCount / blockLength;
            collectionIdNumberOfSitemaps.put(collectionId,numberOfSitemaps);

            //update the siteMapsNumberCounter
            long difference = -(numberOfSitemaps);
            siteMapsNumberCounter.addAndGet(difference);

        }

        getCollectionIdStream(serviceData).forEach(collectionId -> {
            long featureCount = featureCounts.get(collectionId);
            long blockLength=blockLengths.get(collectionId);
            if(blockLength==0)
                blockLength=1;
            long numberOfCompleteBlocks = featureCount / blockLength;
            long lengthOfLastBlock = featureCount % blockLength;

            long from = 0;
            long to = 0;
            for (int i = 0; i < numberOfCompleteBlocks; i++){
                from = i * blockLength;
                to = (i + 1) * blockLength - 1;
                String url = String.format("%s/%s/collections/%s/sitemap_%d_%d.xml", coreServerConfig.getExternalUrl(), serviceData.getId(), collectionId, from, to);
                sitemaps.add(new Site(url));
            }
            if(lengthOfLastBlock != 0){
                if(numberOfCompleteBlocks != 0)
                    from=to + 1;
                to = from + lengthOfLastBlock - 1;
                String url = String.format("%s/%s/collections/%s/sitemap_%d_%d.xml", coreServerConfig.getExternalUrl(), serviceData.getId(), collectionId, from, to);
                sitemaps.add(new Site(url));
            }


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
