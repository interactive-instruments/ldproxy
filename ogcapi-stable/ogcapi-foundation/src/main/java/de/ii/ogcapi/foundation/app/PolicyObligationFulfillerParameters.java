/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ApiSecurity.PolicyAttribute;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.PolicyObligationFulfiller;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PolicyObligationFulfillerParameters implements PolicyObligationFulfiller {

  @Inject
  PolicyObligationFulfillerParameters() {}

  @Override
  public boolean canFulfill(Map<String, PolicyAttribute> obligations, ApiOperation apiOperation) {
    return !apiOperation.getQueryParameters().isEmpty()
        && obligations.values().stream()
            .anyMatch(obligation -> obligation.getParameter().isPresent());
  }

  @Override
  public Tuple<ApiRequestContext, Set<String>> fulfill(
      Map<String, PolicyAttribute> obligations,
      ApiOperation apiOperation,
      ApiRequestContext requestContext,
      Map<String, String> values) {
    Map<String, String> newParameters = new LinkedHashMap<>();
    ApiRequestContext newRequestContext = requestContext;
    Set<String> fulfilled = new HashSet<>();

    obligations.entrySet().stream()
        .filter(
            entry ->
                entry.getValue().getParameter().isPresent() && values.containsKey(entry.getKey()))
        .forEach(
            entry -> {
              String attribute = entry.getKey();
              String parameter = entry.getValue().getParameter().get();

              Optional<OgcApiQueryParameter> queryParameter =
                  apiOperation.getQueryParameters().stream()
                      .filter(
                          ogcApiQueryParameter ->
                              Objects.equals(ogcApiQueryParameter.getName(), parameter))
                      .findFirst();

              if (queryParameter.isPresent()) {
                // TODO: a cleaner solution would be to add a method merge to TypedQueryParameter
                // and call that
                if (Objects.equals(parameter, "filter")
                    && requestContext.getParameters().containsKey(parameter)) {
                  newParameters.put(
                      queryParameter.get().getName(),
                      String.format(
                          "(%s) AND (%s)",
                          requestContext.getParameters().get(parameter), values.get(attribute)));
                } else {
                  newParameters.put(queryParameter.get().getName(), values.get(attribute));
                }
                fulfilled.add(attribute);
              }
            });

    if (!newParameters.isEmpty()) {
      Map<String, String> mergedParameters = new LinkedHashMap<>(requestContext.getParameters());
      mergedParameters.putAll(newParameters);

      newRequestContext = new Builder().from(requestContext).parameters(mergedParameters).build();
    }

    return Tuple.of(newRequestContext, fulfilled);
  }
}
