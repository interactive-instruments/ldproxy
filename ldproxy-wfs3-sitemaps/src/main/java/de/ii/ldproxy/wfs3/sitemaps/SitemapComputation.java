/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.feature.query.api.*;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.service.api.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class SitemapComputation {

    //TODO: don't pass sites, aggregate outside
    //TODO: don't pass collectionId and serviceId, add to baseUrl on outside
    //TODO: pass single featureCount and blockLength
    //TODO: extract functions getSitemapUrl, getSiteUrl
    //TODO: test happy case: is correct number of sites returned?
    public static List<Site> getSites(List<Site> sites, String baseUrl,Long from,Long to,boolean siteIndex,String collectionId,String serviceId,Map<String, Long> featureCounts,Map<String, Long> blockLengths){

        // split from-to into blocks of 10, add items site for each block
        long blockLength ;
        long numberOfCompleteBlocks;
        long lengthOfLastBlock;
        long offset;
        long featureCount;

        if(!siteIndex){
            offset = from;
            blockLength = 10;
            numberOfCompleteBlocks = (to - from) / blockLength;
            lengthOfLastBlock=(to - from) % blockLength;
        }
        else{
            offset=0;
            featureCount = featureCounts.get(collectionId);
            blockLength=blockLengths.get(collectionId);
            if(blockLength==0)
                blockLength=1;
            numberOfCompleteBlocks = featureCount / blockLength;
            lengthOfLastBlock = featureCount % blockLength;
        }

        for (int i = 0; i < numberOfCompleteBlocks; i++){
            from = offset + i * blockLength;
            to = offset + (i + 1) * blockLength-1;
            if(!siteIndex)
                sites.add(new Site(String.format("%s&limit=%d&offset=%d", baseUrl, blockLength, from)));
            else
                sites.add(new Site(String.format("%s/%s/collections/%s/sitemap_%d_%d.xml", baseUrl, serviceId, collectionId, from, to)));
        }

        if(lengthOfLastBlock != 0){
            if(numberOfCompleteBlocks != 0)
                from = to + 1;
            to = from + lengthOfLastBlock - 1;
            if(!siteIndex)
                sites.add(new Site(String.format("%s&limit=%d&offset=%d", baseUrl, blockLength, from)));
            else
                sites.add(new Site(String.format("%s/%s/collections/%s/sitemap_%d_%d.xml", baseUrl, serviceId, collectionId, from, to)));
        }

        return sites;
    }

    //TODO pass collectionId List instead of serviceData
    //TODO test edge cases: zero total features, one collection with more than 22500000 features
    //TODO test happy case: e.g. 10 collection with 250000 total features
    public static Map<String, Long>  getDynamicLength(Wfs3ServiceData serviceData, Map<String, Long> featureCounts) {
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
        return blockLengths;
    }

    //TODO move to Wfs3ServiceData
    public static Stream<String> getCollectionIdStream(Wfs3ServiceData serviceData) {
        return serviceData.getFeatureTypes()
                .values()
                .stream()
                //TODO
                .filter(featureType -> serviceData.isFeatureTypeEnabled(featureType.getId()))
                .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                .map(FeatureTypeConfiguration::getId);
    }

    //TODO: pass FeatureProvider instead of Service, inline featureCounts
    public static Map<String,Long> getFeatureCounts(Service service){
        Wfs3Service wfs3Service = (Wfs3Service)service;

        Map<String, Long> featureCounts = SitemapComputation.getCollectionIdStream(wfs3Service.getData())
                .map(collectionId -> {

                    FeatureCountReader featureCountReader =new FeatureCountReader();
                    FeatureQuery featureQuery= ImmutableFeatureQuery.builder()
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
        return featureCounts;
    }
}
