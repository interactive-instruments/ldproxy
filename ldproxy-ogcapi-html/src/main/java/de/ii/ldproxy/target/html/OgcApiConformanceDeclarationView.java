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
import de.ii.ldproxy.ogcapi.domain.ConformanceDeclaration;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class OgcApiConformanceDeclarationView extends LdproxyView {

    private final ConformanceDeclaration conformanceDeclaration;

    public OgcApiConformanceDeclarationView(ConformanceDeclaration conformanceDeclaration, final List<NavigationDTO> breadCrumbs,
                                            String urlPrefix, HtmlConfig htmlConfig, boolean noIndex, I18n i18n, Optional<Locale> language) {
        super("conformanceDeclaration.mustache", Charsets.UTF_8, null, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                conformanceDeclaration.getLinks(),
                conformanceDeclaration
                        .getTitle()
                        .orElse(i18n.get("conformanceDeclarationTitle", language)),
                conformanceDeclaration
                        .getDescription()
                        .orElse(i18n.get("conformanceDeclarationDescription", language)));
        this.conformanceDeclaration = conformanceDeclaration;
    }

    public List<String> getClasses() {
        return conformanceDeclaration
                .getConformsTo()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
