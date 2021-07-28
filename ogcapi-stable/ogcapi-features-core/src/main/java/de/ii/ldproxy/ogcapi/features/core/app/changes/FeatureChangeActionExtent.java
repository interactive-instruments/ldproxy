/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app.changes;

import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentSpatial;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionMetadataExtentTemporal;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.ChangeContext;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.common.domain.metadata.MetadataType;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class FeatureChangeActionExtent implements FeatureChangeAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangeActionExtent.class);
    private final CollectionDynamicMetadataRegistry metadataRegistry;

    public FeatureChangeActionExtent(@Requires CollectionDynamicMetadataRegistry collectionDynamicMetadataRegistry) {
        this.metadataRegistry = collectionDynamicMetadataRegistry;
    }

    @Override
    public FeatureChangeActionExtent create() {
        return new FeatureChangeActionExtent(metadataRegistry);
    }

    @Override
    public int getSortPriority() {
        return 10;
    }

    // TODO for SQL feature providers there is also a cached extent in xtraplatform;
    //      recomputing the extent for every change will often be too expensive;
    //      how to consolidate?

    @Override
    public void onInsert(ChangeContext changeContext, Consumer<ChangeContext> next) {
        changeContext = onInsertOrUpdate(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    @Override
    public void onUpdate(ChangeContext changeContext, Consumer<ChangeContext> next) {
        changeContext = onInsertOrUpdate(changeContext);

        // next chain for extensions
        next.accept(changeContext);
    }

    private ChangeContext onInsertOrUpdate(ChangeContext changeContext) {
        String apiId = changeContext.getApiData().getId();
        String collectionId = changeContext.getCollectionId();

        changeContext.getBoundingBox()
                     .ifPresent(bbox -> metadataRegistry.update(apiId, collectionId, MetadataType.spatialExtent, CollectionMetadataExtentSpatial.of(bbox)));

        changeContext.getInterval()
                     .ifPresent(interval -> metadataRegistry.update(apiId, collectionId, MetadataType.temporalExtent, CollectionMetadataExtentTemporal.of(interval)));

        return changeContext;
    }

    /*
    @Override
    public void onDelete(ChangeContext changeContext, Consumer<ChangeContext> next) {
        // skip deletes of individual features as we would need to recompute the bbox
        if (changeContext.getFeatureIds().isEmpty()) {
            OgcApiDataV2 apiData = changeContext.getApiData();
            String collectionId = changeContext.getCollectionId();

            final Optional<BoundingBox> bboxNow = apiData.getSpatialExtent(collectionId);
            final Optional<TemporalExtent> intervalNow = apiData.getTemporalExtent(collectionId);

            final Optional<BoundingBox> bboxDelta = changeContext.getBoundingBox();
            final Optional<TemporalExtent> intervalDelta = changeContext.getInterval();

            // also skipped combinations of bbox and interval for the same reason
            if (bboxDelta.isEmpty() && intervalDelta.isPresent() && intervalNow.isPresent()) {
                if (overlaps(intervalDelta.get(), intervalNow.get())) {
                    Optional<TemporalExtent> intervalNew = Optional.of(difference(intervalNow.get(), intervalDelta.get()));
                    LOGGER.debug("Temporal extent of collection '{}' changed to [ {}, {} ]", collectionId, intervalNew.get().getStart(), intervalNew.get().getEnd());
                    changeContext = updateExtent(changeContext, bboxNow, intervalNew);
                } else if (contains(intervalDelta.get(), intervalNow.get())) {
                    LOGGER.debug("Temporal extent of collection '{}' changed to no extent.", collectionId);
                    changeContext = updateExtent(changeContext, Optional.empty(), Optional.empty());
                }
            } else if (bboxDelta.isPresent() && intervalDelta.isEmpty() && bboxNow.isPresent()) {
                if (overlaps(bboxDelta.get(), bboxNow.get())) {
                    Optional<BoundingBox> bboxNew = Optional.of(difference(bboxNow.get(), bboxDelta.get()));
                    LOGGER.debug("Spatial extent of collection '{}' changed to [ {}, {}, {}, {} ]", collectionId, bboxNew.get().getXmin(), bboxNew.get().getYmin(), bboxNew.get().getXmax(), bboxNew.get().getYmax());
                    changeContext = updateExtent(changeContext, bboxNew, intervalNow);
                } else if (contains(bboxDelta.get(), bboxNow.get())) {
                    LOGGER.debug("Spatial extent of collection '{}' changed to no extent.", collectionId);
                    changeContext = updateExtent(changeContext, Optional.empty(), Optional.empty());
                }
            }
        }

        // next chain for extensions
        next.accept(changeContext);
    }

    private ChangeContext updateExtent(ChangeContext changeContext, Optional<BoundingBox> bbox, Optional<TemporalExtent> interval) {
        // TODO currently this just updates the change context, but we also need to update the entity data
        return new ImmutableChangeContext.Builder()
                .from(changeContext)
                .apiData(new ImmutableOgcApiDataV2.Builder()
                                 .from(changeContext.getApiData())
                                 .collections(
                                         changeContext.getApiData()
                                         .getCollections()
                                                .entrySet()
                                                .stream()
                                                .map(entry -> {
                                                    if (!entry.getKey().equals(changeContext.getCollectionId()))
                                                        return entry;

                                                    return new AbstractMap.SimpleImmutableEntry<String, FeatureTypeConfigurationOgcApi>(entry.getKey(), new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                                            .from(entry.getValue())
                                                            .extent(new ImmutableCollectionExtent.Builder()
                                                                            .spatial(bbox)
                                                                            .temporal(interval)
                                                                            .build())
                                                            .build());
                                                })
                                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                                 .build())
                .build();

    }

    private static BoundingBox union(BoundingBox bbox1, BoundingBox bbox2) {
        if (bbox1.getEpsgCrs().getCode() != bbox2.getEpsgCrs().getCode() || bbox1.getEpsgCrs().getForceAxisOrder() != bbox2.getEpsgCrs().getForceAxisOrder())
            throw new IllegalStateException("Both bounding boxes must have the same CRS.");

        return new ImmutableBoundingBox.Builder().xmin(Math.min(bbox1.getXmin(), bbox2.getXmin()))
                                                 .ymin(Math.min(bbox1.getYmin(), bbox2.getYmin()))
                                                 .xmax(Math.max(bbox1.getXmax(), bbox2.getXmax()))
                                                 .ymax(Math.max(bbox1.getYmax(), bbox2.getYmax()))
                                                 .epsgCrs(bbox1.getEpsgCrs())
                                                 .build();
    }

    private static BoundingBox difference(BoundingBox bbox1, BoundingBox bbox2) {
        if (bbox1.getEpsgCrs().getCode() != bbox2.getEpsgCrs().getCode() || bbox1.getEpsgCrs().getForceAxisOrder() != bbox2.getEpsgCrs().getForceAxisOrder())
            throw new IllegalStateException("Both bounding boxes must have the same CRS.");

        if (bbox2.getXmin() <= bbox1.getXmin() && bbox2.getXmax() >= bbox1.getXmax()) {
            if (bbox2.getYmin() <= bbox1.getYmin() && bbox2.getYmax() >= bbox1.getYmin() && bbox2.getYmax() <= bbox1.getYmax()) {
                return new ImmutableBoundingBox.Builder().xmin(bbox1.getXmin())
                                                         .ymin(bbox2.getYmax())
                                                         .xmax(bbox1.getXmax())
                                                         .ymax(bbox1.getYmax())
                                                         .epsgCrs(bbox1.getEpsgCrs())
                                                         .build();
            } else if (bbox2.getYmin() >= bbox1.getYmin() && bbox2.getYmin() <= bbox1.getYmax() && bbox2.getYmax() >= bbox1.getYmax()) {
                return new ImmutableBoundingBox.Builder().xmin(bbox1.getXmin())
                                                         .ymin(bbox1.getYmin())
                                                         .xmax(bbox1.getXmax())
                                                         .ymax(bbox2.getYmin())
                                                         .epsgCrs(bbox1.getEpsgCrs())
                                                         .build();
            }

        }
        else if (bbox2.getYmin() <= bbox1.getYmin() && bbox2.getYmax() >= bbox1.getYmax()) {
            if (bbox2.getXmin() <= bbox1.getXmin() && bbox2.getXmax() >= bbox1.getXmin() && bbox2.getXmax() <= bbox1.getXmax()) {
                return new ImmutableBoundingBox.Builder().ymin(bbox1.getYmin())
                                                         .xmin(bbox2.getXmax())
                                                         .ymax(bbox1.getYmax())
                                                         .xmax(bbox1.getXmax())
                                                         .epsgCrs(bbox1.getEpsgCrs())
                                                         .build();
            } else if (bbox2.getXmin() >= bbox1.getXmin() && bbox2.getXmin() <= bbox1.getXmax() && bbox2.getXmax() >= bbox1.getXmax()) {
                return new ImmutableBoundingBox.Builder().ymin(bbox1.getYmin())
                                                         .xmin(bbox1.getXmin())
                                                         .ymax(bbox1.getYmax())
                                                         .xmax(bbox2.getXmin())
                                                         .epsgCrs(bbox1.getEpsgCrs())
                                                         .build();
            }
        }

        return bbox1;
    }

    private static boolean overlaps(BoundingBox bbox1, BoundingBox bbox2) {
        if (bbox1.getEpsgCrs().getCode() != bbox2.getEpsgCrs().getCode() || bbox1.getEpsgCrs().getForceAxisOrder() != bbox2.getEpsgCrs().getForceAxisOrder())
            throw new IllegalStateException("Both bounding boxes must have the same CRS.");

        return (bbox1.getXmax() >= bbox2.getXmin() && bbox2.getXmax() >= bbox1.getXmin()) &&
                (bbox1.getYmax() >= bbox2.getYmin() && bbox2.getYmax() >= bbox1.getYmin());
    }

    private static boolean contains(BoundingBox bbox1, BoundingBox bbox2) {
        if (bbox1.getEpsgCrs().getCode() != bbox2.getEpsgCrs().getCode() || bbox1.getEpsgCrs().getForceAxisOrder() != bbox2.getEpsgCrs().getForceAxisOrder())
            throw new IllegalStateException("Both bounding boxes must have the same CRS.");

        return (bbox1.getXmax() >= bbox2.getXmax() && bbox1.getXmin() <= bbox2.getXmin()) &&
                (bbox1.getYmax() >= bbox2.getYmax() && bbox1.getYmin() <= bbox2.getYmin());
    }

    private static TemporalExtent union(TemporalExtent int1, TemporalExtent int2) {
        return new ImmutableTemporalExtent.Builder().start(Objects.isNull(int1.getStart()) ? null : Objects.isNull(int2.getStart()) ? null : Math.min(int1.getStart(), int2.getStart()))
                                                    .end(Objects.isNull(int1.getEnd()) ? null : Objects.isNull(int2.getEnd()) ? null : Math.max(int1.getEnd(), int2.getEnd()))
                                                    .build();
    }

    private static TemporalExtent difference(TemporalExtent int1, TemporalExtent int2) {
        if (start(int2) <= start(int1) && end(int2) >= start(int1))
            // int1     -----------
            // int2   -------
            // result        ------
            return new ImmutableTemporalExtent.Builder().start(int2.getEnd())
                                                        .end(int1.getEnd())
                                                        .build();
        else if (start(int2) <= end(int1) && end(int2) >= end(int1))
            // int1     -----------
            // int2            -------
            // result   -------
            return new ImmutableTemporalExtent.Builder().start(int1.getStart())
                                                        .end(int2.getStart())
                                                        .build();

        return int1;
    }

    private static boolean overlaps(TemporalExtent int1, TemporalExtent int2) {
        return end(int1) >= start(int2) && end(int2) >= start(int1);
    }

    private static boolean contains(TemporalExtent int1, TemporalExtent int2) {
        return end(int1) >= end(int2) && start(int1) <= start(int2);
    }

    private static long start(TemporalExtent interval) {
        return Optional.ofNullable(interval.getStart()).orElse(Long.MIN_VALUE);
    }

    private static long end(TemporalExtent interval) {
        return Optional.ofNullable(interval.getEnd()).orElse(Long.MAX_VALUE);
    }
     */

}
