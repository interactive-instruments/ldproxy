/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class VectorTilesRESTApiSpec extends Specification{

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/rest/services/daraa"
    static final String SUT_TILING_SCHEME = "default"
    static final String SUT_COLLECTION = "aeronauticcrv"

    RESTClient restClient = new RESTClient(SUT_URL)

    //TODO - delete tilingSchemes Test if "tilingSchemes" is replaced with "tiles"
    def 'GET Request for the tiling Schemes Page'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/tilingSchemes')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tilingSchemes")
        response.responseData.get("tilingSchemes").get(0).get("identifier") == "default"
    }

    def 'GET Request for a tiling Scheme Page from tilingSchemes'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/tilingSchemes/'+ SUT_TILING_SCHEME)

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
        def response = restClient.get( path: SUT_PATH + '/tiles')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tilingSchemes")
        response.responseData.get("tilingSchemes").get(0).get("identifier") == "default"
    }

    def 'GET Request for a tiling Scheme Page from tiles'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/tiles/'+ SUT_TILING_SCHEME)

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
        def response=restClient.request(SUT_URL, Method.GET, ContentType.JSON,{ req ->
            uri.path = SUT_PATH + '/tiles/'+ SUT_TILING_SCHEME + '/10/413/618'
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
        restClient.request(SUT_URL, Method.GET, ContentType.JSON,{ req ->
            uri.path = SUT_PATH + '/tiles/'+ SUT_TILING_SCHEME + '/10/413/614'
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
        def response = restClient.get( path: SUT_PATH + '/collections/'+ SUT_COLLECTION +"/tiles")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tilingSchemes")
        response.responseData.get("tilingSchemes").get(0).get("identifier") == "default"
    }

    def 'GET Request for a tile of a collection in json format'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILING_SCHEME + "/10/413/614"
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
        restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILING_SCHEME + "/10/413/614"
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
