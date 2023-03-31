/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import de.ii.ogcapi.features.html.domain.Geometry.MultiPolygon;
import de.ii.xtraplatform.features.domain.PropertyBase;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableMeshSurfaceList.Builder.class)
interface MeshSurfaceList {

  String LOD_1_SOLID = "lod1Solid";
  String LOD_2_SOLID = "lod2Solid";
  String SURFACES = "surfaces";
  String SURFACE_TYPE = "surfaceType";
  String LOD_2_MULTI_SURFACE = "lod2MultiSurface";
  String CONSISTS_OF_BUILDING_PART = "consistsOfBuildingPart";

  static MeshSurfaceList of() {
    return ImmutableMeshSurfaceList.builder().build();
  }

  static MeshSurfaceList of(FeatureGltf building) {
    ImmutableMeshSurfaceList.Builder meshSurfaceBuilder = ImmutableMeshSurfaceList.builder();

    building
        .findPropertyByPath(CONSISTS_OF_BUILDING_PART)
        .map(PropertyBase::getNestedProperties)
        .ifPresentOrElse(
            buildingParts ->
                buildingParts.forEach(
                    buildingPart ->
                        collectSolidSurfaces(
                            meshSurfaceBuilder, buildingPart.getNestedProperties())),
            () -> collectSolidSurfaces(meshSurfaceBuilder, building.getProperties()));

    return meshSurfaceBuilder.build();
  }

  List<MeshSurface> getMeshSurfaces();

  @Value.Derived
  default boolean isEmpty() {
    return getMeshSurfaces().isEmpty();
  }

  @Value.Derived
  default double[][] getMinMax() {
    // determine the bounding box; eventually we will translate vertices to the center of the
    // feature to have smaller values (glTF uses float)
    List<Coordinate> coordList =
        getMeshSurfaces().stream()
            .map(MeshSurface::getGeometry)
            .map(MultiPolygon::getCoordinatesFlat)
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableList());
    return new double[][] {
      new double[] {
        coordList.stream().mapToDouble(coord -> coord.get(0)).min().orElseThrow(),
        coordList.stream().mapToDouble(coord -> coord.get(1)).min().orElseThrow(),
        coordList.stream().mapToDouble(coord -> coord.get(2)).min().orElseThrow()
      },
      new double[] {
        coordList.stream().mapToDouble(coord -> coord.get(0)).max().orElseThrow(),
        coordList.stream().mapToDouble(coord -> coord.get(1)).max().orElseThrow(),
        coordList.stream().mapToDouble(coord -> coord.get(2)).max().orElseThrow()
      }
    };
  }

  private static void collectSolidSurfaces(
      ImmutableMeshSurfaceList.Builder meshSurfaceBuilder, List<PropertyGltf> properties) {
    properties.stream()
        .filter(p -> SURFACES.equals(p.getLastPathSegment()))
        .findFirst()
        .ifPresentOrElse(
            surfacesProperty -> addSurfaces(meshSurfaceBuilder, surfacesProperty),
            () -> addSolid(meshSurfaceBuilder, properties));
  }

  private static void addSolid(
      ImmutableMeshSurfaceList.Builder meshSurfaceBuilder, List<PropertyGltf> properties) {
    properties.stream()
        .filter(p -> LOD_2_SOLID.equals(p.getLastPathSegment()))
        .findFirst()
        .ifPresentOrElse(
            p -> addMultiPolygon(meshSurfaceBuilder, p),
            () ->
                properties.stream()
                    .filter(p -> LOD_1_SOLID.equals(p.getLastPathSegment()))
                    .findFirst()
                    .ifPresent(p -> addMultiPolygon(meshSurfaceBuilder, p)));
  }

  private static void addSurfaces(
      ImmutableMeshSurfaceList.Builder meshSurfaceBuilder, PropertyGltf surfacesProperty) {
    surfacesProperty.getNestedProperties().stream()
        .collect(
            Collectors.groupingBy(
                surface ->
                    surface.getNestedProperties().stream()
                        .filter(p -> SURFACE_TYPE.equals(p.getLastPathSegment()))
                        .findFirst()
                        .map(PropertyGltf::getFirstValue)
                        .map(String::toLowerCase)
                        .orElse("unknown")))
        .forEach(
            (key, value) ->
                meshSurfaceBuilder.addMeshSurfaces(
                    MeshSurface.of(
                        MultiPolygon.of(
                            value.stream()
                                .map(
                                    surface ->
                                        surface.getNestedProperties().stream()
                                            .filter(
                                                p ->
                                                    LOD_2_MULTI_SURFACE.equals(
                                                        p.getLastPathSegment()))
                                            .collect(Collectors.toUnmodifiableList()))
                                .flatMap(List::stream)
                                .map(PropertyGltf::getMultiPolygon)
                                .map(MultiPolygon::getCoordinates)
                                .flatMap(List::stream)
                                .collect(Collectors.toUnmodifiableList())),
                        "unknown".equals(key) ? Optional.empty() : Optional.of(key))));
  }

  private static void addMultiPolygon(
      ImmutableMeshSurfaceList.Builder meshSurfaceBuilder, PropertyGltf geometryProperty) {
    meshSurfaceBuilder.addMeshSurfaces(MeshSurface.of(geometryProperty.getMultiPolygon()));
  }

  private static void addMultiPolygon(
      ImmutableMeshSurfaceList.Builder meshSurfaceBuilder,
      PropertyGltf geometryProperty,
      Optional<String> surfaceType) {
    meshSurfaceBuilder.addMeshSurfaces(
        MeshSurface.of(geometryProperty.getMultiPolygon(), surfaceType));
  }
}
