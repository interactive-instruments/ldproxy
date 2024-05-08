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
import de.ii.xtraplatform.tiles.domain.JacksonXmlAnnotation.XmlIgnore;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonPropertyOrder({
  "title",
  "abstract",
  "keywords",
  "identifier",
  "scaleDenominator",
  "topLeftCorner",
  "tileWidth",
  "tileHeight",
  "matrixWidth",
  "matrixHeight"
})
public interface WmtsTileMatrix {

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Title")
  Optional<String> getTitle();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Abstract")
  Optional<String> getAbstract();

  @JacksonXmlElementWrapper(namespace = "http://www.opengis.net/ows/1.1", localName = "Keywords")
  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Keyword")
  List<String> getKeywords();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/ows/1.1", localName = "Identifier")
  String getIdentifier();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "ScaleDenominator")
  Double getScaleDenominator();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "TopLeftCorner")
  @Value.Derived
  @Value.Auxiliary
  default String getTopLeftCorner() {
    return String.join(
        " ", Arrays.stream(getTopLeftCornerValues()).map(String::valueOf).toArray(String[]::new));
  }

  @XmlIgnore
  BigDecimal[] getTopLeftCornerValues();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "TileWidth")
  long getTileWidth();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "TileHeight")
  long getTileHeight();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "MatrixWidth")
  long getMatrixWidth();

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "MatrixHeight")
  long getMatrixHeight();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<WmtsTileMatrix> FUNNEL =
      (from, into) -> {
        from.getTitle().ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
        from.getAbstract().ifPresent(title -> into.putString(title, StandardCharsets.UTF_8));
        from.getKeywords().stream()
            .sorted()
            .forEachOrdered(val -> into.putString(val, StandardCharsets.UTF_8));
        into.putString(from.getIdentifier(), StandardCharsets.UTF_8);
        into.putDouble(from.getScaleDenominator());
        into.putString(from.getTopLeftCorner(), StandardCharsets.UTF_8);
        into.putLong(from.getTileWidth());
        into.putLong(from.getTileHeight());
        into.putLong(from.getMatrixWidth());
        into.putLong(from.getMatrixHeight());
      };
}
