package de.ii.ldproxy.wfs3.styles.representation

import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Specification

class StylesRepresentationRESTApiSpec extends Specification {

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testStyle = "topographic"

    RESTClient restClient = new RESTClient(testURL)

    def 'Get Request for one Style Representation/Map'(){

        when:

        def response = restClient.request(testURL, Method.GET, "text/html",{ req ->
            uri.path = testPath + '/maps/' +testStyle
            headers.Accept = 'text/html'
        })

        then:
        response.status == 200

    }
}
