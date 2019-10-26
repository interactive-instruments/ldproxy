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
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.PageRepresentationWithId;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.wfs3.vt.TileMatrixSets;
import io.dropwizard.views.View;

import java.util.*;
import java.util.stream.Collectors;

public class OgcApiTileMatrixSetsView extends View {
    private final OgcApiDatasetData apiData;
    private final List<NavigationDTO> breadCrumbs;
    public final HtmlConfig htmlConfig;
    public List<OgcApiLink> links;
    public String urlPrefix;
    public String title;
    public String description;
    public List<PageRepresentationWithId> tileMatrixSets;
    public String none;

    public OgcApiTileMatrixSetsView(OgcApiDatasetData apiData,
                                    TileMatrixSets tileMatrixSets,
                                    List<NavigationDTO> breadCrumbs,
                                    String staticUrlPrefix,
                                    HtmlConfig htmlConfig,
                                    URICustomizer uriCustomizer,
                                    I18n i18n,
                                    Optional<Locale> language) {
        super("tileMatrixSets.mustache", Charsets.UTF_8);
        this.tileMatrixSets = tileMatrixSets.getTileMatrixSets();
        this.links = tileMatrixSets.getLinks();
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = staticUrlPrefix;
        this.htmlConfig = htmlConfig;

        this.title = i18n.get("tileMatrixSetsTitle", language);
        this.description = i18n.get("tileMatrixSetsDescription", language);
        this.none = i18n.get ("none", language);

        this.apiData = apiData;
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        return links.stream()
                .filter(link -> Objects.equals(link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                .collect(Collectors.toList());
    }
}
