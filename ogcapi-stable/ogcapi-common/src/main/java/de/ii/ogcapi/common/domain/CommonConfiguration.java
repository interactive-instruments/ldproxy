/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.foundation.domain.Caching;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.xtraplatform.docs.DocMarker;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @example <code>
 * ```yaml
 * - buildingBlock: COMMON
 *   additionalLinks:
 *   - rel: describedby
 *     type: text/html
 *     title: Webseite mit weiteren Informationen
 *     href: 'https://example.com/pfad/zu/dokument'
 *     hreflang: de
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCommonConfiguration.Builder.class)
public interface CommonConfiguration extends ExtensionConfiguration, CachingConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn Always `COMMON`.
   * @langDe Immer `COMMON`.
   */
  @Override
  @JsonAlias("extensionType")
  @Value.Derived
  default String getBuildingBlock() {
    return ExtensionConfiguration.super.getBuildingBlock();
  }

  /**
   * @default true
   */
  @Nullable
  @Override
  Boolean getEnabled();

  /**
   * @since v3.0
   */
  @Nullable
  @Override
  Caching getCaching();

  /**
   * @langEn Add additional links to the *Landing Page* resource. The value is an array of link objects.
   * Required properties of a link are a URI (`href`), a label (`label`) and a relation (`rel`).
   * @langDe Erlaubt es, zusätzliche Links in der Landing Page zu ergänzen. Der Wert ist ein Array von
   * Link-Objekten. Anzugeben sind jeweils mindestens die URI (`href`), der anzuzeigende Text
   * (`label`) und die Link-Relation (`rel`).
   * @default []
   */
  @JsonMerge(OptBoolean.FALSE)
  List<Link> getAdditionalLinks();

  @Override
  default Builder getBuilder() {
    return new ImmutableCommonConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCommonConfiguration.Builder builder =
        new ImmutableCommonConfiguration.Builder().from(source).from(this);

    List<Link> links = Lists.newArrayList(((CommonConfiguration) source).getAdditionalLinks());
    getAdditionalLinks()
        .forEach(
            link -> {
              if (!links.contains(link)) {
                links.add(link);
              }
            });
    builder.additionalLinks(links);

    return builder.build();
  }
}
