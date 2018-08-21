/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import io.dropwizard.views.View;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class Wfs3ConformanceClassesView extends View {

    private final List<String> classes;
    private final List<NavigationDTO> breadCrumbs;
    private final List<Wfs3Link> links;
    private final String urlPrefix;
    public final HtmlConfig htmlConfig;

    public Wfs3ConformanceClassesView(List<String> classes, final List<NavigationDTO> breadCrumbs, List<Wfs3Link> links, String urlPrefix, HtmlConfig htmlConfig) {
        super("conformanceclasses.mustache", Charsets.UTF_8);
        this.classes = classes;
        this.breadCrumbs = breadCrumbs;
        this.links = links;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;
    }

    public List<String> getClasses() {
        return classes;
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        return links
                .stream()
                .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                                                         .toUpperCase()))
                .map(wfs3Link -> new NavigationDTO(wfs3Link.getTypeLabel(), wfs3Link.getHref()))
                .collect(Collectors.toList());
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }
}
