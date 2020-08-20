/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles

import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class StylesRepresentationRESTApiSpec extends Specification {

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/rest/services/daraa"
    static final String SUT_STYLE = "topographic"

    RESTClient restClient = new RESTClient(SUT_URL)

    def 'Get Request for one Style Representation/Map'(){

        when:

        def response = restClient.request(SUT_URL, Method.GET, "text/html",{ req ->
            uri.path = SUT_PATH + '/maps/' +SUT_STYLE
            headers.Accept = 'text/html'
        })

        then:
        response.status == 200

    }
}
