/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter

import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class FilterParameterSpecification extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String PATH = "/rest/services/kataster"
    static final String COLLECTION_FLURSTUECK = "/flurstueck"
    static final String COLLECTION_GEBAEUDEBAUWERK = "/gebaeudebauwerk"
    static final String PATH2 = "/rest/services/oneo_0619"
    static final String COLLECTION_BIOTOP = "/biotop"


    @Ignore
    def "Equality operator"() {
        when: "Request all items with a specific property value"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=flur='008'"
        then: "Return all items with the given property value"
        // result: supported
    }

    @Ignore
    def "IN operator"() {
        when: "Request all items with parameter value that matches one of the elements in the list of values"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=flur IN ('008', '009')"
        then: "Return all items that match the request"
        // result: supported
    }

    @Ignore
    def "Filter by comparing values"() {
        when: "Request all items with property value greater or less than a given value"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=flaeche>5000"
        then: "Return all items that match the request"
        // result: supported
    }

    @Ignore
    def "Filter using multiple conditions"() {
        when: "Request all items with property 'flaeche' greater than 5000 and less than 10000"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=flaeche>5000 AND flaeche<10000"
        then: "Return all items that match the request"
        // result: not supported
    }

    @Ignore
    def "Filter using text"() {
        when: "Request all items with property value similar to the given pattern"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=lagebeztxt LIKE 'Argelanderstr%'"
        then: "Return all items that match the request"
        // result: supported
    }

    @Ignore
    def "Filter by comparing time values -- equal to a date"() {

        when: "Request with operator TEQUALs"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit TEQUALs 2017-26-04T00:00:00Z"
        then: "Return items with update date equal to 26.04.2017 00:00:00"
        // result: not supported

    }

    @Ignore
    def "Filter by comparing time values -- before a date"() {

        when: "Request with the operator BEFORE"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit BEFORE 2015-01-01T00:00:00Z"
        then: "Return items with update date before 01.01.2015 00:00:00"
        // result: supported

    }

    @Ignore
    def "Filter by comparing time values -- before a period"() {

        when: "Request with the operator AFTER"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit BEFORE 2015-01-01T00:00:00Z/2016-06-01T00:00:00Z"
        then: "Return items with update date before the period 01.01.2015-01.06.2016"
        // result: supported

    }

    @Ignore
    def "Filter by comparing time values -- after a date"() {

        when: "Request with the operator AFTER"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit AFTER 2015-01-01T00:00:00Z"
        then: "Return items with update date after 01.01.2015 00:00:00"
        // result: supported

    }

    @Ignore
    def "Filter by comparing time values -- after a period"() {

        when: "Request with the operator AFTER"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit AFTER 2015-01-01T00:00:00Z/2016-06-01T00:00:00Z"
        then: "Return items with update date after the period 01.01.2015-01.06.2016"
        // result: supported

    }

    @Ignore
    def "Filter by comparing time values -- after a date with timezone"() {

        when: "Request with the operator AFTER"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit AFTER 2015-01-01T00:00:00+05:00"
        then: "Return items with update date after 01.01.2015 at 00:00:00 using timezone GMT+5"
        // result: not supported

    }

    @Ignore
    def "Filter by comparing time values -- temporal predicate with duration"() {

        when: "Request with the operator AFTER with duration"
        def uri1 = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit AFTER 2013-04-01T00:00:00Z/P20D"
        def uri2 = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit AFTER 2017-04-26T00:00:00Z/T12H"
        then: "Return items with update date 20 days after 01.04.2013 00:00:00 (uri1) " +
                "or 12 hours after 26.04.2017 00:00:00 (uri2)"
        // result: supported

    }

    @Ignore
    def "Filter by comparing time values -- DURING predicate"() {

        when: "Request with the operator DURING"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=aktualit DURING 2016-02-01T00:00:00Z/2016-12-31T00:00:00Z"
        then: "return items with update date between 01.02.2016 and 31.12.2016"
        // result: supported

    }

    @Ignore
    def "EXISTS operator"() {
        when: "Request with EXISTS"
        def uri = SUT_URL + PATH + COLLECTION_GEBAEUDEBAUWERK + "?filter=name EXISTS"
        then: "Return items that have property 'name'"
        // result: not supported
    }

    @Ignore
    def "Filter values between an interval"() {
        when: "Request with BETWEEN"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=flaeche BETWEEN 1000 AND 3000"
        then: "return items where the property value falls into the given interval"
        // result: not supported
    }

    @Ignore
    def "Filter using compound attributes"() {
        when: "Request nested properties"
        def uri = SUT_URL + PATH2 + COLLECTION_BIOTOP + "?filter=biotoptyp.zusatzbezeichnung.zusatzcode='167391'"
        then: "Return all items with the given nested property value"
        // result: not supported
    }

    @Ignore
    def "Filter using compound attributes and dates"() {
        when: "Request nested properties"
        def uri = SUT_URL + PATH2 + COLLECTION_BIOTOP + "?filter=foto.aufnahmezeitpunkt DURING 2008-11-25T00:00:00Z/2008-12-25T00:00:00Z"
        then: "Return all items that match the request"
        // result: not supported
    }

    @Ignore
    def "Filter using Geometry Relationship -- bounding box"() {
        when: "Request with bounding box"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=BBOX(geometry,50.65,7.02,50.85,7.25)"
        then: "Return only the items within the bounding box"
        // result: supported
    }

    @Ignore
    def "Logical AND"() {
        when: "Request with bounding box and date interval"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=BBOX(geometry,50.65,7.02,50.85,7.25) AND aktualit DURING 2016-08-01T00:00:00Z/2016-09-30T00:00:00Z"
        then: "Return only the items within the bounding box with last update date between 01.08.2016 and 30.09.2016"
        // result: supported
    }

    @Ignore
    def "Logical OR"() {
        when: "Request with OR"
        def uri = SUT_URL + PATH + COLLECTION_FLURSTUECK + "?filter=flur='008' OR flur='009'"
        then: "Return only the items with 'flur' equal to 008 or 009"
        // result: supported
    }

}