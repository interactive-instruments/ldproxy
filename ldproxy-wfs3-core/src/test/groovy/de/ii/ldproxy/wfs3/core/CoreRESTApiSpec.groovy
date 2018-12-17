package de.ii.ldproxy.wfs3.core

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Specification

class CoreRESTApiSpec extends Specification {

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testCollection = "aeronauticcrv"
    static String testFeatureId= "1"



    RESTClient restClient = new RESTClient(testURL)


    def 'GET request for the landing Page'(){

        when:
        def response= restClient.get(path: testPath)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("links")
        response.responseData.containsKey("crs")
        response.responseData.containsKey("collections")

    }

    def 'GET request for the api Page'(){

        when:
        def response= restClient.get(path: testPath +"/api")

        then:
        response.status == 200

    }

    def 'GET request for the conformance Page'(){

        when:
        def response= restClient.get(path: testPath +"/conformance")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("conformsTo")

    }


    def 'GET request for the collections Page'(){

        when:
        def response= restClient.get(path: testPath + "/collections")

        then:
        response.status == 200

        and:
        response.responseData.containsKey("links")
        response.responseData.containsKey("crs")
        response.responseData.containsKey("collections")

    }

    def 'GET request for one collection Page'(){

        when:
        def response= restClient.get(path: testPath + "/collections/" + testCollection)

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
        def response = restClient.request(testURL, Method.GET, ContentType.JSON, { req ->
            uri.path = testPath + '/collections/' + testCollection + "/items"
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
        def response = restClient.request(testURL, Method.GET, ContentType.JSON, { req ->
            uri.path = testPath + '/collections/' + testCollection + "/items/" + testFeatureId
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
        response.responseData.get("id") == testFeatureId
    }
}
