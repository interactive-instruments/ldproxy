/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.crud.app.ImmutableCrudConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title CRUD
 * @langEn Create, replace, update and delete features.
 * @langDe Erzeugen, Ersetzen, Aktualisieren und Löschen von Features.
 * @limitationsEn Only feature types from an SQL feature provider with `dialect` `PGIS`, sourced
 *     from a single table and with auto-incrementing primary keys are supported. See also [issue
 *     #411](https://github.com/interactive-instruments/ldproxy/issues/411).
 * @limitationsDe Es werden nur Objektarten von einem SQL-Feature-Provider mit `dialect` `PGIS`
 *     unterstützt, die aus einer einzigen Tabelle stammen und automatisch inkrementierende
 *     Primärschlüssel verwenden. Siehe auch [Ticket
 *     #411](https://github.com/interactive-instruments/ldproxy/issues/411).
 * @conformanceEn The module is based on the specifications of the conformance classes
 *     "Create/Replace/Delete" and "Features" from the [Draft OGC API - Features - Part 4: Create,
 *     Replace, Update and Delete](https://docs.ogc.org/DRAFTS/20-002.html). The implementation will
 *     change as the draft will evolve during the standardization process.
 * @conformanceDe Das Modul basiert auf den Vorgaben der Konformitätsklassen "Create/Replace/Delete"
 *     und "Features" aus dem [Entwurf von OGC API - Features - Part 4: Create, Replace, Update and
 *     Delete](https://docs.ogc.org/DRAFTS/20-002.html). Die Implementierung wird sich im Zuge der
 *     weiteren Standardisierung der Spezifikation noch ändern.
 * @ref:cfg {@link de.ii.ogcapi.crud.app.CrudConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.crud.app.ImmutableCrudConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.crud.app.EndpointCrud}
 * @ref:pathParameters {@link de.ii.ogcapi.features.core.domain.PathParameterCollectionIdFeatures}
 * @ref:queryParameters {@link de.ii.ogcapi.crud.app.QueryParameterSchemaFeatures}
 */
@Singleton
@AutoBind
public class CrudBuildingBlock implements ApiBuildingBlock {

  public static final Optional<SpecificationMaturity> MATURITY =
      Optional.of(SpecificationMaturity.DRAFT_OGC);
  public static final Optional<ExternalDocumentation> SPEC =
      Optional.of(
          ExternalDocumentation.of(
              "https://docs.ogc.org/DRAFTS/20-002.html",
              "OGC API - Features - Part 4: Create, Replace, Update and Delete (DRAFT)"));

  @Inject
  public CrudBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder()
        .enabled(false)
        .optimisticLockingETag(false)
        .optimisticLockingLastModified(false)
        .build();
  }
}
