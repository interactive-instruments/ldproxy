/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.feature.query.api.*;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.service.api.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class SitemapComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapComputation.class);

    //TODO: test happy case: is correct number of sites returned?
    //TODO: test edge case: if featureCount and blockLength is zero, return empty sites
    static List<Site> getSitemaps(final String baseUrl, final long featureCount, final long blockLength) {

        // split from-to into blocks of 10, add items site for each block
        final long safeBlockLength = blockLength > 0 ? blockLength : 1;
        final long numberOfCompleteBlocks = featureCount / safeBlockLength;
        final long lengthOfLastBlock = featureCount % safeBlockLength;

        return getRanges(safeBlockLength, numberOfCompleteBlocks, lengthOfLastBlock, 0, 0).stream()
                                                                                          .map(range -> new Site(getSitemapUrl(baseUrl, range.lowerEndpoint(), range.upperEndpoint(), blockLength)))
                                                                                          .collect(Collectors.toList());
    }

    static List<Site> getSites(final String baseUrl, final long from, final long to) {

        // split from-to into blocks of 10, add items site for each block
        final long blockLength = 10;
        final long numberOfCompleteBlocks = (to - from) / blockLength;
        final long lengthOfLastBlock = (to - from) % blockLength;

        return getRanges(blockLength, numberOfCompleteBlocks, lengthOfLastBlock, from, to).stream()
                                                                                          .map(range -> new Site(getSiteUrl(baseUrl, range.lowerEndpoint(), range.upperEndpoint(), blockLength)))
                                                                                          .collect(Collectors.toList());
    }

    static List<Range<Long>> getRanges(final long blockLength, final long numberOfCompleteBlocks, final long lengthOfLastBlock, final long initialFrom, final long initialTo) {
        List<Range<Long>> ranges = new ArrayList<>();

        long from = initialFrom;
        long to = initialTo;

        for (int i = 0; i < numberOfCompleteBlocks; i++) {
            from = initialFrom + i * blockLength;
            to = initialFrom + (i + 1) * blockLength - 1;
            ranges.add(Range.closed(from, Math.max(from, to)));
        }

        if (lengthOfLastBlock != 0) {
            if (numberOfCompleteBlocks != 0)
                from = to + 1;
            to = from + lengthOfLastBlock - 1;
            ranges.add(Range.closed(from, Math.max(from, to)));
        }

        return ranges;
    }

    static String getSitemapUrl(final String baseUrl, final long from, final long to, final long blockLength) {
        return String.format("%s/sitemap_%d_%d.xml", baseUrl, from, to);
    }

    static String getSiteUrl(final String baseUrl, final long from, final long to, final long blockLength) {
        return String.format("%s&limit=%d&offset=%d", baseUrl, blockLength, from);
    }

    static List<Site> truncateToUpperLimit(List<Site> sites) {

        if (sites.size() > 50000) {
            LOGGER.warn("Sitemap or Sitemap index has more than 50000 entries, truncating");
            return sites.subList(0, 50000);
        }

        return sites;
    }

    //TODO pass collectionId List instead of serviceData
    //TODO test edge cases: zero total features, one collection with more than 22500000 features
    //TODO test happy case: e.g. 10 collection with 250000 total features
    static Map<String, Long> getDynamicLength(Wfs3ServiceData serviceData, Map<String, Long> featureCounts) {
        AtomicLong siteMapsNumberCounter = new AtomicLong(1);
        Map<String, Long> blockLengths = new HashMap<>();
        Map<String, Long> collectionIdNumberOfSitemaps = new HashMap<>();

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
            collectionIdNumberOfSitemaps.put(collectionId, numberOfSitemaps);
            blockLengths.put(collectionId, blockLength);
        });

        //check if the maximum number of sitemaps is reached
        while (siteMapsNumberCounter.longValue() > 50000) {
            // get collection id with highest number of sitemaps
            Map.Entry<String, Long> maxEntry = null;

            for (Map.Entry<String, Long> entry : collectionIdNumberOfSitemaps.entrySet()) {
                if (maxEntry == null || entry.getValue()
                                             .compareTo(maxEntry.getValue()) > 0) {
                    maxEntry = entry;
                }
            }
            String collectionId = maxEntry.getKey();

            //shift the block length one back
            long blockLength = blockLengths.get(collectionId);
            blockLength = blockLength << 1;
            blockLengths.put(collectionId, blockLength);

            //update the number of Sitemaps for the collection
            long featureCount = featureCounts.get(collectionId);
            if (blockLength == 0)
                blockLength = 1;
            long numberOfSitemaps = featureCount / blockLength;
            collectionIdNumberOfSitemaps.put(collectionId, numberOfSitemaps);

            //update the siteMapsNumberCounter
            long difference = -(numberOfSitemaps);
            siteMapsNumberCounter.addAndGet(difference);

        }
        return blockLengths;
    }

    //TODO move to Wfs3ServiceData
    static Stream<String> getCollectionIdStream(Wfs3ServiceData serviceData) {
        return serviceData.getFeatureTypes()
                          .values()
                          .stream()
                          //TODO
                          .filter(featureType -> serviceData.isFeatureTypeEnabled(featureType.getId()))
                          .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                          .map(FeatureTypeConfiguration::getId);
    }

    //TODO: pass collectionId List and FeatureProvider instead of Service
    static Map<String, Long> getFeatureCounts(Service service) {
        Wfs3Service wfs3Service = (Wfs3Service) service;

        return SitemapComputation.getCollectionIdStream(wfs3Service.getData())
                                 .map(collectionId -> {

                                                                FeatureCountReader featureCountReader = new FeatureCountReader();
                                                                FeatureQuery featureQuery = ImmutableFeatureQuery.builder()
                                                                                                                 .type(collectionId)
                                                                                                                 .hitsOnly(true)
                                                                                                                 .limit(22500000) //TODO fix limit (without limit, the limit and count is 0)
                                                                                                                 .build();

                                                                FeatureProvider featureProvider = wfs3Service.getFeatureProvider();
                                                                FeatureStream<FeatureConsumer> featureStream = featureProvider.getFeatureStream(featureQuery);
                                                                featureStream.apply(featureCountReader)
                                                                             .toCompletableFuture()
                                                                             .join();


                                                                long count = featureCountReader.getFeatureCount()
                                                                                               .getAsLong();

                                                                return new AbstractMap.SimpleImmutableEntry<>(collectionId, count);
                                                            })
                                 .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
