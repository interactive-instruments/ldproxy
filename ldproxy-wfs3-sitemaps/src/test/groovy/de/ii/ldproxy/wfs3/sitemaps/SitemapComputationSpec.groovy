/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Range
import jdk.nashorn.internal.ir.annotations.Immutable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicLong

/**
 * @author zahnen
 */
class SitemapComputationSpec extends Specification {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapComputationSpec.class);


    def 'ranges for zero blocks'() {

        given: "0 blocks of length 10"

        def blockLength = 10
        def numberOfCompleteBlocks = 0
        def lengthOfLastBlock = 0

        when: "getRanges is called"

        def ranges = SitemapComputation.getRanges(blockLength, numberOfCompleteBlocks, lengthOfLastBlock, 0, 0)

        then: 'it should return an empty range list'

        ranges == []
    }

    def 'ranges for empty blocks'() {

        given: "10 blocks of length 0"

        def blockLength = 0
        def numberOfCompleteBlocks = 10
        def lengthOfLastBlock = 0

        when: "getRanges is called"

        def ranges = SitemapComputation.getRanges(blockLength, numberOfCompleteBlocks, lengthOfLastBlock, 0, 0)

        then: 'it should return 10 empty ranges'

        ranges == [Range.closed(0L,0L)] * 10
    }

    def 'ranges for non-empty blocks'() {

        given: "1 block of length 100"

        def blockLength = 100
        def numberOfCompleteBlocks = 1
        def lengthOfLastBlock = 0

        when: "getRanges is called"

        def ranges = SitemapComputation.getRanges(blockLength, numberOfCompleteBlocks, lengthOfLastBlock, 0, 0)

        then: 'it should return 1 range from 0 to 99'

        ranges == [Range.closed(0L,99L)]
    }

    def 'ranges for uneven blocks'() {

        given: "2 blocks of length 10 and 7"

        def blockLength = 10
        def numberOfCompleteBlocks = 1
        def lengthOfLastBlock = 7

        when: "getRanges is called"

        def ranges = SitemapComputation.getRanges(blockLength, numberOfCompleteBlocks, lengthOfLastBlock, 0, 0)

        then: 'it should return 2 ranges from 0 to 9 and 10 to 16'

        ranges == [Range.closed(0L,9L), Range.closed(10L,16L)]
    }

    def 'sites for empty feature count'(){

        given: "feature count and block length are 0"

        def featureCount = 0
        def blockLength = 0
        String baseUrl= "https://services.interactive-instruments.de/vtp/daraa/collections/aeronauticcrv"

        when: "getSitemaps is called"

        def sites = SitemapComputation.getSitemaps(baseUrl,featureCount,blockLength)

        then:'it should return an empty List of sites'

        sites == []


    }

    def 'sites for non-empty feature count'(){

        given: "4096 features and a block lenght of 1024"

        def featureCount = 4096
        def blockLength = 1024
        String baseUrl= "https://services.interactive-instruments.de/vtp/daraa/collections/aeronauticcrv"

        when: "getSitemaps is called"

        def sites = SitemapComputation.getSitemaps(baseUrl,featureCount,blockLength)

        then:'it should return a List of sites with the size of 4'

        sites.size() == 4


    }

    def 'dynamic block length of empty feature count'(){

        given: "a collection with no features in it"

        def featureCount = 0

        when: "getDynamicBlockLength is called"

        def blockLength = SitemapComputation.getDynamicBlockLength(featureCount);

        then: 'it should return a block length of 1'

        blockLength == 1
    }

    def 'dynamic non-shifted block length'(){

        given: "a collection with 450 features in it"

        def featureCount = 450

        when: "getDynamicBlockLength is called"

        def blockLength = SitemapComputation.getDynamicBlockLength(featureCount);

        then: 'it should return a block length of 450'

        blockLength == 450
    }

    def 'dynamic odd-numbered shifted block length'(){

        given: "a collection with 2001 features in it "

        def featureCount = 2001

        when: "getDynamicBlockLength is called"

        def blockLength = SitemapComputation.getDynamicBlockLength(featureCount);

        then: 'it should return a block length of 1000'

        blockLength == 1000
    }

    def 'dynamic even-numbered shifted block length'(){

        given: "a collection with 8192 features in it "

        def featureCount = 8192

        when: "getDynamicBlockLength is called"

        def blockLength = SitemapComputation.getDynamicBlockLength(featureCount);

        then: 'it should return a block length of 1024'

        blockLength == 1024
    }


    def 'dynamic ranges for empty collections'(){

        given: " 0 collections with 0 features"

        def collectionIds = new HashSet()
        def featureCounts = new HashMap()
        def expected = new HashMap()

        when: "getDynamicRanges is called"

        def blockLengths = SitemapComputation.getDynamicRanges(collectionIds,featureCounts)

        then: 'it should return a empty block lenghts map'

        blockLengths == expected

    }

    def 'dynamic ranges for non-empty collections'(){

        given: " 10 collections with 262144 features each "

        def collectionIds = new HashSet<String>(Arrays.asList("collection0","collection1","collection2","collection3","collection4",
                                                              "collection5", "collection6","collection7","collection8","collection9"))
        def featureCounts = new HashMap()
        def expected=new HashMap()

        for(int i = 0; i < 10; i++){
            featureCounts.put("collection" + i, 262144L)
            expected.put("collection" + i, 1024L)
        }


        when: "getDynamicRanges is called"

        def blockLengths = SitemapComputation.getDynamicRanges(collectionIds,featureCounts)

        then: 'it should return a block lenghts map with a block length of 1024 for each collection'


        blockLengths == expected
    }


    def 'expand blocks, if maximum range is not reached'(){

        given: " 25000 collections with 64 features each"

        def featureCounts = new HashMap()
        def blockLengths = new HashMap()
        def collectionIdNumberOfSitemaps = new HashMap()
        def siteMapsNumberCounter = new AtomicLong(25000)
        def expected = new HashMap()


        for(int i=0; i<25000;i++){
            featureCounts.put(i.toString(),64L)
            blockLengths.put(i.toString(),64L)
            collectionIdNumberOfSitemaps.put(i.toString(),1L)
            expected.put(i.toString(),64L)

        }
        when: "getBiggerBlocks is called"

        blockLengths = SitemapComputation.getBiggerBlocks(featureCounts,blockLengths,collectionIdNumberOfSitemaps,siteMapsNumberCounter)

        then: 'it should return the block lenght map unchanged '

        blockLengths == expected
    }

    def 'expand blocks, if maximum range is reached'(){

        given: " 49998 collections with 128 features each and one collection with 32768 features "

        def featureCounts = new HashMap()
        def blockLengths = new HashMap()
        def collectionIdNumberOfSitemaps = new HashMap()
        def expected = new HashMap()

        for(int i=0; i<49998;i++){
            featureCounts.put(i.toString(), 128L)
            blockLengths.put(i.toString(), 128L)
            collectionIdNumberOfSitemaps.put(i.toString(), 1L)
            expected.put(i.toString(), 128L)
        }

        featureCounts.put("49998",32768L)
        blockLengths.put("49998",1024L)
        collectionIdNumberOfSitemaps.put("49998",32L)
        expected.put("49998",16384L)

        def siteMapsNumberCounter = new AtomicLong(49998+32)

        when: "getBiggerBlocks is called"

        blockLengths = SitemapComputation.getBiggerBlocks(featureCounts,blockLengths,collectionIdNumberOfSitemaps,siteMapsNumberCounter)

        then: 'it should return a Map with 49998 collections with a block lenght of 128 and one collection with a block lenght of 16384 '

        blockLengths == expected
    }

    def 'dynamic ranges for collection with maximum features'(){

        given: "1 collections with 22.500.000 features"

        def collectionIds = new HashSet<String>(Arrays.asList("collection"))
        def featureCounts = new HashMap()
        def expected = new HashMap()
        
        featureCounts.put("collection",22500000L)
        expected.put("collection",1373L)

        when: "getDynamicRanges is called"

        def blockLengths = SitemapComputation.getDynamicRanges(collectionIds,featureCounts)

        then: 'it should return a map with the collection and a block lenght of 1373'

        blockLengths == expected
    }
}
