/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain.metadata;

import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.time.Instant;
import java.util.Optional;

public interface CollectionDynamicMetadataRegistry {

    // This has to be in Common, not Collections, because we also include the extent information on the landing page

    /**
     * set a metadata value for a collection
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @param type the metadata element, e.g. spatial extent, temporal extent, last modified timestamp
     * @param value the new metadata value
     */
    void put(String apiId, String collectionId, MetadataType type, CollectionMetadataEntry value);

    /**
     * update a metadata value for a collection; merge the new value with the existing value
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @param type the metadata element, e.g. spatial extent, temporal extent, last modified timestamp
     * @param delta the metadata value for the new data
     * @return {@code true}, if there was a value before
     */
    boolean update(String apiId, String collectionId, MetadataType type, CollectionMetadataEntry delta);

    /**
     * remove a metadata value for a collection
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @param type the metadata element, e.g. spatial extent, temporal extent, last modified timestamp
     * @return {@code true}, if there was a value to remove
     */
    boolean remove(String apiId, String collectionId, MetadataType type);

    /**
     * get a metadata value for a collection
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @param type the metadata element, e.g. spatial extent, temporal extent, last modified timestamp
     * @return the value, if it exists
     */
    Optional<CollectionMetadataEntry> get(String apiId, String collectionId, MetadataType type);

    /**
     * Determine spatial extent of all collections in the dataset.
     *
     * @param apiId the ID of the API
     * @return the bounding box in the default CRS
     */
     Optional<BoundingBox> getSpatialExtent(String apiId);

    /**
     * Determine spatial extent of all collections in the dataset in another CRS.
     *
     * @param apiId the ID of the API
     * @param targetCrs             the target CRS
     * @return the bounding box
     */
    Optional<BoundingBox> getSpatialExtent(String apiId, EpsgCrs targetCrs) throws CrsTransformationException;

    /**
     * Determine spatial extent of a collection in the dataset.
     *
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @return the bounding box in the default CRS
     */
    Optional<BoundingBox> getSpatialExtent(String apiId, String collectionId);

    /**
     * Determine spatial extent of a collection in the dataset in another CRS.
     *
     * @param apiId the ID of the API
     * @param collectionId          the name of the feature type
     * @param targetCrs             the target CRS
     * @return the bounding box in the target CRS
     */
    Optional<BoundingBox> getSpatialExtent(String apiId, String collectionId, EpsgCrs targetCrs) throws CrsTransformationException;

    /**
     * Determine temporal extent of all collections in the dataset.
     *
     * @param apiId the ID of the API
     * @return the temporal extent in the Gregorian calendar
     */
    Optional<TemporalExtent> getTemporalExtent(String apiId);

    /**
     * Determine temporal extent of a collection in the dataset.
     *
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @return the temporal extent in the Gregorian calendar
     */
    Optional<TemporalExtent> getTemporalExtent(String apiId, String collectionId);

    /**
     * Determine timestamp of the last modification of all collections in the dataset.
     *
     * @param apiId the ID of the API
     * @return the timestamp
     */
    Optional<Instant> getLastModified(String apiId);

    /**
     * Determine timestamp of the last modification in a collection.
     *
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @return the timestamp
     */
    Optional<Instant> getLastModified(String apiId, String collectionId);

    /**
     * Determine the number of items in all collections in the dataset.
     *
     * @param apiId the ID of the API
     * @return the number of items
     */
    Optional<Long> getItemCount(String apiId);

    /**
     * Determine the number of items in a collection.
     *
     * @param apiId the ID of the API
     * @param collectionId the name of the feature type
     * @return the number of items
     */
    Optional<Long> getItemCount(String apiId, String collectionId);

    // convenience methods

    default Optional<BoundingBox> getSpatialExtent(String apiId, Optional<String> collectionId) {
        return collectionId.isPresent()
                ? getSpatialExtent(apiId, collectionId.get())
                : getSpatialExtent(apiId);
    }
    default Optional<TemporalExtent> getTemporalExtent(String apiId, Optional<String> collectionId) {
        return collectionId.isPresent()
                ? getTemporalExtent(apiId, collectionId.get())
                : getTemporalExtent(apiId);
    }
    default Optional<Instant> getLastModified(String apiId, Optional<String> collectionId) {
        return collectionId.isPresent()
                ? getLastModified(apiId, collectionId.get())
                : getLastModified(apiId);
    }
    default Optional<Long> getItemCount(String apiId, Optional<String> collectionId) {
        return collectionId.isPresent()
                ? getItemCount(apiId, collectionId.get())
                : getItemCount(apiId);
    }
}
