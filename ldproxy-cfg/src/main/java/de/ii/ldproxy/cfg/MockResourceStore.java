/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.blobs.domain.Blob;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

class MockResourceStore implements ResourceStore {

  @Override
  public CompletableFuture<Void> onReady() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Path getPrefix() {
    return null;
  }

  @Override
  public Optional<Path> asLocalPath(Path path, boolean writable) throws IOException {
    return Optional.empty();
  }

  @Override
  public boolean has(Path path) throws IOException {
    return false;
  }

  @Override
  public Optional<InputStream> content(Path path) throws IOException {
    return Optional.empty();
  }

  @Override
  public Optional<Blob> get(Path path) throws IOException {
    return Optional.empty();
  }

  @Override
  public long size(Path path) throws IOException {
    return 0;
  }

  @Override
  public long lastModified(Path path) throws IOException {
    return 0;
  }

  @Override
  public Stream<Path> walk(Path path, int maxDepth, BiPredicate<Path, PathAttributes> matcher)
      throws IOException {
    return null;
  }

  @Override
  public void put(Path path, InputStream content) throws IOException {}

  @Override
  public void delete(Path path) throws IOException {}
}
