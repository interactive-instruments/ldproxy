/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface WmtsResourceURL {

  @JacksonXmlProperty(isAttribute = true, localName = "format")
  String getFormat();

  @JacksonXmlProperty(isAttribute = true, localName = "resourceType")
  String getResourceType();

  @JacksonXmlProperty(isAttribute = true, localName = "template")
  String getTemplate();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<WmtsResourceURL> FUNNEL =
      (from, into) -> {
        into.putString(from.getFormat(), StandardCharsets.UTF_8);
        into.putString(from.getResourceType(), StandardCharsets.UTF_8);
        into.putString(from.getTemplate(), StandardCharsets.UTF_8);
      };
}
