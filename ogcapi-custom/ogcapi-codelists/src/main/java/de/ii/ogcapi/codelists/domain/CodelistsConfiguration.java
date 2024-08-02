/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import org.immutables.value.Value;

/**
 * @buildingBlock CODELISTS
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: CODELISTS
 *   enabled: true
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "CODELISTS")
@JsonDeserialize(builder = ImmutableCodelistsConfiguration.Builder.class)
public interface CodelistsConfiguration extends ExtensionConfiguration, CachingConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableCodelistsConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCodelistsConfiguration.Builder builder =
        ((ImmutableCodelistsConfiguration.Builder) source.getBuilder()).from(source).from(this);

    return builder.build();
  }
}
