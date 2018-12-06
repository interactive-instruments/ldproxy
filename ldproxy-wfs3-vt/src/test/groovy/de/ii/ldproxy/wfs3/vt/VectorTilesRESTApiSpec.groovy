/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Specification

class VectorTilesRESTApiSpec extends Specification{

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testTilingScheme = "default"
    static String testCollection = "aeronauticcrv"

    RESTClient restClient = new RESTClient(testURL)

    //TODO - delete tilingSchemes Test if "tilingSchemes" is replaced with "tiles"
    def 'GET Request for the tiling Schemes Page'(){

        when:
        def response = restClient.get( path: testPath + '/tilingSchemes')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tilingSchemes")
        response.responseData.get("tilingSchemes").get(0).get("identifier") == "default"
    }

    def 'GET Request for a tiling Scheme Page from tilingSchemes'(){

        when:
        def response = restClient.get( path: testPath + '/tilingSchemes/'+ testTilingScheme)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("TileMatrix")
        response.responseData.containsKey("boundingBox")
        response.responseData.containsKey("identifier")
        response.responseData.containsKey("supportedCRS")
        response.responseData.containsKey("title")
        response.responseData.containsKey("type")
        response.responseData.containsKey("wellKnownScaleSet")
        response.responseData.get("identifier") == "default"
    }

    def 'GET Request for the tiles Page'(){

        when:
        def response = restClient.get( path: testPath + '/tiles')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tilingSchemes")
        response.responseData.get("tilingSchemes").get(0).get("identifier") == "default"
    }

    def 'GET Request for a tiling Scheme Page from tiles'(){

        when:
        def response = restClient.get( path: testPath + '/tiles/'+ testTilingScheme)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("TileMatrix")
        response.responseData.containsKey("boundingBox")
        response.responseData.containsKey("identifier")
        response.responseData.containsKey("supportedCRS")
        response.responseData.containsKey("title")
        response.responseData.containsKey("type")
        response.responseData.containsKey("wellKnownScaleSet")
        response.responseData.get("identifier") == "default"
    }

    def 'GET Request for a empty tile of the dataset'(){

        when:
        def response=restClient.request(testURL, Method.GET, ContentType.JSON,{ req ->
            uri.path = testPath + '/tiles/'+ testTilingScheme + '/10/413/618'
            headers.Accept = 'application/vnd.mapbox-vector-tile'
        })

        then:
        response.status == 200

        and:
        response.responseData == null
    }

    def 'GET Request for a non-empty tile of the dataset'(){

        def status = 404

        when:
        restClient.request(testURL, Method.GET, ContentType.JSON,{ req ->
            uri.path = testPath + '/tiles/'+ testTilingScheme + '/10/413/614'
            headers.Accept = 'application/vnd.mapbox-vector-tile'

            response.success = { resp ->
                println 'request was successful'
                status = resp.status
            }
        })

        then:
        status == 200
    }

    def 'GET Request for a tiling Scheme Page from a collection'(){

        when:
        def response = restClient.get( path: testPath + '/collections/'+ testCollection +"/tiles")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tilingSchemes")
        response.responseData.get("tilingSchemes").get(0).get("identifier") == "default"
    }

    def 'GET Request for a tile of a collection in json format'(){

        when:
        def response = restClient.request(testURL, Method.GET, ContentType.JSON, { req ->
            uri.path = testPath + '/collections/' + testCollection + "/tiles/" + testTilingScheme + "/10/413/614"
            headers.Accept = 'application/json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("type")
        response.responseData.get("type") == "FeatureCollection"
        response.responseData.containsKey("links")
        response.responseData.containsKey("numberReturned")
        response.responseData.containsKey("numberMatched")
        response.responseData.containsKey("timeStamp")
        response.responseData.containsKey("features")
    }

    def 'GET Request for a tile of a collection in mvt format'() {

        def status = 404

        when:
        restClient.request(testURL, Method.GET, ContentType.JSON, { req ->
            uri.path = testPath + '/collections/' + testCollection + "/tiles/" + testTilingScheme + "/10/413/614"
            headers.Accept = 'application/vnd.mapbox-vector-tile'

            response.success = { resp ->
                println 'request was successful'
                status = resp.status
            }
        })

        then:
        status == 200
    }
}
