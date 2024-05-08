/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.hash.Funnel;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JacksonXmlRootElement(namespace = "http://www.opengis.net/wmts/1.0", localName = "Capabilities")
public interface WmtsServiceMetadata {

  @JacksonXmlProperty(isAttribute = true, localName = "version")
  default String getVersion() {
    return "1.0.0";
  }

  @JacksonXmlProperty(
      namespace = "http://www.opengis.net/wmts/1.0",
      localName = "ServiceMetadataURL")
  OwsOnlineResource getServiceMetadataURL();

  @JacksonXmlProperty(
      namespace = "http://www.opengis.net/ows/1.1",
      localName = "ServiceIdentification")
  Optional<OwsServiceIdentification> getServiceIdentification();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "ServiceProvider")
  Optional<OwsServiceProvider> getServiceProvider();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "Contents")
  Optional<WmtsContents> getContents();

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
