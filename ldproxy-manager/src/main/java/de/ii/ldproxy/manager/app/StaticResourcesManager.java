/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.manager.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.web.domain.StaticResources;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class StaticResourcesManager implements StaticResources {

  private final boolean isEnabled;

  @Inject
  StaticResourcesManager(AppContext appContext) {
    this.isEnabled = appContext.getConfiguration().manager.enabled;
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public String getResourcePath() {
    return "/manager";
  }

  @Override
  public String getUrlPath() {
    return "/manager";
  }
}
