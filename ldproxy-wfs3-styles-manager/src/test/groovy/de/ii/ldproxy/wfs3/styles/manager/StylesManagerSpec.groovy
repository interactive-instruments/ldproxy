/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.manager

import spock.lang.Specification

class StylesManagerSpec extends Specification{


    def'validate no request body'(){

        given: 'request body is null'

        def requestBody=null

        when: "validateRequestBodyJSON is called"

        def result = Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        then:'it should return null'

        result==null

    }

    def'validate empty request body'(){

        given: 'request body is empty'

        def requestBody=""

        when: "validateRequestBodyJSON is called"

        def result = Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        then:'it should return null'

        result==null


    }

    def'validate request body with invalid json syntax '(){

        given: 'request body with invalid json'

        def requestBody="{\"id\": \"default}"

        when: "validateRequestBodyJSON is called"

        def result = Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        then:'it should return null'

        result==null

    }

    def'validate request body with valid json syntax '(){

        given: 'request body with invalid json'

        def requestBody="{\"id\": \"default\"}"

        when: "validateRequestBodyJSON is called"

        def result = Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        then:'it should not return null'

        result!=null

    }

    def'validate request body with no version number '(){

        given: 'request body with no version number '

        def requestBody="{\n" +
                "  \"name\": \"default\",\n" +
                "  \"sources\": {\n" +
                "    \"default\": {\n" +
                "      \"type\": \"vector\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\",\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }" +
                "  ]\n" +
                "}"

        def requestBodyJsonNode= Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return false'

        !result

    }

    def'validate request body with invalid version number '(){

        given: 'request body with a version number of 7'

        def requestBody="{\n" +
                "  \"version\": 7,\n" +
                "  \"name\": \"default\",\n" +
                "  \"sources\": {\n" +
                "    \"default\": {\n" +
                "      \"type\": \"vector\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\",\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }" +
                "  ]\n" +
                "}"



        def requestBodyJsonNode= Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return false'

        !result

    }

    def'validate request body with no sources'(){

        given: 'request body with no sources'

        def requestBody="{\n" +
                "  \"version\": 8,\n" +
                "  \"name\": \"default\",\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\",\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }" +
                "  ]\n" +
                "}"


        def requestBodyJsonNode= Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return false'

        !result

    }

    def'validate request body with no layers'(){

        given: 'request body with no layer'

        def requestBody="{\n" +
                "  \"version\": 8,\n" +
                "  \"name\": \"default\",\n" +
                "  \"sources\": {\n" +
                "    \"default\": {\n" +
                "      \"type\": \"vector\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\"\n" +
                "}"

        def requestBodyJsonNode = Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"


        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return false'

        !result

    }

    def'validate request body with layer - missing id'(){

        given: 'request body with a layer and the layer has no id'

        def requestBody="{\n" +
                "  \"version\": 8,\n" +
                "  \"name\": \"default\",\n" +
                "  \"sources\": {\n" +
                "    \"default\": {\n" +
                "      \"type\": \"vector\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\",\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }" +
                "  ]\n" +
                "}"

        def requestBodyJsonNode= Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return false'

        !result
    }

    def'validate request body with layer - type incorrect'(){
        given: 'request body with a layer and the layer has an incorrect type'

        def requestBody="{\n" +
                "  \"version\": 8,\n" +
                "  \"name\": \"default\",\n" +
                "  \"sources\": {\n" +
                "    \"default\": {\n" +
                "      \"type\": \"vector\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\",\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"type\": \"rectangle\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }" +
                "  ]\n" +
                "}"

        def requestBodyJsonNode= Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return false'

        !result
    }

    def'validate request body with layers - not unique ids'(){

        given: 'request body with two layers with the same id'

        def requestBody="{\n" +
                "  \"version\": 8,\n" +
                "  \"name\": \"default\",\n" +
                "  \"sources\": {\n" +
                "    \"default\": {\n" +
                "      \"type\": \"vector\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\",\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }," +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }" +
                "  ]\n" +
                "}"

        def requestBodyJsonNode= Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return false'

        !result
    }


    def'validate request body with valid syntax and content '(){


        given: 'request body with valid json, a version number of 8, sources  and two layers with unique ids and a correct type'

        def requestBody="{\n" +
                "  \"version\": 8,\n" +
                "  \"name\": \"default\",\n" +
                "  \"sources\": {\n" +
                "    \"default\": {\n" +
                "      \"type\": \"vector\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sprite\": \"mapbox://sprites/mapbox/bright-v8\",\n" +
                "  \"layers\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }," +
                "    {\n" +
                "      \"id\": \"2\",\n" +
                "      \"type\": \"fill\",\n" +
                "      \"source\": \"default\",\n" +
                "      \"source-layer\": \"collectionId \",\n" +
                "      \"layout\": \"string\",\n" +
                "      \"paint\": {\n" +
                "        \"fill-color\": \"#11083b\"\n" +
                "      }\n" +
                "    }" +
                "  ]\n" +
                "}"

        def requestBodyJsonNode= Wfs3EndpointStylesManager.validateRequestBodyJSON(requestBody)

        when: "validateRequestBody is called"

        def result = Wfs3EndpointStylesManager.validateRequestBody(requestBodyJsonNode)

        then:'it should return true'

        result


    }


}
