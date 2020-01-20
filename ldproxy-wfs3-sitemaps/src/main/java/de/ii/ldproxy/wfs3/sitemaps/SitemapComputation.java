/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.google.common.collect.Range;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureSourceStream;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SitemapComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapComputation.class);


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

    static List<Range<Long>> getRanges(final long blockLength, final long numberOfCompleteBlocks,
                                       final long lengthOfLastBlock, final long initialFrom, final long initialTo) {
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


    static Map<String, Long> getDynamicRanges(Set<String> collectionIds, Map<String, Long> featureCounts) {
        AtomicLong siteMapsNumberCounter = new AtomicLong(1);
        Map<String, Long> blockLengths = new HashMap<>();
        Map<String, Long> collectionIdNumberOfSitemaps = new HashMap<>();

        // blockLengths = getSmallBlocks(collectionIds,featureCounts,blockLengths,collectionIdNumberOfSitemaps,siteMapsNumberCounter);


        for (String collectionId : collectionIds) {
            //split in little blocks
            long featureCount = featureCounts.get(collectionId);
            long blockLength = getDynamicBlockLength(featureCount);
            long numberOfSitemaps = featureCount / blockLength;
            siteMapsNumberCounter.addAndGet(numberOfSitemaps);
            collectionIdNumberOfSitemaps.put(collectionId, numberOfSitemaps);
            blockLengths.put(collectionId, blockLength);
        }
        getBiggerBlocks(featureCounts, blockLengths, collectionIdNumberOfSitemaps, siteMapsNumberCounter);


        return blockLengths;
    }


    static long getDynamicBlockLength(long featureCount) {

        long blockLength = featureCount;
        while (blockLength > 2000) {
            blockLength = blockLength >> 1;
        }
        if (blockLength == 0) {
            blockLength = 1;
        }
        return blockLength;
    }


    static Map<String, Long> getBiggerBlocks(Map<String, Long> featureCounts, Map<String, Long> blockLengths,
                                             Map<String, Long> collectionIdNumberOfSitemaps,
                                             AtomicLong siteMapsNumberCounter) {
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
    static Stream<String> getCollectionIdStream(OgcApiApiDataV2 datasetData) {
        return datasetData.getCollections()
                          .values()
                          .stream()
                          //TODO
                          .filter(featureType -> datasetData.isCollectionEnabled(featureType.getId()))
                          .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                          .map(FeatureTypeConfiguration::getId);
    }


    static Map<String, Long> getFeatureCounts(Set<String> collectionIds, FeatureProvider2 featureProvider) {

        if (!featureProvider.supportsPassThrough()) {
            throw new IllegalStateException();
        }

        Map<String, Long> featureCounts = new HashMap<>();

        for (String collectionId : collectionIds) {
            FeatureCountReader featureCountReader = new FeatureCountReader();
            FeatureQuery featureQuery = ImmutableFeatureQuery.builder()
                                                             .type(collectionId)
                                                             .hitsOnly(true)
                                                             .limit(22500000) //TODO fix limit (without limit, the limit and count is 0)
                                                             .build();

            FeatureSourceStream featureStream = featureProvider.passThrough().getFeatureSourceStream(featureQuery);
            featureStream.runWith(featureCountReader)
                         .toCompletableFuture()
                         .join();


            long count = featureCountReader.getFeatureCount()
                                           .getAsLong();
            featureCounts.put(collectionId, count);
        }

        return featureCounts;

    }
}
