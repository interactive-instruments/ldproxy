/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.EncodingAwareContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Singleton
@AutoBind
public class JsonFgWriterLinks implements GeoJsonWriter {

    final static String OPEN_TEMPLATE = "{{";
    final static String CLOSE_TEMPLATE = "}}";

    boolean isEnabled;
    List<Link> links;
    List<Link> currentLinks;
    Map<String,String> currentMap = new HashMap<>();

    @Override
    public JsonFgWriterLinks create() {
        return new JsonFgWriterLinks();
    }

    @Override
    public int getSortPriority() {
        return 150;
    }

    @Override
    public void onStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(context.encoding());
        if (isEnabled) {
            links = context.encoding().getApiData()
                                         .getCollections()
                                         .get(context.encoding().getCollectionId())
                                         .getExtension(JsonFgConfiguration.class)
                                         .filter(JsonFgConfiguration::isEnabled)
                                         .map(JsonFgConfiguration::getLinks)
                                         .orElse(ImmutableList.of());
        }

        // next chain for extensions
        next.accept(context);
    }

    @Override
    public void onFeatureStart(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) {
        if (isEnabled) {
            currentLinks = links.stream()
                                .map(link -> {
                                    if (hasTemplate(link))
                                        return link;
                                    context.encoding().getState().addCurrentFeatureLinks(link);
                                    return null;
                                })
                                .filter(Objects::nonNull)
                                .collect(ImmutableList.toImmutableList());
            currentMap.clear();
            currentMap.put("serviceUrl", context.encoding().getServiceUrl());
        }

        // next chain for extensions
        next.accept(context);
    }

    @Override
    public void onValue(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (isEnabled
                && !currentLinks.isEmpty()
                && context.schema()
                          .filter(FeatureSchema::isValue)
                          .isPresent()
                && Objects.nonNull(context.value())) {

            final FeatureSchema schema = context.schema().get();
            currentMap.put(schema.getFullPathAsString(), context.value());
        }

        next.accept(context);
    }

    @Override
    public void onPropertiesEnd(EncodingAwareContextGeoJson context, Consumer<EncodingAwareContextGeoJson> next) throws IOException {
        if (isEnabled) {
            currentLinks.forEach(link -> {
                link = replace(link);
                if (!hasTemplate(link) && !link.getHref().isEmpty() && !link.getRel().isEmpty())
                    context.encoding().getState().addCurrentFeatureLinks(link);
            });
        }

        next.accept(context);
    }

    private Link replace(Link link) {
        if (hasTemplate(link)) {
            final String href = StringTemplateFilters.applyTemplate(link.getHref(), isHtml -> {}, currentMap::get);
            final String rel = StringTemplateFilters.applyTemplate(link.getRel(), isHtml -> {}, currentMap::get);
            final String title = Objects.nonNull(link.getTitle())
                    ? StringTemplateFilters.applyTemplate(link.getTitle(), isHtml -> {}, currentMap::get)
                    : null;
            link = new ImmutableLink.Builder().from(link)
                                              .href(href)
                                              .rel(rel)
                                              .title(title)
                                              .build();
        }
        return link;
    }

    private boolean hasTemplate(Link link) {
        return link.getHref().contains(OPEN_TEMPLATE)
                || Objects.requireNonNullElse(link.getTitle(),"").contains(OPEN_TEMPLATE)
                || Objects.requireNonNullElse(link.getRel(),"").contains(OPEN_TEMPLATE);
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
