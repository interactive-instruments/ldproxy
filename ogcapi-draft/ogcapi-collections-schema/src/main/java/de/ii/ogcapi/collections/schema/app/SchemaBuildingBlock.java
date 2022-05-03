/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.app;

import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.collections.schema.domain.ImmutableSchemaConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * # Collections Schema (SCHEMA)
 * @langEn The module *Collections Schema* may be enabled for every API with a feature provider.
 * It provides a sub-resource *Schema* for the resource *Feature Collection* that publishes
 * the JSON Schema (Draft 07) of the features. The schema is automatically derived from the
 * type definitions in the feature provider.
 *
 * This module has no configuration options.
 * @langDe Das Modul "Collections Schema" kann für jede über ldproxy bereitgestellte API mit einem
 * Feature-Provider aktiviert werden. Es ergänzt Ressourcen als Sub-Ressource zu jeder Feature
 * Collection, die das Schema der GeoJSON Features veröffentlicht. Das Schema wird aus den
 * Schemainformationen im Feature-Provider abgeleitet. Aktuell wird JSON Schema 2019-09 für die
 * GeoJSON-Ausgabe unterstützt.
 *
 * In der Konfiguration können keine Optionen gewählt werden.
 * @see de.ii.ogcapi.collections.schema.domain.SchemaConfiguration
 * @see de.ii.ogcapi.collections.schema.infra.EndpointSchema
 * @see de.ii.ogcapi.foundation.domain.CachingConfiguration
 */
@Singleton
@AutoBind
public class SchemaBuildingBlock implements ApiBuildingBlock {

    @Inject
    public SchemaBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                         .build();
    }

}
