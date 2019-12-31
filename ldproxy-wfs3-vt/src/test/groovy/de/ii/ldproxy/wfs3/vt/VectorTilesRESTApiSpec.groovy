/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt

import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class VectorTilesRESTApiSpec extends Specification{

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/rest/services/daraa"
    static final String SUT_TILE_MATRIX_SET_ID = "WebMercatorQuad"
    static final String SUT_COLLECTION = "aeronauticcrv"
    static final String SUT_COLLECTION2 = "aeronauticsrf"

    RESTClient restClient = new RESTClient(SUT_URL)


    def 'GET Request for the /tileMatrixSets Page'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/tileMatrixSets')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tileMatrixSets")
        response.responseData.get("tileMatrixSets").get(0).get("id") == "WebMercatorQuad"
        response.responseData.get("tileMatrixSets").get(0).get("links").get(0).get("rel") == "item"
        response.responseData.get("tileMatrixSets").get(0).get("links").get(0).get("href") == SUT_URL + SUT_PATH + "/tileMatrixSets/WebMercatorQuad"
    }

    def 'GET Request for the tile matrix set Page from tileMatrixSets'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/tileMatrixSets/'+ SUT_TILE_MATRIX_SET_ID)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tileMatrix")
        response.responseData.containsKey("boundingBox")
        response.responseData.containsKey("identifier")
        response.responseData.containsKey("supportedCRS")
        response.responseData.containsKey("title")
        response.responseData.containsKey("type")
        response.responseData.containsKey("wellKnownScaleSet")
        response.responseData.get("identifier") == "WebMercatorQuad"
    }

    def 'GET Request for the tiles Page'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/tiles')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tileMatrixSetLinks")
        response.responseData.get("tileMatrixSetLinks").get(0).get("tileMatrixSet") == "WebMercatorQuad"
        response.responseData.get("links").any{ it.href.contains("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") }
        response.responseData.get("tileMatrixSetLinks").get(0).get("tileMatrixSetLimits").size() > 0
    }

    def 'GET Request for a empty tile of the dataset'(){

        when:
        def response=restClient.request(SUT_URL, Method.GET, ContentType.JSON,{ req ->
            uri.path = SUT_PATH + '/tiles/'+ SUT_TILE_MATRIX_SET_ID + '/10/413/618'
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
            uri.path = SUT_PATH + '/tiles/'+ SUT_TILE_MATRIX_SET_ID + '/10/413/614'
            headers.Accept = 'application/vnd.mapbox-vector-tile'

            response.success = { resp ->
                println 'request was successful'
                status = resp.status
            }
        })

        then:
        status == 200
    }

    def 'GET Request for a tiles Page from a collection'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/collections/'+ SUT_COLLECTION +"/tiles")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tileMatrixSetLinks")
        response.responseData.get("tileMatrixSetLinks").get(0).get("tileMatrixSet") == "WebMercatorQuad"
        response.responseData.get("links").any{ it.href.contains("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") }
        response.responseData.get("tileMatrixSetLinks").get(0).get("tileMatrixSetLimits").size() > 0
    }

    def 'GET Request for a tile of a collection in json format'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILE_MATRIX_SET_ID + "/10/413/615"
            headers.Accept = 'application/geo+json'
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
        response.responseData.features.size() > 0

    }

    def 'GET Request for a tile of a collection in json format, tile matrix set WorldCRS84Quad'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/WorldCRS84Quad/10/325/1231"
            headers.Accept = 'application/geo+json'
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
        response.responseData.features.size() > 0

    }

    def 'GET Request for a tile of a collection in json format, tile matrix set WorldMercatorWGS84Quad'(){

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/WorldMercatorWGS84Quad/10/414/615"
            headers.Accept = 'application/geo+json'
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
        response.responseData.features.size() > 0

    }

    def 'GET Request for a tile of a collection in mvt format'() {

        def status = 404

        when:
        restClient.request(SUT_URL, Method.GET, ContentType.JSON, { req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILE_MATRIX_SET_ID + "/10/413/614"
            headers.Accept = 'application/vnd.mapbox-vector-tile'

            response.success = { resp ->
                println 'request was successful'
                status = resp.status
            }
        })

        then:
        status == 200
    }

    def 'Vector tiles conformance classes'() {
        when: "request to the conformance page"
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = SUT_PATH + '/conformance'
            headers.Accept = 'application/json'
        })

        then: "check conformance classes"
        response.status == 200
        response.responseData.containsKey("conformsTo")
        response.responseData.get("conformsTo").any { it == 'http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/core' }
        response.responseData.get("conformsTo").any { it == 'http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/collections' }
        response.responseData.get("conformsTo").any { it == 'http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tmxs' }
        response.responseData.get("conformsTo").any { it == 'http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/multitiles' }
        response.responseData.get("conformsTo").any { it == 'http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/cols-multitiles' }
    }

    def 'Landing page request'() {
        when: "request to the landing page"
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = SUT_PATH
            headers.Accept = 'application/json'
        })
        then: "the response shall contain links to the tileMatrixSets page"
        response.responseData.get("links").any {it.rel == "tiling-schemes" && it.href.contains("/tileMatrixSets")}

    }

    def 'Unsupported request parameters (tileMatrixSet, tileMatrix, tileRow, tileCol)'() {
        when: "request tiles for a single collection"
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = requestPath
            headers.Accept = 'application/geo+json'
        })

        then: "the status code of the response is 404"
        thrown(HttpResponseException)

        where:
        requestPath                                                                                         | _
        SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + "foobar" + "/10/413/614"                  | "unknown Tile Matrix Set"
        SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILE_MATRIX_SET_ID + "/32/413/614"    | "tileMatrix out of range"
        SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILE_MATRIX_SET_ID + "/3/413/5"       | "tileRow out of range"
        SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILE_MATRIX_SET_ID + "/3/5/614"       | "tileCol out of range"
    }

    @Ignore
    def 'Tiles multitiles request'() {

        when: "request multitiles for a single collection"
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/" + SUT_TILE_MATRIX_SET_ID
            uri.query = [scaleDenominator:'6.5,7.5', bbox:'333469.2232,6565023.4598,815328.2182,7298818.9635', multiTileType:'url']
            headers.Accept = 'application/json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tileSet")
        response.responseData.get("tileSet").size() == 8
        response.responseData.get("tileSet").get(0).containsKey("tileURL")
        response.responseData.get("tileSet").get(0).containsKey("tileMatrix")
        response.responseData.get("tileSet").get(0).containsKey("tileRow")
        response.responseData.get("tileSet").get(0).containsKey("tileCol")
        response.responseData.get("tileSet").get(0).get("tileURL").contains("f=json")
    }

    @Ignore
    def 'Tiles collection multitiles request'() {
        when: "request multitiles for a single collection"
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = SUT_PATH + '/tiles/' + SUT_TILE_MATRIX_SET_ID
            uri.query = [scaleDenominator:'6.5,7.5', bbox:'333469.2232,6565023.4598,815328.2182,7298818.9635', multiTileType:'url',
                                collections:SUT_COLLECTION + ',' + SUT_COLLECTION2]
            headers.Accept = 'application/json'
        })

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tileSet")
        response.responseData.get("tileSet").size() == 8
        response.responseData.get("tileSet").get(0).containsKey("tileURL")
        response.responseData.get("tileSet").get(0).containsKey("tileMatrix")
        response.responseData.get("tileSet").get(0).containsKey("tileRow")
        response.responseData.get("tileSet").get(0).containsKey("tileCol")
        response.responseData.get("tileSet").get(0).get("tileURL").contains("collections=" + SUT_COLLECTION + "," + SUT_COLLECTION2)
    }

    def 'GET Request to the Tiles page for multitiles URI template'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/collections/'+ SUT_COLLECTION +"/tiles")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("tileMatrixSetLinks")
        response.responseData
                .get("tileMatrixSetLinks")
                .any{it.tileMatrixSet == "WebMercatorQuad"}
        response.responseData
                .get("tileMatrixSetLinks")
                .any{it.tileMatrixSet == "WorldCRS84Quad"}
        response.responseData
                .get("tileMatrixSetLinks")
                .any{it.tileMatrixSet == "WorldMercatorWGS84Quad"}


    }

    def 'filter parameter support'() {

        when:
        def response = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/WebMercatorQuad/10/413/615"
            uri.query = [filter:'fcsubtype=100454']
            headers.Accept = 'application/geo+json'
        })

        then:
        response.status == 200

        and:
        response.responseData.features.size() > 0

    }

    def 'filter-lang parameter'() {

        when:
        def response_correct = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/WebMercatorQuad/10/413/615"
            uri.query = ['filter-lang':'cql-text']
            headers.Accept = 'application/geo+json'
        })


        then:
        response_correct.status == 200

    }

    def 'invalid filter-lang parameter'() {
        when:
        def response_incorrect = restClient.request(SUT_URL, Method.GET, ContentType.JSON, {req ->
            uri.path = SUT_PATH + '/collections/' + SUT_COLLECTION + "/tiles/WebMercatorQuad/10/413/615"
            uri.query = ['filter-lang':'foobar']
            headers.Accept = 'application/geo+json'
        })

        then:
        thrown(HttpResponseException)

    }


}
