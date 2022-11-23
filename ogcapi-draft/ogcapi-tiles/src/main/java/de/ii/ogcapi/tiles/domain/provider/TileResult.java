/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface TileResult {

  enum Status {
    // Tile is available from the tile provider
    Found,
    // Like Found, but the tile is empty (no data) and tiles at more detailed zoom levels
    // are guaranteed to be empty, too
    Empty,
    // Like Found, but the tile has data and tiles at more detailed zoom levels are guaranteed
    // to be identical
    Full,
    // Tile is not available from the provider
    NotFound,
    // Tile is outside the tile matrix set limits
    OutsideLimits,
    // Not a valid tile
    Error
  }

  TileResult NOT_FOUND = new ImmutableTileResult.Builder().status(Status.NotFound).build();

  static TileResult notFound() {
    return NOT_FOUND;
  }

  static TileResult notFound(byte[] content) {
    return new ImmutableTileResult.Builder().status(Status.NotFound).content(content).build();
  }

  static TileResult empty(byte[] content) {
    return new ImmutableTileResult.Builder().status(Status.Empty).content(content).build();
  }

  static TileResult full(byte[] content) {
    return new ImmutableTileResult.Builder().status(Status.Full).content(content).build();
  }

  static TileResult found(byte[] content) {
    return new ImmutableTileResult.Builder().status(Status.Found).content(content).build();
  }

  static TileResult outsideLimits(String message) {
    return new ImmutableTileResult.Builder().status(Status.OutsideLimits).error(message).build();
  }

  static TileResult error(String message) {
    return new ImmutableTileResult.Builder().status(Status.Error).error(message).build();
  }

  Status getStatus();

  Optional<byte[]> getContent();

  Optional<String> getError();

  @Value.Derived
  default boolean isAvailable() {
    return getContent().isPresent();
  }

  @Value.Derived
  default boolean isEmpty() {
    return getStatus() == Status.Empty;
  }

  @Value.Derived
  default boolean isFull() {
    return getStatus() == Status.Full;
  }

  @Value.Derived
  default boolean isNotFound() {
    return getStatus() == Status.NotFound;
  }

  @Value.Derived
  default boolean isOutsideLimits() {
    return getStatus() == Status.OutsideLimits;
  }

  @Value.Derived
  default boolean isError() {
    return getStatus() == Status.Error && getError().isPresent();
  }

  @Value.Check
  default void check() {
    if (getStatus() == Status.Found) {
      Preconditions.checkState(getContent().isPresent(), "content is required for status 'Found'");
    } else if (getStatus() == Status.Empty) {
      Preconditions.checkState(getContent().isPresent(), "content is required for status 'Empty'");
    } else if (getStatus() == Status.Full) {
      Preconditions.checkState(getContent().isPresent(), "content is required for status 'Full'");
    } else if (getStatus() == Status.OutsideLimits) {
      Preconditions.checkState(
          getError().isPresent(), "error is required for status 'OutsideLimits'");
    } else if (getStatus() == Status.Error) {
      Preconditions.checkState(getError().isPresent(), "error is required for status 'Error'");
    }
  }
}
