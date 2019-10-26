/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import io.dropwizard.views.View;

import java.util.*;
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

    public OgcApiConformanceDeclarationView(ConformanceDeclaration conformanceDeclaration, final List<NavigationDTO> breadCrumbs,
                                            String urlPrefix, HtmlConfig htmlConfig, I18n i18n, Optional<Locale> language) {
        super("conformanceDeclaration.mustache", Charsets.UTF_8);
        this.conformanceDeclaration = conformanceDeclaration;
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;
        this.title = conformanceDeclaration
                .getTitle()
                .orElse(i18n.get("conformanceDeclarationTitle", language));
        this.description = conformanceDeclaration
                .getDescription()
                .orElse(i18n.get("conformanceDeclarationDescription", language));
        this.links = conformanceDeclaration
                .getLinks();
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
