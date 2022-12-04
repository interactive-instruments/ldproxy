/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.services.domain.Service;
import java.time.Instant;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface OgcApi extends Service {

  @Override
  OgcApiDataV2 getData();

  <T extends FormatExtension> Optional<T> getOutputFormat(
      @NotNull Class<T> extensionType,
      @NotNull ApiMediaType mediaType,
      @NotNull String path,
      @NotNull Optional<String> collectionId);

  /**
   * Determine spatial extent of all collections in the dataset.
   *
   * @return the bounding box in the default CRS
   */
  Optional<BoundingBox> getSpatialExtent();

  /**
   * Determine spatial extent of all collections in the dataset in another CRS.
   *
   * @param targetCrs the target CRS
   * @return the bounding box
   */
  Optional<BoundingBox> getSpatialExtent(@NotNull EpsgCrs targetCrs);

  /**
   * Determine spatial extent of a collection in the dataset.
   *
   * @param collectionId the name of the feature type
   * @return the bounding box in the default CRS
   */
  Optional<BoundingBox> getSpatialExtent(@NotNull String collectionId);

  /**
   * Determine spatial extent of a collection in the dataset in another CRS.
   *
   * @param collectionId the name of the feature type
   * @param targetCrs the target CRS
   * @return the bounding box in the target CRS
   */
  Optional<BoundingBox> getSpatialExtent(@NotNull String collectionId, @NotNull EpsgCrs targetCrs);

  default Optional<BoundingBox> getSpatialExtent(Optional<String> collectionId, EpsgCrs targetCrs) {
    return collectionId.isPresent()
        ? getSpatialExtent(collectionId.get(), targetCrs)
        : getSpatialExtent(targetCrs);
  }

  /**
   * Update spatial extent of a collection in the dataset.
   *
   * @param collectionId the name of the feature type
   * @param bbox the extent
   * @return {@code true} if the value changed and was non-empty before
   */
  boolean updateSpatialExtent(@NotNull String collectionId, @NotNull BoundingBox bbox);

  /**
   * Determine temporal extent of all collections in the dataset.
   *
   * @return the temporal extent in the Gregorian calendar
   */
  Optional<TemporalExtent> getTemporalExtent();

  /**
   * Determine temporal extent of a collection in the dataset.
   *
   * @param collectionId the name of the feature type
   * @return the temporal extent in the Gregorian calendar
   */
  Optional<TemporalExtent> getTemporalExtent(@NotNull String collectionId);

  /**
   * Update temporal extent of a collection in the dataset.
   *
   * @param collectionId the name of the feature type
   * @param temporalExtent the extent
   * @return {@code true} if the value changed and was non-empty before
   */
  boolean updateTemporalExtent(
      @NotNull String collectionId, @NotNull TemporalExtent temporalExtent);

  /**
   * Determine timestamp of the last modification of all collections in the dataset.
   *
   * @return the timestamp
   */
  Optional<Instant> getLastModified();

  /**
   * Determine timestamp of the last modification in a collection.
   *
   * @param collectionId the name of the feature type
   * @return the timestamp
   */
  Optional<Instant> getLastModified(@NotNull String collectionId);

  /**
   * Update timestamp of the last modification in a collection.
   *
   * @param collectionId the name of the feature type
   * @param lastModified the timestamp
   * @return {@code true} if the value changed and was non-empty before
   */
  @SuppressWarnings("UnusedReturnValue")
  boolean updateLastModified(@NotNull String collectionId, @NotNull Instant lastModified);

  /**
   * Determine the number of items in all collections in the dataset.
   *
   * @return the number of items
   */
  Optional<Long> getItemCount();

  /**
   * Determine the number of items in a collection.
   *
   * @param collectionId the name of the feature type
   * @return the number of items
   */
  Optional<Long> getItemCount(@NotNull String collectionId);

  /**
   * Update the number of items in a collection.
   *
   * @param collectionId the name of the feature type
   * @param itemCount the item count
   * @return {@code true} if the value changed and was non-empty before
   */
  boolean updateItemCount(@NotNull String collectionId, @NotNull Long itemCount);

  // convenience methods

  default Optional<BoundingBox> getSpatialExtent(@NotNull Optional<String> collectionId) {
    return collectionId.isPresent() ? getSpatialExtent(collectionId.get()) : getSpatialExtent();
  }

  @SuppressWarnings("unused")
  default Optional<TemporalExtent> getTemporalExtent(@NotNull Optional<String> collectionId) {
    return collectionId.isPresent() ? getTemporalExtent(collectionId.get()) : getTemporalExtent();
  }

  default Optional<Instant> getLastModified(@NotNull Optional<String> collectionId) {
    return collectionId.isPresent() ? getLastModified(collectionId.get()) : getLastModified();
  }

  @SuppressWarnings("unused")
  default Optional<Long> getItemCount(@NotNull Optional<String> collectionId) {
    return collectionId.isPresent() ? getItemCount(collectionId.get()) : getItemCount();
  }
}
