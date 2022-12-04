/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class QueryParameterProfile extends ApiExtensionCache
    implements OgcApiQueryParameter {

  protected final ExtensionRegistry extensionRegistry;
  protected final SchemaValidator schemaValidator;
  protected final ConcurrentMap<Integer, ConcurrentMap<String, Schema>> schemaMap;

  protected QueryParameterProfile(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    super();
    this.extensionRegistry = extensionRegistry;
    this.schemaValidator = schemaValidator;
    this.schemaMap = new ConcurrentHashMap<>();
  }

  @Override
  public final String getName() {
    return "profile";
  }

  @Override
  public String getDescription() {
    return "Select the profile to be used in the response. If no value is provided, the default profile will be used.";
  }

  @Override
  public final boolean isApplicable(
      OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && isApplicable(apiData, definitionPath));
  }

  protected abstract boolean isApplicable(OgcApiDataV2 apiData, String definitionPath);

  protected abstract List<String> getProfiles(OgcApiDataV2 apiData);

  protected List<String> getProfiles(
      OgcApiDataV2 apiData, @SuppressWarnings("unused") String collectionId) {
    // currently no support for different values for each collection
    return getProfiles(apiData);
  }

  protected abstract String getDefault(OgcApiDataV2 apiData);

  protected String getDefault(
      OgcApiDataV2 apiData, @SuppressWarnings("unused") String collectionId) {
    // currently no support for different values for each collection
    return getDefault(apiData);
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) {
      schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    }
    if (!schemaMap.get(apiHashCode).containsKey("*")) {
      schemaMap
          .get(apiHashCode)
          .put("*", new StringSchema()._enum(getProfiles(apiData))._default(getDefault(apiData)));
    }
    return schemaMap.get(apiHashCode).get("*");
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) {
      schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    }
    if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
      schemaMap
          .get(apiHashCode)
          .put(
              collectionId,
              new StringSchema()
                  ._enum(getProfiles(apiData, collectionId))
                  ._default(getDefault(apiData, collectionId)));
    }
    return schemaMap.get(apiHashCode).get(collectionId);
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }
}
