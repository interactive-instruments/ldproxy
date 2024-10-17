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
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonPropertyOrder({"individualName", "positionName", "role", "contactInfo"})
public interface OwsResponsibleParty {

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "IndividualName")
  Optional<String> getIndividualName();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "ContactInfo")
  Optional<OwsContact> getContactInfo();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<OwsResponsibleParty> FUNNEL =
      (from, into) -> {
        from.getIndividualName().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getContactInfo().ifPresent(val -> OwsContact.FUNNEL.funnel(val, into));
      };
}
