/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.geometry.simplification.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.geometry.simplification.app.ImmutableGeometrySimplificationConfiguration.Builder;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Geometry Simplification
 * @langEn Simplification of geometries with Douglas Peucker.
 * @langDe Vereinfachung von Geometrien mit Douglas-Peucker.
 * @ref:cfg {@link de.ii.ogcapi.geometry.simplification.app.GeometrySimplificationConfiguration}
 * @ref:cfgProperties {@link
 *     de.ii.ogcapi.geometry.simplification.app.ImmutableGeometrySimplificationConfiguration}
 * @ref:queryParameters {@link
 *     de.ii.ogcapi.geometry.simplification.app.QueryParameterMaxAllowableOffsetFeatures}
 */
@Singleton
@AutoBind
public class GeometrySimplificationBuildingBlock implements ApiBuildingBlock {

  public static final Optional<SpecificationMaturity> MATURITY =
      Optional.of(SpecificationMaturity.DRAFT_OGC);
  public static final Optional<ExternalDocumentation> SPEC =
      Optional.of(
          ExternalDocumentation.of(
              "https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/geometry-simplification#readme",
              "OGC API - Features - Part n: Geometry Simplification (PREDRAFT)"));

  @Inject
  public GeometrySimplificationBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
