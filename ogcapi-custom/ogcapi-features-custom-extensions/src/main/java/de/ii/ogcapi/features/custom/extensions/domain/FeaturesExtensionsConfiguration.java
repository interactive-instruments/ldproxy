/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.custom.extensions.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock FEATURES_EXTENSIONS
 * @exampleAll <code>
 * ```yaml
 * - buildingBlock: FEATURES_EXTENSIONS
 *   enabled: true
 *   postOnItems: true
 *   intersectsParameter: true
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFeaturesExtensionsConfiguration.Builder.class)
public interface FeaturesExtensionsConfiguration extends ExtensionConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn Enables support for the POST HTTP method on the "Features" resource.
   * @langDe Aktiviert die Unterstützung für die HTTP-Methode POST auf der Ressource "Features"
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getPostOnItems();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean shouldSupportPostOnItems() {
    return Objects.equals(getPostOnItems(), true);
  }

  /**
   * @langEn Enables support for the `intersects` query parameter on the "Features" resource
   * @langDe Aktiviert die Unterstützung für den Query-Parameter `intersects` auf der Ressource
   *     "Features"
   * @default false
   * @since v3.1
   */
  @Nullable
  Boolean getIntersectsParameter();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean shouldSupportIntersectsParameter() {
    return Objects.equals(getIntersectsParameter(), true);
  }

  @Override
  default Builder getBuilder() {
    return new ImmutableFeaturesExtensionsConfiguration.Builder();
  }
}
