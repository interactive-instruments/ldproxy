/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.csv.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.csv.domain.ImmutableCsvConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features - CSV
 * @langEn Encode features as comma-separated values (CSV).
 * @langDe Kodierung von Features als komma-separierte Werte (CSV).
 * @scopeEn Feature properties that are arrays or objects are always flattened using the `flatten`
 *     transformation, by default using '.' as the separator. Array properties are limited to
 *     `maxMultiplicity` values.
 *     <p>Geometry properties are ignored.
 * @scopeDe Objekteigenschaften, bei denen es sich um Arrays oder Objekte handelt, werden immer mit
 *     der Transformation `flatten` abgeflacht, wobei standardmäßig '.' als Trennzeichen verwendet
 *     wird. Array-Eigenschaften sind auf `maxMultiplicity`-Werte beschränkt.
 *     <p>Geometrien werden nicht kodiert.
 * @ref:cfg {@link de.ii.ogcapi.features.csv.domain.CsvConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.features.csv.domain.ImmutableCsvConfiguration}
 */
@Singleton
@AutoBind
public class FeaturesCsvBuildingBlock implements ApiBuildingBlock {

  public static int DEFAULT_MULTIPLICITY = 3;

  @Inject
  public FeaturesCsvBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableCsvConfiguration.Builder()
        .enabled(false)
        .maxMultiplicity(DEFAULT_MULTIPLICITY)
        .build();
  }
}
