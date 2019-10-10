/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import io.dropwizard.views.View;

import java.util.*;
import java.util.stream.Collectors;

public class OgcApiTileMatrixSetView extends View {
    private final OgcApiDatasetData apiData;
    private final List<NavigationDTO> breadCrumbs;
    public final HtmlConfig htmlConfig;
    public String urlPrefix;
    public Map<String, Object> tileMatrixSet;
    public List<Object> links;
    public String idTitle;
    public String boundingBoxTitle;
    public String wellKnownScaleSetTitle;
    public String supportedCrsTitle;
    public String scaleDenominatorTitle;
    public String topLeftCornerTitle;
    public String tileWidthTitle;
    public String tileHeightTitle;
    public String matrixWidthTitle;
    public String matrixHeightTitle;

    public OgcApiTileMatrixSetView(OgcApiDatasetData apiData,
                                   Map<String, Object> tileMatrixSet,
                                   List<NavigationDTO> breadCrumbs,
                                   String staticUrlPrefix,
                                   HtmlConfig htmlConfig,
                                   URICustomizer uriCustomizer,
                                   I18n i18n,
                                   Optional<Locale> language) {
        super("tileMatrixSet.mustache", Charsets.UTF_8);
        this.tileMatrixSet = tileMatrixSet;
        this.links = (tileMatrixSet.get("links") instanceof List ? ((List<Object>) tileMatrixSet.get("links")) : ImmutableList.of());
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = staticUrlPrefix;
        this.htmlConfig = htmlConfig;

        this.idTitle = i18n.get("idTitle", language);
        this.boundingBoxTitle = i18n.get("boundingBoxTitle", language);
        this.wellKnownScaleSetTitle = i18n.get("wellKnownScaleSetTitle", language);
        this.supportedCrsTitle = i18n.get("supportedCrsTitle", language);
        this.scaleDenominatorTitle = i18n.get("scaleDenominatorTitle", language);
        this.topLeftCornerTitle = i18n.get("topLeftCornerTitle", language);
        this.tileWidthTitle = i18n.get("tileWidthTitle", language);
        this.tileHeightTitle = i18n.get("tileHeightTitle", language);
        this.matrixWidthTitle = i18n.get("matrixWidthTitle", language);
        this.matrixHeightTitle = i18n.get("matrixHeightTitle", language);

        this.apiData = apiData;
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        return links
                .stream()
                .filter(link -> link instanceof OgcApiLink && Objects.equals(((OgcApiLink)link).getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> ((OgcApiLink)link).getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(((OgcApiLink)link).getTypeLabel(), ((OgcApiLink)link).getHref()))
                .collect(Collectors.toList());
    }
}
