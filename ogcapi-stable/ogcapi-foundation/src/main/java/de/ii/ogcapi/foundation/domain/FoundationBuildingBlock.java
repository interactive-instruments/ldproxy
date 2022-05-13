/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Foundation
 * @langEn Adds an API catalog with all published APIs. Provides base functionality for all
 * other modules and therefore cannot be disabled.
 * @langDe Das Modul "ldproxy Foundation" ist für jede über ldproxy bereitgestellte API aktiv.
 * Es stellt im Wesentliche interne Funktionalitäten für die übrigen API-Module bereit.
 *
 * Zusätzlich wird auch die ldproxy-spezifische Ressource "API Catalog" als Liste der
 * aktiven APIs in dem Deployment bereitgestellt.
 * @example {@link de.ii.ogcapi.foundation.domain.FoundationConfiguration}
 * @propertyTable {@link de.ii.ogcapi.foundation.domain.ImmutableFoundationConfiguration}
 * @see ApiCatalog
 */
@Singleton
@AutoBind
public class FoundationBuildingBlock implements ApiBuildingBlock {

    @Inject
    public FoundationBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableFoundationConfiguration.Builder().enabled(true)
                                                             .includeLinkHeader(true)
                                                             .useLangParameter(false)
                                                             .build();
    }
}
