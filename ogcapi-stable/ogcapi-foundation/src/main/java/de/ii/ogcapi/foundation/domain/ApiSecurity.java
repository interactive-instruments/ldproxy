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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.ii.xtraplatform.base.domain.util.Tuple;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn Access control for all API operations (combination of endpoint and HTTP method).
 *     <p>The control mechanism is based on permissions, scopes and roles. A scope is a predefined
 *     group of permissions, these scopes are available:
 *     <p><code>
 * - `discover`: access API landing pages and OpenAPI definitions
 * - `metadata`: access any other metadata
 * - `data:read`: access features
 * - `data:write`: mutate features
 * - `data`: includes `data:read` and `data:write`
 * - `tiles`: access tiles
 * - `styles:read`: access styles
 * - `styles:write`: mutate styles
 * - `styles`: includes `styles:read` and `styles:write`
 * - `queries:read`: access stored queries
 * - `queries:write`: mutate stored queries
 * - `queries`: includes `queries:read` and `queries:write`
 * - `routes:read`: access stored routes
 * - `routes:write`: mutate stored routes
 * - `routes`: includes `routes:read` and `routes:write`
 * - `read`: includes `discover`, `metadata`, `data:read`, `tiles`, `styles:read`, `queries:read` and `routes:write`
 * - `write`: includes `data:write`, `styles:write`, `queries:write` and `routes:write`
 * </code>
 *     <p>Permissions are a combination of a scope prefix and an OpenAPI operation id (without any
 *     prefix), for example `data:getItems` or `tiles:getTile`. These can be used for a more
 *     fine-grained control.
 *     <p>Roles are custom groups of scopes and/or permissions that are defined in `roles`.
 *     <p>**Data-specific permissions**
 *     <p>The scopes and permissions described above will permit access to any API and collection.
 *     To restrict the access to specific APIs or collections, a suffix can be added to scopes and
 *     permissions, for example `read::daraa` or `data:getItems::daraa:AeronauticSrf`.
 *     <p>**Role `public`**
 *     <p>The special role `public` defines the list of scopes and/or permissions that every user
 *     possesses, if authenticated or not.
 *     <p>**Authenticated users**
 *     <p>To support authenticated users, a bearer token has to be included in the `Authorization`
 *     header in requests to the API. Validation and evaluation of these tokens has to be configured
 *     in the [global configuration](../application/70-reference.md).
 * @langDe Absicherung für alle API Operationen (Kombination aus Endpunkt und HTTP-Methode).
 *     <p>Die Absicherung basiert auf Berechtigungen, Scopes und Rollen. Ein Scope ist eine
 *     vordefinierte Gruppe von Berechtigungen, diese Scopes sind verfügbar:
 *     <p><code>
 * - `discover`: Lesen von API Landing Pages und OpenAPI Definitionen
 * - `metadata`: Lesen vopn anderen Metadaten
 * - `data:read`: Lesen von Features
 * - `data:write`: Ändern von Features
 * - `data`: enthält `data:read` und `data:write`
 * - `tiles`: Lesen von Tiles
 * - `styles:read`: Lesen von Styles
 * - `styles:write`: Ändern von Styles
 * - `styles`: enthält `styles:read` und `styles:write`
 * - `queries:read`: Lesen von Stored Queries
 * - `queries:write`: Ändern von Stored Queries
 * - `queries`: enthält `queries:read` und `queries:write`
 * - `routes:read`: Lesen von Stored Routes
 * - `routes:write`: Ändern von Stored Routes
 * - `routes`: enthält `routes:read` und `routes:write`
 * - `read`: enthält `discover`, `metadata`, `data:read`, `tiles`, `styles:read`, `queries:read` und `routes:write`
 * - `write`: enthält `data:write`, `styles:write`, `queries:write` und `routes:write`
 * </code>
 *     <p>Berechtigungen sind eine Kombination aus Scope-Prefix und einer OpenAPI Operation-Id (ohne
 *     jeglichen Prefix), z.B. `data:getItems` oder `tiles:getTile`. Diese können für eine
 *     fein-granularere Absicherung verwendet werden.
 *     <p>Rollen sind benutzerdefinierte Gruppen von Scopes und/oder Berechtigungen, die in `roles`
 *     definiert werden.
 *     <p>**Daten-spezifische Berechtigungen**
 *     <p>Die oben beschiebenen Scopes und Berechtigungen gewähren Zugriff zu jeder API und
 *     Collection. Um den Zugriff auf bestimmte APIs oder Collections einzuschränken, kann ein
 *     Suffix zu Scopes und Berechtigungen hinzugefügt werden, z.B. `read::daraa` oder
 *     `data:getItems::daraa:AeronauticSrf`.
 *     <p>**Rolle `public`**
 *     <p>Die spezielle Rolle `public` definiert die Liste der Scopes und/oder Berechtigungen, die
 *     jeder Benutzer besitzt, ob angemeldet oder nicht. authenticated or not.
 *     <p>**Authentifizierte Benutzer**
 *     <p>Um authentifizierte Benutzer zu unterstützen, muss ein Bearer-Token im
 *     `Authorization`-Header in Anfragen an die API inkludiert werden. Die Validierung und
 *     Auswertung dieser Tokens muss in der [globalen Konfiguration](../application/70-reference.md)
 *     konfiguriert werden.
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableApiSecurity.Builder.class)
public interface ApiSecurity {

  enum Scope {
    READ,
    WRITE;

    public String with(String group) {
      return join(group, toString());
    }

    public String with(String group, String operation) {
      return join(group, operation);
    }

    public Set<String> setOf(String group, String operation) {
      return ImmutableSet.of(this.toString(), group, with(group), with(group, operation));
    }

    public Set<String> setOf(String group, String operation, String api) {
      return setOf(group, operation).stream()
          .flatMap(permission -> Stream.of(permission, join(permission, api, true)))
          .collect(ImmutableSet.toImmutableSet());
    }

    public Set<String> setOf(String group, String operation, String api, String collection) {
      return setOf(group, operation, api).stream()
          .flatMap(
              permission ->
                  permission.endsWith(join("", api, true))
                      ? Stream.of(permission, join(permission, collection))
                      : Stream.of(permission))
          .collect(ImmutableSet.toImmutableSet());
    }

    private static String join(String first, String second) {
      return join(first, second, false);
    }

    private static String join(String first, String second, boolean separator) {
      return first + (separator ? "::" : ":") + second;
    }

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  String SCOPE_DISCOVER = "discover";
  Tuple<Scope, String> SCOPE_DISCOVER_READ = Tuple.of(Scope.READ, SCOPE_DISCOVER);
  String ROLE_PUBLIC = "public";

  /**
   * @langEn Option to disable access control.
   * @langDe Option, um die Absicherung zu deaktivieren.
   * @default true
   * @since v3.3
   */
  @Nullable
  Boolean getEnabled();

  /**
   * @langEn *Deprecated, see `roles'.* List of permissions that every user possesses, if
   *     authenticated or not.
   * @langDe *Deprecated, siehe `roles'.* Liste der Berechtigungen, die jeder Benutzer besitzt, ob
   *     angemeldet oder nicht.
   * @default [read]
   * @since v3.3
   */
  @Deprecated(since = "3.5")
  Set<String> getPublicScopes();

  /**
   * @langEn Definition of roles, the key is the role name, the value a list of scopes and/or
   *     permissions. The role `public` defines the list of permissions that every user possesses,
   *     if authenticated or not.
   * @langDe Definition von Rollen, der Key ist der Name der Rolle, der Werte eine Liste von
   *     Berechtigungen. Die Rolle `public` definiert die Liste der Berechtigungen, die jeder
   *     Benutzer besitzt, ob angemeldet oder nicht.
   * @default {public: [read]}
   * @since v3.5
   */
  Map<String, Set<String>> getRoles();

  @JsonIgnore
  @Value.Derived
  default boolean isEnabled() {
    return !Objects.equals(getEnabled(), false);
  }

  @Value.Check
  default ApiSecurity backwardsCompatibility() {
    if (!getPublicScopes().isEmpty()) {
      Map<String, Set<String>> roles = new LinkedHashMap<>(getRoles());
      Set<String> rolesPublic = new LinkedHashSet<>(getRoles().getOrDefault(ROLE_PUBLIC, Set.of()));
      rolesPublic.addAll(getPublicScopes());
      roles.put(ROLE_PUBLIC, rolesPublic);

      return new ImmutableApiSecurity.Builder()
          .from(this)
          .roles(roles)
          .publicScopes(Set.of())
          .build();
    }
    return this;
  }

  default boolean isRestricted(Set<String> permissions) {
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
