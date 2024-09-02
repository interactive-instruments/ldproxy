/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.AuthConfiguration;
import de.ii.xtraplatform.base.domain.BackgroundTasksConfiguration;
import de.ii.xtraplatform.base.domain.ImmutableAuthConfiguration;
import de.ii.xtraplatform.base.domain.ImmutableModulesConfiguration;
import de.ii.xtraplatform.base.domain.ImmutableStoreConfiguration;
import de.ii.xtraplatform.base.domain.LoggingConfiguration;
import de.ii.xtraplatform.base.domain.MetricsConfiguration;
import de.ii.xtraplatform.base.domain.ModulesConfiguration;
import de.ii.xtraplatform.base.domain.ModulesConfiguration.Startup;
import de.ii.xtraplatform.base.domain.ServerConfiguration;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import io.dropwizard.client.HttpClientConfiguration;
import java.util.Map;

public class AppConfigurationTest extends AppConfiguration {

  @Override
  public ServerConfiguration getServerFactory() {
    return null;
  }

  @Override
  public Map<String, Object> getSubstitutions() {
    return Map.of();
  }

  @Override
  public LoggingConfiguration getLoggingFactory() {
    return null;
  }

  @Override
  public HttpClientConfiguration getHttpClient() {
    return new HttpClientConfiguration();
  }

  @Override
  public MetricsConfiguration getMetricsFactory() {
    return null;
  }

  @Override
  public StoreConfiguration getStore() {
    return new ImmutableStoreConfiguration.Builder().failOnUnknownProperties(false).build();
  }

  @Override
  public AuthConfiguration getAuth() {
    return new ImmutableAuthConfiguration.Builder().build();
  }

  @Override
  public ModulesConfiguration getModules() {
    return new ImmutableModulesConfiguration.Builder().startup(Startup.SYNC).build();
  }

  @Override
  public BackgroundTasksConfiguration getBackgroundTasks() {
    return null;
  }
}
