/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameterBase;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class QueryParameterProfile extends OgcApiQueryParameterBase {

  public static final String PROFILE = "profile";
  protected final ExtensionRegistry extensionRegistry;
  protected final SchemaValidator schemaValidator;

  protected QueryParameterProfile(
      ExtensionRegistry extensionRegistry, SchemaValidator schemaValidator) {
    this.extensionRegistry = extensionRegistry;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public final String getName() {
    return PROFILE;
  }

  @Override
  public String getDescription() {
    return "Select the profiles to be used in the response. If no value is provided, the default profiles will be used.";
  }

  protected abstract List<String> getProfiles(OgcApiDataV2 apiData);

  protected List<String> getProfiles(OgcApiDataV2 apiData, String collectionId) {
    return getProfiles(apiData);
  }

  protected abstract List<String> getDefault(OgcApiDataV2 apiData);

  protected List<String> getDefault(OgcApiDataV2 apiData, String collectionId) {
    return getDefault(apiData);
  }

  protected ConcurrentMap<Integer, ConcurrentMap<String, Schema<?>>> schemaMap =
      new ConcurrentHashMap<>();

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    if (!schemaMap.get(apiHashCode).containsKey("*")) {
      StringSchema schema = new StringSchema()._enum(getProfiles(apiData));
      List<String> defaults = getDefault(apiData);
      if (!defaults.isEmpty()) {
        schema._default(String.join(",", defaults));
      }
      schemaMap.get(apiHashCode).put("*", schema);
    }
    return schemaMap.get(apiHashCode).get("*");
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
      ArraySchema schema =
          new ArraySchema().items(new StringSchema()._enum(getProfiles(apiData, collectionId)));
      List<String> defaults = getDefault(apiData, collectionId);
      if (!defaults.isEmpty()) {
        schema._default(defaults);
      }
      schemaMap.get(apiHashCode).put(collectionId, schema);
    }
    return schemaMap.get(apiHashCode).get(collectionId);
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }
}
