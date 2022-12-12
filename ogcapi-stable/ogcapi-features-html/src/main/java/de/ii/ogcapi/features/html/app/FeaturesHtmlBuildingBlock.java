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
 * @author zahnen
 * @title Features HTML
 * @langEn The module *Features HTML* may be enabled for every API with a feature provider. It
 *     provides the resources *Features* and *Feature* encoded as HTML.
 * @conformanceEn *Features HTML* implements all requirements of conformance class *HTML* of [OGC
 *     API - Features - Part 1: Core
 *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html) for the two mentioned
 *     resources.
 * @langDe Das Modul *Features HTML* kann für jede über ldproxy bereitgestellte API mit einem
 *     Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features
 *     und Feature in HTML.
 * @conformanceDe Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der
 *     Konformitätsklasse "HTML" von [OGC API - Features - Part 1: Core
 *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html).
 * @example {@link de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration}
 * @propertyTable {@link de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration}
 * @queryParameterTable {@link de.ii.ogcapi.features.html.app.QueryParameterBareHtml}
 */
@Singleton
@AutoBind
public class FeaturesHtmlBuildingBlock implements ApiBuildingBlock {

  @Inject
  public FeaturesHtmlBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(true).mapPosition(POSITION.AUTO).style("DEFAULT").build();
  }
}
