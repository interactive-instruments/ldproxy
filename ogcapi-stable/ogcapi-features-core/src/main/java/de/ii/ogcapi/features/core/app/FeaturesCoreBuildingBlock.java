/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.DEFAULT_PAGE_SIZE;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.MAX_PAGE_SIZE;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.MINIMUM_PAGE_SIZE;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Modul "Features Core"
 * @en The module Features Core has to be enabled for every API with a feature provider.
 * It provides the resources Features and Feature.
 *
 * Features Core implements all requirements of conformance class Core of OGC API -
 * Features - Part 1: Core 1.0 for the two mentioned resources.
 * @de Das Modul "Features Core" ist für jede über ldproxy bereitgestellte API mit einem Feature-Provider
 * zu aktivieren. Es stellt die Ressourcen "Features" und "Feature" bereit.
 *
 * "Features Core" implementiert alle Vorgaben der Konformitätsklasse "Core" von OGC API
 * - Features - Part 1: Core 1.0 für die zwei genannten Ressourcen.
 * @see FeaturesCoreConfiguration
 * @see EndpointFeatures
 * @see
 */
@Singleton
@AutoBind
public class FeaturesCoreBuildingBlock implements ApiBuildingBlock {

    @Inject
    public FeaturesCoreBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableFeaturesCoreConfiguration.Builder().enabled(true)
                                                               .itemType(FeaturesCoreConfiguration.ItemType.feature)
                                                               .defaultCrs(FeaturesCoreConfiguration.DefaultCrs.CRS84)
                                                               .minimumPageSize(MINIMUM_PAGE_SIZE)
                                                               .defaultPageSize(DEFAULT_PAGE_SIZE)
                                                               .maximumPageSize(MAX_PAGE_SIZE)
                                                               .showsFeatureSelfLink(false)
                                                               .build();
    }

}
