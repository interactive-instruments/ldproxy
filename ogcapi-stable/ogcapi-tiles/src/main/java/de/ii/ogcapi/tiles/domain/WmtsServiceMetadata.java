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
@JacksonXmlRootElement(namespace = "http://www.opengis.net/wmts/1.0", localName = "Capabilities")
@JsonPropertyOrder({"serviceIdentification", "serviceProvider", "contents", "serviceMetadataURL"})
public interface WmtsServiceMetadata {

  @JacksonXmlProperty(
      isAttribute = true,
      namespace = "http://www.w3.org/2001/XMLSchema-instance",
      localName = "schemaLocation")
  default String getSchemaLocation() {
    return "http://www.opengis.net/wmts/1.0 http://schemas.opengis.net/wmts/1.0/wmtsGetCapabilities_response.xsd";
  }

  @JacksonXmlProperty(isAttribute = true, localName = "version")
  default String getVersion() {
    return "1.0.0";
  }

  @JacksonXmlProperty(
      namespace = "http://www.opengis.net/ows/1.1",
      localName = "ServiceIdentification")
  Optional<OwsServiceIdentification> getServiceIdentification();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "ServiceProvider")
  Optional<OwsServiceProvider> getServiceProvider();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "Contents")
  Optional<WmtsContents> getContents();

  @JacksonXmlProperty(
      namespace = "http://www.opengis.net/wmts/1.0",
      localName = "ServiceMetadataURL")
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
