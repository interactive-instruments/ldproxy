/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.gml.domain.EncodingAwareContextGml;
import de.ii.ogcapi.features.gml.domain.GmlWriter;
import de.ii.ogcapi.features.gml.domain.ModifiableStateGml;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
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
  private static final String POLYGON = "gml:Polygon";
  private static final String MULTI_SURFACE = "gml:MultiSurface";
  private static final String MULTI_GEOMETRY = "MultiGeometry";
  private static final String POS = "gml:pos";
  private static final String POS_LIST = "gml:posList";
  private static final String POINT_MEMBER = "gml:pointMember";
  private static final String CURVE_MEMBER = "gml:curveMember";
  private static final String LINEAR_RING = "gml:LinearRing";
  private static final String EXTERIOR = "gml:exterior";
  private static final String INTERIOR = "gml:interior";
  private static final String SURFACE_MEMBER = "gml:surfaceMember";
  // TODO move mapping to GmlGeometryType
  private static final Map<SimpleFeatureGeometry, String> GEOMETRY_ELEMENT =
      ImmutableMap.of(
          SimpleFeatureGeometry.POINT, POINT,
          SimpleFeatureGeometry.MULTI_POINT, MULTI_POINT,
          SimpleFeatureGeometry.LINE_STRING, LINE_STRING,
          SimpleFeatureGeometry.MULTI_LINE_STRING, MULTI_CURVE,
          SimpleFeatureGeometry.POLYGON, POLYGON,
          SimpleFeatureGeometry.MULTI_POLYGON, MULTI_SURFACE,
          SimpleFeatureGeometry.GEOMETRY_COLLECTION, MULTI_GEOMETRY);

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
  public void onObjectStart(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {
    if (context.schema().filter(SchemaBase::isSpatial).isPresent()
        && context.geometryType().isPresent()) {
      FeatureSchema schema = context.schema().orElseThrow();

      String elementNameProperty = schema.getName();
      context.encoding().write("\n<");
      context.encoding().write(elementNameProperty);

      SimpleFeatureGeometry geometryType = context.geometryType().get();
      context.encoding().getState().setCurrentGeometryType(geometryType);
      String elementNameObject = Objects.requireNonNull(GEOMETRY_ELEMENT.get(geometryType));

      context.encoding().write("><");
      context.encoding().write(elementNameObject);
      context.encoding().write(" srsName=\"");
      context.encoding().write(context.encoding().getTargetCrs().toUriString());
      context.encoding().write("\">");

      context.encoding().pushElement(elementNameProperty, elementNameObject);

      context.encoding().getState().setInGeometry(true);
    }

    next.accept(context);
  }

  @Override
  public void onArrayStart(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    if (context.encoding().getState().getInGeometry()) {
      ModifiableStateGml state = context.encoding().getState();
      SimpleFeatureGeometry geometryType = state.getCurrentGeometryType().orElseThrow();
      int level = context.encoding().openGeometryArray();
      switch (geometryType) {
        case POINT:
          context.encoding().write("<");
          context.encoding().write(POS);
          context.encoding().write(">");
          context.encoding().pushElement(POS);
          break;

        case MULTI_POINT:
          if (level == 1) {
            context.encoding().write("<");
            context.encoding().write(POINT_MEMBER);
            context.encoding().write("><");
            context.encoding().write(POINT);
            context.encoding().write("><");
            context.encoding().write(POS);
            context.encoding().write(">");
            context.encoding().pushElement(POINT_MEMBER, POINT, POS);
          }
          break;

        case LINE_STRING:
          if (level == 0) {
            context.encoding().write("<");
            context.encoding().write(POS_LIST);
            context.encoding().write(">");
            context.encoding().pushElement(POS_LIST);
          }
          break;

        case MULTI_LINE_STRING:
          if (level == 1) {
            context.encoding().write("<");
            context.encoding().write(CURVE_MEMBER);
            context.encoding().write("><");
            context.encoding().write(LINE_STRING);
            context.encoding().write("><");
            context.encoding().write(POS_LIST);
            context.encoding().write(">");
            context.encoding().pushElement(CURVE_MEMBER, LINE_STRING, POS_LIST);
          }
          break;

        case POLYGON:
          if (level == 0) {
            context.encoding().write("<");
            boolean first = context.encoding().getGeometryItem(level) == 0;
            context.encoding().write(first ? EXTERIOR : INTERIOR);
            context.encoding().write("><");
            context.encoding().write(LINEAR_RING);
            context.encoding().write(">");
            context.encoding().pushElement(first ? EXTERIOR : INTERIOR, LINEAR_RING);
          } else if (level == 1) {
            context.encoding().write("<");
            context.encoding().write(POS_LIST);
            context.encoding().write(">");
            context.encoding().pushElement(POS_LIST);
          }
          break;

        case MULTI_POLYGON:
          if (level == 0) {
            context.encoding().write("<");
            context.encoding().write(SURFACE_MEMBER);
            context.encoding().write("><");
            context.encoding().write(POLYGON);
            context.encoding().write(">");
            context.encoding().pushElement(SURFACE_MEMBER, POLYGON);
          } else if (level == 1) {
            context.encoding().write("<");
            boolean first = context.encoding().getGeometryItem(level) == 0;
            context.encoding().write(first ? EXTERIOR : INTERIOR);
            context.encoding().write("><");
            context.encoding().write(LINEAR_RING);
            context.encoding().write(">");
            context.encoding().pushElement(first ? EXTERIOR : INTERIOR, LINEAR_RING);
          } else if (level == 2) {
            context.encoding().write("<");
            context.encoding().write(POS_LIST);
            context.encoding().write(">");
            context.encoding().pushElement(POS_LIST);
          }
          break;

        default:
          throw new IllegalStateException(
              "Unsupported geometry type in GML building block: " + geometryType);
      }
    }

    next.accept(context);
  }

  @Override
  public void onArrayEnd(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    if (context.encoding().getState().getInGeometry()) {
      ModifiableStateGml state = context.encoding().getState();
      SimpleFeatureGeometry geometryType = state.getCurrentGeometryType().orElseThrow();
      int level = context.encoding().closeGeometryArray();
      switch (geometryType) {
        case POINT:
          close(context, false);
          break;

        case MULTI_POINT:
          if (level == 0) {
            close(context, false);
            close(context, true);
          }
          break;

        case LINE_STRING:
          if (level == -1) {
            close(context, false);
          } else {
            context.encoding().nextGeometryItem();
          }
          break;

        case MULTI_LINE_STRING:
          if (level == 0) {
            close(context, true);
            close(context, false);
          } else if (level == 1) {
            context.encoding().nextGeometryItem();
          }
          break;

        case POLYGON:
          if (level == -1) {
            close(context, true);
          } else if (level == 0) {
            close(context, false);
          } else {
            context.encoding().nextGeometryItem();
          }
          break;

        case MULTI_POLYGON:
          if (level == -1) {
            close(context, true);
          } else if (level == 1) {
            close(context, true);
            close(context, false);
          } else {
            context.encoding().nextGeometryItem();
          }
          break;

        default:
          throw new IllegalStateException(
              "Unsupported geometry type in GML building block: " + geometryType);
      }
    }

    next.accept(context);
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

      context.encoding().getState().setInGeometry(false);
    }

    next.accept(context);
  }

  @Override
  public void onValue(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {

    if (context.encoding().getState().getInGeometry()) {
      int idx = context.encoding().nextGeometryItem() - 1;
      if (idx > 0) {
        context.encoding().write(" ");
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

  private void close(EncodingAwareContextGml context, boolean isObject) {
    if (isObject) {
      context.encoding().write("</");
      context.encoding().write(context.encoding().popElement());
      context.encoding().write(">");
    }

    context.encoding().write("</");
    context.encoding().write(context.encoding().popElement());
    context.encoding().write(">");
  }
}
