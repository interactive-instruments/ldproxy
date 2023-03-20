/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.domain.QueryParameterProfile;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title profile
 * @endpoints Feature Schema
 * @langEn Select the JSON Schema version of the response. Supported are "2019-09" (default) and
 *     "07".
 * @langDe Wählt die JSON-Schema-Version der Antwort. Unterstützt werden "2019-09" (Standard) und
 *     "07".
 * @default 2019-09
 */
@Singleton
@AutoBind
public class QueryParameterProfileSchema extends QueryParameterProfile
    implements TypedQueryParameter<String> {

  // TODO #846

  static final List<String> PROFILES = ImmutableList.of("2019-09", "07");

  @Inject
  public QueryParameterProfileSchema(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super(extensionRegistry, schemaValidator);
  }

  @Override
  public String getId() {
    return "profileSchema";
  }

  @Override
  protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return definitionPath.equals("/collections/{collectionId}/schemas/{type}");
  }

  @Override
  protected List<String> getProfiles(OgcApiDataV2 apiData) {
    return PROFILES;
  }

  @Override
  protected String getDefault(OgcApiDataV2 apiData) {
    return "2019-09";
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SchemaConfiguration.class;
  }

  @Override
  public String parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    return value;
  }
}
