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
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title profileSchema
 * @endpoints Feature Schema
 * @langEn TODO_DOCS
 * @langDe TODO_DOCS
 */
@Singleton
@AutoBind
public class QueryParameterProfileSchema extends QueryParameterProfile {

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
}
