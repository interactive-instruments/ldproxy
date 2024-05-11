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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.hash.Funnel;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JacksonXmlRootElement(namespace = WmtsServiceMetadata.XMLNS, localName = "Capabilities")
@JsonPropertyOrder({"serviceIdentification", "serviceProvider", "contents", "serviceMetadataURL"})
public interface WmtsServiceMetadata {

  String XMLNS = "http://www.opengis.net/wmts/1.0";
  String XMLNS_OWS = "http://www.opengis.net/ows/1.1";
  String XMLNS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
  String XMLNS_XLINK = "http://www.w3.org/1999/xlink";

  @JacksonXmlProperty(isAttribute = true, namespace = XMLNS_XSI, localName = "schemaLocation")
  default String getSchemaLocation() {
    return String.format(
        "%s %s", XMLNS, "http://schemas.opengis.net/wmts/1.0/wmtsGetCapabilities_response.xsd");
  }

  @JacksonXmlProperty(isAttribute = true, localName = "version")
  default String getVersion() {
    return "1.0.0";
  }

  @JacksonXmlProperty(
      namespace = WmtsServiceMetadata.XMLNS_OWS,
      localName = "ServiceIdentification")
  Optional<OwsServiceIdentification> getServiceIdentification();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS_OWS, localName = "ServiceProvider")
  Optional<OwsServiceProvider> getServiceProvider();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS, localName = "Contents")
  Optional<WmtsContents> getContents();

  @JacksonXmlProperty(namespace = WmtsServiceMetadata.XMLNS, localName = "ServiceMetadataURL")
  OwsOnlineResource getServiceMetadataURL();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<WmtsServiceMetadata> FUNNEL =
      (from, into) -> {
        OwsOnlineResource.FUNNEL.funnel(from.getServiceMetadataURL(), into);
        from.getServiceIdentification()
            .ifPresent(val -> OwsServiceIdentification.FUNNEL.funnel(val, into));
        from.getServiceProvider().ifPresent(val -> OwsServiceProvider.FUNNEL.funnel(val, into));
        from.getContents().ifPresent(val -> WmtsContents.FUNNEL.funnel(val, into));
      };
}
