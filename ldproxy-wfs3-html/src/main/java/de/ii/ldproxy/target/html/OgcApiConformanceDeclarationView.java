/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import io.dropwizard.views.View;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class OgcApiConformanceDeclarationView extends View {

    private final ConformanceDeclaration conformanceDeclaration;
    private final List<NavigationDTO> breadCrumbs;
    private final String urlPrefix;
    private final HtmlConfig htmlConfig;
    public List<OgcApiLink> links;
    public String title;
    public String description;

    public OgcApiConformanceDeclarationView(ConformanceDeclaration conformanceDeclaration, final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig) {
        super("conformanceDeclaration.mustache", Charsets.UTF_8);
        this.conformanceDeclaration = conformanceDeclaration;
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;
        this.title = conformanceDeclaration
                .getTitle()
                .orElse("Conformance Declaration");
        this.description = conformanceDeclaration
                .getDescription()
                .orElse("");
        this.links = conformanceDeclaration
                .getLinks()
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|home)$"))
                .collect(Collectors.toList());

    }

    public List<String> getClasses() {
        return conformanceDeclaration
                .getConformsTo()
                .stream()
                .sorted()
                .collect(Collectors.toList());
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
