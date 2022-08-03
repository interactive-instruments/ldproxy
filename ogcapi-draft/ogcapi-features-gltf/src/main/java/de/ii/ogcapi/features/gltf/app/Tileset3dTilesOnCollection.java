/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.CollectionExtension;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiCollection.Builder;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.features.gltf.domain._3dTilesConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/** add a link to the 3D Tiles tileset to the collection */
@Singleton
@AutoBind
public class Tileset3dTilesOnCollection implements CollectionExtension {

  private final I18n i18n;

  @Inject
  public Tileset3dTilesOnCollection(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return _3dTilesConfiguration.class;
  }

  @Override
  public Builder process(
      Builder collection,
      FeatureTypeConfigurationOgcApi featureTypeConfiguration,
      OgcApi api,
      URICustomizer uriCustomizer,
      boolean isNested,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      Optional<Locale> language) {
    if (isExtensionEnabled(featureTypeConfiguration, GltfConfiguration.class) && !isNested) {
      collection.addAllLinks(
          ImmutableList.<Link>builder()
              .add(
                  new ImmutableLink.Builder()
                      .href(
                          uriCustomizer
                              .copy()
                              .ensureNoTrailingSlash()
                              .ensureLastPathSegment("3dtiles")
                              .removeParameters("f")
                              .toString())
                      .rel("3dtiles") // TODO
                      .type(MediaType.APPLICATION_JSON)
                      .title(i18n.get("3dtilesLink", language)) // TODO
                      .build())
              .build());
    }

    return collection;
  }
}
