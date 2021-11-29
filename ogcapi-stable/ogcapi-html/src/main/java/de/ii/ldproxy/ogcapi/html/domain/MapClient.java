/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.domain;

import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
public interface MapClient {

  enum Type {MAP_LIBRE, OPEN_LAYERS, CESIUM}

  enum Popup {HOVER_ID, CLICK_PROPERTIES}

  @Value.Default
  default Type getType() {
    return Type.MAP_LIBRE;
  }

  Optional<String> getStyleUrl();

  Optional<String> getBackgroundUrl();

  Optional<Source> getData();

  Optional<String> getAttribution();

  @Value.Default
  default boolean getSavePosition()  { return false; }

  Optional<Map<String, String>> getBounds();

  @Value.Default
  default boolean drawBounds() { return false; }

  @Value.Default
  default boolean isInteractive() {
    return true;
  }

  @Value.Default
  default MapClient.Style getDefaultStyle() {
    return new ImmutableStyle.Builder().build();
  }

  @Value.Default
  default boolean getRemoveZoomLevelConstraints() {
    return false;
  }

  Optional<Popup> getPopup();

  @Value.Lazy
  default boolean isMapLibre() {
    return getType() == Type.MAP_LIBRE;
  }

  @Value.Immutable
  interface Source {
    enum TYPE {geojson, vector}

    TYPE getType();

    String getUrl();

    Multimap<String, List<String>> getLayers();
  }

  @Value.Immutable
  interface Style {

    @Value.Default
    default String getColor() {
      return "#1D4E89";
    }

    @Value.Default
    default double getOpacity() {
      return 1.0;
    }

    @Value.Default
    default int getCircleRadius() {
      return 8;
    }

    @Value.Default
    default int getLineWidth() {
      return 4;
    }

    @Value.Default
    default double getFillOpacity() {
      return 0.2;
    }

    @Value.Default
    default int getOutlineWidth() {
      return 2;
    }
  }
}
