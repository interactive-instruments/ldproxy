/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles

import spock.lang.Specification

import javax.ws.rs.NotFoundException

class Wfs3EndpointTilesSingleCollectionSpec extends Specification{

    def 'Check tiles support in collection with no enabled map'(){

        given: "enabledMap is not initialized "

        def collectionId = "collection"
        def enabledMap = null

        when: "checkTilesParameterCollection is called"

        Wfs3EndpointTilesSingleCollection.checkTilesParameterCollection(enabledMap,collectionId)

        then: thrown NotFoundException
    }

    def 'Check tiles support in collection with unknown collection id'(){

        given: "collectionId is unknown for the formatsMap"

        def collectionId = "unknown"
        def enabledMap = new HashMap<String,Boolean>()

        enabledMap.put("collection", true)

        when: "checkTilesParameterCollection is called"

        Wfs3EndpointTilesSingleCollection.checkTilesParameterCollection(enabledMap,collectionId)

        then: thrown NotFoundException
    }

    def 'Check tiles support enabled in collection'(){

        given: "collectionId and enabled map with an entry with this id"

        def collectionId = "collection"
        def enabledMap = new HashMap<String,Boolean>()

        enabledMap.put("collection", true)

        when: "checkTilesParameterCollection is called"

        def result = Wfs3EndpointTilesSingleCollection.checkTilesParameterCollection(enabledMap,collectionId)

        then: "it should return true"

        result
    }

    def 'Check tiles support disabled in collection'(){

        given: "collectionId and enabled map with an entry with this id"

        def collectionId = "collection"
        def enabledMap = new HashMap<String,Boolean>()

        enabledMap.put("collection", false)

        when: "checkTilesParameterCollection is called"

        Wfs3EndpointTilesSingleCollection.checkTilesParameterCollection(enabledMap,collectionId)

        then: thrown NotFoundException
    }
}
