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
import de.ii.xtraplatform.crs.domain.ImmutableBoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.immutables.value.Value;

import javax.annotation.Nonnull;
import java.util.Objects;

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
    default CollectionMetadataEntry updateWith(CollectionMetadataEntry delta) {
        if (Objects.isNull(delta))
            return this;
        if (!(delta instanceof CollectionMetadataExtentSpatial))
            throw new IllegalStateException(String.format("Instance of CollectionMetadataEntry has invalid value. Expected 'CollectionMetadataExtentSpatial', found '%s'", delta.getClass().getSimpleName()));
;
        return CollectionMetadataExtentSpatial.of(union(this.getValue(), (BoundingBox) delta.getValue()));
    }

    private static BoundingBox union(@Nonnull BoundingBox bbox1, @Nonnull BoundingBox bbox2) {
        if (bbox1.getEpsgCrs().getCode() != bbox2.getEpsgCrs().getCode() || bbox1.getEpsgCrs().getForceAxisOrder() != bbox2.getEpsgCrs().getForceAxisOrder())
        throw new IllegalStateException("Both bounding boxes must have the same CRS.");

        return new ImmutableBoundingBox.Builder().xmin(Math.min(bbox1.getXmin(), bbox2.getXmin()))
                                                 .ymin(Math.min(bbox1.getYmin(), bbox2.getYmin()))
                                                 .xmax(Math.max(bbox1.getXmax(), bbox2.getXmax()))
                                                 .ymax(Math.max(bbox1.getYmax(), bbox2.getYmax()))
                                                 .epsgCrs(bbox1.getEpsgCrs())
                                                 .build();
    }
}
