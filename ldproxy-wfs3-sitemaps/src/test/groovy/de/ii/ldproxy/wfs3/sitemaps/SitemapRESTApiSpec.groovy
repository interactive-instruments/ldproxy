/*
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps

import groovyx.net.http.RESTClient
import spock.lang.Specification

class SitemapRESTApiSpec extends Specification{

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testCollection = "/eronauticcrv"

    RESTClient restClient = new RESTClient(testURL)

    def 'GET Request for the sitemap index Page should be possible'(){

        when:
        def response = restClient.get( path: testPath + '/sitemap_index.xml')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("sitemaps")
        response.responseData.get("sitemaps").get(0).get("loc") == testURL + testPath +"/sitemap_landingPage.xml"
        response.responseData.get("sitemaps").get(0).get("lastmod")
        response.responseData.get("sitemaps").get(0).get("changefreq")
        response.responseData.get("sitemaps").get(0).get("priority")
    }

    def 'GET Request for the sitemap landing Page should be possible'(){
        when:
        def response = restClient.get( path: testPath + '/sitemap_landingPage.xml')

        then:
        response.status == 200

        and:
        response.responseData.containsKey("sites")
        response.responseData.get("sites").get(0).get("loc") == testURL + testPath +"?f=html"
        response.responseData.get("sites").get(0).get("lastmod")
        response.responseData.get("sites").get(0).get("changefreq")
        response.responseData.get("sites").get(0).get("priority")
    }

    def 'GET Request for the sitemap collection Page should be possible'(){

        setup:
        def indexRequest = restClient.get( path: testPath + '/sitemap_index.xml')
        def collectionUrl = indexRequest.responseData.get("sitemaps").get(1).get("loc")

        when:
        def response = restClient.get( path: collectionUrl)

        then:
        response.status == 200

        and:
        response.responseData.containsKey("sites")
        response.responseData.get("sites").get(0).get("loc") == testURL + testPath +"/collections/" + testCollection + "/items?f=html&limit=10&offset=0"
        response.responseData.get("sites").get(0).get("lastmod")
        response.responseData.get("sites").get(0).get("changefreq")
        response.responseData.get("sites").get(0).get("priority")





    }


}
