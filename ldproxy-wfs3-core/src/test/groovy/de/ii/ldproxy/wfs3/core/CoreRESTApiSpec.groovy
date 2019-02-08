/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class CoreRESTApiSpec extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/rest/services/daraa"
    static final String SUT_COLLECTION = "aeronauticcrv"
    static final String SUT_ID = "1"



    RESTClient restClient = new RESTClient(SUT_URL)


    def 'GET request for the landing Page'(){

        when:
        def response= restClient.get(path: SUT_PATH)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("links")
        response.responseData.containsKey("crs")
        response.responseData.containsKey("collections")

    }

    def 'GET request for the api Page'(){

        when:
        def response= restClient.get(path: SUT_PATH +"/api")

        then:
        response.status == 200

    }

    def 'GET request for the conformance Page'(){

        when:
        def response= restClient.get(path: SUT_PATH +"/conformance")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("conformsTo")

    }


    def 'GET request for the collections Page'(){

        when:
        def response= restClient.get(path: SUT_PATH + "/collections")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("links")
        response.responseData.containsKey("crs")
        response.responseData.containsKey("collections")

    }

    def 'GET request for one collection Page'(){

        when:
        def response= restClient.get(path: SUT_PATH + "/collections/" + SUT_COLLECTION)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("name")
        response.responseData.containsKey("title")
        response.responseData.containsKey("extent")
        response.responseData.containsKey("links")

    }

    def 'GET request for one collections items Page'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items"
            headers.Accept = 'application/json'
        })
        then:
        response.status == 200

        and:
        response.responseData.containsKey("type")
        response.responseData.containsKey("links")
        response.responseData.containsKey("numberReturned")
        response.responseData.containsKey("numberMatched")
        response.responseData.containsKey("features")
    }

    def 'GET request for one feature in a collection'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items/" + SUT_ID
            headers.Accept = 'application/json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("type")
        response.responseData.containsKey("links")
        response.responseData.containsKey("geometry")
        response.responseData.get("geometry").containsKey("type")
        response.responseData.get("geometry").containsKey("coordinates")
        response.responseData.containsKey("properties")
        response.responseData.containsKey("id")
        response.responseData.get("id") == SUT_ID
    }
}
