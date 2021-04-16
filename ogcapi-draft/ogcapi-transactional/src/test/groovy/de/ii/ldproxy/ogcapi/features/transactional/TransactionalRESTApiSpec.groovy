/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.transactional


import groovyx.net.http.*
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class TransactionalRESTApiSpec extends Specification{

    static String SUT_URL = System.getenv('SUT_URL')
    static String SUT_PATH = "/rest/services/daraa"
    static String SUT_COLLECTION = "aeronauticcrv"
    static String SUT_ID = "21"

    RESTClient restClient = new RESTClient(SUT_URL)

    def 'POST Request for one feature of a collection'(){


        when:
        restClient.encoder.'application/geo+json' = restClient.encoder."application/json"

        def response = restClient.post(
                path:  SUT_PATH + '/collections/' + SUT_COLLECTION + "/items/",
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
                path:  SUT_PATH + '/collections/' + SUT_COLLECTION + "/items/" + SUT_ID,
                body : "{\"type\": \"Feature\",\"geometry\": {\"type\": \"Point\"},\"properties\": {}}",
                requestContentType : "application/geo+json"
        )

        then:
        response.status == 204

        and:
        response.responseData == null
    }

}
