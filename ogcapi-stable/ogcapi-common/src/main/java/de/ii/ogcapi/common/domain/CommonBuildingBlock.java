/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Common Core
 * @langEn The common resources of all OGC Web APIs.
 * @langDe Gemeinsame Ressourcen aller OGC Web APIs.
 * @conformanceEn *Common Core* implements all requirements of conformance class *Core* of [OGC API
 *     - Features - Part 1: Core 1.0.1](https://docs.ogc.org/is/17-069r4/17-069r4.html#rc_core) for
 *     the three mentioned resources.
 *     <p>::: info The conformance class has been split into three building blocks in ldproxy, since
 *     other OGC API standards reuse parts. The modules "Common Core" and "Feature Collections"
 *     reflect this. :::
 *     <p>
 * @conformanceDe *Common Core* implementiert alle Vorgaben der Konformitätsklasse *Core* von [OGC
 *     API - Features - Part 1: Core 1.0.1](https://docs.ogc.org/is/17-069r4/17-069r4.html#rc_core)
 *     für die drei genannten Ressourcen.
 *     <p>::: info Die Konformitätsklasse wurde in ldproxy auf drei Module aufgeteilt, da auch
 *     andere OGC API Standards bestimmte Teile wiederverwenden. Die Module "Common Core" und
 *     "Feature Collections" bilden dies ab. :::
 *     <p>
 * @ref:cfg {@link de.ii.ogcapi.common.domain.CommonConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.common.domain.ImmutableCommonConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.common.infra.EndpointLandingPage}, {@link
 *     de.ii.ogcapi.common.infra.EndpointConformance}, {@link
 *     de.ii.ogcapi.common.infra.EndpointDefinition}
 * @ref:queryParameters {@link de.ii.ogcapi.common.domain.QueryParameterFCommon}, {@link
 *     de.ii.ogcapi.common.domain.QueryParameterFApiDefinition}, {@link
 *     de.ii.ogcapi.common.domain.QueryParameterLang}, {@link
 *     de.ii.ogcapi.common.domain.QueryParameterToken}
 */
@Singleton
@AutoBind
public class CommonBuildingBlock implements ApiBuildingBlock {

  @Inject
  public CommonBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableCommonConfiguration.Builder().enabled(true).build();
  }
}
