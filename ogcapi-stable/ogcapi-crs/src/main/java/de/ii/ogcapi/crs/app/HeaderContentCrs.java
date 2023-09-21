/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ogcapi.crs.domain.CrsSupport;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class HeaderContentCrs extends ApiExtensionCache implements ApiHeader {

  private final Schema<?> schema = new StringSchema().format("uri");
  private final SchemaValidator schemaValidator;
  private final CrsSupport crsSupport;

  @Inject
  HeaderContentCrs(SchemaValidator schemaValidator, CrsSupport crsSupport) {
    this.schemaValidator = schemaValidator;
    this.crsSupport = crsSupport;
  }

  @Override
  public String getId() {
    return "Content-Crs";
  }

  @Override
  public String getDescription() {
    return "The coordinate reference system of coordinates in the response.";
  }

  @Override
  public boolean isResponseHeader() {
    return true;
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && ((method == HttpMethods.GET && definitionPath.endsWith("/items"))
                    || (method == HttpMethods.GET
                        && definitionPath.endsWith("/items/{featureId}"))));
  }

  private final ConcurrentMap<Integer, Schema<?>> schemaMap = new ConcurrentHashMap<>();

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) {
      List<String> crsList =
          crsSupport.getSupportedCrsList(apiData).stream()
              .map(EpsgCrs::toUriString)
              .map(this::toUriInHeader)
              .collect(ImmutableList.toImmutableList());
      String defaultCrs =
          toUriInHeader(
              apiData
                  .getExtension(FeaturesCoreConfiguration.class)
                  .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                  .map(EpsgCrs::toUriString)
                  .orElse(OgcCrs.CRS84_URI));
      schemaMap.put(apiHashCode, new StringSchema()._enum(crsList)._default(defaultCrs));
    }
    return schemaMap.get(apiHashCode);
  }

  private String toUriInHeader(String uri) {
    return String.format("<%s>", uri);
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return isExtensionEnabled(apiData, CrsConfiguration.class);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CrsConfiguration.class;
  }

  @Override
  public Optional<SpecificationMaturity> getSpecificationMaturity() {
    return CrsBuildingBlock.MATURITY;
  }

  @Override
  public Optional<ExternalDocumentation> getSpecificationRef() {
    return CrsBuildingBlock.SPEC;
  }
}
