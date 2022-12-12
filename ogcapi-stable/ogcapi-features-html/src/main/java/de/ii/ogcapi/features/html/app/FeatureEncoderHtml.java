/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureEncoderHtml extends FeatureObjectEncoder<PropertyHtml, FeatureHtml> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderHtml.class);
  private static final String SCHEMA_PLACE = "http://schema.org/Place";
  private static final String LIMIT_OFFSET = "limit=%d&offset=%d";

  private final FeatureTransformationContextHtml transformationContext;

  public FeatureEncoderHtml(FeatureTransformationContextHtml transformationContext) {
    super();
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
    if (transformationContext.isFeatureCollection()
        && context.metadata().getNumberReturned().isPresent()) {
      long returned = context.metadata().getNumberReturned().getAsLong();
      long matched = context.metadata().getNumberMatched().orElse(-1);

      long pages = Math.max(transformationContext.getPage(), 0);
      if (returned > 0 && matched > -1) {
        pages =
            Math.max(
                pages,
                matched / transformationContext.getLimit()
                    + (matched % transformationContext.getLimit() > 0 ? 1 : 0));
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("numberMatched {}", matched);
        LOGGER.debug("numberReturned {}", returned);
        LOGGER.debug("pageSize {}", transformationContext.getLimit());
        LOGGER.debug("page {}", transformationContext.getPage());
        LOGGER.debug("pages {}", pages);
      }

      ImmutableList.Builder<NavigationDTO> pagination = new ImmutableList.Builder<>();
      ImmutableList.Builder<NavigationDTO> metaPagination = new ImmutableList.Builder<>();
      initPagination(pagination, metaPagination);

      if (matched > -1) {
        withNumberMatched(pages, pagination, metaPagination);
      } else {
        withoutNumberMatched(returned, pagination, metaPagination);
      }

      transformationContext.collectionView().pagination = pagination.build();
      transformationContext.collectionView().metaPagination = metaPagination.build();

    } else if (transformationContext.isFeatureCollection()) {
      LOGGER.error(
          "Pagination not supported by feature provider, the number of matched items was not provided.");
    }
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private void withoutNumberMatched(
      long returned,
      ImmutableList.Builder<NavigationDTO> pagination,
      ImmutableList.Builder<NavigationDTO> metaPagination) {
    int from = Math.max(1, transformationContext.getPage() - 2);
    int to = transformationContext.getPage();
    for (int i = from; i <= to; i++) {
      if (i == transformationContext.getPage()) {
        pagination.add(new NavigationDTO(String.valueOf(i), true));
      } else {
        pagination.add(
            new NavigationDTO(
                String.valueOf(i),
                String.format(
                    LIMIT_OFFSET,
                    transformationContext.getLimit(),
                    (i - 1) * transformationContext.getLimit())));
      }
    }
    if (returned >= transformationContext.getLimit()) {
      pagination.add(
          new NavigationDTO(
              "›",
              String.format(
                  LIMIT_OFFSET,
                  transformationContext.getLimit(),
                  transformationContext.getOffset() + transformationContext.getLimit())));
      metaPagination.add(
          new NavigationDTO(
              "next",
              String.format(
                  LIMIT_OFFSET,
                  transformationContext.getLimit(),
                  transformationContext.getOffset() + transformationContext.getLimit())));
    } else {
      pagination.add(new NavigationDTO("›"));
    }
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private void withNumberMatched(
      long pages,
      ImmutableList.Builder<NavigationDTO> pagination,
      ImmutableList.Builder<NavigationDTO> metaPagination) {
    long from = Math.max(1, transformationContext.getPage() - 2);
    long to = Math.min(pages, from + 4);
    if (to == pages) {
      from = Math.max(1, to - 4);
    }
    for (long i = from; i <= to; i++) {
      if (i == transformationContext.getPage()) {
        pagination.add(new NavigationDTO(String.valueOf(i), true));
      } else {
        pagination.add(
            new NavigationDTO(
                String.valueOf(i),
                String.format(
                    LIMIT_OFFSET,
                    transformationContext.getLimit(),
                    (i - 1) * transformationContext.getLimit())));
      }
    }

    if (transformationContext.getPage() < pages) {
      pagination
          .add(
              new NavigationDTO(
                  "›",
                  String.format(
                      LIMIT_OFFSET,
                      transformationContext.getLimit(),
                      transformationContext.getOffset() + transformationContext.getLimit())))
          .add(
              new NavigationDTO(
                  "»",
                  String.format(
                      LIMIT_OFFSET,
                      transformationContext.getLimit(),
                      (pages - 1) * transformationContext.getLimit())));
      metaPagination.add(
          new NavigationDTO(
              "next",
              String.format(
                  LIMIT_OFFSET,
                  transformationContext.getLimit(),
                  transformationContext.getOffset() + transformationContext.getLimit())));
    } else {
      pagination.add(new NavigationDTO("›")).add(new NavigationDTO("»"));
    }
  }

  private void initPagination(
      ImmutableList.Builder<NavigationDTO> pagination,
      ImmutableList.Builder<NavigationDTO> metaPagination) {
    if (transformationContext.getPage() > 1) {
      pagination
          .add(
              new NavigationDTO(
                  "«", String.format(LIMIT_OFFSET, transformationContext.getLimit(), 0)))
          .add(
              new NavigationDTO(
                  "‹",
                  String.format(
                      LIMIT_OFFSET,
                      transformationContext.getLimit(),
                      transformationContext.getOffset() - transformationContext.getLimit())));
      metaPagination.add(
          new NavigationDTO(
              "prev",
              String.format(
                  LIMIT_OFFSET,
                  transformationContext.getLimit(),
                  transformationContext.getOffset() - transformationContext.getLimit())));
    } else {
      pagination.add(new NavigationDTO("«")).add(new NavigationDTO("‹"));
    }
  }

  @Override
  public void onFeature(FeatureHtml feature) {
    transformationContext
        .featuresHtmlConfiguration()
        .getFeatureTitleTemplate()
        .ifPresent(
            template -> {
              Function<String, String> lookup =
                  pathString ->
                      feature
                          .findPropertyByPath(pathString)
                          .map(PropertyHtml::getFirstValue)
                          .orElse(null);
              String name = StringTemplateFilters.applyTemplate(template, lookup);
              if (Objects.nonNull(name) && !name.isEmpty()) {
                feature.name(name);
              }
            });

    // TODO: generalize as value transformer
    if (transformationContext.getI18n().isPresent()) {
      translateBooleans(
          feature.getProperties(),
          transformationContext.getI18n().get(),
          transformationContext.getLanguage());
    }

    if (transformationContext.isFeatureCollection()) {
      feature.inCollection(true);
    } else {
      transformationContext.collectionView().title = feature.getName();
      transformationContext
              .collectionView()
              .breadCrumbs
              .get(transformationContext.collectionView().breadCrumbs.size() - 1)
              .label =
          feature.getName();
    }

    if (transformationContext.isSchemaOrgEnabled()) {
      feature.itemType(SCHEMA_PLACE);
    }

    transformationContext.collectionView().features.add(feature);
  }

  @Override
  public void onEnd(ModifiableContext context) {

    try (OutputStreamWriter writer =
        new OutputStreamWriter(new OutputStreamToByteConsumer(this::push))) {
      transformationContext
          .mustacheRenderer()
          .render(transformationContext.collectionView(), writer);
      writer.flush();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void translateBooleans(
      List<PropertyHtml> properties, I18n i18n, Optional<Locale> language) {
    for (PropertyHtml property : properties) {
      if (property.isValue()) {
        if (Objects.nonNull(property.getValue())
            && property
                .getSchema()
                .filter(schema -> schema.getType() == Type.BOOLEAN)
                .isPresent()) {
          if (property.getValue().matches("[fF](alse|ALSE)?|0")) {
            property.value(i18n.get("false", language));
          } else if (property.getValue().matches("[tT](rue|RUE)?|[\\-+]?1")) {
            property.value(i18n.get("true", language));
          }
        }
      } else {
        translateBooleans(property.getNestedProperties(), i18n, language);
      }
    }
  }
}
