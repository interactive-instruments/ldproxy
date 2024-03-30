/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app

import groovyx.net.http.RESTClient
import spock.lang.Requires
import spock.lang.Specification

@Requires({env['SUT_URL'] != null})
class StylesRESTApiSpec extends Specification{

    static final String SUT_URL = System.getenv('SUT_URL')
    static final String SUT_PATH = "/daraa"
    static final String SUT_COLLECTION = "aeronauticcrv"
    static final String SUT_STYLE = "daraa"


    RESTClient restClient = new RESTClient(SUT_URL)


    def 'GET Request for the styles Page of the dataset'(){

        when:
        def response = restClient.get( path: SUT_PATH + '/styles')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("styles")
        response.responseData.get("styles").get(0).get("identifier")
        response.responseData.get("styles").get(0).get("links")
    }

}
