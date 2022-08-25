/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CesiumData {

  public final List<FeatureHtml> features;
  private final boolean clampToGround;
  public final String encodedUrl;
  private Double minLon = null;
  private Double maxLon = null;
  private Double minLat = null;
  private Double maxLat = null;
  private Double minHeight = null;
  private Double maxHeight = null;

  public CesiumData(List<FeatureHtml> features, String url) {
    this.features = features;
    this.clampToGround = true; // TODO make configurable
    this.encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
  }

  public String getFeatureExtent() {
    determineBbox();
    return "Rectangle.fromDegrees(" + minLon + "," + minLat + "," + maxLon + "," + maxLat + ")";
  }

  public String getEncodedRegion() {
    determineBbox();
    return URLEncoder.encode(
        Math.toRadians(minLon)
            + ","
            + Math.toRadians(minLat)
            + ","
            + Math.toRadians(maxLon)
            + ","
            + Math.toRadians(maxLat)
            + ","
            + minHeight
            + ","
            + maxHeight,
        StandardCharsets.UTF_8);
  }

  private void determineBbox() {
    if (Objects.isNull(minLon)) {
      List<Geometry<?>> geometries =
          getGeometries(
              features,
              ImmutableList.of(
                  "lod2Solid",
                  "lod1Solid",
                  "consistsOfBuildingPart.lod2Solid",
                  "consistsOfBuildingPart.lod1Solid"));
      List<Coordinate> coordinates =
          geometries.stream()
              .map(Geometry::getCoordinatesFlat)
              .map(
                  coords -> {
                    if (!clampToGround) {
                      return coords;
                    }
                    final Optional<Double> optionalMin = getMin(coords, 2);
                    if (optionalMin.isEmpty()) {
                      return coords;
                    }
                    final double min = optionalMin.get();
                    return coords.stream()
                        .map(coord -> Coordinate.of(coord.get(0), coord.get(1), coord.get(2) - min))
                        .collect(Collectors.toUnmodifiableList());
                  })
              .flatMap(List::stream)
              .collect(Collectors.toUnmodifiableList());
      minLon = getMin(coordinates, 0).orElse(-180.0);
      maxLon = getMax(coordinates, 0).orElse(180.0);
      minLat = getMin(coordinates, 1).orElse(-90.0);
      maxLat = getMax(coordinates, 1).orElse(90.0);
      minHeight = getMin(coordinates, 2).orElse(0.0);
      maxHeight = getMax(coordinates, 2).orElse(0.0);
    }
  }

  private List<Geometry<?>> getGeometries(
      List<FeatureHtml> features, List<String> geometryProperties) {
    return features.stream()
        .map(
            feature -> {
              List<PropertyHtml> geomProperties = ImmutableList.of();
              for (String geometryProperty : geometryProperties) {
                geomProperties = feature.findPropertiesByPath(geometryProperty);
                if (!geomProperties.isEmpty()) break;
              }
              if (geomProperties.isEmpty()) {
                Optional<PropertyHtml> defaultGeom = feature.getGeometry();
                if (defaultGeom.isPresent()) {
                  geomProperties = ImmutableList.of(defaultGeom.get());
                }
              }
              return geomProperties.stream()
                  .map(PropertyHtml::parseGeometry)
                  .collect(Collectors.toUnmodifiableList());
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  private Optional<Double> getMin(List<Geometry.Coordinate> coordinates, int axis) {
    return coordinates.stream().map(coord -> coord.get(axis)).min(Comparator.naturalOrder());
  }

  private Optional<Double> getMax(List<Geometry.Coordinate> coordinates, int axis) {
    return coordinates.stream().map(coord -> coord.get(axis)).max(Comparator.naturalOrder());
  }
}
