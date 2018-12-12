package de.ii.ldproxy.wfs3.styles.manager

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Specification

class StylesManagerRESTApiSpec extends Specification {

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testCollection = "aeronauticcrv"
    static String testStyle1 = "default"


    RESTClient restClient = new RESTClient(testURL)

    def 'PUT Request for a style of the dataset'(){

        when:
        def response=restClient.request(testURL, Method.PUT, ContentType.JSON,{ req ->
            uri.path = testPath + '/styles/'+ testStyle1
            headers.Accept = 'application/json'
            body="{\"id\": \"default\"}"
        })


        then:
        response.status == 204

        and:
        response.responseData == null
    }

    def 'PUT Request for a style of a collection'(){

        when:
        def response=restClient.request(testURL, Method.PUT, ContentType.JSON,{ req ->
            uri.path = testPath +'/collections/' +testCollection + '/styles/'+ testStyle1
            headers.Accept = 'application/json'
            body="{\"id\": \"default\"}"
        })


        then:
        response.status == 204

        and:
        response.responseData == null
    }




}
