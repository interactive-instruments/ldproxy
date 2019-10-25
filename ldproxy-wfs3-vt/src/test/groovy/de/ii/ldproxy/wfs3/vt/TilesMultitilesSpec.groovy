/*
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt

import de.ii.xtraplatform.crs.api.BoundingBox
import de.ii.xtraplatform.crs.api.CoordinateTuple
import de.ii.xtraplatform.crs.api.CrsTransformationException
import de.ii.xtraplatform.crs.api.CrsTransformer
import de.ii.xtraplatform.crs.api.EpsgCrs
import spock.lang.Specification

import javax.ws.rs.NotFoundException

class TilesMultitilesSpec extends Specification {

    MultitilesGenerator multitiles = new MultitilesGenerator()
    CollectionsMultitilesGenerator collectionsMultitiles = new CollectionsMultitilesGenerator();
    CrsTransformer transformer = createCrsTransformer()

    def "Test bbox parameter parsing"() {
        when:
        def bbox = multitiles.parseBbox(bboxString)
        then:
        bbox == expectedResult

        where:
        bboxString                  | expectedResult
        "50.0, 140.0, 110.24, 175"  | [50.0, 140.0, 110.24, 175.0] as double[]
        null                        | [-179.9999, -85.05, 180.0, 85.05] as double[]
    }

    def "Incorrect values of bbox parameter"() {
        when:
        def bbox = multitiles.parseBbox("50.0, 140.0, 110.24")

        then:
        thrown(NotFoundException)

        where:
        bboxString                              | _
        "50.0, 140.0, 110.24"                   | _
        "50.0, 140.0, 110.24, 160.25, -10.55"   | _

    }

    def "Test scaleDenominator parameter parsing"() {
        when:
        def tileMatrices = multitiles.parseScaleDenominator(scaleDenominator)

        then:
        tileMatrices == expectedResult

        where:
        scaleDenominator    | expectedResult
        "0, 0.5"            | [0]
        "0.5, 4.5"          | [1,2,3,4]
        "3, 10"             | [3,4,5,6,7,8,9]
        null                | [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23]
    }

    def "Incorrect of out-of-range values of scaleDenominator parameter"() {
        when:
        def tileMatrices = multitiles.parseScaleDenominator(scaleDenominator)

        then:
        thrown(NotFoundException)

        where:
        scaleDenominator    | _
        "0, 0"              | _
        "0, 35.5"           | _
        "-35.5, 0"          | _
        "0.1,2.2,4.4"       | _
        "5.5"               | _
    }

    def "Conversion of longitude/latitude coordinates to tile coordinates for different tile matrix(level) values"() {
        when:
        def tile = multitiles.pointToTile(lon, lat, tileMatrix, transformer)

        then:
        tile == expectedResult

        where:
        lon           | lat           | tileMatrix    | expectedResult
        -0.222        | 51.52         | 1             | [0, 0]
        -0.222        | 51.52         | 3             | [3, 2]
        151.22        | -33.95        | 10            | [942, 614]
        -72.986584    | -51.039667    | 12            | [1217, 2725]
        151.215256    | -33.856159    | 18            | [241183, 157299]
    }



    def createCrsTransformer() {
        return new CrsTransformer() {
            @Override
            EpsgCrs getSourceCrs() {
                return null
            }

            @Override
            EpsgCrs getTargetCrs() {
                return null
            }

            @Override
            boolean isTargetMetric() {
                return false
            }

            @Override
            CoordinateTuple transform(double lat, double lon) {
                double originShift = 2 * Math.PI * 6378137 / 2.0;
                double mx = lon * originShift / 180.0;
                double my = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
                my = my * originShift / 180.0;
                return new CoordinateTuple(mx, my)
            }

            @Override
            CoordinateTuple transform(CoordinateTuple coordinateTuple, boolean swap) {
                return null
            }

            @Override
            double[] transform(double[] coordinates, int numberOfPoints, boolean swap) {
                return new double[0]
            }

            @Override
            BoundingBox transformBoundingBox(BoundingBox boundingBox) throws CrsTransformationException {
                return null
            }

            @Override
            double getSourceUnitEquivalentInMeters() {
                return 0
            }

            @Override
            double getTargetUnitEquivalentInMeters() {
                return 0
            }

            @Override
            boolean needsCoordinateSwap() {
                return false
            }
        }
    }

}
