/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.wfs3.Wfs3Collection;
import de.ii.ldproxy.wfs3.Wfs3Collections;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import io.dropwizard.views.View;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class Wfs3DatasetView extends View {
    private final Wfs3Collections wfs3Dataset;
    private final List<NavigationDTO> breadCrumbs;
    private final String urlPrefix;
    public final HtmlConfig htmlConfig;

    public Wfs3DatasetView(Wfs3Collections wfs3Dataset, final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig) {
        super("service.mustache", Charsets.UTF_8);
        this.wfs3Dataset = wfs3Dataset;
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;
    }

    public Wfs3Collections getWfs3Dataset() {
        return wfs3Dataset;
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public String getApiUrl() {
        return wfs3Dataset.getLinks().stream()
                .filter(wfs3Link -> wfs3Link.rel.equals("service") && wfs3Link.type.equals(Wfs3MediaTypes.HTML))
                .map(wfs3Link -> wfs3Link.href)
                .findFirst()
                .orElse("");
    }

    public List<FeatureType> getFeatureTypes() {
        return wfs3Dataset.getCollections().stream()
                          .map(FeatureType::new)
                          .collect(Collectors.toList());
    }

    public List<NavigationDTO> getFormats() {
        return wfs3Dataset.getLinks().stream()
                          .filter(wfs3Link -> wfs3Link.rel.equals("alternate"))
                          .map(wfs3Link -> new NavigationDTO(Wfs3MediaTypes.NAMES.get(wfs3Link.type), wfs3Link.href))
                          .collect(Collectors.toList());
    }

    static class FeatureType extends Wfs3Collection {
        public FeatureType(Wfs3Collection collection) {
            super(collection.getName(), collection.getTitle(), collection.getDescription(), collection.getExtent(), collection.getLinks(), collection.getPrefixedName());
        }

        public String getUrl() {
            return this.getLinks().stream()
                              .filter(wfs3Link -> wfs3Link.rel.equals("item") && wfs3Link.type.equals(Wfs3MediaTypes.HTML))
                              .map(wfs3Link -> wfs3Link.href)
                              .findFirst()
                              .orElse("");
        }
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }
}
