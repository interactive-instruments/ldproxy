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
import de.ii.ogcapi.foundation.domain.PermissionGroup.Base;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn Access control for all API operations (combination of endpoint and HTTP method).
 *     <p>#### Permissions
 *     <p>Access control is based on permissions, predefined permission groups and custom permission
 *     groups. These **predefined permission groups** are available:
 *     <p><code>
 * - `discover`: access API landing pages, conformance declarations and OpenAPI definitions
 * - `collections:read`: access feature collection metadata
 * - `collections`: includes `collections:read`
 * - `data:read`: access and query features
 * - `data:write`: mutate features
 * - `data`: includes `data:read` and `data:write`
 * - `tiles:read`: access tiles
 * - `tiles`: includes `tiles:read`
 * - `styles:read`: access styles and their metadata
 * - `styles:write`: mutate styles and update their metadata
 * - `styles`: includes `styles:read` and `styles:write`
 * - `resources:read`: access file resources
 * - `resources:write`: mutate file resources
 * - `resources`: includes `resources:read` and `resources:write`
 * - `search:read`: access stored queries and their parameters
 * - `search:write`: mutate stored queries
 * - `search`: includes `search:read` and `search:write`
 * - `routes:read`: access stored routes and their definition
 * - `routes:write`: compute and store routes, delete stored routes
 * - `routes`: includes `routes:read` and `routes:write`
 * - `read`: includes `discover`, `collections:read`, `data:read`, `tiles:read`, `styles:read`, `resources:read`, `search:read` and `routes:read`
 * - `write`: includes `data:write`, `styles:write`, `resources:write`, `search:write` and `routes:write`
 * </code>
 *     <p>**Permissions** are a combination of a group prefix and an OpenAPI operation id (without
 *     any prefix), for example `data:getItems` or `tiles:getTile`. These can be used for a more
 *     fine-grained control.
 *     <p>**Custom permission groups** are defined in `groups`, they may contain permissions and/or
 *     predefined permission groups.
 *     <p>**Data-specific permissions**
 *     <p>The permissions described above will permit access to any API and collection. To restrict
 *     the access to specific APIs or collections, a suffix can be added to permission groups and
 *     permissions, for example `read::daraa` or `data:getItems::daraa:AeronauticSrf`.
 *     <p>**Permission group `public`**
 *     <p>The special permission group `public` defines the list of permissions and/or predefined
 *     permission groups that every user possesses, if authenticated or not.
 *     <p>#### Authentication and authorization
 *     <p>To support authenticated users, a bearer token has to be included in the `Authorization`
 *     header in requests to the API. Validation and evaluation of these tokens has to be configured
 *     in the [global configuration](../application/70-reference.md).
 *     <p>To determine if a user is authorized to perform the requested operation, the following
 *     steps are executed:
 *     <p><code>
 * 1. If the operation is covered by the `public` group, authorization is granted, even if no token or an invalid token were provided.
 * 2. If no token or an invalid token (wrong signature or expired) are provided, authorization is rejected.
 * 3. If 'audience' is non-empty and does not intersect the audience claim of the provided token, authorization is rejected.
 * 4. If 'scopes' is non-empty and the scope claim of the provided token does not contain at least one permission group that covers
 *    the requested operation, authorization is rejected.
 * 5. If the permissions claim of the provided token does not contain at least one permission, predefined permission group or custom
 *    permission group that covers the requested operation, authorization is rejected.
 * </code>
 * @langDe Absicherung für alle API Operationen (Kombination aus Endpunkt und HTTP-Methode).
 *     <p>#### Berechtigungen
 *     <p>Die Absicherung basiert auf Berechtigungen, vordefinierten Berechtigungsgruppen und
 *     benutzerdefinierten Berechtigungsgruppen. Diese **vordefinierten Berechtigungsgruppen** sind
 *     verfügbar:
 *     <p><code>
 * - `discover`: Lesen von API Landing Pages, Conformance Declarations und OpenAPI Definitionen
 * - `collections:read`: Lesen von Metadaten zu Feature Collections
 * - `collections`: enthält `collections:read`
 * - `data:read`: Lesen und Abfragen von Features
 * - `data:write`: Ändern von Features
 * - `data`: enthält `data:read` und `data:write`
 * - `tiles:read`: Lesen von Tiles
 * - `tiles`: enthält `tiles:read`
 * - `styles:read`: Lesen von Styles und deren Metadaten
 * - `styles:write`: Ändern von Styles und deren Metadaten
 * - `styles`: enthält `styles:read` und `styles:write`
 * - `resources:read`: Lesen von Dateiressourcen
 * - `resources:write`: Ändern von Dateiressourcen
 * - `resources`: enthält `resources:read` und `resources:write`
 * - `search:read`: Lesen von Stored Queries und deren Parameter
 * - `search:write`: Ändern von Stored Queries
 * - `search`: enthält `search:read` und `search:write`
 * - `routes:read`: Lesen von gespeicherten Routen und deren Definition
 * - `routes:write`: Berechnen und Speichern von Routen, Löschen gespeicherter Routen
 * - `routes`: enthält `routes:read` und `routes:write`
 * - `read`: enthält `discover`, `collections:read`, `data:read`, `tiles:read`, `styles:read`, `resources:read`, `search:read` und `routes:read`
 * - `write`: enthält `data:write`, `styles:write`, `resources:write`, `search:write` und `routes:write`
 * </code>
 *     <p>**Berechtigungen** sind eine Kombination aus Gruppen-Prefix und einer OpenAPI Operation-Id
 *     (ohne jeglichen Prefix), z.B. `data:getItems` oder `tiles:getTile`. Diese können für eine
 *     fein-granularere Absicherung verwendet werden.
 *     <p>**Benutzerdefinierte Berechtigungsgruppen** werden in `groups` definiert, sie können
 *     Berechtigungen und/oder vordefinierte Berechtigungsgruppen enthalten.
 *     <p>**Daten-spezifische Berechtigungen**
 *     <p>Die oben beschriebenen Berechtigungen gewähren Zugriff zu jeder API und Collection. Um den
 *     Zugriff auf bestimmte APIs oder Collections einzuschränken, kann ein Suffix zu
 *     Berechtigungsgruppen und Berechtigungen hinzugefügt werden, z.B. `read::daraa` oder
 *     `data:getItems::daraa:AeronauticSrf`.
 *     <p>**Berechtigungsgruppe `public`**
 *     <p>Die spezielle Berechtigungsgruppe `public` definiert die Liste der Berechtigungen und/oder
 *     vordefinierten Berechtigungsgruppen, die jeder Benutzer besitzt, ob angemeldet oder nicht.
 *     <p>#### Authentifizierung and Autorisierung
 *     <p>Um authentifizierte Benutzer zu unterstützen, muss ein Bearer-Token im
 *     `Authorization`-Header in Anfragen an die API inkludiert werden. Die Validierung und
 *     Auswertung dieser Tokens muss in der [globalen Konfiguration](../application/70-reference.md)
 *     konfiguriert werden.
 *     <p>Um zu bestimmen ob ein Benutzer autorisiert ist die angefragte Operation auszuführen,
 *     werden die folgenden Schritte durchgeführt:
 *     <p><code>
 * 1. Wenn die Operation von der `public`-Gruppe abgedeckt ist, wird die Autorisierung gewährt, auch wenn kein Token oder ein invalides
 *    Token bereitgestellt wurden.
 * 2. Wenn kein Token oder ein invalides Token (falsche Signatur oder abgelaufen) bereitgestellt wurden, wird die Autorisierung verweigert.
 * 3. Wenn 'audience' nicht leer ist und sich nicht mit dem Audience-Claim des gegebenen Tokens überschneidet, wird die Autorisierung verweigert.
 * 4. Wenn 'scopes' nicht leer ist und der Scope-Claim des gegebenen Tokens nicht mindestens eine Berechtigungsgruppe enthält, die die Operation
 *    abdeckt, wird die Autorisierung verweigert.
 * 5. Wenn der Permissions-Claim des gegebenen Tokens nicht mindestens eine Berechtigung, vordefinierte
 *    Berechtigungsgruppe oder benutzerdefinierte Berechtigungsgruppe enthält, die die Operation
 *    abdeckt, wird die Autorisierung verweigert.
 * </code>
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableApiSecurity.Builder.class)
public interface ApiSecurity {

  enum ScopeGranularity {
    NONE,
    BASE,
    PARENT,
    GROUP,
    CUSTOM
  }

  String GROUP_DISCOVER = "discover";
  PermissionGroup GROUP_DISCOVER_READ =
      PermissionGroup.of(
          Base.READ,
          GROUP_DISCOVER,
          "access API landing pages, conformance declarations and OpenAPI definitions");
  String GROUP_PUBLIC = "public";

  /**
   * @langEn Option to disable access control.
   * @langDe Option, um die Absicherung zu deaktivieren.
   * @default true
   * @since v3.3
   */
  @Nullable
  Boolean getEnabled();

  /**
   * @langEn *Deprecated, see `groups'.* List of permissions that every user possesses, if
   *     authenticated or not.
   * @langDe *Deprecated, siehe `groups'.* Liste der Berechtigungen, die jeder Benutzer besitzt, ob
   *     angemeldet oder nicht.
   * @default [read]
   * @since v3.3
   */
  @Deprecated(since = "3.5")
  Set<String> getPublicScopes();

  /**
   * @langEn Definition of custom permission groups, the key is the group name, the value a list of
   *     permissions and/or predefined permission groups. The group `public` defines the list of
   *     permissions that every user possesses, if authenticated or not.
   * @langDe Definition von benutzerdefinierten Berechtigungsgruppen, der Key ist der Name der
   *     Gruppe, der Werte eine Liste von Berechtigungen und/oder vordefinierten
   *     Berechtigungsgruppen. Die Gruppe `public` definiert die Liste der Berechtigungen, die jeder
   *     Benutzer besitzt, ob angemeldet oder nicht.
   * @default {public: [read]}
   * @since v3.5
   */
  Map<String, Set<String>> getGroups();

  /**
   * @langEn If non-empty, *OAuth2 Scopes* are added to the OpenAPI definition. Then only tokens
   *     that contain at least one scope that covers the requested operation are accepted. Values
   *     can be any combination of `BASE` (e.g. `read`), `PARENT` (e.g. `data`), `GROUP` (e.g.
   *     `data:read`) and `CUSTOM` (everything defined in `groups` besides `public`).
   * @langDe Wenn nicht leer, werden *OAuth2 Scopes* zur OpenAPI Definition hinzugefügt. Dann werden
   *     nur Tokens akzeptiert, die mindestens einen Scope enthalten, der die angeforderte Operation
   *     abdeckt. Werte können jede Kombination von `BASE` (z.B. `read`), `PARENT` (z.B. `data`),
   *     `GROUP` (z.B. `data:read`) und `CUSTOM` (alles in `groups` definierte außer `public`) sein.
   * @default []
   * @since v3.5
   */
  Set<ScopeGranularity> getScopes();

  /**
   * @langEn If non-empty, only tokens that contain at least one of the given values in the audience
   *     claim are accepted.
   * @langDe Wenn nicht leer, werden nur Tokens akzeptiert, die mindestens einen der gegebenen Werte
   *     im Audience-Claim enthalten.
   * @default []
   * @since v3.5
   */
  Set<String> getAudience();

  @JsonIgnore
  @Value.Derived
  default boolean isEnabled() {
    return !Objects.equals(getEnabled(), false);
  }

  @Value.Check
  default ApiSecurity backwardsCompatibility() {
    if (!getPublicScopes().isEmpty()) {
      Map<String, Set<String>> groups = new LinkedHashMap<>(getGroups());
      Set<String> groupPublic =
          new LinkedHashSet<>(getGroups().getOrDefault(GROUP_PUBLIC, Set.of()));
      groupPublic.addAll(getPublicScopes());
      groups.put(GROUP_PUBLIC, groupPublic);

      return new ImmutableApiSecurity.Builder()
          .from(this)
          .groups(groups)
          .publicScopes(Set.of())
          .build();
    }
    return this;
  }

  default boolean isRestricted(Set<String> permissions) {
    return isEnabled()
        && Sets.intersection(getGroups().getOrDefault(GROUP_PUBLIC, Set.of()), permissions)
            .isEmpty();
  }

  default Set<String> getGroupsWith(Set<String> permissions) {
    return getGroups().entrySet().stream()
        .filter(group -> !Sets.intersection(group.getValue(), permissions).isEmpty())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }
}
