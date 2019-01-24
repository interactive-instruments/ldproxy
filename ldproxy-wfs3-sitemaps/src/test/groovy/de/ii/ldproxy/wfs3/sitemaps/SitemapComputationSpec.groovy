/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps

import com.google.common.collect.Range
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

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

}
