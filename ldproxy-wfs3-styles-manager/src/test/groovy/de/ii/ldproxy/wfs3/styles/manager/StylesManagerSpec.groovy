package de.ii.ldproxy.wfs3.styles.manager

import spock.lang.Specification

class StylesManagerSpec extends Specification{


    def'validate no request body'(){

        given: 'request body is null'

        def requestBody=null

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBody)

        then:'it should return false'

        !result

    }

    def'validate empty request body'(){

        given: 'request body is empty'

        def requestBody=""

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBody)

        then:'it should return true'

        result


    }

    def'validate request body with invalid json syntax '(){

        given: 'request body with invalid json'

        def requestBody="{\"id\": \"default}"

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBody)

        then:'it should return false'

        !result

    }

    def'validate request body with invalid content '(){
        //TODO if content is clear
    }

    def'validate request body with valid syntax and content '(){

        //TODO if content is clear

        given: 'request body with invalid json'

        def requestBody="{\"id\": \"default\"}"

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBody)

        then:'it should return true'

        result


    }


}
