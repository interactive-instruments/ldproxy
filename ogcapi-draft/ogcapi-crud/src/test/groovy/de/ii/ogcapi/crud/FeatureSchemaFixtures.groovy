/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud


import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.features.domain.SchemaBase.Type

class FeatureSchemaFixtures {

    static FeatureSchema INSTANT = new ImmutableFeatureSchema.Builder()
            .name("instant")
            .sourcePath("/instant")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.INTEGER)
                    .role(SchemaBase.Role.ID))
            .putProperties2("foo", new ImmutableFeatureSchema.Builder()
                    .type(Type.OBJECT)
                    .putProperties2("instant", new ImmutableFeatureSchema.Builder()
                            .sourcePath("instant")
                            .type(Type.DATETIME)
                            .role(SchemaBase.Role.PRIMARY_INSTANT)))
            .putProperties2("bar", new ImmutableFeatureSchema.Builder()
                    .type(Type.FEATURE_REF)
                    .sourcePath("fk"))
            .build()

    static FeatureSchema INTERVAL = new ImmutableFeatureSchema.Builder()
            .name("interval")
            .sourcePath("/interval")
            .type(Type.OBJECT)
            .putProperties2("id", new ImmutableFeatureSchema.Builder()
                    .sourcePath("id")
                    .type(Type.INTEGER)
                    .role(SchemaBase.Role.ID))
            .putProperties2("foo", new ImmutableFeatureSchema.Builder()
                    .type(Type.OBJECT)
                    .putProperties2("begin", new ImmutableFeatureSchema.Builder()
                            .sourcePath("begin")
                            .type(Type.DATETIME)
                            .role(SchemaBase.Role.PRIMARY_INTERVAL_START))
                    .putProperties2("end", new ImmutableFeatureSchema.Builder()
                            .sourcePath("end")
                            .type(Type.DATETIME)
                            .role(SchemaBase.Role.PRIMARY_INTERVAL_END)))
            .putProperties2("bar", new ImmutableFeatureSchema.Builder()
                    .type(Type.FEATURE_REF)
                    .sourcePath("fk"))
            .build()

}
