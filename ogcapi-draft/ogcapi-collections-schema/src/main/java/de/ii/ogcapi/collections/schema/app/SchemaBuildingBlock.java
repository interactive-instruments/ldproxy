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
 * @title Collections Schema
 * @langEn The module *Collections Schema* may be enabled for every API with a feature provider.
 * It provides a sub-resource *Schema* for the resource *Feature Collection* that publishes
 * the JSON Schema (Draft 07) of the features. The schema is automatically derived from the
 * type definitions in the feature provider.
 * @langDe Das Modul "Collections Schema" kann für jede über ldproxy bereitgestellte API mit einem
 * Feature-Provider aktiviert werden. Es ergänzt Ressourcen als Sub-Ressource zu jeder Feature
 * Collection, die das Schema der GeoJSON Features veröffentlicht. Das Schema wird aus den
 * Schemainformationen im Feature-Provider abgeleitet. Aktuell wird JSON Schema 2019-09 für die
 * GeoJSON-Ausgabe unterstützt.
 * @propertyTable {@link de.ii.ogcapi.collections.schema.domain.ImmutableSchemaConfiguration}
 * @endpointTable {@link de.ii.ogcapi.collections.schema.infra.EndpointSchema},
 * @queryParameterTable {@link de.ii.ogcapi.collections.schema.domain.QueryParameterFSchema},
 * {@link de.ii.ogcapi.collections.schema.domain.QueryParameterProfileSchema}
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
