/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Sets;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn Access control for all API operations (combination of endpoint and HTTP method).
 *     <p>The control mechanism is based on scopes, currently only two exist:
 *     <p><code>
 * - `write`: every operation with HTTP method `POST`, `PUT`, `PATCH` or `DELETE`
 * - `read`: any other operation
 * </code>
 *     <p>To support authenticated users, a bearer token has to be included in the `Authorization`
 *     header in requests to the API. Validation and evaluation of these tokens has to be configured
 *     in the [global configuration](../application/70-reference.md).
 * @langDe Absicherung für alle API Operationen (Kombination aus Endpunkt und HTTP-Methode).
 *     <p>Die Absicherung basiert auf Scopes, aktuell existieren nur zwei:
 *     <p><code>
 * - `write`: alle Operationen mit HTTP-Methode `POST`, `PUT`, `PATCH` oder `DELETE`
 * - `read`: alle anderen Operationen
 * </code>
 *     <p>Um authentifizierte Benutzer zu unterstützen, muss ein Bearer-Token im
 *     `Authorization`-Header in Anfragen an die API inkludiert werden. Die Validierung und
 *     Auswertung dieser Tokens muss in der [globalen Konfiguration](../application/70-reference.md)
 *     konfiguriert werden.
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableApiSecurity.Builder.class)
public interface ApiSecurity {

  enum ScopeElements {
    READ_WRITE,
    TAG,
    MODULE,
    OPERATION,
    METHOD,
    FORMAT
  }

  enum Scope {
    READ,
    WRITE;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  String SCOPE_PUBLIC = "public";
  Tuple<Scope, String> SCOPE_PUBLIC_READ = Tuple.of(Scope.READ, SCOPE_PUBLIC);
  String ROLE_PUBLIC = "public";

  /**
   * @langEn Option to disable access control.
   * @langDe Option, um die Absicherung zu deaktivieren.
   * @default true
   * @since v3.3
   */
  @Nullable
  Boolean getEnabled();

  @JsonIgnore
  Set<ScopeElements> getScopeElements();

  /**
   * @langEn List of permissions that every user possesses, if authenticated or not.
   * @langDe Liste der Berechtigungen, die jeder Benutzer besitzt, ob angemeldet oder nicht.
   * @default [read]
   * @since v3.3
   */
  Set<String> getPublicScopes();

  Map<String, Set<String>> getRoles();

  @JsonIgnore
  @Value.Derived
  default boolean isEnabled() {
    return !Objects.equals(getEnabled(), false);
  }

  default boolean isSecured(Set<String> permissions) {
    return isEnabled()
        && Sets.intersection(getRoles().getOrDefault("public", Set.of()), permissions).isEmpty();
  }

  default Set<String> getRolesWith(Set<String> permissions) {
    return getRoles().entrySet().stream()
        .filter(role -> !Sets.intersection(role.getValue(), permissions).isEmpty())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }
}
