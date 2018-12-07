package de.ii.ldproxy.wfs3.styles.manager

import groovyx.net.http.RESTClient
import spock.lang.Specification

class StylesManagerRESTApiSpec extends Specification {

    static String testURL = "http://localhost:7080"
    static String testPath = "/rest/services/daraa"
    static String testCollection = "aeronauticcrv"
    static String testStyle = "daraa"


    RESTClient restClient = new RESTClient(testURL)




}
