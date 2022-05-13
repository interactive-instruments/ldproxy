/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.projections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.projections.app.ImmutableProjectionsConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * @title Projections
 * @langEn The module *Projections* may be enabled for every API with a feature provider.
 * It adds the following query parameters:
 *
 * * `properties` (for resources *Features*, *Feature* and *Vector Tile*): if set only the
 * given properties are included in the output. Only applies to GeoJSON  `properties` and
 * Mapbox Vector Tiles `tags`.
 * * `skipGeometry` (for resources *Features* and *Feature*): if set to `true`, geometries
 * will be skipped in the output.<br>_since version 2.2
 *
 * This module has no configuration options.
 * @langDe Das Modul "Projections" kann für jede über ldproxy bereitgestellte API mit einem
 * Feature-Provider aktiviert werden. Es ergänzt die folgenden Query-Parameter:
 *
 * * `properties` (Ressourcen "Features", "Feature" und "Vector Tile"): Ist der Parameter
 * angegeben, werden nur die angegeben Objekteigenschaften ausgegeben. Die Angabe begrenzt
 * nur die Eigenschaften, die in GeoJSON im `properties`-Objekt bzw. in Mapbox Vector Tiles
 * im `tags`-Feld enthalten sind;
 * * `skipGeometry` (Ressourcen "Features" und "Feature"): Bei `true` werden Geometrien in der
 * Ausgabe unterdrückt.
 *
 * In der Konfiguration können keine Optionen gewählt werden.
 * @propertyTable {@link de.ii.ogcapi.projections.app.ImmutableProjectionsConfiguration}
 * @queryParameterTable {@link de.ii.ogcapi.projections.app.QueryParameterProperties},
 * {@link de.ii.ogcapi.projections.app.QueryParameterSkipGeometry}
 */
@Singleton
@AutoBind
public class ProjectionsBuildingBlock implements ApiBuildingBlock {

    @Inject
    public ProjectionsBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                              .build();
    }

}
