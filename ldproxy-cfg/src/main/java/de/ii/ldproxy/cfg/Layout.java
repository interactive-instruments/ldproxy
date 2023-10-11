/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.base.domain.StoreSourceFs;
import de.ii.xtraplatform.blobs.domain.StoreMigration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Layout {

  enum Version {
    V3,
    V4
  }

  static Layout of(Path directory) {
    if (!directory.isAbsolute()) {
      throw new IllegalArgumentException("Path is not absolute: " + directory);
    }
    Optional<StoreSourceFs> source = LayoutImpl.detectSource(directory);

    if (source.isEmpty()) {
      throw new IllegalArgumentException("No store source detected in " + directory);
    }
    return Layout.of(source.get());
  }

  static Layout of(StoreSourceFs source) {
    return new LayoutImpl(source);
  }

  interface Info {
    Path path();

    String label();

    Version version();

    String size() throws IOException;

    Map<String, Long> entities() throws IOException;

    Map<String, Long> resources() throws IOException;
  }

  Info info();

  List<String> check();

  void create();

  void upgrade();

  List<StoreMigration> migrations();
}
