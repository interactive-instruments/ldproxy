/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock RESOURCES
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: RESOURCES
 *   enabled: true
 *   managerEnabled: true
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "RESOURCES")
@JsonDeserialize(builder = ImmutableResourcesConfiguration.Builder.class)
public interface ResourcesConfiguration extends ExtensionConfiguration, CachingConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn Controls whether the resources should be able to be created and deleted via PUT and
   *     DELETE through the API.
   * @langDe Steuert, ob die Ressourcen über PUT und DELETE über die API erzeugt und gelöscht werden
   *     können sollen.
   * @default false
   */
  @Nullable
  Boolean getManagerEnabled();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isManagerEnabled() {
    return Objects.equals(getManagerEnabled(), true);
  }

  @Override
  default Builder getBuilder() {
    return new ImmutableResourcesConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableResourcesConfiguration.Builder builder =
        ((ImmutableResourcesConfiguration.Builder) source.getBuilder()).from(source).from(this);

    return builder.build();
  }
}
