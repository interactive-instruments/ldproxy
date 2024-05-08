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
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface WmtsTileMatrixSetLink {

  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "TileMatrixSet")
  String getTileMatrixSet();

  @JacksonXmlElementWrapper(
      namespace = "http://www.opengis.net/wmts/1.0",
      localName = "TileMatrixSetLimits")
  @JacksonXmlProperty(namespace = "http://www.opengis.net/wmts/1.0", localName = "TileMatrixLimits")
  List<TileMatrixSetLimits> getTileMatrixSetLimits();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<WmtsTileMatrixSetLink> FUNNEL =
      (from, into) -> {
        into.putString(from.getTileMatrixSet(), StandardCharsets.UTF_8);
        from.getTileMatrixSetLimits().forEach(val -> TileMatrixSetLimits.FUNNEL.funnel(val, into));
      };
}
