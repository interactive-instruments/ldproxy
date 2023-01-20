/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Resources
 * @langEn Publish and manage file resources, especially for styles (icons, sprites).
 * @langDe Bereitstellung und Verwaltung von Datei-Ressourcen, vor allem für Styles (Symbole,
 *     Sprites).
 * @ref:cfgProperties {@link de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.resources.infra.EndpointResource}, {@link
 *     de.ii.ogcapi.resources.infra.EndpointResources}
 */
@Singleton
@AutoBind
public class ResourcesBuildingBlock implements ApiBuildingBlock {

  @Inject
  public ResourcesBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder().enabled(false).managerEnabled(false).build();
  }
}
