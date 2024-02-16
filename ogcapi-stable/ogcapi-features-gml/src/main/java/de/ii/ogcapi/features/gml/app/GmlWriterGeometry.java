/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import static de.ii.ogcapi.features.gml.domain.GmlConfiguration.GmlVersion.GML21;
import static de.ii.ogcapi.features.gml.domain.GmlConfiguration.GmlVersion.GML31;
import static de.ii.ogcapi.features.gml.domain.GmlConfiguration.GmlVersion.GML32;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.gml.domain.EncodingAwareContextGml;
import de.ii.ogcapi.features.gml.domain.GmlConfiguration.GmlVersion;
import de.ii.ogcapi.features.gml.domain.GmlWriter;
import de.ii.ogcapi.features.gml.domain.ModifiableStateGml;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Singleton;

@SuppressWarnings("ConstantConditions")
@Singleton
@AutoBind
public class GmlWriterGeometry implements GmlWriter {

  private static final String POINT = "gml:Point";
  private static final String MULTI_POINT = "gml:MultiPoint";
  private static final String LINE_STRING = "gml:LineString";
  private static final String MULTI_CURVE = "gml:MultiCurve";
  private static final String MULTI_LINE_STRING = "gml21:MultiLineString";
  private static final String POLYGON = "gml:Polygon";
  private static final String MULTI_SURFACE = "gml:MultiSurface";
  private static final String MULTI_POLYGON = "gml21:MultiPolygon";
  private static final String MULTI_GEOMETRY = "gml:MultiGeometry";
  private static final String SOLID = "gml:Solid";
  private static final String COMPOSITE_SURFACE = "gml:CompositeSurface";
  private static final String COMPOSITE_CURVE = "gml:CompositeCurve";
  private static final String POS = "gml:pos";
  private static final String POS_LIST = "gml:posList";
  private static final String POINT_MEMBER = "gml:pointMember";
  private static final String CURVE_MEMBER = "gml:curveMember";
  private static final String LINEAR_RING = "gml:LinearRing";
  private static final String EXTERIOR = "gml:exterior";
  private static final String INTERIOR = "gml:interior";
  private static final String SURFACE_MEMBER = "gml:surfaceMember";
  private static final Map<GmlVersion, Map<SimpleFeatureGeometry, String>> GEOMETRY_ELEMENT =
      ImmutableMap.of(
          GML32,
          ImmutableMap.of(
              SimpleFeatureGeometry.POINT, POINT,
              SimpleFeatureGeometry.MULTI_POINT, MULTI_POINT,
              SimpleFeatureGeometry.LINE_STRING, LINE_STRING,
              SimpleFeatureGeometry.MULTI_LINE_STRING, MULTI_CURVE,
              SimpleFeatureGeometry.POLYGON, POLYGON,
              SimpleFeatureGeometry.MULTI_POLYGON, MULTI_SURFACE,
              SimpleFeatureGeometry.GEOMETRY_COLLECTION, MULTI_GEOMETRY),
          GML31,
          ImmutableMap.of(
              SimpleFeatureGeometry.POINT, "gml31" + POINT.substring(3),
              SimpleFeatureGeometry.MULTI_POINT, "gml31" + MULTI_POINT.substring(3),
              SimpleFeatureGeometry.LINE_STRING, "gml31" + LINE_STRING.substring(3),
              SimpleFeatureGeometry.MULTI_LINE_STRING, "gml31" + MULTI_CURVE.substring(3),
              SimpleFeatureGeometry.POLYGON, "gml31" + POLYGON.substring(3),
              SimpleFeatureGeometry.MULTI_POLYGON, "gml31" + MULTI_SURFACE.substring(3),
              SimpleFeatureGeometry.GEOMETRY_COLLECTION, "gml31" + MULTI_GEOMETRY.substring(3)),
          GML21,
          ImmutableMap.of(
              SimpleFeatureGeometry.POINT, "gml21" + POINT.substring(3),
              SimpleFeatureGeometry.MULTI_POINT, "gml21" + MULTI_POINT.substring(3),
              SimpleFeatureGeometry.LINE_STRING, "gml21" + LINE_STRING.substring(3),
              SimpleFeatureGeometry.MULTI_LINE_STRING, MULTI_LINE_STRING.substring(3),
              SimpleFeatureGeometry.POLYGON, "gml21" + POLYGON.substring(3),
              SimpleFeatureGeometry.MULTI_POLYGON, MULTI_POLYGON.substring(3),
              SimpleFeatureGeometry.GEOMETRY_COLLECTION, "gml21" + MULTI_GEOMETRY.substring(3)));
  private static final String OUTER_BOUNDARY_IS = "gml21:outerBoundaryIs";
  private static final String INNER_BOUNDARY_IS = "gml21:innerBoundaryIs";
  private static final String LINE_STRING_MEMBER = "gml21:lineStringMember";
  private static final String COORDINATES = "gml21:coordinates";
  private static final String POLYGON_MEMBER = "gml21:polygonMember";

  @Inject
  public GmlWriterGeometry() {}

  @Override
  public GmlWriterGeometry create() {
    return new GmlWriterGeometry();
  }

  @Override
  public int getSortPriority() {
    return 30;
  }

  @Override
  public void onFeatureEnd(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {
    context.encoding().getState().setDeferredSolidGeometry(false);
    context.encoding().getState().setDeferredPolygonId(0);

    next.accept(context);
  }

  @Override
  public void onObjectStart(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {
    if (context.schema().filter(SchemaBase::isSpatial).isPresent()
        && context.geometryType().isPresent()) {
      FeatureSchema schema = context.schema().orElseThrow();

      String elementNameProperty = schema.getName();
      context.encoding().write("<");
      context.encoding().write(elementNameProperty);

      SimpleFeatureGeometry geometryType = context.geometryType().get();
      context.encoding().getState().setCurrentGeometryType(geometryType);

      if (geometryType.equals(SimpleFeatureGeometry.MULTI_POLYGON)
          && schema.getConstraints().flatMap(SchemaConstraints::getComposite).orElse(false)
          && !context.encoding().getGmlVersion().equals(GML21)) {
        if (schema.getConstraints().flatMap(SchemaConstraints::getClosed).orElse(false)) {
          // a solid with an outer shell
          context.encoding().write("><");
          context.encoding().write(getGmlElementName(context, SOLID));
          context.encoding().write(" srsName=\"");
          context.encoding().write(context.encoding().getTargetCrs().toUriString());
          context.encoding().write("\"");
          addGmlIdOnGeometry(context, "", false);
          context.encoding().write("><");
          context.encoding().write(getGmlElementName(context, EXTERIOR));
          context.encoding().write("><");
          context.encoding().write(getGmlElementName(context, COMPOSITE_SURFACE));
          addGmlIdOnGeometry(context, ".composite", false);
          context.encoding().write(">");

          context
              .encoding()
              .pushElement(
                  elementNameProperty,
                  getGmlElementName(context, SOLID),
                  getGmlElementName(context, EXTERIOR),
                  getGmlElementName(context, COMPOSITE_SURFACE));
          context.encoding().getState().setCompositeGeometry(true);
          context.encoding().getState().setClosedGeometry(true);
        } else {
          // a composite surface
          context.encoding().write("><");
          context.encoding().write(getGmlElementName(context, COMPOSITE_SURFACE));
          context.encoding().write(" srsName=\"");
          context.encoding().write(context.encoding().getTargetCrs().toUriString());
          context.encoding().write("\"");
          addGmlIdOnGeometry(context, "", false);
          context.encoding().write(">");

          context
              .encoding()
              .pushElement(elementNameProperty, getGmlElementName(context, COMPOSITE_SURFACE));
          context.encoding().getState().setCompositeGeometry(true);
        }
      } else if (geometryType.equals(SimpleFeatureGeometry.MULTI_LINE_STRING)
          && schema.getConstraints().flatMap(SchemaConstraints::getComposite).orElse(false)
          && !context.encoding().getGmlVersion().equals(GML21)) {
        // a composite curve
        context.encoding().write("><");
        context.encoding().write(getGmlElementName(context, COMPOSITE_CURVE));
        context.encoding().write(" srsName=\"");
        context.encoding().write(context.encoding().getTargetCrs().toUriString());
        context.encoding().write("\"");
        addGmlIdOnGeometry(context, "", false);
        context.encoding().write(">");

        context
            .encoding()
            .pushElement(elementNameProperty, getGmlElementName(context, COMPOSITE_CURVE));
        context.encoding().getState().setCompositeGeometry(true);
      } else {
        // a regular multi-surface
        String elementNameObject =
            Objects.requireNonNull(
                GEOMETRY_ELEMENT.get(context.encoding().getGmlVersion()).get(geometryType));

        context.encoding().write("><");
        context.encoding().write(elementNameObject);
        context.encoding().write(" srsName=\"");
        context.encoding().write(context.encoding().getTargetCrs().toUriString());
        context.encoding().write("\"");
        addGmlIdOnGeometry(context, "", false);
        context.encoding().write(">");

        context.encoding().pushElement(elementNameProperty, elementNameObject);
      }

      context.encoding().getState().setInGeometry(true);
    }

    next.accept(context);
  }

  private static void addGmlIdOnGeometry(
      EncodingAwareContextGml context, String gmlIdSuffix, boolean force) {
    if (context.encoding().getGmlIdOnGeometries() || force) {
      context.encoding().write(" ");
      context.encoding().write(context.encoding().getGmlPrefix());
      context.encoding().write(":id=\"");
      context.encoding().write(context.encoding().getCurrentGmlId());
      context.encoding().write(".geom");
      context.encoding().write(gmlIdSuffix);
      context.encoding().write("\"");
    }
  }

  private String getGmlElementName(EncodingAwareContextGml context, String elementName) {
    if (context.encoding().getGmlVersion().equals(GML32)) {
      return elementName;
    } else if (context.encoding().getGmlVersion().equals(GML31)) {
      return context.encoding().getGmlPrefix() + elementName.substring(3);
    } else if (elementName.equals(EXTERIOR)) {
      return OUTER_BOUNDARY_IS;
    } else if (elementName.equals(INTERIOR)) {
      return INNER_BOUNDARY_IS;
    } else if (elementName.equals(MULTI_CURVE)) {
      return MULTI_LINE_STRING;
    } else if (elementName.equals(MULTI_SURFACE)) {
      return MULTI_POLYGON;
    } else if (elementName.equals(CURVE_MEMBER)) {
      return LINE_STRING_MEMBER;
    } else if (elementName.equals(SURFACE_MEMBER)) {
      return POLYGON_MEMBER;
    } else if (elementName.equals(POS_LIST)) {
      return COORDINATES;
    } else if (elementName.equals(POS)) {
      return COORDINATES;
    }

    return context.encoding().getGmlPrefix() + elementName.substring(3);
  }

  @Override
  public void onArrayStart(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    if (context.encoding().getState().getInGeometry()) {
      SimpleFeatureGeometry geometryType =
          context
              .encoding()
              .getState()
              .getCurrentGeometryType()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "GML building block: In a geometry, but no geometry type has been set"));
      int level = context.encoding().openGeometryArray();
      writeOpeningTagsGeometry(context, geometryType, level);
    }

    next.accept(context);
  }

  // the code is straightforward for those that understand the structure of the GML geometries,
  // it does not seem necessary to change the structure
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  private void writeOpeningTagsGeometry(
      EncodingAwareContextGml context, SimpleFeatureGeometry geometryType, int level) {
    switch (geometryType) {
      case POINT:
        writeOpeningTags(context, Optional.empty(), false, getGmlElementName(context, POS));
        break;

      case MULTI_POINT:
        if (level == 1) {
          writeOpeningTags(
              context,
              Optional.of(String.format(".%d", context.encoding().getGeometryItem(0))),
              false,
              getGmlElementName(context, POINT_MEMBER),
              getGmlElementName(context, POINT),
              getGmlElementName(context, POS));
        }
        break;

      case LINE_STRING:
        if (level == 0) {
          writeOpeningTags(context, Optional.empty(), false, getGmlElementName(context, POS_LIST));
        }
        break;

      case MULTI_LINE_STRING:
        if (level == 1) {
          writeOpeningTags(
              context,
              Optional.of(String.format(".%d", context.encoding().getGeometryItem(0))),
              false,
              getGmlElementName(context, CURVE_MEMBER),
              getGmlElementName(context, LINE_STRING),
              getGmlElementName(context, POS_LIST));
        }
        break;

      case POLYGON:
        if (level == 1) {
          boolean first = context.encoding().getGeometryItem(level - 1) == 0;
          writeOpeningTags(
              context,
              Optional.empty(),
              false,
              first ? getGmlElementName(context, EXTERIOR) : getGmlElementName(context, INTERIOR),
              getGmlElementName(context, LINEAR_RING),
              getGmlElementName(context, POS_LIST));
        }
        break;

      case MULTI_POLYGON:
        if (level == 0) {
          if (context.schema().orElseThrow().getName().equals("lod2Solid")) {
            context.encoding().writeSurfaceMemberPlaceholder();
            context.encoding().getState().setDeferredSolidGeometry(true);
            context.encoding().getState().setDeferredPolygonId(1);
          }
        } else if (level == 1) {
          int nextLocalPolygonId = context.encoding().getState().getDeferredPolygonId();
          if (context.schema().orElseThrow().getName().equals("lod2MultiSurface")
              && nextLocalPolygonId > 0) {
            context.encoding().getState().setDeferredPolygonId(nextLocalPolygonId + 1);
            String polygonId =
                String.format("%s.%d", context.encoding().getCurrentGmlId(), nextLocalPolygonId);
            context
                .encoding()
                .writeAsSurfaceMemberLink(getGmlElementName(context, SURFACE_MEMBER), polygonId);
            writeOpeningTags(
                context,
                Optional.of(String.format(".%d", nextLocalPolygonId)),
                true,
                getGmlElementName(context, SURFACE_MEMBER),
                getGmlElementName(context, POLYGON));
          } else if (!context.encoding().getState().getDeferredSolidGeometry()) {
            writeOpeningTags(
                context,
                Optional.of(String.format(".%d", context.encoding().getGeometryItem(0))),
                false,
                getGmlElementName(context, SURFACE_MEMBER),
                getGmlElementName(context, POLYGON));
          }
        } else if (level == 2) {
          if (!context.encoding().getState().getDeferredSolidGeometry()) {
            boolean first = context.encoding().getGeometryItem(level - 1) == 0;
            writeOpeningTags(
                context,
                Optional.empty(),
                false,
                first ? getGmlElementName(context, EXTERIOR) : getGmlElementName(context, INTERIOR),
                getGmlElementName(context, LINEAR_RING),
                getGmlElementName(context, POS_LIST));
          }
        }
        break;

      default:
        throw new IllegalStateException(
            String.format("Unsupported geometry type in GML building block: %s", geometryType));
    }
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void writeOpeningTags(
      EncodingAwareContextGml context,
      Optional<String> gmlId,
      boolean forceGmlId,
      String... elementNames) {
    IntStream.range(0, elementNames.length)
        .forEachOrdered(
            i -> {
              context.encoding().write("<");
              context.encoding().write(elementNames[i]);
              if (i == 1 && gmlId.isPresent()) {
                addGmlIdOnGeometry(context, gmlId.get(), forceGmlId);
              }
              context.encoding().write(">");
            });
    context.encoding().pushElement(elementNames);
  }

  @Override
  public void onArrayEnd(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    if (context.encoding().getState().getInGeometry()) {
      ModifiableStateGml state = context.encoding().getState();
      SimpleFeatureGeometry geometryType = state.getCurrentGeometryType().orElseThrow();
      int level = context.encoding().closeGeometryArray();
      writeClosingTagsGeometry(context, geometryType, level);
    }

    next.accept(context);
  }

  // the code is straightforward for those that understand the structure of the GML geometries,
  // it does not seem necessary to change the structure
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  private void writeClosingTagsGeometry(
      EncodingAwareContextGml context, SimpleFeatureGeometry geometryType, int level) {
    switch (geometryType) {
      case POINT:
        writeClosingTags(context, false);
        break;

      case MULTI_POINT:
        if (level == 0) {
          writeClosingTags(context, false);
          writeClosingTags(context, true);
          context.encoding().nextGeometryItem();
        }
        break;

      case LINE_STRING:
        if (level == -1) {
          writeClosingTags(context, false);
        } else if (level == 0) {
          context.encoding().nextGeometryItem();
        }
        break;

      case MULTI_LINE_STRING:
      case POLYGON:
        if (level == 0) {
          writeClosingTags(context, true);
          writeClosingTags(context, false);
          context.encoding().nextGeometryItem();
        } else if (level == 1) {
          context.encoding().nextGeometryItem();
        }
        break;

      case MULTI_POLYGON:
        if (level == -1) {
          context.encoding().getState().setDeferredSolidGeometry(false);
        } else if (level == 0) {
          if (!context.encoding().getState().getDeferredSolidGeometry()) {
            writeClosingTags(context, true);
          }
          context.encoding().nextGeometryItem();
        } else if (level == 1) {
          if (!context.encoding().getState().getDeferredSolidGeometry()) {
            writeClosingTags(context, true);
            writeClosingTags(context, false);
          }
          context.encoding().nextGeometryItem();
        } else if (level == 2) {
          context.encoding().nextGeometryItem();
        }
        break;

      default:
        throw new IllegalStateException(
            String.format("Unsupported geometry type in GML building block: %s", geometryType));
    }
  }

  private void writeClosingTags(EncodingAwareContextGml context, boolean isObject) {
    if (isObject) {
      context.encoding().write("</");
      context.encoding().write(context.encoding().popElement());
      context.encoding().write(">");
    }

    context.encoding().write("</");
    context.encoding().write(context.encoding().popElement());
    context.encoding().write(">");
  }

  @Override
  public void onObjectEnd(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {
    if (context.schema().filter(SchemaBase::isSpatial).isPresent()
        && context.encoding().getState().getInGeometry()) {
      context.encoding().write("</");
      context.encoding().write(context.encoding().popElement());
      context.encoding().write(">");

      context.encoding().write("</");
      context.encoding().write(context.encoding().popElement());
      context.encoding().write(">");

      if (context.encoding().getState().getClosedGeometry()) {
        context.encoding().write("</");
        context.encoding().write(context.encoding().popElement());
        context.encoding().write(">");

        context.encoding().getState().setClosedGeometry(false);
      }

      if (context.encoding().getState().getCompositeGeometry()) {
        context.encoding().write("</");
        context.encoding().write(context.encoding().popElement());
        context.encoding().write(">");

        context.encoding().getState().setCompositeGeometry(false);
      }

      context.encoding().getState().setInGeometry(false);
    }

    next.accept(context);
  }

  @Override
  public void onValue(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    if (context.encoding().getState().getInGeometry()
        && !context.encoding().getState().getDeferredSolidGeometry()) {
      int idx = context.encoding().nextGeometryItem() - 1;
      if (idx > 0) {
        context.encoding().write(context.encoding().getGmlVersion().equals(GML21) ? "," : " ");
      } else {
        int level = context.encoding().getGeometryArrayLevel();
        if (level > 0) {
          int idxParent = context.encoding().getGeometryItem(level - 1);
          if (idxParent > 0) {
            context.encoding().write(" ");
          }
        }
      }
      context.encoding().write(Objects.requireNonNull(context.value()));
    }

    next.accept(context);
  }
}
