/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.Process;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.Processing;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;

import java.util.*;
import java.util.stream.Collectors;

public class ObservationProcessingProcessingView extends OgcApiView {
    private List<Process> processes;
    public final String linkToApiDefinitionTitle;
    public final String linkToDapaEndpointTitle;
    public String mediaTypesTitle;
    public String none;
    // sum must be 12 for bootstrap
    public Integer idCols = 3;
    public Integer descCols = 9;

    public ObservationProcessingProcessingView(OgcApiDataV2 apiData,
                                               Processing processing,
                                               List<NavigationDTO> breadCrumbs,
                                               String staticUrlPrefix,
                                               HtmlConfiguration htmlConfig,
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
        this.linkToDapaEndpointTitle = i18n.get("linkToDapaEndpointTitle", language);
        this.linkToApiDefinitionTitle = i18n.get("linkToApiDefinitionTitle", language);
        this.mediaTypesTitle = i18n.get("mediaTypesTitle", language);
        this.none = i18n.get ("none", language);
    }

    public List<Map<String, Object>> getProcesses() {

        Comparator<Process> byId = Comparator.comparing(process -> process.getId());

        return processes.stream()
                .sorted(byId)
                .map(process -> new ImmutableMap.Builder<String, Object>()
                        .put("title", process.getTitle().orElse(process.getId()))
                        .put("description", process.getDescription().orElse(""))
                        .put("id", process.getId())
                        .put("mediaTypes", String.join(", ", process.getMediaTypes()))
                        .put("externalDocs", process.getExternalDocs())
                        .put("descriptionLink", process.getLinks()
                                .stream()
                                .filter(link -> link.getRel().equalsIgnoreCase("ogc-dapa-endpoint-documentation"))
                                .findFirst()
                                .orElse(null))
                        .put("dapaEndpointLink", process.getLinks()
                                .stream()
                                .filter(link -> link.getRel().equalsIgnoreCase("ogc-dapa-endpoint"))
                                .findFirst()
                                .orElse(null))
                        .build())
                .collect(Collectors.toList());
    }
}
