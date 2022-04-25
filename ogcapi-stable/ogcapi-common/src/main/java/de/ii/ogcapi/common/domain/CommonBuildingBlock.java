/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a
 * copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * # Common Core (COMMON)
 *
 * @langEn The module *Common Core* is always enabled. It provides the resources *Landing Page*,
 * *Conformance Declaration* and *API Definition*. *Common Core* implements all requirements of
 * conformance class *Core* of [OGC API - Features - Part 1: Core
 * 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) for the three mentioned
 * resources.
 * <p>
 * > **Note**: The conformance class *Core* was split up into multiple modules in anticipation of
 * the upcoming standard *OGC API Common*.
 * <p>
 * @langDe Das Modul *Common Core* ist für jede über ldproxy bereitgestellte API aktiv. Es stellt
 * die Ressourcen *Landing Page*, *Conformance Declaration* und *API Definition* bereit. *Common
 * Core* implementiert alle Vorgaben der Konformitätsklasse *Core* von [OGC API - Features - Part 1:
 * Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) für die drei genannten
 * Ressourcen.
 * <p>
 * > **Hinweis**: Die Konformitätsklasse wurde in ldproxy auf drei Module aufgeteilt, da vorgesehen
 * ist, die jeweiligen Anforderungen für die Nutzung in anderen OGC API Standards als zwei Teile von
 * OGC API Common zu veröffentlichen. Die Module "Common Core" und "Feature Collections" bilden dies
 * ab.
 * <p>
 * @propertyTable {@link de.ii.ogcapi.common.domain.ImmutableCommonConfiguration}
 * @example {@link de.ii.ogcapi.common.domain.CommonConfiguration}
 * @endpointTable {@link de.ii.ogcapi.common.infra.EndpointLandingPage},
 * {@link de.ii.ogcapi.common.infra.EndpointConformance},
 * {@link de.ii.ogcapi.common.infra.EndpointDefinition}
 */
@Singleton
@AutoBind
public class CommonBuildingBlock implements ApiBuildingBlock {

  @Inject
  public CommonBuildingBlock() {
  }

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableCommonConfiguration.Builder().enabled(true).build();
  }
}
