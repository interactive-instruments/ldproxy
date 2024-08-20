/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.app;

import static de.ii.ogcapi.features.cityjson.domain.CityJsonConfiguration.Version.V11;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.cityjson.domain.CityJsonConfiguration;
import de.ii.ogcapi.features.cityjson.domain.CityJsonWriterRegistry;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @title CityJSON
 */
@Singleton
@AutoBind
public class FeaturesFormatCityJson extends FeaturesFormatCityJsonBase {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "city+json"))
          .label("CityJSON")
          .parameter("cityjson")
          .fileExtension("city.json")
          .build();

  @Inject
  public FeaturesFormatCityJson(
      FeaturesCoreProviders providers,
      ValueStore valueStore,
      FeaturesCoreValidation featuresCoreValidator,
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection,
      CityJsonWriterRegistry cityJsonWriterRegistry,
      CrsTransformerFactory crsTransformerFactory,
      CrsInfo crsInfo,
      ExtensionRegistry extensionRegistry) {
    super(
        providers,
        valueStore,
        featuresCoreValidator,
        schemaGeneratorFeature,
        schemaGeneratorFeatureCollection,
        cityJsonWriterRegistry,
        crsTransformerFactory,
        crsInfo,
        extensionRegistry);
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    CityJsonConfiguration.Version version =
        transformationContext
            .getApiData()
            .getExtension(CityJsonConfiguration.class, transformationContext.getCollectionId())
            .flatMap(CityJsonConfiguration::getVersion)
            .orElse(V11);
    return super.getFeatureEncoder(transformationContext, language, version, false);
  }
}
