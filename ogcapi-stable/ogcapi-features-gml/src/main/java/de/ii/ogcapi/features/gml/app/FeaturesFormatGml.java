/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class FeaturesFormatGml implements ConformanceClass, FeatureFormatExtension {

  private static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(
              new MediaType(
                  "application",
                  "gml+xml",
                  ImmutableMap.of(
                      "version",
                      "3.2",
                      "profile",
                      "http://www.opengis.net/def/profile/ogc/2.0/gml-sf2")))
          .label("GML")
          .parameter("xml")
          .build();
  public static final ApiMediaType COLLECTION_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "xml"))
          .label("XML")
          .parameter("xml")
          .build();

  private final FeaturesCoreProviders providers;

  @Inject
  public FeaturesFormatGml(FeaturesCoreProviders providers) {
    this.providers = providers;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf2");
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(new ObjectSchema())
        .schemaRef("#/components/schemas/anyObject")
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public ApiMediaType getCollectionMediaType() {
    return COLLECTION_MEDIA_TYPE;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return GmlConfiguration.class;
  }

  @Override
  public boolean canPassThroughFeatures() {
    return true;
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoderPassThrough(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    return Optional.of(
        new FeatureEncoderGmlUpgrade(
            ImmutableFeatureTransformationContextGml.builder()
                .from(transformationContext)
                .namespaces(
                    ((ConnectionInfoWfsHttp)
                            ((WithConnectionInfo<?>)
                                    providers
                                        .getFeatureProviderOrThrow(
                                            transformationContext.getApiData())
                                        .getData())
                                .getConnectionInfo())
                        .getNamespaces())
                .build()));
  }
}
