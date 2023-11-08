/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import java.util.Set;
import org.immutables.value.Value;

/**
 * @buildingBlock SCHEMA
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: SCHEMA
 *   enabled: true
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "SCHEMA")
@JsonDeserialize(builder = ImmutableSchemaConfiguration.Builder.class)
public interface SchemaConfiguration extends ExtensionConfiguration, CachingConfiguration {

  /**
   * @langEn **Deprecated** List of enabled JSON Schema versions. Supported are 2020-12 (`V202012`),
   *     2019-09 (`V201909`) and 07 (`V7`). The draft specification only supports 2020-12.
   * @langDe **Deprecated** Steuert, welche JSON-Schema-Versionen unterstützt werden sollen. Zur
   *     Verfügung stehen 2020-12 (`V202012`), 2019-09 (`V201909`) und 07 (`V7`). Der Entwurf der
   *     Spezifikation unterstützt nur 2020-12.
   * @default [ "V202012" ]
   */
  @Deprecated(since = "3.6")
  Set<JsonSchemaDocument.VERSION> getVersions();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableSchemaConfiguration.Builder();
  }
}
