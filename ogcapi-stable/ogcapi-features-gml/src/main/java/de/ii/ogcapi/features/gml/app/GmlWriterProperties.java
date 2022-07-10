/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.gml.domain.EncodingAwareContextGml;
import de.ii.ogcapi.features.gml.domain.GmlWriter;
import de.ii.ogcapi.features.gml.domain.ModifiableStateGml;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

@SuppressWarnings("ConstantConditions")
@Singleton
@AutoBind
public class GmlWriterProperties implements GmlWriter {

  @Inject
  public GmlWriterProperties() {}

  @Override
  public GmlWriterProperties create() {
    return new GmlWriterProperties();
  }

  @Override
  public int getSortPriority() {
    return 40;
  }

  @Override
  public void onPropertiesEnd(
      EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next) {

    next.accept(context);
  }

  @Override
  public void onObjectStart(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {
    if (context.schema().filter(FeatureSchema::isObject).isPresent()) {
      FeatureSchema schema = context.schema().orElseThrow();

      String elementNameProperty = schema.getName();
      context.encoding().write("\n<");
      context.encoding().write(elementNameProperty);

      String objectType = schema.getObjectType().orElseThrow();

      if ("Link".equals(objectType)) {
        context.encoding().getState().setInLink(true);
      } else if ("Measure".equals(objectType)) {
        context.encoding().getState().setInMeasure(true);
        context.encoding().getState().setFirstMeasureProperty(Optional.empty());
        context.encoding().pushElement(elementNameProperty);
      } else {
        String elementNameObject = context.encoding().startGmlObject(schema);

        context.encoding().write(">");
        context.encoding().write("\n<");
        context.encoding().write(elementNameObject);
        context.encoding().writeXmlAttPlaceholder();
        context.encoding().write(">");

        context.encoding().pushElement(elementNameProperty, elementNameObject);
      }
    }

    next.accept(context);
  }

  @Override
  public void onObjectEnd(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {
    if (context.schema().filter(FeatureSchema::isObject).isPresent()) {
      boolean inLink = context.encoding().getState().getInLink();
      boolean inMeasure = context.encoding().getState().getInMeasure();

      if (inLink) {
        context.encoding().write("/>");
        context.encoding().getState().setInLink(false);
      } else if (inMeasure) {
        context.encoding().write("</");
        context.encoding().write(context.encoding().popElement());
        context.encoding().write(">");
        context.encoding().getState().setInMeasure(false);
      } else {
        context.encoding().write("\n</");
        context.encoding().write(context.encoding().popElement());
        context.encoding().write(">");

        context.encoding().write("\n</");
        context.encoding().write(context.encoding().popElement());
        context.encoding().write(">");

        context.encoding().closeGmlObject();
      }
    }

    next.accept(context);
  }

  @Override
  public void onValue(EncodingAwareContextGml context, Consumer<EncodingAwareContextGml> next)
      throws IOException {
    if (!shouldSkipProperty(context)) {
      FeatureSchema schema = context.schema().orElseThrow();
      String value = context.value();

      ModifiableStateGml state = context.encoding().getState();
      boolean inLink = state.getInLink();
      boolean inMeasure = state.getInMeasure();

      if (inLink) {
        context.encoding().write(" xlink:");
        context.encoding().write(schema.getName());
        context.encoding().write("=\"");
        writeValue(context, value, schema.getType());
        context.encoding().write("\"");
      } else if (inMeasure) {
        state
            .getFirstMeasureProperty()
            .ifPresentOrElse(
                consumerMayThrow(
                    other -> {
                      String uom = "uom".equals(schema.getName()) ? value : other;
                      String val = "value".equals(schema.getName()) ? value : other;
                      context.encoding().write(" uom=\"");
                      context.encoding().write(uom);
                      context.encoding().write("\"");
                      context.encoding().write(">");
                      writeValue(context, val, schema.getType());
                    }),
                () -> state.setFirstMeasureProperty(Optional.ofNullable(value)));
      } else {
        if (context.encoding().getXmlAttributes().contains(schema.getFullPathAsString())) {
          // encode as XML attribute
          context.encoding().writeAsXmlAtt(schema.getName(), value);
        } else {
          context.encoding().write("\n<");
          context.encoding().write(schema.getName());
          if (schema.getType() == Type.FLOAT || schema.getType() == Type.INTEGER) {
            // write as gml:MeasureType, if we have a numeric property with a 'unit'
            // property in the provider schema
            schema
                .getUnit()
                .ifPresent(
                    consumerMayThrow(
                        uom -> {
                          context.encoding().write(" uom=\"");
                          context.encoding().write(uom);
                          context.encoding().write("\"");
                        }));
          }
          context.encoding().write(">");
          writeValue(context, value, schema.getType());
          context.encoding().write("</");
          context.encoding().write(schema.getName());
          context.encoding().write(">");
        }

        state
            .getVariableNameProperty()
            .ifPresent(
                p -> {
                  // check for variable object element name property
                  if (p.equals(schema.getName())) {
                    String mappedValue =
                        Objects.requireNonNullElse(
                            state.getVariableNameMapping().get(value), value);
                    context.encoding().setCurrentObjectElement(mappedValue);
                  }
                });
      }
    }

    next.accept(context);
  }

  private boolean shouldSkipProperty(EncodingAwareContextGml context) {
    return !hasMappingAndValue(context)
        || context.schema().orElseThrow().isId()
        || context.inGeometry();
  }

  private boolean hasMappingAndValue(EncodingAwareContextGml context) {
    return context.schema().filter(FeatureSchema::isValue).isPresent()
        && Objects.nonNull(context.value());
  }

  private void writeValue(EncodingAwareContextGml context, String value, Type type) {
    if (type == Type.BOOLEAN) {
      context.encoding().write(Boolean.parseBoolean(value));
    } else {
      context.encoding().write(value);
    }
  }
}
