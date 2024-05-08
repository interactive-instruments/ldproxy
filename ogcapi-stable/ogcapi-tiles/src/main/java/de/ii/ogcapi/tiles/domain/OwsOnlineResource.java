/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonPropertyOrder({
  "title",
  "abstract",
  "keywords",
  "serviceType",
  "serviceTypeVersion",
  "fees",
  "accessConstraints"
})
public interface OwsOnlineResource {

  @JacksonXmlProperty(
      namespace = "http://www.w3.org/1999/xlink",
      localName = "href",
      isAttribute = true)
  String getHref();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<OwsOnlineResource> FUNNEL =
      (from, into) -> {
        into.putString(from.getHref(), StandardCharsets.UTF_8);
      };
}
