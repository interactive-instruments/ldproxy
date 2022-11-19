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
    Error
  }

  TileResult NOT_FOUND = new ImmutableTileResult.Builder().status(Status.NotFound).build();

  static TileResult notFound() {
    return NOT_FOUND;
  }

  static TileResult found(InputStream content) {
    return new ImmutableTileResult.Builder().status(Status.Found).content(content).build();
  }

  static TileResult error(String message) {
    return new ImmutableTileResult.Builder().status(Status.Error).error(message).build();
  }

  Status getStatus();

  Optional<InputStream> getContent();

  Optional<String> getError();

  @Value.Derived
  default boolean isAvailable() {
    return getStatus() == Status.Found && getContent().isPresent();
  }

  @Value.Derived
  default boolean isNotFound() {
    return getStatus() == Status.NotFound;
  }

  @Value.Derived
  default boolean isError() {
    return getStatus() == Status.Error && getError().isPresent();
  }

  @Value.Check
  default void check() {
    if (getStatus() == Status.Found) {
      Preconditions.checkState(getContent().isPresent(), "content is required for status 'Found'");
    } else if (getStatus() == Status.Error) {
      Preconditions.checkState(getError().isPresent(), "error is required for status 'Error'");
    }
  }
}
