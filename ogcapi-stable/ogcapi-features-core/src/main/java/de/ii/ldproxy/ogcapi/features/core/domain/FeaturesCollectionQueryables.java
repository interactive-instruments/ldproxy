/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeaturesCollectionQueryables.Builder.class)
public interface FeaturesCollectionQueryables {

    List<String> getSpatial();

    List<String> getTemporal();

    List<String> getOther();

    @Value.Derived
    default List<String> getAll() {
        return ImmutableList.<String>builder()
                .addAll(getSpatial())
                .addAll(getTemporal())
                .addAll(getOther())
                .build();
    }
}
