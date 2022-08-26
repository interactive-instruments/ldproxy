/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.xtraplatform.features.domain.PropertyBase;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableMeshSurface.Builder.class)
interface MeshSurface {

  String LOD_1_SOLID = "lod1Solid";
  String LOD_2_SOLID = "lod2Solid";
  String SURFACES = "surfaces";
  String SURFACE_TYPE = "surfaceType";
  String LOD_2_MULTI_SURFACE = "lod2MultiSurface";
  String CONSISTS_OF_BUILDING_PART = "consistsOfBuildingPart";

  Geometry.MultiPolygon getGeometry();

  Optional<String> getSurfaceType();

  static MeshSurface of(Geometry.MultiPolygon geometry) {
    return ImmutableMeshSurface.builder().geometry(geometry).build();
  }

  static MeshSurface of(Geometry.MultiPolygon geometry, String surfaceType) {
    return ImmutableMeshSurface.builder().geometry(geometry).surfaceType(surfaceType).build();
  }

  static MeshSurface of(Geometry.MultiPolygon geometry, Optional<String> surfaceType) {
    return ImmutableMeshSurface.builder().geometry(geometry).surfaceType(surfaceType).build();
  }

  static List<MeshSurface> collectSolidSurfaces(FeatureGltf building) {
    ImmutableList.Builder<MeshSurface> meshSurfaceBuilder = ImmutableList.builder();

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

  static void collectSolidSurfaces(
      ImmutableList.Builder<MeshSurface> meshSurfaceBuilder, List<PropertyGltf> properties) {
    properties.stream()
        .filter(p -> SURFACES.equals(p.getLastPathSegment()))
        .findFirst()
        .ifPresentOrElse(
            surfacesProperty -> addSurfaces(meshSurfaceBuilder, surfacesProperty),
            () -> addSolid(meshSurfaceBuilder, properties));
  }

  private static void addSolid(
      ImmutableList.Builder<MeshSurface> meshSurfaceBuilder, List<PropertyGltf> properties) {
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
      ImmutableList.Builder<MeshSurface> meshSurfaceBuilder, PropertyGltf surfacesProperty) {
    surfacesProperty
        .getNestedProperties()
        .forEach(
            surface -> {
              Optional<String> surfaceType =
                  surface.getNestedProperties().stream()
                      .filter(p -> SURFACE_TYPE.equals(p.getLastPathSegment()))
                      .findFirst()
                      .map(PropertyGltf::getFirstValue)
                      .map(String::toLowerCase);
              surface.getNestedProperties().stream()
                  .filter(p -> LOD_2_MULTI_SURFACE.equals(p.getLastPathSegment()))
                  .forEach(p -> addMultiPolygon(meshSurfaceBuilder, p, surfaceType));
            });
  }

  private static void addMultiPolygon(
      ImmutableList.Builder<MeshSurface> meshSurfaceBuilder, PropertyGltf geometryProperty) {
    meshSurfaceBuilder.add(MeshSurface.of(GltfHelper.getMultiPolygon(geometryProperty)));
  }

  private static void addMultiPolygon(
      ImmutableList.Builder<MeshSurface> meshSurfaceBuilder,
      PropertyGltf geometryProperty,
      Optional<String> surfaceType) {
    meshSurfaceBuilder.add(
        MeshSurface.of(GltfHelper.getMultiPolygon(geometryProperty), surfaceType));
  }
}
