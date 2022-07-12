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
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * @title CRUD
 * @langEn The module is based on the specifications of the conformance classes
 *     "Create/Replace/Delete" and "Features" from the [Draft OGC API - Features - Part 4: Create,
 *     Replace, Update and Delete](https://docs.ogc.org/DRAFTS/20-002.html). The implementation will
 *     change as the draft is further standardized.
 * @langDe Das Modul basiert auf den Vorgaben der Konformitätsklassen "Create/Replace/Delete" und
 *     "Features" aus dem [Entwurf von OGC API - Features - Part 4: Create, Replace, Update and
 *     Delete](https://docs.ogc.org/DRAFTS/20-002.html). Die Implementierung wird sich im Zuge der
 *     weiteren Standardisierung des Entwurfs noch ändern.
 * @propertyTable {@link ImmutableCrudConfiguration}
 * @endpointTable {@link EndpointCrud}
 */
@Singleton
@AutoBind
public class CrudBuildingBlock implements ApiBuildingBlock {

  @Inject
  public CrudBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).build();
  }
}
