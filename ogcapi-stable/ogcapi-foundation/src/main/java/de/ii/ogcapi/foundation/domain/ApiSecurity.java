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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn Access control for all API operations (combination of endpoint and HTTP method).
 *     <p>The control mechanism is based on scopes, currently only two exist:
 *     <p><code>
 * - `WRITE`: every operation with HTTP method `POST`, `PUT`, `PATCH` or `DELETE`
 * - `READ`: any other operation
 *     </code>
 *     <p>To support authenticated users, a bearer token has to be included in the `Authorization`
 *     header in requests to the API. Validation and evaluation of these tokens has to configured in
 *     the [global configuration](../../70-reference.md).
 * @langDe Absicherung für alle API Operationen (Kombination aus Endpunkt und HTTP-Methode).
 *     <p>Die Absicherung basiert auf Scopes, aktuell existieren nur zwei:
 *     <p><code>
 * - `WRITE`: alle Operatione mit HTTP-Methode `POST`, `PUT`, `PATCH` or `DELETE`
 * - `READ`: alle anderen Operationen
 *     </code>
 *     <p>Um authentifizierte Benutzer zu unterstützen, muss ein Bearer-Token im
 *     `Authorization`-Header in Anfragen an die API inkludiert werden. Die Validierung und
 *     Auswertung dieser Tokens muss in der [globalen Konfiguration](../../70-reference.md)
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

  String SCOPE_READ = "read";
  String SCOPE_WRITE = "write";

  /**
   * @langEn Option to disable access control.
   * @langDe Option, um die Absicherung zu deaktivieren.
   * @default true
   */
  @Nullable
  Boolean getEnabled();

  @JsonIgnore
  Set<ScopeElements> getScopeElements();

  /**
   * @langEn List of permissions that every user possesses, if authenticated or not.
   * @langDe Liste der Berechtigungen, die jeder Benutzer besitzt, ob angemeldet oder nicht.
   * @default [READ]
   */
  List<String> getPublicScopes();

  @JsonIgnore
  @Value.Derived
  default boolean isEnabled() {
    return !Objects.equals(getEnabled(), false);
  }

  default boolean isSecured(String scope) {
    return isEnabled() && !getPublicScopes().contains(scope);
  }
}
