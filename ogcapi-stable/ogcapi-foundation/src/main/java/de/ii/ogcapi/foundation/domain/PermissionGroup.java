/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ApiSecurity.ScopeGranularity;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.immutables.value.Value;

@Value.Immutable
public interface PermissionGroup {

  static PermissionGroup of(Base base, String group, String description) {
    return ImmutablePermissionGroup.of(base, group, description);
  }

  @Value.Parameter
  Base base();

  @Value.Parameter
  String group();

  @Value.Parameter
  String description();

  @Value.Derived
  default String name() {
    return base().with(group());
  }

  @Value.Derived
  default Set<String> setOf() {
    return base().setOf(group());
  }

  default Set<String> setOf(Set<ScopeGranularity> scopeGranularities) {
    Set<String> scopes = new HashSet<>();

    if (scopeGranularities.contains(ScopeGranularity.BASE)) {
      scopes.add(base().toString());
    }
    if (scopeGranularities.contains(ScopeGranularity.PARENT)) {
      scopes.add(group());
    }
    if (scopeGranularities.contains(ScopeGranularity.MAIN)) {
      scopes.add(base().with(group()));
    }

    return scopes;
  }

  default Set<String> setOf(String operation) {
    return base().setOf(group(), operation);
  }

  default Set<String> setOf(String operation, String api) {
    return base().setOf(group(), operation, api);
  }

  default Set<String> setOf(String operation, String api, String collection) {
    return base().setOf(group(), operation, api, collection);
  }

  enum Base {
    READ,
    WRITE;

    public String with(String group) {
      return join(group, toString());
    }

    public String with(String group, String operation) {
      return join(group, operation);
    }

    public Set<String> setOf() {
      return ImmutableSet.of(this.toString());
    }

    public Set<String> setOf(String group) {
      return ImmutableSet.of(this.toString(), group, with(group));
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
}
