package de.ii.ldproxy.wfs3.vt

import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.crs.api.EpsgCrs
import spock.lang.Specification


class VectorTileSeedingSpec extends Specification{


    def 'Compute the minimum and maximum rows and cols for zoom Level zero'() {

        given: "a zoom Level of 0 and a spatial extent of the following Tile: zoom level 8, row 103, col 153"

        def zoomLevel = 0
        def tilingScheme = new DefaultTilingScheme()

        def crsTransformation = null

        //coordinates from http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ in EPSG: 3857
        def xMin = 3913575.8482010253
        def yMin = 3757032.814272985
        def xMax = 4070118.8821290657
        def yMax = 3913575.8482010253

        def targetCrs = new EpsgCrs(3857)


        when: "computeMinMax is called"

        def result = VectorTileSeeding.computeMinMax(zoomLevel,tilingScheme, crsTransformation,xMin,xMax,yMin,yMax,targetCrs)

        then: 'it should return a map with min/max row/col with values of 0'

        result == new HashMap(ImmutableMap.of("rowMax",0,"rowMin",0,"colMax",0, "colMin",0))

    }
    def 'Compute the minimum and maximum rows and cols for no specified spatial extent'(){

        given: "a zoom Level of 10 and a spatial extent with default values"

        def zoomLevel = 10
        def tilingScheme = new DefaultTilingScheme()

        def crsTransformation = null

        //coordinates in EPSG: 3857
        def xMin = -20037508.342789244
        def yMin = -20037508.342789244
        def xMax = 20037508.342789244
        def yMax = 20037508.342789244

        def targetCrs = new EpsgCrs(3857)


        when: "computeMinMax is called"

        def result = VectorTileSeeding.computeMinMax(zoomLevel,tilingScheme, crsTransformation,xMin,xMax,yMin,yMax,targetCrs)

        then: 'it should return a map with and the minimum and maximum values for the row and col for that specific zoom level'

        result == new HashMap(ImmutableMap.of("rowMax",1023,"rowMin",0,"colMax",1023, "colMin",0))

    }

    def 'Compute the minimum and maximum rows and cols for non-zero zoomLevel and specified spatial extent'(){


        given: "a zoom Level of 12 and a spatial extent of the following Tile: zoom level 8, row 103, col 153"

        def zoomLevel = 12
        def tilingScheme = new DefaultTilingScheme()

        def crsTransformation = null

        //coordinates from http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ in EPSG: 3857
        def xMin = 3913575.8482010253
        def yMin = 3757032.814272985
        def xMax = 4070118.8821290657
        def yMax = 3913575.8482010253

        def targetCrs = new EpsgCrs(3857)


        when: "computeMinMax is called"

        def minMaxMap = VectorTileSeeding.computeMinMax(zoomLevel,tilingScheme, crsTransformation,xMin,xMax,yMin,yMax,targetCrs)

        then: 'it should return a map with min/max row/col values'

        minMaxMap == new HashMap(ImmutableMap.of("rowMax",1663,"rowMin",1647,"colMax",2464, "colMin",2448))
    }
}
