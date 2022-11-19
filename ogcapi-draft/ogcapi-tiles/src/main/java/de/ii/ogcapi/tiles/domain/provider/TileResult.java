/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.google.common.base.Preconditions;
import java.io.InputStream;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface TileResult {

  enum Status {
    Found,
    NotFound,
    OutOfBounds
  }

  TileResult NOT_FOUND = new ImmutableTileResult.Builder().status(Status.NotFound).build();
  TileResult OUT_OF_BOUNDS = new ImmutableTileResult.Builder().status(Status.OutOfBounds).build();

  static TileResult notFound() {
    return NOT_FOUND;
  }

  static TileResult outOfBounds() {
    return OUT_OF_BOUNDS;
  }

  static TileResult found(InputStream content) {
    return new ImmutableTileResult.Builder().status(Status.Found).content(content).build();
  }

  Status getStatus();

  Optional<InputStream> getContent();

  @Value.Derived
  default boolean isAvailable() {
    return getStatus() == Status.Found && getContent().isPresent();
  }

  @Value.Derived
  default boolean isNotFound() {
    return getStatus() == Status.NotFound;
  }

  @Value.Check
  default void check() {
    if (getStatus() == Status.Found) {
      Preconditions.checkState(getContent().isPresent(), "content is required for status 'found'");
    }
  }
}
