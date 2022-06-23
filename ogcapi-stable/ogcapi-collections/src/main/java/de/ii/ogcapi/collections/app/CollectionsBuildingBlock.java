/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.domain.ImmutableCollectionsConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Feature Collections
 * @langEn The module *Feature Collections* has to be enabled for every API with a feature provider.
 *     It provides the resources *Feature Collections* and *Feature Collection*. Currently feature
 *     collections are the only supported type of collection.
 * @conformanceEn *Feature Collections* implements all requirements of conformance class *Core* of
 *     [OGC API - Features - Part 1: Core
 *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) for the two mentioned
 *     resources.
 *     <p>::: info additional links for a specific *Collection* can be defined in the collection
 *     configuration. :::
 *     <p>
 * @langDe Das Modul *Feature Collections* ist für jede über ldproxy bereitgestellte API mit einem
 *     Feature-Provider zu aktivieren. Es stellt die Ressourcen "Collections" und "Collection"
 *     bereit. Derzeit sind Feature Collections die einzige unterstütze Art von Collections.
 * @conformanceDe "Feature Collections" implementiert alle Vorgaben der Konformitätsklasse "Core"
 *     von [OGC API - Features - Part 1: Core
 *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) für die zwei genannten
 *     Ressourcen.
 *     <p>::: info Zusätzliche Links zu einer bestimmten Feature Collection einzelnen können bei der
 *     Konfiguration der Collection angegeben werden. :::
 *     <p>
 * @example {@link de.ii.ogcapi.collections.domain.CollectionsConfiguration}
 * @propertyTable {@link de.ii.ogcapi.collections.domain.ImmutableCollectionsConfiguration}
 * @endpointTable {@link de.ii.ogcapi.collections.infra.EndpointCollection}, {@link
 *     de.ii.ogcapi.collections.infra.EndpointCollections}
 * @queryParameterTable {@link de.ii.ogcapi.collections.domain.QueryParameterFCollection}, {@link
 *     de.ii.ogcapi.collections.domain.QueryParameterFCollections}
 */
@Singleton
@AutoBind
public class CollectionsBuildingBlock implements ApiBuildingBlock {
  @Inject
  public CollectionsBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableCollectionsConfiguration.Builder().enabled(true).build();
  }
}
