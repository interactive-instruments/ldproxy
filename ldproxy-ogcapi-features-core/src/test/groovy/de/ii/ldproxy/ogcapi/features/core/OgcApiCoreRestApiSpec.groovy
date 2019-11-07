/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class OgcApiCoreRestApiSpec extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/rest/services/daraa"
    static final String SUT_COLLECTION = "aeronauticcrv"
    static final String SUT_ID = "1"

    RESTClient restClient = new RESTClient(SUT_URL)


    def 'GET request to the landing page'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH
            headers.Accept = 'application/json'
        })

        then:
        response.status == 200
        response.getContentType() == "application/json"

        and:
        response.responseData.containsKey("links")
        response.responseData.links.any{it.rel == "service-desc"}
        response.responseData.links.any{it.rel == "service-doc"}
        response.responseData.links.any{it.rel == "conformance"}
        response.responseData.links.any{it.rel == "data"}
    }

    def 'GET request to the API page'(){

        when:
        def response= restClient.get(path: SUT_PATH +"/api")

        then:
        response.status == 200

    }

    def 'GET request to the Conformance page'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/conformance'
            headers.Accept = 'application/json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("conformsTo")
        response.responseData.conformsTo.size > 0

    }


    def 'GET request to the collections page'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections'
            headers.Accept = 'application/json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("links")
        response.responseData.containsKey("collections")
        response.responseData.links.any{ it.rel == "self" }
        response.responseData.links.any{ it.rel == "alternate" }

        and: "Requirement 13B: all links shall include the 'rel' and 'type' link parameters:"
        response.responseData.links.every{ it.rel?.trim() }
        response.responseData.links.every{ it.type?.trim() }

        and: "Requirement 14, 15A: for each feature collection provided by the server, an item SHALL be provided in the property 'collections'"
        response.responseData.collections.every{ it.links.any{ it.rel == 'items' } }

        and: "Requirement 15B: all links SHALL include the rel and type properties"
        response.responseData.collections.every{ it.links.every{ it.rel == 'items' ? it.type?.trim() : true} }

        and: "Requirement 16A: extent property"
        response.responseData.collections.every{ it.containsKey("extent") ?
                it.extent.containsKey("spatial") || it.extent.containsKey("temporal") : true }

        }

    def "GET request for single collection"() {

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION
            headers.Accept = 'application/json'
        })
        def collectionsResponse = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections'
            headers.Accept = 'application/json'
        })
        def collection = getCollection(SUT_COLLECTION, collectionsResponse.responseData.collections)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("title")
        response.responseData.containsKey("links")
        response.responseData.containsKey("id")
        response.responseData.containsKey("crs")
        response.responseData.containsKey("extent")

        and: "The response shall be consistent with the content in the /collections response (id, title, description, extent)"
        collection.title == response.responseData.title
        collection.description == response.responseData.description
        if (collection.extent.containsKey("spatial")) {
            collection.extent.spatial.crs == response.responseData.extent.spatial.crs
            collection.extent.spatial.bbox == response.responseData.extent.spatial.bbox
        }
        if (collection.extent.containsKey("temporal")) {
            collection.extent.temporal.trs == response.responseData.extent.temporal.trs
            collection.extent.temporal.interval == response.responseData.extent.temporal.interval
        }


    }

    def "GET request to one collection's items page"() {
        int limit = 5

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items"
            uri.query = [limit:Integer.toString(limit)]
            headers.Accept = 'application/geo+json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("type")
        response.responseData.containsKey("links")
        response.responseData.containsKey("numberReturned")
        response.responseData.numberReturned == limit
        response.responseData.containsKey("numberMatched")
        response.responseData.containsKey("features")
        response.responseData.features.size() == limit

    }

    def 'GET request for one feature in a collection'() {

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items/" + SUT_ID
            headers.Accept = 'application/geo+json'
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


    def getCollection(collectionId, collections) {
        for (collection in collections) {
            if (collectionId == collection.id) {
                return collection
            }
        }
        return null
    }
}
