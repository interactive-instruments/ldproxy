/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.application.I18n
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension
import de.ii.xtraplatform.crs.api.CrsTransformation
import spock.lang.Specification

import javax.ws.rs.NotAcceptableException
import javax.ws.rs.NotFoundException

class VectorTileSpec extends Specification{

    def 'Check format with no formats map'(){

        given: "formatsMap is not initialized "

        def collectionId = "collection"
        def formatsMap = null
        def mediaTypeToCheck = "application/json"
        def dataset = false

        when: "checkFormat is called"

        VectorTile.checkFormat(formatsMap,collectionId,mediaTypeToCheck,dataset)

        then: thrown NotFoundException

    }

    def 'Check format with unknown collection id'(){

        given: "collectionId is not in the known for the formatsMap"

        def collectionId = "unknown"
        def formatsMap = new HashMap<String,List<String>>()
        def mediaTypeToCheck = "application/json"
        def dataset = false

        formatsMap.put("collection", ImmutableList.of("application/json"))

        when: "checkFormat is called"

        VectorTile.checkFormat(formatsMap,collectionId,mediaTypeToCheck,dataset)

        then: thrown NotFoundException
    }

    def 'Check format in collection successfully'(){

        given: "collectionId and formats map with an entry with this id"

        def collectionId = "collection"
        def formatsMap = new HashMap<String,List<String>>()
        def mediaTypeToCheck = "application/json"
        def dataset = false

        formatsMap.put("collection", ImmutableList.of("application/json"))

        when: "checkFormat is called"

        def result=VectorTile.checkFormat(formatsMap,collectionId,mediaTypeToCheck,dataset)

        then: "it should return true"

        result


    }

    def 'Check format in collection not successfully'(){

        given: "collectionId and formats map with an entry with this id"

        def collectionId = "collection"
        def formatsMap = new HashMap<String,List<String>>()
        def mediaTypeToCheck = "application/vnd.mapbox-vector-tile"
        def dataset = false

        formatsMap.put("collection", ImmutableList.of("application/json"))

        when: "checkFormat is called"

        VectorTile.checkFormat(formatsMap,collectionId,mediaTypeToCheck,dataset)

        then: thrown NotAcceptableException
    }

    def 'Check format in dataset not successfully'(){

        given: "collectionId and formats map with an entry with this id"

        def collectionId = "collection"
        def formatsMap = new HashMap<String,List<String>>()
        def mediaTypeToCheck = "application/vnd.mapbox-vector-tile"
        def dataset = true

        formatsMap.put("collection", ImmutableList.of("application/json"))

        when: "checkFormat is called"

        def result = VectorTile.checkFormat(formatsMap,collectionId,mediaTypeToCheck,dataset)

        then: "it should return false"

        !result
    }

    def 'check zoom level in collection - zoom levels map is null'(){

        given: 'a zoom levels map which is not initialized, a zoom level of 10, a collection Id \'collection\' and a tiling scheme id \'default\''

        def zoomLevelsMap = null
        def zoomLevel = 10
        def collectionId='collection'
        def tilingSchemeId= 'WebMercatorQuad'
        def i18n = new I18n();

        when: "checkZoomLevel is called"

        def result=VectorTile.checkZoomLevel(zoomLevel,zoomLevelsMap,Mock(OgcApiDataset),Mock(OgcApiFeatureFormatExtension),collectionId,tilingSchemeId,"application/json","1024","512",false, Mock(VectorTilesCache),true,Mock(OgcApiRequestContext),Mock(CrsTransformation),i18n)

        then: "it should return an empty map"

        result == new HashMap<String,String>()

    }

    def 'check zoom level in collection - no collections with tiles extension'(){

        given: 'an empty zoom levels map, a zoom level of 10, a collection Id \'collection\' and a tiling scheme id \'default\''

        def zoomLevelsMap = new HashMap()
        def zoomLevel = 10
        def collectionId='collection'
        def tilingSchemeId= 'WebMercatorQuad'
        def i18n = new I18n();

        when: "checkZoomLevel is called"

        def result=VectorTile.checkZoomLevel(zoomLevel,zoomLevelsMap,Mock(OgcApiDataset),Mock(OgcApiFeatureFormatExtension),collectionId,tilingSchemeId,"application/json","1024","512",false, Mock(VectorTilesCache),true,Mock(OgcApiRequestContext),Mock(CrsTransformation),i18n)

        then: "it should return an empty map"

        result == new HashMap<String,String>()

    }


    def 'check zoom level in collection - no zoom level specified in configuration'(){
        given: 'a zoom level of 10, a collection Id \'collection1\', a tiling scheme id \'default\' and a map with a collection and zoom level entries,' +
                ' that were not specified in the configuration '

        def zoomLevel = 10
        def collectionId='collection1'
        def tilingSchemeId= 'WebMercatorQuad'
        def zoomLevelsMap =  new HashMap()
        def i18n = new I18n();

        zoomLevelsMap.put("collection0",null)
        zoomLevelsMap.put("collection1",null)
        zoomLevelsMap.put("collection2",null)
        zoomLevelsMap.put("collection3",null)

        when: "checkZoomLevel is called"

        def result=VectorTile.checkZoomLevel(zoomLevel,zoomLevelsMap,Mock(OgcApiDataset),Mock(OgcApiFeatureFormatExtension),collectionId,tilingSchemeId,"application/json","1024","512",false, Mock(VectorTilesCache),true,Mock(OgcApiRequestContext),Mock(CrsTransformation),i18n)

        then: "it should return a map with the max and min zoom level of the tiling scheme from the requested collection and an entry with the key \'collection1\' and the value \'true\'"

        result == new HashMap(ImmutableMap.of("max","24","min","0","collection1","true"))

    }




    def 'check zoom level in collection - zoom level specified and in range'(){
        given: 'a zoom level of 12, a collection Id \'collection2\', a tiling scheme id \'default\'and a map with collctions and specified zoom level entries'

        def zoomLevel = 12
        def collectionId='collection2'
        def tilingSchemeId= 'WebMercatorQuad'
        def zoomLevelsMap =  new HashMap(ImmutableMap.of(
                "collection0", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(15).min(4).build()),
                "collection1", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(21).min(3).build()),
                "collection2", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(18).min(6).build()),
                "collection3", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(24).min(13).build())))
        def i18n = new I18n();


        when: "checkZoomLevel is called"

        def result=VectorTile.checkZoomLevel(zoomLevel,zoomLevelsMap,Mock(OgcApiDataset),Mock(OgcApiFeatureFormatExtension),collectionId,tilingSchemeId,"application/json","1024","512",false, Mock(VectorTilesCache),true,Mock(OgcApiRequestContext),Mock(CrsTransformation),i18n)

        then: "it should return a map with the max and min zoom level of the requested collection and an entry with the key \'collection2\' and the value \'true\'"

        result == new HashMap(ImmutableMap.of("max","18","min","6","collection2","true"))


    }

    def 'check zoom level in collection - invalid zoom level range in configuration'(){

        given: 'a zoom level of 10, a collection id \'collection\', a tiling scheme id \'default \' and ' +
               'a map with one entry of the collection id and an invalid zoom level range for the tiling scheme'

        def zoomLevel = 10
        def collectionId='collection'
        def tilingSchemeId= 'WebMercatorQuad'
        def zoomLevelsMap =  new HashMap<>(ImmutableMap.of(
                "collection", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(25).min(0).build())))
        def i18n = new I18n();

        when: "checkZoomLevel is called"

        VectorTile.checkZoomLevel(zoomLevel,zoomLevelsMap,Mock(OgcApiDataset),Mock(OgcApiFeatureFormatExtension),collectionId,tilingSchemeId,"application/json","1024","512",false, Mock(VectorTilesCache),true,Mock(OgcApiRequestContext),Mock(CrsTransformation),i18n)

        then: thrown NotFoundException

    }



    def 'check zoom level in collection - zoom level specified and not in range'(){

        given: 'a zoom level of 12, a collection Id \'collection3\', a tiling scheme id \'default\'and a map with collctions and specified zoom level entries'

        def zoomLevel = 12
        def collectionId='collection3'
        def tilingSchemeId= 'WebMercatorQuad'
        def zoomLevelsMap =  new HashMap(ImmutableMap.of(
                "collection0", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(15).min(4).build()),
                "collection1", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(21).min(3).build()),
                "collection2", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(18).min(6).build()),
                "collection3", ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder().max(24).min(13).build())))
        def i18n = new I18n();

        when: "checkZoomLevel is called"

        def result=VectorTile.checkZoomLevel(zoomLevel,zoomLevelsMap,Mock(OgcApiDataset),Mock(OgcApiFeatureFormatExtension),collectionId,tilingSchemeId,"application/json","1024","512",false, Mock(VectorTilesCache),true,Mock(OgcApiRequestContext),Mock(CrsTransformation),i18n)

        then: "it should return a map with the max and min zoom level of the requested collection and an entry with the key \'collection3\' and the value \'false\'"

        result == new HashMap(ImmutableMap.of("max","24","min","13","collection3","false"))

    }
}
