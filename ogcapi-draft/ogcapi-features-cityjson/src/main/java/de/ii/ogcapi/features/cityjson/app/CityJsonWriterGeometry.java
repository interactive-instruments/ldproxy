/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.cityjson.app;

import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.cityjson.domain.CityJsonWriter;
import de.ii.ogcapi.features.cityjson.domain.EncodingAwareContextCityJson;
import de.ii.ogcapi.features.cityjson.domain.FeatureTransformationContextCityJson;
import de.ii.ogcapi.features.cityjson.domain.FeatureTransformationContextCityJson.StateCityJson.Section;
import de.ii.ogcapi.features.cityjson.domain.Vertices;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class CityJsonWriterGeometry implements CityJsonWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CityJsonWriterGeometry.class);

  private final CrsTransformerFactory crsTransformerFactory;

  private int arrayLevel = -1;
  private final List<String> currentLod2Solid = new ArrayList<>();
  private String currentPolygon = null;
  private final List<String> currentSemanticSurfacePolygons = new ArrayList<>();
  private List<Integer> semanticSurfaceValues = new ArrayList<>();
  private boolean semanticSurfaceGeometryTypeIsMulti;
  private final List<String> semanticSurfaceTypes = new ArrayList<>();
  private int currentSemanticSurfaceId = -1;

  @Inject
  public CityJsonWriterGeometry(CrsTransformerFactory crsTransformerFactory) {
    this.crsTransformerFactory = crsTransformerFactory;
  }

  @Override
  public CityJsonWriterGeometry create() {
    return new CityJsonWriterGeometry(crsTransformerFactory);
  }

  @Override
  public int getSortPriority() {
    return 30;
  }

  @Override
  public void onStart(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {

    assert context.encoding().getGeometryPrecision().size() >= 3;
    double xScale = 1.0 / Math.pow(10, context.encoding().getGeometryPrecision().get(0));
    double yScale = 1.0 / Math.pow(10, context.encoding().getGeometryPrecision().get(1));
    double zScale = 1.0 / Math.pow(10, context.encoding().getGeometryPrecision().get(2));
    ArrayList<Double> currentScale = new ArrayList<>(3);
    currentScale.add(xScale);
    currentScale.add(yScale);
    currentScale.add(zScale);
    context.getState().setCurrentScale(currentScale);

    EpsgCrs crs = context.encoding().getCrs();
    Optional<BoundingBox> bbox =
        crs.equals(OgcCrs.CRS84h) || crs.equals(OgcCrs.CRS84)
            ? context.encoding().getApi().getSpatialExtent(context.encoding().getCollectionId())
            : context
                .encoding()
                .getApi()
                .getSpatialExtent(context.encoding().getCollectionId(), crs);
    if (bbox.isEmpty() && LOGGER.isErrorEnabled()) {
      LOGGER.error(
          "CityJSON: the bounding box is empty and cannot be used to compute the translation vector. The bbox in WGS84: {}",
          context.encoding().getApi().getSpatialExtent(context.encoding().getCollectionId()));
    }
    ArrayList<Double> currentTranslate = new ArrayList<>(3);
    currentTranslate.add(bbox.map(b -> (b.getXmin() + b.getXmax()) / 2.0).orElse(0.0));
    currentTranslate.add(bbox.map(b -> (b.getYmin() + b.getYmax()) / 2.0).orElse(0.0));
    currentTranslate.add(
        bbox.map(
                b ->
                    (Objects.requireNonNullElse(b.getZmin(), 0.0)
                            + Objects.requireNonNullElse(b.getZmax(), 0.0))
                        / 2.0)
            .orElse(0.0));
    context.getState().setCurrentTranslate(currentTranslate);

    context.encoding().getJson().writeFieldName(CityJsonWriter.TRANSFORM);
    context.encoding().getJson().writeStartObject();
    context.encoding().getJson().writeFieldName(CityJsonWriter.SCALE);
    context.encoding().getJson().writeStartArray(3);
    context.encoding().getJson().writeNumber(currentScale.get(0));
    context.encoding().getJson().writeNumber(currentScale.get(1));
    context.encoding().getJson().writeNumber(currentScale.get(2));
    context.encoding().getJson().writeEndArray();
    context.encoding().getJson().writeFieldName(CityJsonWriter.TRANSLATE);
    context.encoding().getJson().writeStartArray(3);
    context.encoding().getJson().writeNumber(currentTranslate.get(0));
    context.encoding().getJson().writeNumber(currentTranslate.get(1));
    context.encoding().getJson().writeNumber(currentTranslate.get(2));
    context.encoding().getJson().writeEndArray();
    context.encoding().getJson().writeEndObject();

    if (!context.encoding().getTextSequences()) {
      context.getState().setCurrentVertices(new Vertices());
    }

    next.accept(context);
  }

  @Override
  public void onEnd(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {

    if (!context.encoding().getTextSequences()) {
      writeVertices(context);
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {

    context.encoding().startGeometry();

    if (context.encoding().getTextSequences()) {
      context.getState().setCurrentVertices(new Vertices());
    }

    // next chain for extensions
    next.accept(context);
  }

  @Override
  public void onFeatureEnd(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {

    try {
      if (context.getState().inSection() == Section.WAITING_FOR_SURFACES) {
        // No surfaces found, close geometry object
        TokenBuffer buffer = context.getState().getGeometryBuffer().get();
        buffer.writeEndObject();
      }

      context.encoding().stopAndFlushGeometry();
    } catch (com.fasterxml.jackson.core.JsonGenerationException e) {
      LOGGER.error("{} {}", context.getState().getCurrentId(), context.encoding().getJson());
    }

    next.accept(context);
  }

  @Override
  public void onFeatureEndEnd(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {
    if (context.encoding().getTextSequences()) {
      writeVertices(context);
    }

    next.accept(context);
  }

  @Override
  public void onObjectStart(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {
    if (context.schema().isPresent() && context.schema().get().isObject()) {
      if (context.getState().inBuildingPart()
          && CityJsonWriter.CONSISTS_OF_BUILDING_PART.equals(context.schema().get().getName())) {
        context.encoding().startGeometry();
      } else if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES) {
        currentSemanticSurfacePolygons.clear();
        currentPolygon = "";
      }
    } else if (context.schema().isPresent()
        && context.schema().get().isSpatial()
        && context.geometryType().isPresent()
        && context.getState().inSection()
            != FeatureTransformationContextCityJson.StateCityJson.Section.IN_ADDRESS) {
      FeatureSchema schema = context.schema().get();
      if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES) {
        semanticSurfaceGeometryTypeIsMulti =
            context.geometryType().get().equals(SimpleFeatureGeometry.MULTI_POLYGON);
        context
            .encoding()
            .changeSection(
                FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACE_GEOMETRY);
      } else if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACE_GEOMETRY) {
        // nothing to do
      } else if (context.getState().inSection()
              == FeatureTransformationContextCityJson.StateCityJson.Section.IN_BUILDING
          && schema.getName().matches("^lod[12]Solid$")) {
        String lod = "lod2Solid".equals(schema.getName()) ? "2" : "1";
        arrayLevel = -1;

        TokenBuffer buffer = context.getState().getGeometryBuffer().orElseThrow();
        buffer.writeStartObject();
        buffer.writeStringField(TYPE, CityJsonWriter.SOLID);
        buffer.writeStringField(CityJsonWriter.LOD, lod);
        buffer.writeFieldName(CityJsonWriter.BOUNDARIES);

        if (lod.equals("2")) {
          context
              .encoding()
              .changeSection(
                  FeatureTransformationContextCityJson.StateCityJson.Section
                      .IN_GEOMETRY_WITH_SURFACES);
          currentLod2Solid.clear();
        } else {
          context
              .encoding()
              .changeSection(
                  FeatureTransformationContextCityJson.StateCityJson.Section.IN_GEOMETRY);
        }
      } else {
        context
            .encoding()
            .changeSection(
                FeatureTransformationContextCityJson.StateCityJson.Section.IN_GEOMETRY_IGNORE);
      }
    }

    next.accept(context);
  }

  @Override
  public void onObjectEnd(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {
    next.accept(context);

    if (context.schema().isPresent() && context.schema().get().isObject()) {
      if (context.getState().inBuildingPart()
          && CONSISTS_OF_BUILDING_PART.equals(context.schema().get().getName())) {
        context.encoding().stopAndFlushGeometry();
      } else if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
              "{}: {}", semanticSurfaceTypes.get(currentSemanticSurfaceId), currentPolygon);
        }
        for (int i = 0; i < currentLod2Solid.size(); i++) {
          for (String currentSemanticSurfacePolygon : currentSemanticSurfacePolygons) {
            if (currentSemanticSurfacePolygon.equals(currentLod2Solid.get(i))) {
              semanticSurfaceValues.set(i, currentSemanticSurfaceId);
            }
          }
        }
        currentSemanticSurfaceId++;
      }
    } else if (context.schema().isPresent()
        && context.schema().get().isSpatial()
        && context.getState().getGeometryBuffer().isPresent()) {
      if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_GEOMETRY) {
        // close geometry object
        context.getState().getGeometryBuffer().get().writeEndObject();
        context
            .encoding()
            .changeSection(FeatureTransformationContextCityJson.StateCityJson.Section.IN_BUILDING);
      } else if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_GEOMETRY_WITH_SURFACES) {
        // keep geometry object open for semantic surface information
        context
            .encoding()
            .changeSection(
                FeatureTransformationContextCityJson.StateCityJson.Section.WAITING_FOR_SURFACES);
      } else if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACE_GEOMETRY) {
        context
            .encoding()
            .changeSection(FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES);
      } else if (context.getState().inSection()
          == FeatureTransformationContextCityJson.StateCityJson.Section.IN_GEOMETRY_IGNORE) {
        context.encoding().returnToPreviousSection();
      }
    }
  }

  @Override
  public void onArrayStart(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {
    if (context.schema().isPresent() && context.schema().get().isArray()) {
      FeatureSchema schema = context.schema().get();

      if (CityJsonWriter.SURFACES.equals(schema.getName())) {
        if (context.getState().inSection()
            == FeatureTransformationContextCityJson.StateCityJson.Section.WAITING_FOR_SURFACES) {
          context
              .encoding()
              .changeSection(
                  FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES);
          currentSemanticSurfaceId = 0;
          semanticSurfaceTypes.clear();
          semanticSurfaceValues = new ArrayList<>(currentLod2Solid.size());
          currentLod2Solid.forEach(s -> semanticSurfaceValues.add(null));
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("LoD 2 solid: {}", currentLod2Solid);
          }
        } else {
          context
              .encoding()
              .changeSection(
                  FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES_IGNORE);
        }
      }
    } else if (context.getState().inActiveGeometry()) {
      arrayLevel++;
      if (arrayLevel == 0 && context.getState().getGeometryBuffer().isPresent()) {
        // cast MultiPolygon to Solid, wrap in additional array
        context.getState().getGeometryBuffer().get().writeStartArray();
      }
      if (arrayLevel != 3 && context.getState().getGeometryBuffer().isPresent()) {
        context.getState().getGeometryBuffer().get().writeStartArray();
        if (context.getState().inSection()
            == FeatureTransformationContextCityJson.StateCityJson.Section
                .IN_GEOMETRY_WITH_SURFACES) {
          if (arrayLevel == 1) {
            currentPolygon = "";
          } else if (arrayLevel == 2 && !currentPolygon.isEmpty()) {
            // this is a hole
            currentPolygon += "_";
          }
        }
      }
    } else if (context.getState().inSection()
        == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACE_GEOMETRY) {
      arrayLevel++;
      if (arrayLevel != 3) {
        if (arrayLevel == 1) {
          currentPolygon = "";
        } else if (arrayLevel == 2 && !currentPolygon.isEmpty()) {
          // this is a hole
          currentPolygon += "_";
        }
      }
    }

    next.accept(context);
  }

  @Override
  public void onArrayEnd(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {
    next.accept(context);

    if (context.schema().isPresent() && context.schema().get().isArray()) {

      if (context.getState().inSection()
              == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES
          && context.getState().getGeometryBuffer().isPresent()) {
        TokenBuffer buffer = context.getState().getGeometryBuffer().get();
        buffer.writeFieldName(CityJsonWriter.SEMANTICS);
        buffer.writeStartObject();

        buffer.writeFieldName(CityJsonWriter.SURFACES);
        buffer.writeStartArray();
        for (String semanticSurfaceType : semanticSurfaceTypes) {
          buffer.writeStartObject();
          buffer.writeStringField(TYPE, semanticSurfaceType);
          buffer.writeEndObject();
        }
        buffer.writeEndArray();

        buffer.writeFieldName(CityJsonWriter.VALUES);
        buffer.writeStartArray();
        buffer.writeStartArray();
        for (Integer semanticSurfaceValue : semanticSurfaceValues) {
          if (Objects.nonNull(semanticSurfaceValue)) {
            buffer.writeNumber(semanticSurfaceValue);
          } else {
            buffer.writeNull();
          }
        }
        buffer.writeEndArray();
        buffer.writeEndArray();

        // close semantics object
        buffer.writeEndObject();

        // close geometry object
        buffer.writeEndObject();
        context
            .encoding()
            .changeSection(FeatureTransformationContextCityJson.StateCityJson.Section.IN_BUILDING);
      }
    } else if (context.getState().inActiveGeometry()
        && context.getState().getGeometryBuffer().isPresent()) {
      TokenBuffer buffer = context.getState().getGeometryBuffer().get();
      if (arrayLevel != 3) {
        buffer.writeEndArray();
        if (context.getState().inSection()
                == FeatureTransformationContextCityJson.StateCityJson.Section
                    .IN_GEOMETRY_WITH_SURFACES
            && arrayLevel == 1) {
          currentLod2Solid.add(currentPolygon);
        }
      }
      if (arrayLevel == 0) {
        // cast MultiPolygon to Solid
        buffer.writeEndArray();
      }
      arrayLevel--;
    } else if (context.getState().inSection()
        == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACE_GEOMETRY) {
      if (arrayLevel != 3) {
        if ((arrayLevel == 1 && semanticSurfaceGeometryTypeIsMulti)
            || (arrayLevel == 0 && !semanticSurfaceGeometryTypeIsMulti)) {
          currentSemanticSurfacePolygons.add(currentPolygon);
        }
      }
      arrayLevel--;
    }
  }

  @Override
  public void onValue(
      EncodingAwareContextCityJson context, Consumer<EncodingAwareContextCityJson> next)
      throws IOException {

    if (context.getState().inSection()
        == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACES) {
      if (context.schema().isPresent()
          && CityJsonWriter.SURFACE_TYPE.equals(context.schema().get().getName())) {
        switch (Objects.requireNonNull(context.value())) {
          case CityJsonWriter.WALL_SURFACE:
          case "wall":
            semanticSurfaceTypes.add(CityJsonWriter.WALL_SURFACE);
            break;
          case CityJsonWriter.ROOF_SURFACE:
          case "roof":
            semanticSurfaceTypes.add(CityJsonWriter.ROOF_SURFACE);
            break;
          case CityJsonWriter.GROUND_SURFACE:
          case "ground":
            semanticSurfaceTypes.add(CityJsonWriter.GROUND_SURFACE);
            break;
          case CityJsonWriter.CLOSURE_SURFACE:
          case "closure":
            semanticSurfaceTypes.add(CityJsonWriter.CLOSURE_SURFACE);
            break;
          case CityJsonWriter.OUTER_CEILING_SURFACE:
          case "outer_ceiling":
            semanticSurfaceTypes.add(CityJsonWriter.OUTER_CEILING_SURFACE);
            break;
          case CityJsonWriter.OUTER_FLOOR_SURFACE:
          case "outer_floor":
            semanticSurfaceTypes.add(CityJsonWriter.OUTER_FLOOR_SURFACE);
            break;
          case CityJsonWriter.WINDOW:
          case "window":
            semanticSurfaceTypes.add(CityJsonWriter.WINDOW);
            break;
          case CityJsonWriter.DOOR:
          case "door":
            semanticSurfaceTypes.add(CityJsonWriter.DOOR);
            break;
          case CityJsonWriter.INTERIOR_WALL_SURFACE:
          case "interior_wall":
            semanticSurfaceTypes.add(CityJsonWriter.INTERIOR_WALL_SURFACE);
            break;
          case CityJsonWriter.CEILING_SURFACE:
          case "ceiling":
            semanticSurfaceTypes.add(CityJsonWriter.CEILING_SURFACE);
            break;
          case CityJsonWriter.FLOOR_SURFACE:
          case "floor":
            semanticSurfaceTypes.add(CityJsonWriter.FLOOR_SURFACE);
            break;
          default:
            throw new IllegalStateException(
                String.format("Unknown semantic surface type: %s", context.value()));
        }
      }
    } else if (context.getState().inSection()
        == FeatureTransformationContextCityJson.StateCityJson.Section.IN_SURFACE_GEOMETRY) {
      Optional<Integer> optionalIndex = context.encoding().processOrdinate(context.value());
      optionalIndex.ifPresent(
          integer -> currentPolygon += (currentPolygon.isEmpty() ? "" : "_") + integer);
    } else if (context.getState().inActiveGeometry()) {
      Optional<Integer> optionalIndex = context.encoding().processOrdinate(context.value());
      if (optionalIndex.isPresent()) {
        context.getState().getGeometryBuffer().orElseThrow().writeNumber(optionalIndex.get());
        if (context.getState().inSection()
            == FeatureTransformationContextCityJson.StateCityJson.Section
                .IN_GEOMETRY_WITH_SURFACES) {
          currentPolygon += (currentPolygon.isEmpty() ? "" : "_") + optionalIndex.get();
        }
      }
    }

    next.accept(context);
  }

  private void writeVertices(EncodingAwareContextCityJson context) throws IOException {
    Vertices vertices = context.getState().getCurrentVertices().orElse(null);
    if (Objects.nonNull(vertices)) {
      vertices.lock();
      context.encoding().getJson().writeFieldName(CityJsonWriter.VERTICES);
      context.encoding().getJson().writeStartArray();
      int size = vertices.getSize();
      for (int i = 0; i < size; i++) {
        context.encoding().getJson().writeArray(vertices.getVertex(i), 0, 3);
      }
      context.encoding().getJson().writeEndArray();

      LOGGER.trace("Vertices: {}", size);
    } else {
      context.encoding().getJson().writeFieldName(CityJsonWriter.VERTICES);
      context.encoding().getJson().writeStartArray();
      context.encoding().getJson().writeEndArray();

      LOGGER.trace("No Vertices");
    }
  }
}
