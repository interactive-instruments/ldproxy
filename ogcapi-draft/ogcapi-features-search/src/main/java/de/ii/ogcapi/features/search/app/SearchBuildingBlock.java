/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.search.domain.ImmutableSearchConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Search
 * @langEn The building block *Search* adds support for fetching features from multiple collections
 *     as well as for stored queries.
 * @langDe Das Modul *Search* unterstützt den Abruf von Features aus mehreren Collections sowie für
 *     gespeicherte Abfragen.
 * @scopeEn This building block supports searching for features from one or more collections. That
 *     is, it supports queries that cannot be expressed, or cannot be conveniently expressed, using
 *     the filtering mechanisms available through the building blocks *Features* and *Filter*.
 *     <p>Examples of the types of queries that can be expressed using Part 5 are:
 *     <p><code>
 * - queries with a long expression text that cannot be conveniently specified as URL;
 * - queries that, in a single request, fetch resources from one or more collections;
 * - stored queries;
 * - stored queries referencing multiple resource collections;
 * - stored queries with parameters.
 *     </code>
 * @scopeDe Dieses Modul unterstützt die Suche nach Features aus einer oder mehreren Collections.
 *     Das heißt, es unterstützt Abfragen, die mit den Filtermechanismen, die durch die Module
 *     *Features* und *Filter* zur Verfügung stehen, nicht oder nicht bequem ausgedrückt werden
 *     können.
 *     <p>Beispiele für die Arten von Abfragen, die mit Teil 5 ausgedrückt werden können, sind:
 *     <p><code>
 * - Abfragen mit einem langen Ausdruckstext, der nicht bequem als URL angegeben werden kann;
 * - Abfragen, die in einer einzigen Anfrage Ressourcen aus einer oder mehreren Collections abrufen;
 * - gespeicherte Abfragen;
 * - gespeicherte Abfragen, die mehrere Collections referenzieren;
 * - gespeicherte Abfragen mit Parametern.
 *     </code>
 * @conformanceEn This building block implements the OGC API Features extensions specified in the
 *     OGC Testbed-18 Filtering Service and Rule Set Engineering Report (to be published).
 * @conformanceDe Dieses Modul implementiert die OGC-API-Features-Erweiterungen, die in dem OGC
 *     Testbed-18 Filtering Service and Rule Set Engineering Report spezifiziert sind (wird noch
 *     veröffentlicht).
 * @ref:cfg {@link de.ii.ogcapi.features.search.domain.SearchConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.features.search.domain.ImmutableSearchConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.features.search.app.EndpointAdHocQuery}, {@link
 *     de.ii.ogcapi.features.search.app.EndpointStoredQueries}, {@link
 *     de.ii.ogcapi.features.search.app.EndpointStoredQuery}, {@link
 *     de.ii.ogcapi.features.search.app.EndpointStoredQueriesManager}, {@link
 *     de.ii.ogcapi.features.search.app.EndpointStoredQueryDefinition}, {@link
 *     de.ii.ogcapi.features.search.app.EndpointParameters}, {@link
 *     de.ii.ogcapi.features.search.app.EndpointParameter}
 * @ref:queryParameters {@link de.ii.ogcapi.features.search.app.QueryParameterFAdHocQuery}, {@link
 *     de.ii.ogcapi.features.search.app.QueryParameterFStoredQueries}, {@link
 *     de.ii.ogcapi.features.search.app.QueryParameterFStoredQuery}, {@link
 *     de.ii.ogcapi.features.search.app.QueryParameterFQueryDefinition}, {@link
 *     de.ii.ogcapi.features.search.app.QueryParameterDryRunStoredQueriesManager}
 * @ref:pathParameters {@link de.ii.ogcapi.features.search.app.PathParameterQueryId}, {@link
 *     de.ii.ogcapi.features.search.app.PathParameterName}
 */
@Singleton
@AutoBind
public class SearchBuildingBlock implements ApiBuildingBlock {

  @Inject
  public SearchBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableSearchConfiguration.Builder()
        .enabled(false)
        .managerEnabled(false)
        .validationEnabled(false)
        .allLinksAreLocal(false)
        .build();
  }
}
