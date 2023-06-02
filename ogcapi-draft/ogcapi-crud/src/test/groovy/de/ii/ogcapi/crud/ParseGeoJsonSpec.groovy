/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.ii.ogcapi.crud.app.CommandHandlerCrudImpl
import de.ii.ogcapi.crud.app.GeoJsonHelper
import de.ii.xtraplatform.crs.domain.BoundingBox
import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import org.threeten.extra.Interval
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant

class ParseGeoJsonSpec extends Specification {

    @Shared
    ObjectMapper objectMapper = new ObjectMapper()

    def 'instant'() {

        given:
        JsonNode feature = objectMapper.readTree(new File('src/test/resources/instant.geo.json'))

        when:
        Optional<Interval> result1 = GeoJsonHelper.getTemporalExtent(feature, Optional.of(FeatureSchemaFixtures.INSTANT), Optional.empty())
        Optional<BoundingBox> result2 = GeoJsonHelper.getSpatialExtent(feature, OgcCrs.CRS84)

        then:
        result1 == Optional.of(Interval.of(Instant.parse('2019-02-05T07:00:00Z'), Instant.parse('2019-02-05T07:00:00Z')))
        result2 == Optional.of(BoundingBox.of(7.2789399, 50.7772485, 7.2789399, 50.7772485, OgcCrs.CRS84))

    }

    def 'instant, flat'() {

        given:
        JsonNode feature = objectMapper.readTree(new File('src/test/resources/instant_flat.geo.json'))

        when:
        Optional<Interval> result1 = GeoJsonHelper.getTemporalExtent(feature, Optional.of(FeatureSchemaFixtures.INSTANT), Optional.of("."))
        Optional<BoundingBox> result2 = GeoJsonHelper.getSpatialExtent(feature, OgcCrs.CRS84)

        then:
        result1 == Optional.of(Interval.of(Instant.parse('2019-02-05T07:00:00Z'), Instant.parse('2019-02-05T07:00:00Z')))
        result2 == Optional.of(BoundingBox.of(7.2789399, 50.7772485, 7.2789399, 50.7772485, OgcCrs.CRS84))

    }

    def 'interval'() {

        given:
        JsonNode feature = objectMapper.readTree(new File('src/test/resources/interval.geo.json'))

        when:
        Optional<Interval> result1 = GeoJsonHelper.getTemporalExtent(feature, Optional.of(FeatureSchemaFixtures.INTERVAL), Optional.empty())
        Optional<BoundingBox> result2 = GeoJsonHelper.getSpatialExtent(feature, EpsgCrs.of(25832))

        then:
        result1 == Optional.of(Interval.of(Instant.parse('2019-02-01T00:00:00Z'), Instant.parse('2019-02-01T23:59:59Z')))
        result2 == Optional.of(BoundingBox.of(358055.17, 5530962.0, 359308.81, 5531618.35, EpsgCrs.of(25832)))

    }

    def 'interval, flat'() {

        given:
        JsonNode feature = objectMapper.readTree(new File('src/test/resources/interval_flat.geo.json'))

        when:
        Optional<Interval> result1 = GeoJsonHelper.getTemporalExtent(feature, Optional.of(FeatureSchemaFixtures.INTERVAL), Optional.of("_"))
        Optional<BoundingBox> result2 = GeoJsonHelper.getSpatialExtent(feature, EpsgCrs.of(25832))

        then:
        result1 == Optional.of(Interval.of(Instant.parse('2019-02-01T00:00:00Z'), Instant.parse('2019-02-01T23:59:59Z')))
        result2 == Optional.of(BoundingBox.of(358055.17, 5530962.0, 359308.81, 5531618.35, EpsgCrs.of(25832)))

    }
}
