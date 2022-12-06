/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.OgcApiView;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public abstract class StyleView extends OgcApiView {
  public abstract String styleUrl();

  public abstract String layerIds();

  public Map<String, String> bbox() {
    return spatialExtent()
        .map(
            bbox ->
                ImmutableMap.of(
                    "minLng", Double.toString(bbox.getXmin()),
                    "minLat", Double.toString(bbox.getYmin()),
                    "maxLng", Double.toString(bbox.getXmax()),
                    "maxLat", Double.toString(bbox.getYmax())))
        .orElse(null);
  }

  public abstract MapClient mapClient();

  public abstract OgcApiDataV2 apiData();

  public abstract Optional<BoundingBox> spatialExtent();

  public abstract String styleId();

  public abstract boolean popup();

  public abstract boolean layerControl();

  public abstract Map<String, Collection<String>> layerMap();

  public Map<String, String> getBbox() {
    return bbox();
  }

  public StyleView() {
    super("/templates/style.template");
  }
}
