/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class QueryParameterDryRun extends ApiExtensionCache
    implements OgcApiQueryParameter, TypedQueryParameter<Boolean> {

  protected final Schema<?> schema = new BooleanSchema()._default(false);
  protected final SchemaValidator schemaValidator;

  protected QueryParameterDryRun(SchemaValidator schemaValidator) {
    super();
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getName() {
    return "dry-run";
  }

  @Override
  public Boolean parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    return Objects.nonNull(value) && Boolean.parseBoolean(value);
  }

  public void applyTo(Object builder, QueryParameterSet parameters) {
    try {
      Method method = builder.getClass().getMethod("dryRun", boolean.class);
      boolean dryRun =
          Objects.requireNonNullElse((Boolean) parameters.getTypedValues().get(getName()), false);
      method.invoke(builder, dryRun);
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getDescription() {
    return "'true' just validates the request without creating or updating the resource; "
        + "returns 400, if validation fails, otherwise 204.";
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return schema;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }
}
