/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableMvtFeature.Builder.class)
public abstract class MvtFeature implements Comparable {
    public abstract Geometry getGeometry();

    public abstract Map<String, Object> getProperties();

    public abstract Long getId();

    @Override
    public int compareTo(Object o) {
        return o instanceof MvtFeature ?
                ((MvtFeature) o).getId().compareTo(getId()) :
                0;
    }
}
