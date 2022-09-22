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

@Singleton
@AutoBind
public class CapabilityCsv implements ApiBuildingBlock {

  public static int DEFAULT_MULTIPLICITY = 1;

  @Inject
  public CapabilityCsv() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableCsvConfiguration.Builder()
        .enabled(false)
        .maxMultiplicity(DEFAULT_MULTIPLICITY)
        .build();
  }
}
