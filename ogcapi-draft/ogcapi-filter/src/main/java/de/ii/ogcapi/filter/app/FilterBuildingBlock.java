/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.filter.app;

import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.filter.domain.ImmutableFilterConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * # Filter - CQL (FILTER)
 * @langEn Adds support for CQL filter expressions in queries to select [Features](features-core.md) or [Vector Tiles](tiles.md).
 * @langDe Das Modul "Filter / CQL" kann für jede über ldproxy bereitgestellte API mit einem
 * Feature-Provider aktiviert werden. Es aktiviert die Angabe der Query-Parameter `filter`
 * und `filter-lang` für die Ressourcen "Features" und "Vector Tile". Unterstützt werden die
 * Filtersprachen `cql-text` und `cql-json`.
 *
 * Das Modul basiert auf den Vorgaben der Konformitätsklassen "Filter", "Features Filter",
 * "Simple CQL", "CQL Text" und "CQL JSON" aus dem [Entwurf von OGC API - Features - Part 3: Common
 * Query Language](http://docs.opengeospatial.org/DRAFTS/19-079.html#filter-queryables).
 * Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.
 *
 * Die Veröffentlichung der Queryables wird über des
 * [Modul "Collections Queryables"](queryables.md) gesteuert. Ist "Filter / CQL" aktiviert,
 * dann muss "Collection Queryables" aktiviert sein, damit Clients die abfragbaren
 * Objekteigenschaften bestimmen können.
 *
 * @queryParameterTable {@link de.ii.ogcapi.filter.api.QueryParameterFilter},
 * {@link de.ii.ogcapi.filter.api.QueryParameterFilterCrs},
 * {@link de.ii.ogcapi.filter.api.QueryParameterFilterLang}
 */
@Singleton
@AutoBind
public class FilterBuildingBlock implements ApiBuildingBlock {

    @Inject
    public FilterBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                         .build();
    }
}
