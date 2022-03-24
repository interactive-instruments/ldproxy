/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.oas30.domain.ImmutableOas30Configuration;
import de.ii.ogcapi.oas30.domain.Oas30Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title OpenAPI 3.0 (OAS30)
 * @en The module *OpenAPI 3.0* may be enabled for every API with a feature provider.
 * It provides the resource *API Definition*.
 *
 * *OpenAPI 3.0* implements all requirements of conformance class *OpenAPI 3.0* from
 * [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_oas30)
 * for the mentioned resource.
 * This module has no configuration options.
 * @de Das Modul "OpenAPI 3.0" ist für jede über ldproxy bereitgestellte API aktiv. Es stellt die Ressource "API Definition" bereit.
 *
 * "OpenAPI 3.0" implementiert alle Vorgaben der gleichnamigen Konformitätsklasse von
 * [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_oas30).
 *
 * In der Konfiguration können keine Optionen gewählt werden.
 * @see Oas30Configuration
 */
@Singleton
@AutoBind
public class OpenApiBuildingBlock implements ApiBuildingBlock {

    @Inject
    public OpenApiBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableOas30Configuration.Builder().enabled(true)
                                                        .build();
    }

}
