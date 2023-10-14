/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import de.ii.xtraplatform.web.domain.StaticResourceReader;
import de.ii.xtraplatform.web.domain.StaticResourceReader.CachedResource;
import de.ii.xtraplatform.web.domain.StaticResources;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class StaticResourcesCustom implements StaticResources {

  private final BlobStore assetStore;

  @Inject
  StaticResourcesCustom(BlobStore blobStore) {
    this.assetStore = blobStore.with("html", "assets");
  }

  @Override
  public String getResourcePath() {
    return "";
  }

  @Override
  public String getUrlPath() {
    return "/custom/assets";
  }

  @Override
  public Optional<StaticResourceReader> getResourceReader() {
    return Optional.of(
        (path, defaultPage) -> {
          Path assetPath = Path.of("/").relativize(Path.of(path));
          try {
            if (assetStore.has(assetPath)) {
              return Optional.of(
                  CachedResource.of(
                      assetStore.content(assetPath).get().readAllBytes(),
                      assetStore.lastModified(assetPath)));
            }
            if (defaultPage.isPresent()) {
              Path defaultPath = assetPath.resolve(defaultPage.get());
              if (assetStore.has(defaultPath)) {
                return Optional.of(
                    CachedResource.of(
                        assetStore.content(defaultPath).get().readAllBytes(),
                        assetStore.lastModified(defaultPath)));
              }
            }
          } catch (IOException e) {
            // ignore
          }
          return Optional.empty();
        });
  }
}
