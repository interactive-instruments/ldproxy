/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class GeoJsonWriterId implements GeoJsonWriter {

  @Inject
  public GeoJsonWriterId() {}

  @Override
  public GeoJsonWriterId create() {
    return new GeoJsonWriterId();
  }

  private String currentId;
  private boolean currentIdIsInteger;
  private boolean writeAtFeatureEnd = false;
  private Stack<String> embeddedIds;
  private Stack<Boolean> embeddedIdIsInteger;
  private Stack<Boolean> writeAtEmbeddedFeatureEnd;
  private int embeddedFeatureNestingLevel = 0;

  @Override
  public int getSortPriority() {
    return 10;
  }

  @Override
  public void onFeatureStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    this.writeAtFeatureEnd = false;
    this.embeddedIds = new Stack<>();
    this.embeddedIdIsInteger = new Stack<>();
    this.writeAtEmbeddedFeatureEnd = new Stack<>();
    this.embeddedFeatureNestingLevel = 0;

    next.accept(context);
  }

  @Override
  public void onFeatureEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (writeAtFeatureEnd) {
      this.writeAtFeatureEnd = false;

      if (Objects.nonNull(currentId)) {
        if (currentIdIsInteger)
          context.encoding().getJson().writeNumberField("id", Long.parseLong(currentId));
        else context.encoding().getJson().writeStringField("id", currentId);
        addLinks(context, currentId);
        this.currentId = null;
        this.currentIdIsInteger = false;
      }
    }

    next.accept(context);
  }

  @Override
  public void onObjectStart(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (context
        .schema()
        .flatMap(FeatureSchema::getRole)
        .filter(r -> r == Role.EMBEDDED_FEATURE)
        .isPresent()) {
      this.embeddedFeatureNestingLevel++;
      this.writeAtEmbeddedFeatureEnd.push(null);
      this.embeddedIds.push(null);
      this.embeddedIdIsInteger.push(null);
    }

    next.accept(context);
  }

  @Override
  public void onObjectEnd(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (embeddedFeatureNestingLevel > 0
        && context
            .schema()
            .flatMap(FeatureSchema::getRole)
            .filter(r -> r == Role.EMBEDDED_FEATURE)
            .isPresent()) {
      if (Boolean.TRUE.equals(writeAtEmbeddedFeatureEnd.pop())) {
        String id = embeddedIds.pop();
        Boolean isInteger = embeddedIdIsInteger.pop();
        if (Objects.nonNull(id)) {
          if (Boolean.TRUE.equals(isInteger)) {
            context.encoding().getJson().writeNumberField("id", Long.parseLong(id));
          } else {
            context.encoding().getJson().writeStringField("id", id);
          }
        }
      }

      this.embeddedFeatureNestingLevel--;
    }

    next.accept(context);
  }

  @Override
  public void onValue(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {

    if (context.schema().isPresent() && Objects.nonNull(context.value())) {
      FeatureSchema currentSchema = context.schema().get();

      if (currentSchema.isId()) {
        String id = context.value();

        // always a string for a multi-collection query
        boolean isInteger =
            currentSchema.getType() == Type.INTEGER
                && context.encoding().getFeatureSchemas().size() == 1;

        if (writeAtFeatureEnd) {
          currentId = id;
          currentIdIsInteger = isInteger;
        } else {
          if (isInteger) context.encoding().getJson().writeNumberField("id", Long.parseLong(id));
          else context.encoding().getJson().writeStringField("id", id);

          addLinks(context, context.value());
        }
      } else if (currentSchema.isEmbeddedId()) {
        String id = context.value();

        // always a string for a multi-collection query
        boolean isInteger =
            currentSchema.getType() == Type.INTEGER
                && context.encoding().getFeatureSchemas().size() == 1;

        if (Boolean.TRUE.equals(writeAtEmbeddedFeatureEnd.peek())) {
          embeddedIds.set(embeddedIds.size() - 1, id);
          embeddedIdIsInteger.set(embeddedIdIsInteger.size() - 1, isInteger);
        } else {
          if (isInteger) context.encoding().getJson().writeNumberField("id", Long.parseLong(id));
          else context.encoding().getJson().writeStringField("id", id);
          this.writeAtEmbeddedFeatureEnd.set(writeAtEmbeddedFeatureEnd.size() - 1, false);
        }
      } else if (embeddedFeatureNestingLevel > 0
          && Objects.isNull(writeAtEmbeddedFeatureEnd.peek())) {
        this.writeAtEmbeddedFeatureEnd.set(writeAtEmbeddedFeatureEnd.size() - 1, true);
      } else {
        this.writeAtFeatureEnd = true;
      }
    }

    next.accept(context);
  }

  @Override
  public void onCoordinates(
      EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next)
      throws IOException {
    this.writeAtFeatureEnd = true;

    next.accept(context);
  }

  private void addLinks(EncodingAwareContextGeoJson context, String featureId) throws IOException {
    if (context.encoding().isFeatureCollection()
        && Objects.nonNull(featureId)
        && !featureId.isEmpty()) {
      context
          .encoding()
          .getState()
          .addCurrentFeatureLinks(
              new ImmutableLink.Builder()
                  .rel("self")
                  .href(
                      context.encoding().getServiceUrl()
                          + "/collections/"
                          + context.encoding().getCollectionId()
                          + "/items/"
                          + featureId)
                  .build());
      Optional<String> template =
          context
              .encoding()
              .getApiData()
              .getCollections()
              .get(context.encoding().getCollectionId())
              .getPersistentUriTemplate();
      if (template.isPresent()) {
        context
            .encoding()
            .getState()
            .addCurrentFeatureLinks(
                new ImmutableLink.Builder()
                    .rel("canonical")
                    .href(StringTemplateFilters.applyTemplate(template.get(), featureId))
                    .build());
      }
    }
  }
}
