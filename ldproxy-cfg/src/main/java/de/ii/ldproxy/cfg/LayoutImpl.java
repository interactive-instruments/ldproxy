/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.base.domain.ImmutableStoreSourceDefault;
import de.ii.xtraplatform.base.domain.ImmutableStoreSourceFsV3;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSourceFs;
import de.ii.xtraplatform.base.domain.StoreSourceFsV3;
import de.ii.xtraplatform.base.domain.StoreSourceHttpV3;
import de.ii.xtraplatform.store.app.StoreMigrationV4;
import de.ii.xtraplatform.store.domain.BlobSource;
import de.ii.xtraplatform.store.domain.StoreMigration;
import de.ii.xtraplatform.store.infra.BlobSourceFs;
import io.dropwizard.util.DataSize;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LayoutImpl implements Layout {

  static Optional<StoreSourceFs> detectSource(Path dataDirectory) {
    StoreSourceFs v3 = new ImmutableStoreSourceFsV3.Builder().src(dataDirectory.toString()).build();
    StoreSourceFs v4 =
        new ImmutableStoreSourceDefault.Builder().src(dataDirectory.toString()).build();

    if (dataDirectory.resolve("store/entities").toFile().isDirectory()) {
      return Optional.of(v3);
    } else if (dataDirectory.resolve("entities/instances").toFile().isDirectory()) {
      return Optional.of(v4);
    } else if (dataDirectory.resolve("api-resources").toFile().isDirectory()) {
      return Optional.of(v3);
    } else if (dataDirectory.resolve("resources").toFile().isDirectory()) {
      return Optional.of(v4);
    }
    // TODO: parse cfg.yml

    return Optional.empty();
  }

  private final StoreSourceFs source;
  private final Layout.Info info;
  private final Layout.Version version;
  private final BlobSource blobSource;

  public LayoutImpl(StoreSourceFs source) {
    this.source = source;
    this.info = new InfoImpl();
    this.version = detect();
    this.blobSource = new BlobSourceFs(Path.of(source.getSrc()));
  }

  private Version detect() {
    if (source instanceof StoreSourceFsV3 || source instanceof StoreSourceHttpV3) {
      return Version.V3;
    }
    return Version.V4;
  }

  @Override
  public Info info() {
    return info;
  }

  @Override
  public List<String> check() {
    return null;
  }

  @Override
  public void create() {}

  @Override
  public void upgrade() {}

  @Override
  public List<StoreMigration> migrations() {
    return List.of(new StoreMigrationV4(source, () -> blobSource));
  }

  class InfoImpl implements Layout.Info {

    @Override
    public Path path() {
      return source.getAbsolutePath(Path.of(""));
    }

    @Override
    public String label() {
      String label = source.getLabelSpaces();

      if (version == Version.V3) {
        return label.replaceFirst(" ", "_V3 ");
      }
      return label;
    }

    @Override
    public Version version() {
      return version;
    }

    @Override
    public String size() throws IOException {
      // TODO: only files belonging to source
      try (Stream<Path> files = blobSource.walk(Path.of(""), 16, (p, a) -> a.isValue())) {
        long sum =
            files
                .mapToLong(
                    p -> {
                      try {
                        return blobSource.size(p);
                      } catch (IOException e) {
                        return 0;
                      }
                    })
                .sum();

        DataSize size = DataSize.bytes(sum);

        if (sum > 1073741824) {
          return size.toGigabytes() + "GB";
        } else if (sum > 1048576) {
          return size.toMegabytes() + "MB";
        }

        return size.toKilobytes() + "KB";
      }
    }

    @Override
    public Map<String, Long> entities() throws IOException {
      if (source.isSingleContent() && source.getContent() != Content.INSTANCES) {
        return Map.of();
      }
      Optional<StoreSource> entitiesSource =
          source.getContent() == Content.MULTI
              ? source.explode().stream()
                  .filter(s -> s.getContent() == Content.ENTITIES)
                  .findFirst()
              : Optional.of(source);

      if (entitiesSource.isEmpty()) {
        return Map.of();
      }

      Path path =
          entitiesSource
              .get()
              .getPath(version == Version.V3 ? Content.INSTANCES_OLD : Content.INSTANCES);

      Map<String, Long> result = new TreeMap<>();

      if (blobSource.has(path)) {
        try (Stream<Path> entities =
            blobSource.walk(
                path,
                16,
                (p, a) ->
                    p.getNameCount() > 1
                        && a.isValue()
                        && p.getFileName().toString().endsWith(".yml"))) {
          Map<String, Long> collected =
              entities.collect(
                  Collectors.groupingBy(
                      p -> p.getParent().getFileName().toString(), Collectors.counting()));

          collected.forEach(
              (k, v) -> {
                if (result.containsKey(k)) {
                  result.put(k, result.get(k) + v);
                } else {
                  result.put(k, v);
                }
              });
        }
      }

      return result;
    }

    @Override
    public Map<String, Long> resources() throws IOException {
      if (source.isSingleContent() && source.getContent() != Content.RESOURCES) {
        return Map.of();
      }
      List<StoreSource> sources =
          source.getContent() == Content.MULTI
              ? source.explode().stream()
                  .filter(s -> s.getContent() == Content.RESOURCES)
                  .collect(Collectors.toList())
              : List.of(source);

      if (sources.isEmpty()) {
        return Map.of();
      }

      Map<String, Long> result = new TreeMap<>();

      for (StoreSource s : sources) {
        Path path = s.getPath(Content.RESOURCES);
        Optional<String> prefix = s.getPrefix().map(p -> Path.of(p).getName(0).toString());

        if (blobSource.has(path)) {
          try (Stream<Path> entities =
              blobSource.walk(path, 16, (p, a) -> p.getNameCount() > 1 && a.isValue())) {
            Map<String, Long> collected =
                entities.collect(
                    Collectors.groupingBy(
                        p -> prefix.orElse(p.getName(0).toString()), Collectors.counting()));

            collected.forEach(
                (k, v) -> {
                  if (result.containsKey(k)) {
                    result.put(k, result.get(k) + v);
                  } else {
                    result.put(k, v);
                  }
                });
          }
        }
      }

      result.remove("resources");

      return result;
    }
  }
}
