/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * # `defaultExtent`
 * @lang_en Default value for spatial (`spatial`) and/or temporal (`temporal`) extent for
 * each collection, if not set in the collection configuration. Required keys for
 * spatial extents (all values in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Required keys
 * for temporal extents (all values in milliseconds since 1 January 1970): `start`, `end`.
 * If the spatial extent should be derived from the data source on startup, set `spatialComputed`
 * to `true`. If the temporal extent should be derived from the data source on startup, set
 * `temporalComputed` to `true`. For big datasets this will slow down the startup. Note:
 * This is not the extent for the whole dataset, that will always be derived from the collection extents.
 * @lang_de Es kann ein Standardwert für die räumliche (`spatial`) und/oder zeitliche (`temporal`)
 * Ausdehnung der Daten angeben werden, die bei den Objektarten verwendet wird, wenn dort keine
 * anderslautende Ausdehnung spezifiziert wird. Für die räumliche Ausdehnung sind die folgenden
 * Eigenschaften anzugeben (alle Angaben in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Für die
 * zeitliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in Millisekunden
 * seit dem 1.1.1970): `start`, `end`. Soll die räumliche Ausdehnung aus den Daten einer Objektart
 * standardmäßig automatisch beim Start von ldproxy ermittelt werden, kann `spatialComputed` mit dem
 * Wert `true` angegeben werden. Soll die zeitliche Ausdehnung aus den Daten einer Objektart
 * standardmäßig automatisch beim Start von ldproxy ermittelt werden, kann `temporalComputed`
 * mit dem Wert `true` angegeben werden. Bei großen Datenmengen verzögern diese Optionen allerdings
 * die Zeitdauer, bis die API verfügbar ist. Hinweis: Es handelt sich hierbei nicht um die Ausdehnung
 * des Datensatzes insgesamt, dieser wird stets automatisch aus den Ausdehnungen der einzelnen
 * Objektarten ermittelt.
 * @default
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableCollectionExtent.Builder.class)
public interface CollectionExtent {

    Optional<TemporalExtent> getTemporal();

    Optional<BoundingBox> getSpatial();

    Optional<Boolean> getSpatialComputed();

    Optional<Boolean> getTemporalComputed();
}
