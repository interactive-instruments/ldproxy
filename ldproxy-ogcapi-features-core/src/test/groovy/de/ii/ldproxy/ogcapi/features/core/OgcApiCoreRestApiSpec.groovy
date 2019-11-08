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
import groovyx.net.http.URIBuilder
import spock.lang.Requires
import spock.lang.Specification

import java.time.ZonedDateTime
import java.time.ZoneOffset
import  java.time.format.DateTimeFormatter

@Requires({env['SUT_URL'] != null})
class OgcApiCoreRestApiSpec extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/rest/services/daraa"
    static final String SUT_COLLECTION = "agriculturesrf"
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

        and: "Requirement 18B: The response shall be consistent with the content in the /collections response (id, title, description, extent)"
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

        ZonedDateTime timestamp = ZonedDateTime.now(ZoneOffset.UTC)
        String formattedTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(timestamp)

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items"
            headers.Accept = 'application/geo+json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("type")
        response.responseData.containsKey("links")
        response.responseData.containsKey("numberReturned")
        response.responseData.containsKey("features")
        and: "Requirement 27: include a link to this response document and a link to the response document in other supported formats"
        response.responseData.links.any{ it.rel == "self" }
        response.responseData.links.any{ it.rel == "alternate" }
        and: "Requirement 28: all links shall include the rel and type link parameters"
        response.responseData.link.every{ it.rel?.trim() }
        response.responseData.link.every{ it.type?.trim() }
        and: "Requirement 29: if included, 'timestamp' shall be set to the time stamp when the response was generated"
        if (response.responseData.containsKey("timestamp")) {
            response.responseData.timestamp >= formattedTimestamp
        }
    }

    def "GET request to one collection's items page (limit filter parameter)"() {

        given:
        int limit = 5

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items"
            uri.query = [limit:Integer.toString(limit)]
            headers.Accept = 'application/geo+json'
        })

        then: "Requirement 20: limit parameter support"
        response.status == 200

        and: "Requirement 21: the response shall not contain more features than specified by the optional limit parameter"
        response.responseData.numberReturned == limit
        response.responseData.containsKey("features")
        response.responseData.features.size() == limit
    }

    def "GET request to one collection's items page (bbox filter parameter)"() {

        given:
        def bbox = "35.898213,32.675795,36.023426,32.8370158"

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items"
            uri.query = [bbox:bbox, limit:30]
            headers.Accept = 'application/geo+json'
        })

        then: "bbox parameter support"
        response.status == 200

        and: "Requirement 23: only features that have a spatial geometry that intersects the bounding box shall be part of the result set"
        response.responseData.numberReturned == 15
        response.responseData.numberMatched == 15
        response.responseData.containsKey("features")
        response.responseData.features.size() == 15
    }

    def "GET request to one collection's items page (dateTime filter parameter)"() {

        given: "Time interval between November 1, 2014 and November 1, 2019"
        def interval = "2014-11-01T00%3A00%3A00Z%2F2019-11-01T00%3A00%3A00Z"

        when:
        def datetimeUri = new URIBuilder(
                new URI(SUT_URL + SUT_PATH + '/collections/' + SUT_COLLECTION + '/items?limit=30&datetime=' + interval)
        )
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri = datetimeUri
            headers.Accept = 'application/geo+json'
        })

        then: "Requirement 24: datetime parameter support"
        response.status == 200

        and: "Requirement 25: only features that have a temporal geometry that intersects the temporal information" +
                " in the datetime parameter shall be part of the result set"
        response.responseData.numberMatched == 12
        response.responseData.numberReturned == 12
        response.responseData.containsKey("features")
        response.responseData.features.size() == 12
    }

    def "GET request to one collection's items page (combination of filter parameters)" () {

        given:
        int limit = 5
        String interval = "2014-11-01T00%3A00%3A00Z%2F2019-11-01T00%3A00%3A00Z"
        String bbox = "36.097641,32.586742,36.259689,32.776306"

        when:
        def fullUri = new URIBuilder(
                new URI(SUT_URL + SUT_PATH + '/collections/' + SUT_COLLECTION + '/items?limit=' + limit +
                        '&datetime=' + interval + '&bbox=' + bbox)
        )
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri = fullUri
            headers.Accept = 'application/geo+json'
        })

        then:
        response.status == 200

        and: "Requirement 30: if included, 'numberMatched' shall be identical to the number of features that match the filter parameters"
        if (response.responseData.containsKey("numberMatched")) {
            response.responseData.numberMatched == 9
        }
        and: "Requirement 31: if included, 'numberReturned' shall be identical to the number of features returned in the response"
        if (response.responseData.containsKey("numberReturned")) {
            response.responseData.numberReturned == limit
            response.responseData.features.size() == limit
        }

    }

    def 'GET request for one feature in a collection'() {

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/items/" + SUT_ID
            headers.Accept = 'application/geo+json'
        })

        then: "Requirement 32, 33: HTTP GET support at path '/collections/{collectionId}/items/{featureId}'"
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
        and: "Requirement 35: all links shall include the 'rel' and 'type' link parameters"
        response.responseData.links.every{ it.rel?.trim() }
        response.responseData.links.every{ it.type?.trim() }
        and: "Requirement 34: links with relations 'self', 'alternate', 'collection'"
        response.responseData.links.any{ it.rel == "self" }
        response.responseData.links.any{ it.rel == "alternate" }
        response.responseData.links.any{ it.rel == "collection" }
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
