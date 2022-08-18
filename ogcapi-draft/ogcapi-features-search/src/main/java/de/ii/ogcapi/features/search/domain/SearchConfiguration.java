/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @langEn TODO
 * @langDe TODO
 * @example <code>
 * ```yaml
 * - buildingBlock: SEARCH
 *   enabled: true
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableSearchConfiguration.Builder.class)
public interface SearchConfiguration extends ExtensionConfiguration, CachingConfiguration {

  /**
   * @langEn Option to manage stored queries using PUT and DELETE.
   * @langDe Steuert, ob Stored Queries über PUT und DELETE verwaltet werden können.
   * @default `false`
   */
  @Nullable
  Boolean getManagerEnabled();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isManagerEnabled() {
    return Objects.equals(getManagerEnabled(), true);
  }

  /**
   * @langEn Option to validate stored queries when using PUT by setting a `Prefer` header with
   *     `handling=strict`.
   * @langDe Steuert, ob bei PUT von Stored Queries die Validierung über den Header `Prefer` (Wert
   *     `handling=strict`) unterstützt werden soll.
   * @default `false`
   */
  @Nullable
  Boolean getValidationEnabled();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isValidationEnabled() {
    return Objects.equals(getValidationEnabled(), true);
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableSearchConfiguration.Builder().from(this);
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableSearchConfiguration.Builder builder =
        new ImmutableSearchConfiguration.Builder().from(source).from(this);

    return builder.build();
  }
}
