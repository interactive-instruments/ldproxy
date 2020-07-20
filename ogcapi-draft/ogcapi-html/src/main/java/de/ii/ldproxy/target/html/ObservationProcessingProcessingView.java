/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.processing.Process;
import de.ii.ldproxy.ogcapi.features.processing.Processing;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class ObservationProcessingProcessingView extends OgcApiView {
    public List<Process> processes;
    public String linkToApiDefinitionTitle;
    public String mediaTypesTitle;
    public String none;
    // sum must be 12 for bootstrap
    public Integer idCols = 3;
    public Integer descCols = 9;

    public ObservationProcessingProcessingView(OgcApiApiDataV2 apiData,
                                               Processing processing,
                                               List<NavigationDTO> breadCrumbs,
                                               String staticUrlPrefix,
                                               HtmlConfig htmlConfig,
                                               boolean noIndex,
                                               URICustomizer uriCustomizer,
                                               I18n i18n,
                                               Optional<Locale> language) {
        super("dapa.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                processing.getLinks(),
                processing.getTitle().orElse(i18n.get("processingTitle", language)),
                processing.getDescription().orElse(i18n.get("processingDescription", language)));

        this.processes = processing.getEndpoints();
        Integer maxIdLength = this.processes
                .stream()
                .map(process -> process.getId())
                .filter(Objects::nonNull)
                .mapToInt(id -> id.length())
                .max()
                .orElse(0);
        idCols = Math.min(Math.max(1, 1 + maxIdLength/12),4);
        descCols = 12 - idCols;
        this.linkToApiDefinitionTitle = i18n.get("linkToApiDefinitionTitle", language);
        this.mediaTypesTitle = i18n.get("mediaTypesTitle", language);
        this.none = i18n.get ("none", language);
    }
}
