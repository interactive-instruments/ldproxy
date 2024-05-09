/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock OAS30
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: OAS30
 *   enabled: false
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "OAS30")
@JsonDeserialize(builder = ImmutableOas30Configuration.Builder.class)
public interface Oas30Configuration extends ExtensionConfiguration {

  /**
   * @default true
   */
  @Nullable
  @Override
  Boolean getEnabled();

  /**
   * @langEn In the HTML view of the OpenAPI definition, schema constraints except `enum` and
   *     `default` values are not displayed. Set this to `true` to display also `minimum`,
   *     `maximum`, `pattern`, `minLength`, and `maxLength` constraints.
   * @langDe In der HTML-Ansicht der OpenAPI-Definition werden Schemabeschränkungen mit Ausnahme von
   *     `enum`- und `default`-Werten nicht angezeigt. Setzen Sie dies auf `true`, um auch die
   *     Einschränkungen `minimum`, `maximum`, `pattern`, `minLength` und `maxLength` anzuzeigen.
   * @default false
   */
  @Nullable
  Boolean getShowSchemaConstraintsInHtml();

  @Value.Derived
  @Value.Auxiliary
  @DocIgnore
  default boolean isShowSchemaConstraintsInHtml() {
    return getShowSchemaConstraintsInHtml() != null && getShowSchemaConstraintsInHtml();
  }

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableOas30Configuration.Builder();
  }
}
