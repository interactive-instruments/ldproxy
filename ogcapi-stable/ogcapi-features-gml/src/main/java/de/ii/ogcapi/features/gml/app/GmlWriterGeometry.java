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
import java.util.Arrays;
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
      context.encoding().write("<");
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
        writeOpeningTags(context, POS);
        break;

      case MULTI_POINT:
        if (level == 1) {
          writeOpeningTags(context, POINT_MEMBER, POINT, POS);
        }
        break;

      case LINE_STRING:
        if (level == 0) {
          writeOpeningTags(context, POS_LIST);
        }
        break;

      case MULTI_LINE_STRING:
        if (level == 1) {
          writeOpeningTags(context, CURVE_MEMBER, LINE_STRING, POS_LIST);
        }
        break;

      case POLYGON:
        if (level == 1) {
          boolean first = context.encoding().getGeometryItem(level) == 0;
          writeOpeningTags(context, first ? EXTERIOR : INTERIOR, LINEAR_RING, POS_LIST);
        }
        break;

      case MULTI_POLYGON:
        if (level == 1) {
          writeOpeningTags(context, SURFACE_MEMBER, POLYGON);
        } else if (level == 2) {
          boolean first = context.encoding().getGeometryItem(level) == 0;
          writeOpeningTags(context, first ? EXTERIOR : INTERIOR, LINEAR_RING, POS_LIST);
        }
        break;

      default:
        throw new IllegalStateException(
            String.format("Unsupported geometry type in GML building block: %s", geometryType));
    }
  }

  private void writeOpeningTags(EncodingAwareContextGml context, String... elementNames) {
    Arrays.stream(elementNames)
        .forEachOrdered(
            elementName -> {
              context.encoding().write("<");
              context.encoding().write(elementName);
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
        if (level == 0) {
          writeClosingTags(context, true);
          context.encoding().nextGeometryItem();
        } else if (level == 1) {
          writeClosingTags(context, true);
          writeClosingTags(context, false);
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
}
