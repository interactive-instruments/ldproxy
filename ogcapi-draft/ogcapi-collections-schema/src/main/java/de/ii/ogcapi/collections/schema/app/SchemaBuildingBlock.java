/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.schema.domain.ImmutableSchemaConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Feature Collections - Schema
 * @langEn Publish the JSON Schema of the features.
 * @langDe Veröffentlichen des JSON Schema der Features.
 * @scopeEn The schema is automatically derived from the type definitions in the feature provider.
 *     Currently JSON Schema 2019-09 for GeoJSON outputs is supported.
 * @scopeDe Das Schema wird aus den Schemainformationen im Feature-Provider abgeleitet. Aktuell wird
 *     JSON Schema 2019-09 für die GeoJSON-Ausgabe unterstützt.
 * @conformanceEn *Feature Collections - Schema* is based on the [OGC API Features proposal for a
 *     new part
 *     'Schemas'](https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/schemas)
 *     and [ongoing discussions](https://github.com/opengeospatial/ogcapi-features/projects/11).
 * @conformanceDe Das Modul basiert auf dem [Vorschlag für einen neuen Teil 'Schemas' von OGC API
 *     Features](https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/schemas)
 *     und [laufenden Diskussionen](https://github.com/opengeospatial/ogcapi-features/projects/11).
 * @ref:cfg {@link de.ii.ogcapi.collections.schema.domain.SchemaConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.collections.schema.domain.ImmutableSchemaConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.collections.schema.infra.EndpointSchema},
 * @ref:queryParameters {@link de.ii.ogcapi.collections.schema.domain.QueryParameterFSchema}, {@link
 *     de.ii.ogcapi.collections.schema.domain.QueryParameterProfileSchema}
 * @ref:pathParameters {@link
 *     de.ii.ogcapi.collections.schema.domain.PathParameterCollectionIdSchema}, {@link
 *     de.ii.ogcapi.collections.schema.domain.PathParameterTypeSchema}
 */
@Singleton
@AutoBind
public class SchemaBuildingBlock implements ApiBuildingBlock {

  @Inject
  public SchemaBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
