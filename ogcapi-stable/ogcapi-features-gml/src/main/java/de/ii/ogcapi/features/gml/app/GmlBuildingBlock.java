/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * # Features GML (GML)
 * @langEn The module *Features GML* may be enabled for every API with a WFS feature provider.
 * It provides the resources *Features* and *Feature* encoded as GML.
 *
 * @conformanceEn *Features GML* implements all requirements of conformance class *Geography Markup Language
 * (GML), Simple Features Profile, Level 2* from
 * [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_gmlsf2)
 * for the two mentioned resources.
 *
 * This module has no configuration options.
 * @langDe Das Modul "Features GML" kann für jede über ldproxy bereitgestellte API mit einem
 * WFS-Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen
 * Features und Feature in GML.
 *
 * @conformanceDe Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der
 * Konformitätsklasse "Geography Markup Language (GML), Simple Features Profile, Level 2" von
 * [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_gmlsf2).
 *
 * In der Konfiguration können keine weiteren Optionen gewählt werden.
 * @propertyTable {@link de.ii.ogcapi.features.gml.app.GmlConfiguration}
 */
@Singleton
@AutoBind
public class GmlBuildingBlock implements ApiBuildingBlock {

    @Inject
    public GmlBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableGmlConfiguration.Builder().enabled(false)
                                                      .build();
    }

}
