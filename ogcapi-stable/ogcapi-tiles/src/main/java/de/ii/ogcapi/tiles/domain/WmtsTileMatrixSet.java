/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface WmtsTileMatrixSet {

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Title")
  Optional<String> getTitle();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Abstract")
  Optional<String> getAbstract();

  @JacksonXmlElementWrapper(namespace = "http://www.opengis.net/ows/1.1", localName = "Keywords")
  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Keyword")
  List<String> getKeywords();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Identifier")
  String getIdentifier();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "SupportedCRS")
  String getSupportedCRS();

  @JacksonXmlProperty(
      namespace = "http://www.opengis.net/wmts/1.0",
      localName = "WellKnownScaleSet")
  Optional<String> getWellKnownScaleSet();

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "TileMatrix")
  List<WmtsTileMatrix> getTileMatrix();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<WmtsTileMatrixSet> FUNNEL =
      (from, into) -> {
        from.getTitle().ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
        from.getAbstract().ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
        from.getKeywords().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        into.putString(from.getIdentifier(), StandardCharsets.UTF_8);
        into.putString(from.getSupportedCRS(), StandardCharsets.UTF_8);
        from.getWellKnownScaleSet().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getTileMatrix().forEach(val -> WmtsTileMatrix.FUNNEL.funnel(val, into));
      };
}
