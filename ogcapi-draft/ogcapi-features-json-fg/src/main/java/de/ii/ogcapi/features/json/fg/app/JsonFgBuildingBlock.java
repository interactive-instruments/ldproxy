/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.json.fg.app;

import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.features.json.fg.domain.ImmutableWhereConfiguration;
import de.ii.ogcapi.features.json.fg.domain.ImmutableJsonFgConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * @title Modul "Features JSON-FG"
 * @langEn The Features JSON-FG module can be enabled for any API provided through ldproxy with a feature provider.
 * It enables the provisioning of the Features and Feature resources in JSON-FG.
 *
 * The module is based on the [drafts for JSON-FG](https://github.com/opengeospatial/ogc-feat-geo-json).
 * The implementation will change as the draft is further standardized.
 * @langDe Das Modul "Features JSON-FG" kann für jede über ldproxy bereitgestellte API mit einem
 * Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features
 * und Feature in JSON-FG.
 *
 * Das Modul basiert auf den [Entwürfen für JSON-FG](https://github.com/opengeospatial/ogc-feat-geo-json).
 * Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.
 * @example {@link de.ii.ogcapi.features.json.fg.domain.JsonFgConfiguration}
 * @propertyTable {@link de.ii.ogcapi.features.json.fg.domain.ImmutableJsonFgConfiguration}
 */
@Singleton
@AutoBind
public class JsonFgBuildingBlock implements ApiBuildingBlock {

    @Inject
    public JsonFgBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                         .describedby(true)
                                                         .when(true)
                                                         .coordRefSys(true)
                                                         .where(new ImmutableWhereConfiguration.Builder().enabled(true)
                                                                                                         .alwaysIncludeGeoJsonGeometry(false)
                                                                                                         .build())
                                                         .build();
    }

}
