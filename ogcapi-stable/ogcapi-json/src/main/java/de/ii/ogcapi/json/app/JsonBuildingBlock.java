/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.json.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.json.domain.ImmutableJsonConfiguration.Builder;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * # JSON (JSON)
 * @langEn The module *JSON* may be enabled for every API. It is enabled by default. Provides JSON encoding for every
 * supported resource that does not have more specific rules (like [Features](geojson.md)).
 *
 * This module has no configuration options.
 * @langDe Das Modul "JSON" kann für jede über ldproxy bereitgestellte API aktiviert werden und ist
 * standardmäßig aktiviert. Soweit für eine Ressource keine speziellen Regelungen für die
 * Ausgabeformate bestehen (wie zum Beispiel für [Features](geojson.md)) und die Ressource
 * JSON unterstützt, können Clients das Ausgabeformat anfordern.
 *
 * Es gibt keine konfigurierbaren Optionen.
 * @propertyTable {@link de.ii.ogcapi.json.domain.ImmutableJsonConfiguration}
 */
@Singleton
@AutoBind
public class JsonBuildingBlock implements ApiBuildingBlock {

    @Inject
    public JsonBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(true)
                                                       .useFormattedJsonOutput(false)
                                                       .build();
    }

}
