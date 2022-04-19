/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;


import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title
 * @en The module *Collections Queryables* may be enabled for every API with a feature provider.
 * It provides the sub-resource *Queryables* for the resource *Feature Collection* that
 * publishes the feature properties that may be used in queries.
 *
 * *Collections Queryables* implements all requirements of conformance class *Queryables*
 * from the draft of [OGC API - Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html#rc_queryables).
 * The resource will change in the future due to the harmonization with the requirements
 * for *Queryables* from the draft of
 * [OGC API - Features - Part 3: Common Query Language](http://docs.opengeospatial.org/DRAFTS/19-079.html#filter-queryables).
 * @de Das Modul "Collections Queryables" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt eine Ressource als Sub-Ressource zu jeder Feature Collection, die die Objekteigenschaften, die zur Selektion in Queries verwendet werden können, in der API veröffentlicht.
 *
 * Das Modul basiert auf den Vorgaben der Konformitätsklasse "Filter" aus dem
 * [Entwurf von OGC API - Features - Part 3: Filtering](https://docs.ogc.org/DRAFTS/19-079r1.html#filter-queryables).
 *
 * @see de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration
 * @see EndpointQueryables
 * @see de.ii.ogcapi.foundation.domain.CachingConfiguration
 */
@Singleton
@AutoBind
public class QueryablesBuildingBlock implements ApiBuildingBlock {

    @Inject
    public QueryablesBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                             .build();
    }

}
