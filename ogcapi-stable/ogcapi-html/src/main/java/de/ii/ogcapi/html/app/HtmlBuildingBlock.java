/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.html.domain.ImmutableHtmlConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title HTML
 * @langEn HTML encoding for every supported resource.
 * @langDe HTML-Enkodierung für alle unterstützten Ressourcen.
 * @conformanceEn TODO_DOCS
 * @conformanceDe TODO_DOCS
 * @ref:cfg {@link de.ii.ogcapi.html.domain.HtmlConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.html.domain.ImmutableHtmlConfiguration}
 */
@Singleton
@AutoBind
public class HtmlBuildingBlock implements ApiBuildingBlock {

  @Inject
  public HtmlBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableHtmlConfiguration.Builder()
        .enabled(true)
        .noIndexEnabled(true)
        .schemaOrgEnabled(true)
        .collectionDescriptionsInOverview(false)
        .sendEtags(false)
        .legalName("Legal notice")
        .legalUrl("")
        .privacyName("Privacy notice")
        .privacyUrl("")
        .basemapUrl("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
        .basemapAttribution(
            "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors")
        .defaultStyle("NONE")
        .footerText("")
        .build();
  }
}
