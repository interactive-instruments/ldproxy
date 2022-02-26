package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.Constants.ENV;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
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
  public Path getConfigurationFile() {
    return null;
  }

  @Override
  public AppConfiguration getConfiguration() {
    AppConfiguration appConfiguration = new AppConfiguration(true);
    appConfiguration.store = new StoreConfiguration();
    appConfiguration.store.failOnUnknownProperties = true;
    return appConfiguration;
  }

  @Override
  public URI getUri() {
    return null;
  }
}
