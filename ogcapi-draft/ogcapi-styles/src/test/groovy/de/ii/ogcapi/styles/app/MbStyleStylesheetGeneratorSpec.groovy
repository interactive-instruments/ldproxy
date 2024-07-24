/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app

import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2
import de.ii.ogcapi.foundation.domain.OgcApiDataV2
import de.ii.xtraplatform.entities.domain.EntityData
import de.ii.xtraplatform.entities.domain.EntityDataStore
import spock.lang.PendingFeature
import spock.lang.Shared
import spock.lang.Specification

class MbStyleStylesheetGeneratorSpec extends Specification {

    @Shared
    EntityDataStore<EntityData> entityDataStore = Stub(EntityDataStore<EntityData>) {
        forType(OgcApiDataV2.class) >> Stub(EntityDataStore<OgcApiDataV2>) {
            has("api") >> true
            get("api") >> new ImmutableOgcApiDataV2.Builder().id("api")
                    .putCollections("collection1", new ImmutableFeatureTypeConfigurationOgcApi.Builder().id("collection1").label("collection1"))
                    .putCollections("collection2", new ImmutableFeatureTypeConfigurationOgcApi.Builder().id("collection2").label("collection2"))
                    .build()
        }
    }
    @Shared
    MbStyleStylesheetGenerator generator = new MbStyleStylesheetGenerator(entityDataStore)

    @PendingFeature
    def 'analyze: should return collections for api name'() {

        given:

        when:

        def result = generator.analyze("api")

        then:

        result == ["collection1", "collection2"]

    }

    @PendingFeature
    def 'generate: should return style with layers for each collection'() {

        given:

        when:

        def result = generator.generate("api", ["collection1", "collection2"])

        then:

        result.layers.any { it.id == "collection1" }
        result.layers.any { it.id == "collection2" }

    }

}
