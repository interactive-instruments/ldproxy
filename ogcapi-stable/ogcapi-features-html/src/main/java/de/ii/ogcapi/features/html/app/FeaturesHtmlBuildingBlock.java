/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features - HTML
 * @langEn Encode features as HTML.
 * @langDe Kodierung von Features als HTML.
 * @conformanceEn *Features HTML* implements all requirements of conformance class *HTML* of [OGC
 *     API - Features - Part 1: Core 1.0](https://docs.ogc.org/is/17-069r4/17-069r4.html#rc_html)
 *     for the two mentioned resources.
 * @conformanceDe Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der
 *     Konformitätsklasse "HTML" von [OGC API - Features - Part 1: Core
 *     1.0](https://docs.ogc.org/is/17-069r4/17-069r4.html#rc_html).
 * @ref:cfg {@link de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration}
 */
@Singleton
@AutoBind
public class FeaturesHtmlBuildingBlock implements ApiBuildingBlock {

  @Inject
  public FeaturesHtmlBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder()
        .enabled(true)
        .mapPosition(POSITION.AUTO)
        .style("DEFAULT")
        .propertyTooltips(true)
        .propertyTooltipsOnItems(false)
        .build();
  }
}
