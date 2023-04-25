/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import java.util.Comparator;
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

  default <U> Optional<U> getValue(TypedQueryParameter<U> parameter) {
    //noinspection unchecked
    return Optional.ofNullable((U) getTypedValues().get(parameter.getName()));
  }

  default QueryParameterSet evaluate(
      OgcApi api, Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    Map<String, String> values = new LinkedHashMap<>(getValues());
    Map<String, Object> typedValues = new LinkedHashMap<>();

    int numberOfParsingPasses =
        getDefinitions().stream()
            .filter(parameter -> parameter instanceof TypedQueryParameter<?>)
            .map(parameter -> ((TypedQueryParameter<?>) parameter).getPriority())
            .max(Comparator.naturalOrder())
            .orElse(1);
    for (int pass = 1; pass <= numberOfParsingPasses; pass++) {
      for (OgcApiQueryParameter parameter : getDefinitions()) {
        if (parameter instanceof TypedQueryParameter<?>
            && ((TypedQueryParameter<?>) parameter).getPriority() == pass
            && values.containsKey(parameter.getName())) {
          Object parsedValue =
              ((TypedQueryParameter<?>) parameter)
                  .parse(values.get(parameter.getName()), typedValues, api, optionalCollectionData);
          if (parsedValue != null) {
            typedValues.put(parameter.getName(), parsedValue);
          }
        }
      }
    }

    return new ImmutableQueryParameterSet.Builder()
        .definitions(getDefinitions())
        .values(values)
        .typedValues(typedValues)
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
