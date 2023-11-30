/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.ImmutableHtmlConfiguration;
import de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration;
import de.ii.ogcapi.resources.domain.ResourcesConfiguration;
import de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityMigration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StylesMigrationV4 extends EntityMigration<OgcApiDataV2, OgcApiDataV2> {

  public StylesMigrationV4(EntityMigrationContext context) {
    super(context);
  }

  @Override
  public String getSubject() {
    return "building block STYLES, properties 'resourcesEnables', 'resourceManagerEnabled', and 'defaultStyle'";
  }

  @Override
  public String getDescription() {
    return "is deprecated and will be upgraded to the RESOURCES and HTML building blocks";
  }

  @Override
  public boolean isApplicable(EntityData entityData, Optional<EntityData> defaults) {
    if (!(entityData instanceof OgcApiDataV2)) {
      return false;
    }

    OgcApiDataV2 apiData = (OgcApiDataV2) entityData;

    for (FeatureTypeConfigurationOgcApi collection : apiData.getCollections().values()) {
      Optional<StylesConfiguration> stylesConfiguration =
          collection.getExtension(StylesConfiguration.class);

      if (hasDeprecatedProperties(stylesConfiguration)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public OgcApiDataV2 migrate(OgcApiDataV2 entityData, Optional<OgcApiDataV2> defaults) {
    Map<String, FeatureTypeConfigurationOgcApi> collections =
        entityData.getCollections().entrySet().stream()
            .map(
                entry -> {
                  FeatureTypeConfigurationOgcApi collectionOld = entry.getValue();
                  Optional<StylesConfiguration> stylesConfigurationOld =
                      collectionOld.getExtension(StylesConfiguration.class);
                  Optional<ResourcesConfiguration> resourcesConfigurationOld =
                      collectionOld.getExtension(ResourcesConfiguration.class);
                  Optional<HtmlConfiguration> htmlConfigurationOld =
                      collectionOld.getExtension(HtmlConfiguration.class);

                  if (!hasDeprecatedProperties(stylesConfigurationOld)) {
                    return Map.entry(entry.getKey(), collectionOld);
                  }

                  StylesConfiguration stylesConfiguration =
                      new ImmutableStylesConfiguration.Builder()
                          .from(stylesConfigurationOld.get())
                          .resourcesEnabled(null)
                          .resourceManagerEnabled(null)
                          .defaultStyle(null)
                          .build();

                  // If a ResourcesConfiguration is already present, simply ignore the
                  // deprecated values.
                  ResourcesConfiguration resourcesConfiguration =
                      resourcesConfigurationOld.isPresent()
                          ? null
                          : new ImmutableResourcesConfiguration.Builder()
                              .enabled(stylesConfigurationOld.get().isResourcesEnabled())
                              .managerEnabled(
                                  stylesConfigurationOld.get().isResourceManagerEnabled())
                              .build();

                  // If a HtmlConfiguration exists with a defaultStyle value, simply ignore the
                  // deprecated value.
                  HtmlConfiguration htmlConfiguration =
                      (Objects.isNull(stylesConfigurationOld.get().getDefaultStyle())
                              || htmlConfigurationOld
                                  .filter(cfg -> Objects.nonNull(cfg.getDefaultStyle()))
                                  .isPresent())
                          ? null
                          : (htmlConfigurationOld.isPresent()
                                  ? new ImmutableHtmlConfiguration.Builder()
                                      .from(htmlConfigurationOld.get())
                                  : new ImmutableHtmlConfiguration.Builder())
                              .defaultStyle(stylesConfigurationOld.get().getDefaultStyle())
                              .build();

                  FeatureTypeConfigurationOgcApi collection =
                      FeatureTypeConfigurationOgcApi.replaceOrAddExtensions(
                          collectionOld,
                          stylesConfiguration,
                          resourcesConfiguration,
                          htmlConfiguration);

                  return Map.entry(entry.getKey(), collection);
                })
            .collect(
                Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    return new ImmutableOgcApiDataV2.Builder().from(entityData).collections(collections).build();
  }

  private boolean hasDeprecatedProperties(Optional<StylesConfiguration> stylesConfiguration) {
    return stylesConfiguration
        .map(
            cfg ->
                cfg.isResourceManagerEnabled()
                    || cfg.isResourcesEnabled()
                    || Objects.nonNull(cfg.getDefaultStyle()))
        .isPresent();
  }
}
