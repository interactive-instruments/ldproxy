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
import de.ii.ldproxy.ogcapi.observation_processing.application.Variable;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variables;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class ObservationProcessingVariablesView extends OgcApiView {
    public List<Variable> variables;
    public String uomTitle;
    public String none;
    // sum must be 12 for bootstrap
    public Integer idCols = 3;
    public Integer descCols = 9;

    public ObservationProcessingVariablesView(OgcApiApiDataV2 apiData,
                                              Variables variables,
                                              List<NavigationDTO> breadCrumbs,
                                              String staticUrlPrefix,
                                              HtmlConfig htmlConfig,
                                              boolean noIndex,
                                              URICustomizer uriCustomizer,
                                              I18n i18n,
                                              Optional<Locale> language) {
        super("variables.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                variables.getLinks(),
                i18n.get("variablesTitle", language),
                i18n.get("variablesDescription", language));

        this.variables = variables.getVariables();
        Integer maxIdLength = this.variables
                .stream()
                .map(variable -> variable.getId())
                .filter(id -> Objects.nonNull(id))
                .mapToInt(id -> id.length())
                .max()
                .orElse(0);
        idCols = Math.min(Math.max(1, 1 + maxIdLength/12),4);
        descCols = 12 - idCols;
        this.uomTitle = i18n.get("uomTitle", language);
        this.none = i18n.get ("none", language);
    }
}
