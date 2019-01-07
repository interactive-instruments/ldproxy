package de.ii.ldproxy.wfs3.transactional


import groovyx.net.http.*
import spock.lang.Specification

class TransactionalRESTApiSpec extends Specification{

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testCollection = "aeronauticcrv"
    static String testFeatureId2= "21"

    RESTClient restClient = new RESTClient(testURL)

    def 'POST Request for one feature of a collection'(){


        when:
        restClient.encoder.'application/geo+json' = restClient.encoder."application/json"

        def response = restClient.post(
                path:  testPath + '/collections/' + testCollection + "/items/",
                body : "{\"type\": \"Feature\",\"geometry\": {\"type\": \"Point\"},\"properties\": {}}",
                requestContentType : "application/geo+json"
        )

        then:
        response.status == 204

        and:
        response.responseData == null
    }

    def 'PUT Request for one feature of a collection'(){


        when:
        restClient.encoder.'application/geo+json' = restClient.encoder."application/json"


        def response = restClient.put(
                path:  testPath + '/collections/' + testCollection + "/items/" + testFeatureId2,
                body : "{\"type\": \"Feature\",\"geometry\": {\"type\": \"Point\"},\"properties\": {}}",
                requestContentType : "application/geo+json"
        )

        then:
        response.status == 204

        and:
        response.responseData == null
    }

}
