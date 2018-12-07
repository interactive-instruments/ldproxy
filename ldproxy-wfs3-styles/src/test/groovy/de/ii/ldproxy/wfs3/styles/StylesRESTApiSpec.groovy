package de.ii.ldproxy.wfs3.styles

import groovyx.net.http.RESTClient
import spock.lang.Specification

class StylesRESTApiSpec extends Specification{

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testCollection = "aeronauticcrv"
    static String testStyle = "daraa"


    RESTClient restClient = new RESTClient(testURL)


    def 'GET Request for the styles Page of the dataset'(){

        when:
        def response = restClient.get( path: testPath + '/styles')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("styles")
        response.responseData.get("styles").get(0).get("identifier")
        response.responseData.get("styles").get(0).get("links")
    }

    def 'GET Request for the styles Page of a Collection'(){

        when:
        def response = restClient.get( path: testPath + '/collections/' + testCollection + '/styles')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("styles")
        response.responseData.get("styles").get(0).get("identifier")
        response.responseData.get("styles").get(0).get("links")
    }

    def 'GET Request for a style Page'(){

        when:
        def response = restClient.get( path: testPath + '/collections/' + testCollection + '/styles/' + testStyle)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("id")
    }
}
