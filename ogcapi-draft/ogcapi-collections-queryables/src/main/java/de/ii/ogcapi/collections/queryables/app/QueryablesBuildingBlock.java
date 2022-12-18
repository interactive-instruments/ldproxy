/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Collections Queryables
 * @langEn Metadata about properties that can be used in queries and filter expressions.
 * @langDe Metadaten über Objekteigenschaften die in Queries und Filter-Ausdrücken verwendet werden
 *     können.
 * @conformanceEn *Collections Queryables* implements all requirements of conformance class
 *     *Queryables* from the draft of [OGC API -
 *     Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html#rc_queryables). The resource will
 *     change in the future due to the harmonization with the requirements for *Queryables* from the
 *     draft of [OGC API - Features - Part 3: Common Query
 *     Language](http://docs.opengeospatial.org/DRAFTS/19-079.html#filter-queryables).
 * @conformanceDe Das Modul basiert auf den Vorgaben der Konformitätsklasse "Filter" aus dem
 *     [Entwurf von OGC API - Features - Part 3:
 *     Filtering](https://docs.ogc.org/DRAFTS/19-079r1.html#filter-queryables).
 * @ref:cfgProperties {@link
 *     de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.collections.queryables.app.EndpointQueryables}
 */
@Singleton
@AutoBind
public class QueryablesBuildingBlock implements ApiBuildingBlock {

  @Inject
  public QueryablesBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
