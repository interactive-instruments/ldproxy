/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.immutables.value.Value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableStyleMetadata.class)
public abstract class StyleMetadata extends PageRepresentation {

    public abstract Optional<String> getId();

    public abstract List<String> getKeywords();

    public abstract Optional<String> getPointOfContact();

    public abstract Optional<String> getAccessConstraints();

    public abstract Optional<StyleDates> getDates();

    public abstract Optional<String> getScope();

    public abstract Optional<String> getVersion();

    public abstract List<StylesheetMetadata> getStylesheets();

    public abstract List<StyleLayer> getLayers();

    @JsonIgnore
    public StyleMetadata replaceParameters(URICustomizer uriCustomizer) {

        // any template parameters in links?
        boolean templated = this.getStylesheets()
                                    .stream()
                                    .map(styleSheet -> styleSheet.getLink().orElse(null))
                                    .filter(Objects::nonNull)
                                    .anyMatch(link -> Objects.requireNonNullElse(link.getTemplated(),false) && link.getHref().matches("^.*\\{serviceUrl\\}.*$")) ||
                this.getLayers()
                        .stream()
                        .map(layer -> layer.getSampleData().orElse(null))
                        .filter(Objects::nonNull)
                        .anyMatch(link -> Objects.requireNonNullElse(link.getTemplated(),false) && link.getHref().matches("^.*\\{serviceUrl\\}.*$")) ||
                this.getLinks()
                        .stream()
                        .anyMatch(link -> Objects.requireNonNullElse(link.getTemplated(),false) && link.getHref().matches("^.*\\{serviceUrl\\}.*$"));

        if (!templated)
            return this;

        String serviceUrl = uriCustomizer.copy()
                                         .removeLastPathSegments(3)
                                         .clearParameters()
                                         .ensureNoTrailingSlash()
                                         .toString();

        return ImmutableStyleMetadata.builder()
                                     .from(this)
                                     .stylesheets(this.getStylesheets()
                                                          .stream()
                                                          .map(styleSheet -> ImmutableStylesheetMetadata.builder()
                                                                                                        .from(styleSheet)
                                                                                                        .link(!(styleSheet.getLink().isPresent()) ?
                                                                                                              Optional.empty() :
                                                                                                              Objects.requireNonNullElse(styleSheet.getLink().get().getTemplated(), false) ?
                                                                                                                      Optional.of(new ImmutableLink.Builder()
                                                                                                                                          .from(styleSheet.getLink().get())
                                                                                                                                          .href(styleSheet.getLink().get()
                                                                                                                                                          .getHref()
                                                                                                                                                          .replace("{serviceUrl}", serviceUrl))
                                                                                                                                          .templated(null)
                                                                                                                                          .build()) :
                                                                                                                      styleSheet.getLink())
                                                                                                        .build())
                                                          .collect(ImmutableList.toImmutableList()))
                                     .layers(this.getLayers()
                                                     .stream()
                                                     .map(layer -> ImmutableStyleLayer.builder()
                                                                                      .from(layer)
                                                                                      .sampleData(!(layer.getSampleData().isPresent()) ?
                                                                                                          Optional.empty() :
                                                                                                          Objects.requireNonNullElse(layer.getSampleData().get().getTemplated(), false) ?
                                                                                                                  Optional.of(new ImmutableLink.Builder()
                                                                                                                                      .from(layer.getSampleData()
                                                                                                                                                 .get())
                                                                                                                                      .href(layer.getSampleData()
                                                                                                                                                 .get()
                                                                                                                                                 .getHref()
                                                                                                                                                 .replace("{serviceUrl}", serviceUrl))
                                                                                                                                      .templated(null)
                                                                                                                                      .build()) :
                                                                                                                  layer.getSampleData())
                                                                                      .build())
                                                     .collect(ImmutableList.toImmutableList()))
                                     .links(this.getLinks()
                                                    .stream()
                                                    .map(link -> new ImmutableLink.Builder()
                                                            .from(link)
                                                            .href(link.getHref()
                                                                      .replace("{serviceUrl}", serviceUrl))
                                                            .templated(null)
                                                            .build())
                                                    .collect(ImmutableList.toImmutableList()))
                                     .build();
    }
}
