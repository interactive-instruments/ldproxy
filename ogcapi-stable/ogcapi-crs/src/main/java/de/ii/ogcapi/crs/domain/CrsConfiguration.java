/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crs.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.util.Set;
import org.immutables.value.Value;

/**
 * @langEn The default coordinate reference system `ogc:CRS84` corresponds to `epsg:4326` with the
 *     axis order longitude/latitude, `ogc:CRS84h` corresponds to `epsg:4979` in the axis order
 *     longitude/latitude/height. By specifying the query parameter `crs` at the resources
 *     "Features" and "Feature" the coordinates can be requested in one of the configured coordinate
 *     reference systems.
 * @langDe Das Default-Koordinatenreferenzsystem `ogc:CRS84` entspricht `epsg:4326` in der
 *     Achsenreihenfolge Länge/Breite, `ogc:CRS84h` entspricht `epsg:4979` in der Achsenreihenfolge
 *     Länge/Breite/Höhe. Durch Angabe des Query-Parameters `crs` bei den Ressourcen "Features" und
 *     "Feature" können die Koordinaten in einem der konfigurierten Koordinatenreferenzsysteme
 *     angefordert werden.
 * @example <code>
 * ```yaml
 * - buildingBlock: CRS
 *   additionalCrs:
 *   - code: 25832
 *     forceAxisOrder: NONE
 *   - code: 4258
 *     forceAxisOrder: NONE
 *   - code: 4326
 *     forceAxisOrder: NONE
 *   - code: 3857
 *     forceAxisOrder: NONE
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCrsConfiguration.Builder.class)
public interface CrsConfiguration extends ExtensionConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn Add additonal coordinate reference systems to an API or a collection.
   * @langDe Steuert, welche weitere Koordinatenreferenzsysteme in einer API oder für eine Feature
   *     Collection unterstützt werden sollen. Das native Koordinatenreferenzsystem der Daten und
   *     das Default-Koordinatenreferenzsystem der API sind automatisch aktiviert.
   *     Koordinatenreferenzsysteme werden über ihren EPSG-Code identifiziert (`code`). Zusätzlich
   *     ist in `forceAxisOrder` die Reihenfolge der Koordinatenachsen anzugeben (`NONE`: wie im
   *     Koordinatenreferenzsystem, `LON_LAT` oder `LAT_LON`: die Reihenfolge im
   *     Koordinatenreferenzsystem wird ignoriert und die angegebene Reihenfolge wird verwendet).
   * @default `{}`
   */
  Set<EpsgCrs> getAdditionalCrs();

  @Override
  default Builder getBuilder() {
    return new ImmutableCrsConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCrsConfiguration.Builder builder = getBuilder().from(source).from(this);

    getAdditionalCrs()
        .forEach(
            epsgCrs -> {
              if (!((CrsConfiguration) source).getAdditionalCrs().contains(epsgCrs)) {
                builder.addAdditionalCrs(epsgCrs);
              }
            });

    return builder.build();
  }
}
