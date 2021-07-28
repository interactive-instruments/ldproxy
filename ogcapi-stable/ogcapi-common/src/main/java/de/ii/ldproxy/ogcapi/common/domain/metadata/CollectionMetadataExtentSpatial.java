/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain.metadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableCollectionMetadataExtentSpatial.Builder.class)
public interface CollectionMetadataExtentSpatial extends CollectionMetadataEntry {

    static CollectionMetadataExtentSpatial of(double xmin, double ymin, double xmax, double ymax) {
        return of(BoundingBox.of(xmin, ymin, xmax, ymax, OgcCrs.CRS84));
    }

    static CollectionMetadataExtentSpatial of(BoundingBox bbox) {
        return new ImmutableCollectionMetadataExtentSpatial.Builder().value(Objects.requireNonNullElse(bbox, BoundingBox.of(-180, -90, 180, 90, OgcCrs.CRS84)))
                                                                     .build();
    }

    @Override
    BoundingBox getValue();

    @Override
    default Optional<CollectionMetadataEntry> updateWith(CollectionMetadataEntry delta) {
        if (Objects.isNull(delta) || Objects.isNull(delta.getValue()))
            return Optional.empty();
        if (Objects.isNull(getValue()))
            return Optional.of(delta);
        if (!(delta instanceof CollectionMetadataExtentSpatial))
            throw new IllegalStateException(String.format("Instance of CollectionMetadataEntry has invalid value. Expected 'CollectionMetadataExtentSpatial', found '%s'", delta.getClass().getSimpleName()));
;
        BoundingBox deltaExtent = ((CollectionMetadataExtentSpatial) delta).getValue();
        if (this.getValue().getEpsgCrs().getCode() != deltaExtent.getEpsgCrs().getCode()
                || this.getValue().getEpsgCrs().getForceAxisOrder() != deltaExtent.getEpsgCrs().getForceAxisOrder())
            throw new IllegalStateException("Both bounding boxes must have the same CRS.");

        if (getValue().getXmin() <= deltaExtent.getXmin() && getValue().getYmin() <= deltaExtent.getYmin()
                && getValue().getXmax() >= deltaExtent.getXmax() && getValue().getYmax() >= deltaExtent.getYmax())
            return Optional.empty();

        return Optional.of(CollectionMetadataExtentSpatial.of(BoundingBox.of(Math.min(getValue().getXmin(), deltaExtent.getXmin()),
                                                                             Math.min(getValue().getYmin(), deltaExtent.getYmin()),
                                                                             Math.max(getValue().getXmax(), deltaExtent.getXmax()),
                                                                             Math.max(getValue().getYmax(), deltaExtent.getYmax()),
                                                                             OgcCrs.CRS84)));
    }
}
