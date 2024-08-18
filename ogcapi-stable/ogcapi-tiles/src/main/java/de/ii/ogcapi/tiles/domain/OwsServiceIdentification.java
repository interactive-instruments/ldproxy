/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
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
public interface OwsServiceIdentification {

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "Title")
  Optional<String> getTitle();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "Abstract")
  Optional<String> getAbstract();

  @JacksonXmlElementWrapper(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "Keywords")
  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "Keyword")
  List<String> getKeywords();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "ServiceType")
  default String getServiceType() {
    return "OGC WMTS";
  }

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "ServiceTypeVersion")
  default String getServiceTypeVersion() {
    return "1.0.0";
  }

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "Fees")
  Optional<String> getFees();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "AccessConstraints")
  Optional<String> getAccessConstraints();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<OwsServiceIdentification> FUNNEL =
      (from, into) -> {
        from.getTitle().ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
        from.getAbstract().ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
        from.getKeywords().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getFees().ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
        from.getAccessConstraints()
            .ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
      };
}
