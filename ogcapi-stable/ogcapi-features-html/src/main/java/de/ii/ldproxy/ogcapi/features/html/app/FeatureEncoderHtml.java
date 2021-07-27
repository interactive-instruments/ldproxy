/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.xtraplatform.dropwizard.domain.MustacheRenderer;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class FeatureEncoderHtml extends FeatureObjectEncoder<PropertyHtml, FeatureHtml, byte[]> {

  private final FeatureTransformationContextHtml transformationContext;

  public FeatureEncoderHtml(FeatureTransformationContextHtml transformationContext) {
    this.transformationContext = transformationContext;
  }

  @Override
  public FeatureHtml createFeature() {
    return ModifiableFeatureHtml.create();
  }

  @Override
  public PropertyHtml createProperty() {
    return ModifiablePropertyHtml.create();
  }

  @Override
  public void onStart(ModifiableContext context) {
  }

  @Override
  public void onFeature(FeatureHtml feature) {
    transformationContext.getHtmlConfiguration().getItemLabelFormat().ifPresent(label -> transformationContext.getFeatureTypeDataset().name = label);

    //TODO
    if (feature.hasObjects()) {
      transformationContext.getFeatureTypeDataset().complexObjects = true;
      transformationContext.getFeatureTypeDataset().classic = false;
    } else {
      transformationContext.getFeatureTypeDataset().complexObjects = false;
      transformationContext.getFeatureTypeDataset().classic = true;
      feature.getProperties().forEach(propertyHtml -> propertyHtml.name(propertyHtml.getSchema().map(
          FeatureSchema::getName).orElseGet(propertyHtml::getName)).propertyPath(ImmutableList.of(propertyHtml.getName())));
    }

    if (transformationContext.getFeatureTypeDataset().hideMap && feature.hasGeometry()) {
      transformationContext.getFeatureTypeDataset().hideMap = false;
    }

    Optional<String> itemLabelFormat = transformationContext.getHtmlConfiguration().getItemLabelFormat();
    if (itemLabelFormat.isPresent()) {
      feature.name(StringTemplateFilters.applyTemplate(itemLabelFormat.get(),
          pathString -> feature.findPropertyByPath(pathString).map(PropertyHtml::getFirstValue)));
    }

    //TODO: generalize as schema/value transformer
    transformLinks(feature.getProperties());

    //TODO: generalize as value transformer
    if (transformationContext.getI18n().isPresent()) {
      translateBooleans(feature.getProperties(), transformationContext.getI18n().get(), transformationContext.getLanguage());
    }

    if (!transformationContext.isFeatureCollection()) {
      transformationContext.getFeatureTypeDataset().title = feature.getName();
      transformationContext.getFeatureTypeDataset().breadCrumbs.get(transformationContext.getFeatureTypeDataset().breadCrumbs.size() - 1).label = feature.getName();
    }

    if (transformationContext.getHtmlConfiguration().getSchemaOrgEnabled()) {
      feature.itemType("http://schema.org/Place");
    }

    transformationContext.getFeatureTypeDataset().features.add(feature);
  }

  @Override
  public void onEnd(ModifiableContext context) {

    //TODO: FeatureTokenEncoderBytes.getOutputStream
    OutputStreamWriter writer = new OutputStreamWriter(new OutputStreamToByteConsumer(this::push));

    try {
      ((MustacheRenderer)transformationContext.getMustacheRenderer()).render(transformationContext.getFeatureTypeDataset(), writer);
      writer.flush();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void transformLinks(List<PropertyHtml> properties) {
    for (int i = 0; i < properties.size(); i++) {
      PropertyHtml property = properties.get(i);
      if (property.isObject()
        && property.getSchema()
            .flatMap(schema -> schema.getObjectType()
                .filter(objectType -> Objects.equals(objectType, "Link")))
            .isPresent()) {

          String href = property.getNestedProperties().stream()
              .filter(valueProperty -> valueProperty.getSchema()
                  .filter(schema -> schema.getName().equals("href")).isPresent())
              .findFirst()
              .flatMap(valueProperty -> Optional.ofNullable(valueProperty.getValue()))
              .orElse("");
          String title = property.getNestedProperties().stream()
              .filter(valueProperty -> valueProperty.getSchema()
                  .filter(schema -> schema.getName().equals("title")).isPresent())
              .findFirst()
              .flatMap(valueProperty -> Optional.ofNullable(valueProperty.getValue()))
              .orElse("Link");

          property.type(PropertyBase.Type.VALUE);
          property.value(String.format("<a href=\"%s\">%s</a>", href, title));
      } else {
        transformLinks(property.getNestedProperties());
      }
    }
  }

  private void translateBooleans(List<PropertyHtml> properties, I18n i18n, Optional<Locale> language) {
    for (PropertyHtml property : properties) {
      if (property.isValue()) {
        if (Objects.nonNull(property.getValue())
            && property.getSchema().filter(schema -> schema.getType() == Type.BOOLEAN).isPresent()) {
          if (property.getValue().matches("[fF](alse|ALSE)?|0")) {
            property.value(i18n.get("false", language));
          } else if (property.getValue().matches("[tT](rue|RUE)?|[\\-\\+]?1")) {
            property.value(i18n.get("true", language));
          }
        }
      } else {
        translateBooleans(property.getNestedProperties(), i18n, language);
      }
    }
  }
}
