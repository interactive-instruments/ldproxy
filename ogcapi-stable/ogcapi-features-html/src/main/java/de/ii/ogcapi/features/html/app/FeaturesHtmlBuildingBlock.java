/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * @title Features HTML (FEATURES_HTML)
 * @en The module *Features HTML* may be enabled for every API with a feature provider. It provides the
 * resources *Features* and *Feature* encoded as HTML.
 *
 * *Features HTML* implements all requirements of conformance class *HTML* of [OGC API - Features -
 * Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html) for the two mentioned
 * resources.
 * @de Das Modul "Features HTML" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider
 * aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features und Feature in HTML.
 *
 * Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der Konformitätsklasse
 * "HTML" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html).
 * @see de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration
 */
@Singleton
@AutoBind
public class FeaturesHtmlBuildingBlock implements ApiBuildingBlock {

    @Inject
    public FeaturesHtmlBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder()
            .enabled(true)
            .mapPosition(POSITION.AUTO)
            .style("DEFAULT")
            .build();
    }

}
