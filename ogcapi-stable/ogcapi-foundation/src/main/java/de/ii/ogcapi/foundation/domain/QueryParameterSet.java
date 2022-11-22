/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.immutables.value.Value;

@Value.Immutable
public interface QueryParameterSet {

  static QueryParameterSet of(List<OgcApiQueryParameter> definitions, Map<String, String> values) {
    return new ImmutableQueryParameterSet.Builder().definitions(definitions).values(values).build();
  }

  Set<OgcApiQueryParameter> getDefinitions();

  Map<String, String> getValues();

  Map<String, Object> getTypedValues();

  Set<String> getFilterKeys();

  default <U> Optional<U> getValue(TypedQueryParameter<U> parameter) {
    return Optional.ofNullable((U) getTypedValues().get(parameter.getName()));
  }

  default QueryParameterSet evaluate(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    Map<String, String> values = new LinkedHashMap<>(getValues());
    Map<String, Object> typedValues = new LinkedHashMap<>();
    Set<String> filterKeys = ImmutableSet.of();

    for (OgcApiQueryParameter parameter : getDefinitions()) {
      if (collectionData.isPresent()) {
        values = parameter.transformParameters(collectionData.get(), values, apiData);
        filterKeys =
            parameter.getFilterParameters(filterKeys, apiData, collectionData.get().getId());
      }
    }

    for (OgcApiQueryParameter parameter : getDefinitions()) {
      if (parameter instanceof TypedQueryParameter<?> && values.containsKey(parameter.getName())) {
        typedValues.put(
            parameter.getName(),
            ((TypedQueryParameter<?>) parameter).parse(values.get(parameter.getName()), apiData));
      }
    }

    return new ImmutableQueryParameterSet.Builder()
        .definitions(getDefinitions())
        .values(values)
        .typedValues(typedValues)
        .filterKeys(filterKeys)
        .build();
  }

  default <U> void forEach(Class<U> clazz, Consumer<U> consumer) {
    for (OgcApiQueryParameter parameter : getDefinitions()) {
      if (clazz.isInstance(parameter)) {
        consumer.accept(clazz.cast(parameter));
      }
    }
  }
}
