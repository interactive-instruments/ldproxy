/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles

import de.ii.xtraplatform.crs.domain.BoundingBox
import de.ii.xtraplatform.geometries.domain.EpsgCrs
import spock.lang.Specification

class VectorTileSeedingSpec extends Specification{


    def 'Compute the minimum and maximum rows and cols for zoom Level zero'() {

        given: "a zoom Level of 0 and a spatial extent of the following Tile: zoom level 8, row 103, col 153"

        def zoomLevel = 0
        def tilingScheme = new WebMercatorQuad()

        def crsTransformation = null

        //coordinates from http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ in EPSG: 3857
        def xMin = 3913575.8482010253
        def yMin = 3757032.814272985
        def xMax = 4070118.8821290657
        def yMax = 3913575.8482010253

        def targetCrs = EpsgCrs.of(3857)
        def bbox = new BoundingBox(xMin, yMin, xMax, yMax, targetCrs)


        when: "getLimits is called"

        def result = tilingScheme.getLimits(zoomLevel,bbox)

        then: 'it should return a map with min/max row/col with values of 0'

        result.minTileCol == 0
        result.maxTileCol == 0
        result.minTileRow == 0
        result.maxTileRow == 0

    }
    def 'Compute the minimum and maximum rows and cols for no specified spatial extent'(){

        given: "a zoom Level of 10 and a spatial extent with default values"

        def zoomLevel = 10
        def tilingScheme = new WebMercatorQuad()

        def crsTransformation = null

        //coordinates in EPSG: 3857
        def xMin = -20037508.342789244
        def yMin = -20037508.342789244
        def xMax = 20037508.342789244
        def yMax = 20037508.342789244

        def targetCrs = EpsgCrs.of(3857)
        def bbox = new BoundingBox(xMin, yMin, xMax, yMax, targetCrs)

        when: "getLimits is called"

        def result = tilingScheme.getLimits(zoomLevel,bbox)

        then: 'it should return a map with and the minimum and maximum values for the row and col for that specific zoom level'

        result.minTileCol == 0
        result.maxTileCol == 1023
        result.minTileRow == 0
        result.maxTileRow == 1023

    }

    def 'Compute the minimum and maximum rows and cols for non-zero zoomLevel and specified spatial extent'(){


        given: "a zoom Level of 12 and a spatial extent of the following Tile: zoom level 8, row 103, col 153"

        def zoomLevel = 12
        def tilingScheme = new WebMercatorQuad()

        def crsTransformation = null

        //coordinates from http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ in EPSG: 3857
        def xMin = 3913575.8482010253
        def yMin = 3757032.814272985
        def xMax = 4070118.8821290657
        def yMax = 3913575.8482010253

        def targetCrs = EpsgCrs.of(3857)
        def bbox = new BoundingBox(xMin, yMin, xMax, yMax, targetCrs)

        when: "getLimits is called"

        def result = tilingScheme.getLimits(zoomLevel,bbox)

        then: 'it should return a map with min/max row/col values'

        result.minTileCol == 2448
        result.maxTileCol == 2464
        result.minTileRow == 1647
        result.maxTileRow == 1663

    }
}
