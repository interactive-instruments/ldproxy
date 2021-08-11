/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class JsonFgWriterLinks implements GeoJsonWriter {

    final static String OPEN_TEMPLATE = "{{";
    final static String CLOSE_TEMPLATE = "}}";

    boolean isEnabled;
    List<Link> links;
    List<Link> currentLinks;

    @Override
    public JsonFgWriterLinks create() {
        return new JsonFgWriterLinks();
    }

    @Override
    public int getSortPriority() {
        return 150;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(transformationContext);
        if (isEnabled) {
            links = transformationContext.getApiData()
                                         .getCollections()
                                         .get(transformationContext.getCollectionId())
                                         .getExtension(JsonFgConfiguration.class)
                                         .filter(JsonFgConfiguration::isEnabled)
                                         .map(JsonFgConfiguration::getLinks)
                                         .orElse(ImmutableList.of());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) {
        if (isEnabled) {
            currentLinks = links.stream()
                                .map(link -> {
                                    link = replace(link, "serviceUrl", transformationContext.getServiceUrl());
                                    if (hasTemplate(link))
                                        return link;
                                    transformationContext.getState().addCurrentFeatureLinks(link);
                                    return null;
                                })
                                .filter(Objects::nonNull)
                                .collect(ImmutableList.toImmutableList());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext,
                           Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled
                && !currentLinks.isEmpty()
                && transformationContext.getState()
                                        .getCurrentFeatureProperty()
                                        .isPresent()
                && transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent()) {

            final FeatureProperty currentFeatureProperty = transformationContext.getState()
                                                                                .getCurrentFeatureProperty()
                                                                                .get();
            final String currentPropertyName = currentFeatureProperty.getName();

            final String currentValue = transformationContext.getState()
                                                       .getCurrentValue()
                                                       .get();

            currentLinks = currentLinks.stream()
                                       .map(link -> {
                                           link = replace(link, currentPropertyName, currentValue);
                                           if (hasTemplate(link))
                                               return link;
                                           if (!link.getHref().isEmpty() && !link.getRel().isEmpty())
                                               transformationContext.getState().addCurrentFeatureLinks(link);
                                           return null;
                                       })
                                       .filter(Objects::nonNull)
                                       .collect(ImmutableList.toImmutableList());
        }

        next.accept(transformationContext);
    }

    private Link replace(Link link, String param, String value) {
        if (hasTemplate(link, param)) {
            final String href = StringTemplateFilters.applyTemplate(link.getHref(), value, isHtml -> {}, param);
            final String rel = StringTemplateFilters.applyTemplate(link.getRel(), value, isHtml -> {}, param);
            final String title = Objects.nonNull(link.getTitle())
                    ? StringTemplateFilters.applyTemplate(link.getTitle(), value, isHtml -> {}, param)
                    : null;
            link = new ImmutableLink.Builder().from(link)
                                              .href(href)
                                              .rel(rel)
                                              .title(title)
                                              .build();
        }
        return link;
    }

    private boolean hasTemplate(Link link, String param) {
        final String templateParam = OPEN_TEMPLATE + param + CLOSE_TEMPLATE;
        final String templateParam2 = OPEN_TEMPLATE + param + " ";
        return link.getHref().contains(templateParam)
                || Objects.requireNonNullElse(link.getTitle(),"").contains(templateParam)
                || link.getHref().contains(templateParam2)
                || Objects.requireNonNullElse(link.getTitle(),"").contains(templateParam2);
    }

    private boolean hasTemplate(Link link) {
        return link.getHref().contains(OPEN_TEMPLATE) || Objects.requireNonNullElse(link.getTitle(),"").contains(OPEN_TEMPLATE);
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getApiData()
                                    .getCollections()
                                    .get(transformationContext.getCollectionId())
                                    .getExtension(JsonFgConfiguration.class)
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(cfg -> !Objects.requireNonNullElse(cfg.getLinks(),ImmutableList.of()).isEmpty())
                                    .filter(cfg -> cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.links) ||
                                            transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
                                    .isPresent();
    }
}
