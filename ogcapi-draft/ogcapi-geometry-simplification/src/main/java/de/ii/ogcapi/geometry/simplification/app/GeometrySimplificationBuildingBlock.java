/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.geometry.simplification.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.geometry.simplification.app.ImmutableGeometrySimplificationConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * # Geometry Simplification (GEOMETRY_SIMPLIFICATION)
 * @lang_en The module *Geometry Simplification* may be enabled for every API with a feature provider.
 * It adds the following query parameters:
 *
 * * `maxAllowableOffset` (for resources *Features* and *Feature*): if set all geometries are
 * simplified using the
 * [Douglas Peucker algorithm](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm).
 * The value defines the maximum distance between original and simplified geometry
 * ([Hausdorff distance](https://en.wikipedia.org/wiki/Hausdorff_distance)).
 * The value has to use the unit of the given coordinate reference system
 * (`CRS84` or the value of parameter `crs`).
 *
 * This module has no configuration options.
 * @lang_de Das Modul "Geometry Simplification" kann für jede über ldproxy bereitgestellte API mit
 * einem Feature-Provider aktiviert werden. Es ergänzt den Query-Parameter `maxAllowableOffset`
 * für die Ressourcen "Features" und "Feature". Ist der Parameter angegeben, werden alle
 * Geometrien mit dem
 * [Douglas-Peucker-Algorithmus](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm)
 * vereinfacht. Der Wert von `maxAllowableOffset` legt den maximalen Abstand zwischen der
 * Originalgeometrie und der vereinfachten Geometrie fest
 * ([Hausdorff-Abstand](https://en.wikipedia.org/wiki/Hausdorff_distance)).
 * Der Wert ist in den Einheiten des Koordinatenreferenzsystems der Ausgabe
 * (`CRS84` bzw. der Wert des Parameters Query-Parameters `crs`) angegeben.
 *
 * In der Konfiguration können keine Optionen gewählt werden.
 * @see GeometrySimplificationConfiguration
 * @see QueryParameterMaxAllowableOffsetFeatures
 */
@Singleton
@AutoBind
public class GeometrySimplificationBuildingBlock implements ApiBuildingBlock {

    @Inject
    public GeometrySimplificationBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                                         .build();
    }

}
