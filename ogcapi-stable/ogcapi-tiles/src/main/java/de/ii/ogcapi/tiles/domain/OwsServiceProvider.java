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
@JsonPropertyOrder({"providerName", "providerSite", "serviceContact"})
public interface OwsServiceProvider {

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "ProviderName")
  String getProviderName();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "ProviderSite")
  Optional<OwsOnlineResource> getProviderSite();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "ServiceContact")
  OwsResponsibleParty getServiceContact();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<OwsServiceProvider> FUNNEL =
      (from, into) -> {
        into.putString(from.getProviderName(), StandardCharsets.UTF_8);
        from.getProviderSite().ifPresent(val -> OwsOnlineResource.FUNNEL.funnel(val, into));
      };
}
