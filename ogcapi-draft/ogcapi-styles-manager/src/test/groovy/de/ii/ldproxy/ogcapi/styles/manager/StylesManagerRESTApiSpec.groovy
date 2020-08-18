/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.manager

import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class StylesManagerRESTApiSpec extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/rest/services/daraa"
    static final String SUT_COLLECTION = "aeronauticcrv"
    static final String SUT_STYLE = "default"


    RESTClient restClient = new RESTClient(SUT_URL)

    def 'PUT Request for a style of the dataset'(){

        when:
        def response=restClient.request(SUT_URL, Method.PUT, ContentType.JSON,{ req ->
            uri.path = SUT_PATH + '/styles/'+ SUT_STYLE
            headers.Accept = 'application/json'
            body="{\"id\": \"default\"}"
        })


        then:
        response.status == 204

        and:
        response.responseData == null
    }

}
