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
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Filter
 * @langEn The module "Filter / CQL2" can be enabled for any API with a feature provider. It enables
 *     the specification of the query parameters `filter` and `filter-long` for
 *     [features](features_core.md) and [vector tiles](vector_tiles.md). Supported are the filter
 *     languages `cql2-text` and `cql2-json`.
 * @conformanceEn This module implements requirements of the conformance classes *Filter* and
 *     *Features Filter* from the draft specification [OGC API - Features - Part 3: Common Query
 *     Language](https://docs.ogc.org/DRAFTS/19-079r1.html) as well as the conformance classes
 *     *Basic CQL2*, *Advanced Comparison Operators*, *Case-insensitive Comparisons*,
 *     *Accent-insensitive Comparisons*, *Basic Spatial Operators*, *Spatial Operators*, *Temporal
 *     Operators*, *Array Operators*, *Property-Property Comparisons*, *CQL2 Text encoding*, and
 *     *CQL2 JSON encoding* from the draft specification [Common Query Language
 *     (CQL2](https://docs.ogc.org/DRAFTS/21-065.html). The implementation is subject to change in
 *     the course of the development and approval process of the draft.
 * @langDe Das Modul "Filter / CQL2" kann für jede über ldproxy bereitgestellte API mit einem
 *     Feature-Provider aktiviert werden. Es aktiviert die Angabe der Query-Parameter `filter` und
 *     `filter-lang` für [Features](features_core.md) und [Vector Tiles](vector_tiles.md).
 *     Unterstützt werden die Filtersprachen `cql2-text` und `cql2-json`.
 *     <p>The publication of the queryables is controlled by the [Collections Queryables
 *     module](collections_queryables.md). If "Filter / CQL2" is enabled, then "Collections
 *     Queryables" must be enabled, too, so that clients can determine the feature properties that
 *     can be queried.
 * @conformanceDe Dieses Modul implementiert die Anforderungen der Konformitätsklassen *Filter* und
 *     *Features Filter* aus dem Entwurf der Spezifikation [OGC API - Features - Part 3: Common
 *     Query Language](https://docs.ogc.org/DRAFTS/19-079r1.html) sowie die Konformitätsklassen
 *     *Basic CQL2*, *Advanced Comparison Operators*, *Case-insensitive Comparisons*,
 *     *Accent-insensitive Comparisons*, *Basic Spatial Operators*, *Spatial Operators*, *Temporal
 *     Operators*, *Array Operators*, *Property-Property Comparisons*, *CQL2 Text encoding*, und
 *     *CQL2 JSON encoding* aus dem Entwurf der Spezifikation [Common Query Language
 *     (CQL2](https://docs.ogc.org/DRAFTS/21-065.html). Die Implementierung kann sich im Zuge der
 *     weiteren Standardisierung des Entwurfs noch ändern.
 *     <p>Die Veröffentlichung der Queryables wird über das [Modul "Collections
 *     Queryables"](collections_queryables.md) gesteuert. Ist "Filter / CQL2" aktiviert, dann muss
 *     "Collection Queryables" aktiviert sein, damit Clients die abfragbaren Objekteigenschaften
 *     bestimmen können.
 * @ref:queryParameters {@link de.ii.ogcapi.filter.api.QueryParameterFilter}, {@link
 *     de.ii.ogcapi.filter.api.QueryParameterFilterCrs}, {@link
 *     de.ii.ogcapi.filter.api.QueryParameterFilterLang}
 */
@Singleton
@AutoBind
public class FilterBuildingBlock implements ApiBuildingBlock {

  @Inject
  public FilterBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
