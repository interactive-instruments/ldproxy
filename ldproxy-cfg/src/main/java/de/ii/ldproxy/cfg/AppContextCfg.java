/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Constants.ENV;
import de.ii.xtraplatform.base.domain.ImmutableStoreConfiguration;
import java.net.URI;
import java.nio.file.Path;

class AppContextCfg implements AppContext {

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public ENV getEnvironment() {
    return null;
  }

  @Override
  public Path getDataDir() {
    return null;
  }

  @Override
  public Path getTmpDir() {
    return null;
  }

  @Override
  public AppConfiguration getConfiguration() {
    AppConfiguration appConfiguration = new AppConfiguration(true);
    appConfiguration.store =
        new ImmutableStoreConfiguration.Builder().failOnUnknownProperties(false).build();
    return appConfiguration;
  }

  @Override
  public URI getUri() {
    return null;
  }
}
