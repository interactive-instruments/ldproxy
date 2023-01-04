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
import de.ii.ogcapi.projections.app.ImmutableProjectionsConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Projections
 * @langEn Thinning of feature responses.
 * @langDe Ausdünnen von Features in Rückgaben.
 * @conformanceEn TODO_DOCS
 * @conformanceDe TODO_DOCS
 * @ref:cfgProperties {@link de.ii.ogcapi.projections.app.ImmutableProjectionsConfiguration}
 * @ref:queryParameters {@link de.ii.ogcapi.projections.app.QueryParameterProperties}, {@link
 *     de.ii.ogcapi.projections.app.QueryParameterSkipGeometry}
 */
@Singleton
@AutoBind
public class ProjectionsBuildingBlock implements ApiBuildingBlock {

  @Inject
  public ProjectionsBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
