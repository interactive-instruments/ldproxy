package de.ii.ldproxy.config;

import de.ii.xtraplatform.dropwizard.api.AbstractConfigurationProvider;
import de.ii.xtraplatform.dropwizard.api.XtraPlatformConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;

@Component
@Provides
@Instantiate
public class ConfigurationProviderLdproxy extends AbstractConfigurationProvider<XtraPlatformConfiguration> {

    @Override
    public Class<XtraPlatformConfiguration> getConfigurationClass() {
        return XtraPlatformConfiguration.class;
    }
}
