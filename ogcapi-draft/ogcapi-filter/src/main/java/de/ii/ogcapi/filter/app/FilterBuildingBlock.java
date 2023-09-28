/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.filter.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.filter.domain.ImmutableFilterConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Filter
 * @langEn Filter features with CQL2 expressions.
 * @langDe Features mit CQL2-Ausdrücken filtern.
 * @scopeEn This module provides query parameters to filter features using CQL2 (in Text or JSON
 *     encoding). Depending on the feature provider, some capabilities of CQL2 may not be supported.
 *     Specifically, in GeoPackage feature providers, queryables in a JSON column that are arrays
 *     are not supported. In PostgreSQL/PostGIS feature providers, the `A_OVERLAPS` operator is not
 *     supported for queryables in a JSON column.
 * @scopeDe Dieses Modul bietet Abfrageparameter zum Filtern von Features mit CQL2 (in Text- oder
 *     JSON-Kodierung). Je nach Feature-Provider werden einige Funktionen von CQL2 möglicherweise
 *     nicht unterstützt. Insbesondere werden in GeoPackage Feature-Providern Queryables in einer
 *     JSON-Spalte, die Arrays sind, nicht unterstützt. In PostgreSQL/PostGIS-Feature-Anbietern wird
 *     der Operator `A_OVERLAPS` für Queryables in einer JSON-Spalte nicht unterstützt.
 * @conformanceEn This module implements requirements of the conformance classes *Filter* and
 *     *Features Filter* from the draft specification [OGC API - Features - Part 3: Common Query
 *     Language](https://docs.ogc.org/DRAFTS/19-079r1.html) as well as the conformance classes
 *     *Basic CQL2*, *Advanced Comparison Operators*, *Case-insensitive Comparisons*,
 *     *Accent-insensitive Comparisons*, *Basic Spatial Operators*, *Spatial Operators*, *Temporal
 *     Operators*, *Array Operators*, *Property-Property Comparisons*, *CQL2 Text encoding*, and
 *     *CQL2 JSON encoding* from the draft specification [Common Query Language
 *     (CQL2](https://docs.ogc.org/DRAFTS/21-065.html). The implementation is subject to change in
 *     the course of the development and approval process of the draft.
 *     <p>The publication of queryables is controlled via [Feature Collections -
 *     Queryables](feature_collections_-_queryables.md) and is a prerequisite for clients to be able
 *     to determine the queryable feature properties.
 * @conformanceDe Dieses Modul implementiert die Anforderungen der Konformitätsklassen *Filter* und
 *     *Features Filter* aus dem Entwurf der Spezifikation [OGC API - Features - Part 3: Common
 *     Query Language](https://docs.ogc.org/DRAFTS/19-079r1.html) sowie die Konformitätsklassen
 *     *Basic CQL2*, *Advanced Comparison Operators*, *Case-insensitive Comparisons*,
 *     *Accent-insensitive Comparisons*, *Basic Spatial Operators*, *Spatial Operators*, *Temporal
 *     Operators*, *Array Operators*, *Property-Property Comparisons*, *CQL2 Text encoding*, und
 *     *CQL2 JSON encoding* aus dem Entwurf der Spezifikation [Common Query Language
 *     (CQL2](https://docs.ogc.org/DRAFTS/21-065.html). Die Implementierung wird sich im Zuge der
 *     weiteren Standardisierung der Spezifikation noch ändern.
 *     <p>Die Veröffentlichung der Queryables wird über [Feature Collections -
 *     Queryables](feature_collections_-_queryables.md) gesteuert und ist Voraussetzung, damit
 *     Clients die abfragbaren Objekteigenschaften bestimmen können.
 * @ref:cfg {@link de.ii.ogcapi.filter.domain.FilterConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.filter.domain.ImmutableFilterConfiguration}
 * @ref:queryParameters {@link de.ii.ogcapi.filter.api.QueryParameterFilter}, {@link
 *     de.ii.ogcapi.filter.api.QueryParameterFilterCrs}, {@link
 *     de.ii.ogcapi.filter.api.QueryParameterFilterLang}
 */
@Singleton
@AutoBind
public class FilterBuildingBlock implements ApiBuildingBlock {

  public static final Optional<SpecificationMaturity> MATURITY =
      Optional.of(SpecificationMaturity.DRAFT_OGC);
  public static final Optional<ExternalDocumentation> SPEC =
      Optional.of(
          ExternalDocumentation.of(
              "https://docs.ogc.org/DRAFTS/19-079r1.html",
              "OGC API - Features - Part 3: Filtering (DRAFT)"));

  @Inject
  public FilterBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
