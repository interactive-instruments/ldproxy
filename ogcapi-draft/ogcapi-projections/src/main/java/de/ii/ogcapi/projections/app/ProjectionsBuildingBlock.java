/*
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
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.projections.app.ImmutableProjectionsConfiguration.Builder;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Projections
 * @langEn Select the feature properties included in the feature response.
 * @langDe Auswahl der Feature-Eigenschaften in Rückgaben.
 * @conformanceEn *Projections* is based on the [OGC API Features proposal for a new part 'Property
 *     Selection'](https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/property-selection)
 *     and [ongoing discussions](https://github.com/opengeospatial/ogcapi-features/projects/12).
 * @conformanceDe Das Modul basiert auf dem [Vorschlag für einen neuen Teil 'Property Selection' von
 *     OGC API
 *     Features](https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/property-selection)
 *     und [laufenden Diskussionen](https://github.com/opengeospatial/ogcapi-features/projects/12).
 * @ref:cfg {@link de.ii.ogcapi.projections.app.ProjectionsConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.projections.app.ImmutableProjectionsConfiguration}
 * @ref:queryParameters {@link de.ii.ogcapi.projections.app.QueryParameterProperties}, {@link
 *     de.ii.ogcapi.projections.app.QueryParameterSkipGeometry}
 */
@Singleton
@AutoBind
public class ProjectionsBuildingBlock implements ApiBuildingBlock {

  public static final Optional<SpecificationMaturity> MATURITY =
      Optional.of(SpecificationMaturity.DRAFT_OGC);
  public static final Optional<ExternalDocumentation> SPEC =
      Optional.of(
          ExternalDocumentation.of(
              "https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/property-selection#readme",
              "OGC API - Features - Part n: Property Selection (PREDRAFT)"));

  @Inject
  public ProjectionsBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
