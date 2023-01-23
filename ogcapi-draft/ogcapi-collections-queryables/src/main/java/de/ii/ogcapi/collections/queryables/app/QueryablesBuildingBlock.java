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
 * @title Feature Collections - Queryables
 * @langEn Metadata about the properties of the features in a feature collection that can be used in
 *     filter expressions.
 * @langDe Metadaten über die Eigenschaften von Objekten aus einer Feature Collection, die in
 *     Filter-Ausdrücken verwendet werden können.
 * @scopeEn The queryables are represented as a schema where each queryable is a property. The
 *     schema for each queryable is automatically derived from the definition of the property in the
 *     feature provider. Supported encodings are JSON Schema 2019-09 and HTML.
 * @scopeDe Die Queryables werden als Schema kodiert, wobei jede Queryable eine Objekteigenschaft
 *     ist. Das Schema für jede abfragbare Eigenschaft wird automatisch aus der Definition der
 *     Eigenschaft im Feature-Provider abgeleitet. Unterstützte Kodierungen sind JSON Schema 2019-09
 *     und HTML.
 * @conformanceEn *Feature Collections - Queryables* implements all requirements and recommendations
 *     of section 6.2 ("Queryables") of the [draft OGC API - Features - Part 3:
 *     Filtering](https://docs.ogc.org/DRAFTS/19-079r1.html#filter-queryables).
 * @conformanceDe Das Modul implementiert die Vorgaben und Empfehlungen aus Abschnitt 6.2
 *     ("Queryables") des [Entwurfs von OGC API - Features - Part 3:
 *     Filtering](https://docs.ogc.org/DRAFTS/19-079r1.html#filter-queryables).
 * @ref:cfgProperties {@link
 *     de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.collections.queryables.app.EndpointQueryables}
 * @ref:queryParameters {@link de.ii.ogcapi.collections.queryables.app.QueryParameterFQueryables}
 * @ref:pathParameters {@link
 *     de.ii.ogcapi.collections.queryables.app.PathParameterCollectionIdQueryables}
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
