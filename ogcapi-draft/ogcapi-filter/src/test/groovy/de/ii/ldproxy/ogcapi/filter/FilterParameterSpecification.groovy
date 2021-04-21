/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

/** These test assume that the following APIs are available:
 * <ul>
 *     <li>daraa (same data and configuration as https://demo.ldproxy.net/daraa)
 * </ul>
 */
@Requires({env['SUT_URL'] != null})
class FilterParameterSpecification extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String API_PATH_DARAA = "/rest/services/daraa"
    static final String CULTURE_PNT = "CulturePnt"
    static final String TRANSPORTATION_GROUND_CRV = "TransportationGroundCrv"
    static final String GEO_JSON = "application/geo+json";

    RESTClient restClient = new RESTClient(SUT_URL)

    // Comparison predicates

    def "Operator eq"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        when: "1. Data is selected using a filter F_CODE=F_CODE"
        def twoProperties = getRequest(restClient, path, [filter:"F_CODE=F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter F_CODE='AL030'"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE='AL030'"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE=='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using F_CODE=AL030"
        def usingQueryParam = getRequest(restClient, path, [F_CODE:"AL030"])

        then: "Success and returns GeoJSON"
        assertSuccess(usingQueryParam)

        and: "Returns the same number of features as a filter"
        propertyAndLiteralString.responseData.numberReturned == usingQueryParam.responseData.numberReturned

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], usingQueryParam.responseData.features[i])
        }

        when: "4. Data is selected using a filter ZI037_REL=11"
        def propertyAndLiteralNumeric = getRequest(restClient, path, [filter:"ZI037_REL=11"])
        def propertyAndLiteralNumericCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.ZI037_REL==11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'=F_CODE"
        def literalAndProperty = getRequest(restClient, path, [filter:"'AL030'=F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns one feature"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected feature"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }
    }

    def "Operator neq"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        when: "1. Data is selected using a filter F_CODE<>F_CODE"
        def twoProperties = getRequest(restClient, path, [filter:"F_CODE<>F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns no features"
        twoProperties.responseData.numberReturned == 0

        when: "2. Data is selected using a filter F_CODE<>'AL030'"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE<>'AL030'"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE!='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL<>11"
        def propertyAndLiteralNumeric = getRequest(restClient, path, [filter:"ZI037_REL<>11"])
        def propertyAndLiteralNumericCheck = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL!=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL<>10"
        def propertyAndLiteralNumeric2 = getRequest(restClient, path, [filter:"ZI037_REL<>10"])
        def propertyAndLiteralNumeric2Check = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL!=10 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'<>F_CODE"
        def literalAndProperty = getRequest(restClient, path, [filter:"'AL030'<>F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }
    }

    def "Operator lt"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        when: "1. Data is selected using a filter F_CODE<F_CODE"
        def twoProperties = getRequest(restClient, path, [filter:"F_CODE<F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns no features"
        twoProperties.responseData.numberReturned == 0

        when: "2. Data is selected using a filter F_CODE<'AL030'"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE<'AL030'"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL<11"
        def propertyAndLiteralNumeric = getRequest(restClient, path, [filter:"ZI037_REL<11"])
        def propertyAndLiteralNumericCheck = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL<12"
        def propertyAndLiteralNumeric2 = getRequest(restClient, path, [filter:"ZI037_REL<12"])
        def propertyAndLiteralNumeric2Check = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<12 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'<F_CODE"
        def literalAndProperty = getRequest(restClient, path, [filter:"'AL030'<F_CODE"])
        def literalAndPropertyCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            // TODO assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }
    }

    def "Operator gt"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        when: "1. Data is selected using a filter F_CODE>F_CODE"
        def twoProperties = getRequest(restClient, path, [filter:"F_CODE>F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns no features"
        twoProperties.responseData.numberReturned == 0

        when: "2. Data is selected using a filter F_CODE>'AL030'"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE>'AL030'"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL>11"
        def propertyAndLiteralNumeric = getRequest(restClient, path, [filter:"ZI037_REL>11"])
        def propertyAndLiteralNumericCheck = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL>0"
        def propertyAndLiteralNumeric2 = getRequest(restClient, path, [filter:"ZI037_REL>0"])
        def propertyAndLiteralNumeric2Check = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>0 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'>F_CODE"
        def literalAndProperty = getRequest(restClient, path, [filter:"'AL030'>F_CODE"])
        def literalAndPropertyCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<'AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            // TODO assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }
    }

    def "Operator lteq"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        when: "1. Data is selected using a filter F_CODE<=F_CODE"
        def twoProperties = getRequest(restClient, path, [filter:"F_CODE<=F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter F_CODE<='AL030'"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE<='AL030'"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL<=11"
        def propertyAndLiteralNumeric = getRequest(restClient, path, [filter:"ZI037_REL<=11"])
        def propertyAndLiteralNumericCheck = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL<=10"
        def propertyAndLiteralNumeric2 = getRequest(restClient, path, [filter:"ZI037_REL<=10"])
        def propertyAndLiteralNumeric2Check = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL<=10 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'<=F_CODE"
        def literalAndProperty = getRequest(restClient, path, [filter:"'AL030'<=F_CODE"])
        def literalAndPropertyCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            // TODO assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }
    }

    def "Operator gteq"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        when: "1. Data is selected using a filter F_CODE>=F_CODE"
        def twoProperties = getRequest(restClient, path, [filter:"F_CODE>=F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter F_CODE<='AL030'"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE>='AL030'"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE>='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter ZI037_REL>=11"
        def propertyAndLiteralNumeric = getRequest(restClient, path, [filter:"ZI037_REL>=11"])
        def propertyAndLiteralNumericCheck = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL>=12"
        def propertyAndLiteralNumeric2 = getRequest(restClient, path, [filter:"ZI037_REL>=12"])
        def propertyAndLiteralNumeric2Check = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && f.properties.ZI037_REL>=12 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

        when: "5. Data is selected using a filter 'AL030'>=F_CODE"
        def literalAndProperty = getRequest(restClient, path, [filter:"'AL030'>=F_CODE"])
        def literalAndPropertyCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE<='AL030' ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralString.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            // TODO assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }
    }

    def "Operator like"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        /* TODO does not yet work
        when: "1. Data is selected using a filter F_CODE LiKe F_CODE"
        def twoProperties = getRequest(restClient, path, [filter:"F_CODE LiKe F_CODE"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allFeatures.responseData.numberReturned
         */

        when: "2. Data is selected using a filter F_CODE LiKe 'AL0%'"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE LiKe 'AL0%'"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.startsWith('AL0') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "3. Data is selected using a filter F_CODE LiKe 'AL0*' wildCard '*'"
        def propertyAndLiteralString2 = getRequest(restClient, path, [filter:"F_CODE LiKe 'AL0*' wildCard '*'"])

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString2)

        and: "Returns the same number of features"
        propertyAndLiteralString2.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString2.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "4. Data is selected using a filter F_CODE LiKe 'AL0..' singleChar '.'"
        def propertyAndLiteralString3 = getRequest(restClient, path, [filter:"F_CODE LiKe 'AL0..' singleChar '.'"])

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString3)

        and: "Returns the same number of features"
        propertyAndLiteralString3.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString3.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString3.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "5. Data is selected using a filter F_CODE LiKe 'al0..' singleChar '.' ESCAPECHAR '?'"
        def propertyAndLiteralString4 = getRequest(restClient, path, [filter:"F_CODE LiKe 'al0..' singleChar '.' ESCAPECHAR '?'"])

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString4)

        and: "Returns the same number of features"
        propertyAndLiteralString4.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString4.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString4.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "6. Data is selected using a filter F_CODE LiKe 'al0%' NoCasE true"
        def propertyAndLiteralString5 = getRequest(restClient, path, [filter:"F_CODE LiKe 'al0%' NoCasE true"])
        def propertyAndLiteralString5Check = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.toLowerCase().startsWith('al0') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString5)

        and: "Returns the same number of features"
        propertyAndLiteralString5.responseData.numberReturned == propertyAndLiteralString5Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString5.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString5.responseData.features[i], propertyAndLiteralString5Check.get(i))
        }

        when: "7. Data is selected using a filter F_CODE LiKe 'al0%' NoCasE FALSE"
        // TODO currently only true/false, work, not the other options: "TRUE" | "FALSE" | "T" | "t" | "F" | "f" | "1" | "0"
        def propertyAndLiteralString6 = getRequest(restClient, path, [filter:"F_CODE LiKe 'al0%' NoCasE false"]) // TODO use FALSE
        def propertyAndLiteralString6Check = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.startsWith('al0') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString6)

        and: "Returns the same number of features"
        propertyAndLiteralString6.responseData.numberReturned == propertyAndLiteralString6Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString6.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString6.responseData.features[i], propertyAndLiteralString6Check.get(i))
        }
    }

    def "Operator between"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        /* TODO not yet supported
        when: "1. Data is selected using a filter ZI037_REL BeTweeN ZI037_REL AnD ZI037_REL"
        def twoProperties = getRequest(restClient, path, [filter:"ZI037_REL BeTweeN ZI037_REL AnD ZI037_REL"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties)

        and: "Returns all features"
        twoProperties.responseData.numberReturned == allFeatures.responseData.numberReturned

        when: "2. Data is selected using a filter ZI037_REL NoT BeTweeN 0 AnD ZI037_REL"
        def twoProperties2 = getRequest(restClient, path, [filter:"ZI037_REL NoT BeTweeN 0 AnD ZI037_REL"])

        then: "Success and returns GeoJSON"
        assertSuccess(twoProperties2)

        and: "Returns no features"
        twoProperties2.responseData.numberReturned == 0
         */

        when: "3. Data is selected using a filter ZI037_REL BeTweeN 0 AnD 10"
        def propertyAndLiteral = getRequest(restClient, path, [filter:"ZI037_REL BeTweeN 0 AnD 10"])
        def propertyAndLiteralCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.ZI037_REL>=0 && f.properties.ZI037_REL<=10 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL BeTweeN 0 AnD 11"
        def propertyAndLiteral2 = getRequest(restClient, path, [filter:"ZI037_REL BeTweeN 0 AnD 11"])
        def propertyAndLiteral2Check = allFeatures.responseData.features.stream().filter( f -> f.properties.ZI037_REL>=0 && f.properties.ZI037_REL<=11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of features"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteral2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteral2Check.get(i))
        }

        /* TODO not yet supported
        when: "4. Data is selected using a filter ZI037_REL NoT BeTweeN 0 AnD 10"
        def propertyAndLiteral3 = getRequest(restClient, path, [filter:"ZI037_REL NoT BeTweeN 0 AnD 10"])
        def propertyAndLiteral3Check = allFeatures.responseData.features.stream().filter( f -> f.properties.ZI037_REL<0 && f.properties.ZI037_REL>10 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral3)

        and: "Returns the same number of features"
        propertyAndLiteral3.responseData.numberReturned == propertyAndLiteral3Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral3.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral3.responseData.features[i], propertyAndLiteral3Check.get(i))
        }

        when: "5. Data is selected using a filter ZI037_REL NoT BeTweeN 0 AnD 11"
        def propertyAndLiteral4 = getRequest(restClient, path, [filter:"ZI037_REL NoT BeTweeN 0 AnD 11"])
        def propertyAndLiteral4Check = allFeatures.responseData.features.stream().filter( f -> f.properties.ZI037_REL<0 && f.properties.ZI037_REL>11 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral4)

        and: "Returns the same number of features"
        propertyAndLiteral4.responseData.numberReturned == propertyAndLiteral4Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral4.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral4.responseData.features[i], propertyAndLiteral4Check.get(i))
        }

        when: "7. Data is selected using a filter 6 BeTweeN 0 AnD ZI037_REL"
        def literalAndProperty = getRequest(restClient, path, [filter:"6 BeTweeN 0 AnD ZI037_REL"])
        def literalAndPropertyCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.ZI037_REL>=6 ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(literalAndProperty)

        and: "Returns the same number of features"
        literalAndProperty.responseData.numberReturned == propertyAndLiteralCheck.responseData.numberReturned

        and: "Returns the expected features"
        for (int i=0; i<literalAndProperty.responseData.numberReturned; i++) {
            assertFeature(literalAndProperty.responseData.features[i], literalAndPropertyCheck.get(i))
        }
         */
    }

    def "Operator in"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        // TODO need test dataset to test expressions with two properties

        /* TODO not yet supported?
        when: "1. Data is selected using a filter F_CODE iN ('AL030', 'AL012')"
        def propertyAndLiteralString = getRequest(restClient, path, [filter:"F_CODE iN ('AL030', 'AL012')"])
        def propertyAndLiteralStringCheck = allFeatures.responseData.features.stream().filter( f -> f.properties.F_CODE.equals('AL012') || f.properties.F_CODE.equals('AL030') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString)

        and: "Returns the same number of features"
        propertyAndLiteralString.responseData.numberReturned == propertyAndLiteralStringCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString.responseData.features[i], propertyAndLiteralStringCheck.get(i))
        }

        when: "1. Data is selected using a filter F_CODE NoT iN ('AL030', 'AL012')"
        def propertyAndLiteral2String = getRequest(restClient, path, [filter:"F_CODE NoT iN ('AL030', 'AL012')"])
        def propertyAndLiteral2StringCheck = allFeatures.responseData.features.stream().filter( f -> !f.properties.F_CODE.equals('AL012') && !f.properties.F_CODE.equals('AL030') ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralString2)

        and: "Returns the same number of features"
        propertyAndLiteralString2.responseData.numberReturned == propertyAndLiteralString2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteralString2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralString2.responseData.features[i], propertyAndLiteralString2Check.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL iN (11, 12)"
        def propertyAndLiteralNumeric = getRequest(restClient, path, [filter:"ZI037_REL iN (11, 12)"])
        def propertyAndLiteralNumericCheck = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && (f.properties.ZI037_REL==11 || f.properties.ZI037_REL==12) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric.responseData.numberReturned == propertyAndLiteralNumericCheck.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric.responseData.features[i], propertyAndLiteralNumericCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL NoT iN (11, 12)"
        def propertyAndLiteralNumeric2 = getRequest(restClient, path, [filter:"ZI037_REL NoT iN (11, 12)"])
        def propertyAndLiteralNumeric2Check = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) && (f.properties.ZI037_REL==11 || f.properties.ZI037_REL==12) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteralNumeric2)

        and: "Returns the same number of features"
        propertyAndLiteralNumeric2.responseData.numberReturned == propertyAndLiteralNumeric2Check.size()

        and: "Returns the expected features"
        for (int i=0; i<propertyAndLiteralNumeric2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteralNumeric2.responseData.features[i], propertyAndLiteralNumeric2Check.get(i))
        }

         */
    }

    def "Operator null"() {
        given: "CulturePnt features in the Daraa dataset"
        def path = API_PATH_DARAA + "/collections/" + CULTURE_PNT + "/items"

        when:
        def allFeatures = getRequest(restClient, path, null)

        then: "Success and returns GeoJSON"
        assertSuccess(allFeatures)

        when: "3. Data is selected using a filter ZI037_REL iS NulL"
        def propertyAndLiteral = getRequest(restClient, path, [filter:"ZI037_REL iS NulL"])
        def propertyAndLiteralCheck = allFeatures.responseData.features.stream().filter( f -> Objects.isNull(f.properties.ZI037_REL) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral)

        and: "Returns the same number of features"
        propertyAndLiteral.responseData.numberReturned == propertyAndLiteralCheck.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral.responseData.features[i], propertyAndLiteralCheck.get(i))
        }

        when: "4. Data is selected using a filter ZI037_REL iS NoT NulL"
        def propertyAndLiteral2 = getRequest(restClient, path, [filter:"ZI037_REL iS NoT NulL"])
        def propertyAndLiteral2Check = allFeatures.responseData.features.stream().filter( f -> Objects.nonNull(f.properties.ZI037_REL) ).toList()

        then: "Success and returns GeoJSON"
        assertSuccess(propertyAndLiteral2)

        and: "Returns the same number of features"
        propertyAndLiteral2.responseData.numberReturned == propertyAndLiteral2Check.size()

        and: "Returns the same feature arrays"
        for (int i=0; i<propertyAndLiteral2.responseData.numberReturned; i++) {
            assertFeature(propertyAndLiteral2.responseData.features[i], propertyAndLiteral2Check.get(i))
        }
    }

    // Logical operators TODO

    // Spatial predicates TODO

    // Temporal predicates TODO

    // Array predicates TODO

    // CQL JSON TODO

    static void assertSuccess(Object response) {
        assert response.status == 200
        assert response.getContentType() == "application/geo+json"
    }

    static void assertFeature(Object feature1, Object feature2) {
        assert feature1.id == feature2.id
        assert feature1.type == feature2.type
        assert feature1.properties == feature2.properties
        assert feature1.geometry == feature2.geometry
    }

    static Object getRequest(restClient, path, query) {
        return restClient.request(Method.GET,  ContentType.JSON, { req ->
            uri.path = path
            uri.query = query
            headers.Accept = GEO_JSON
        })
    }

}