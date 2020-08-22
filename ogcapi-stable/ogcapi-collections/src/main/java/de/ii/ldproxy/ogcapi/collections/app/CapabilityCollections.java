package de.ii.ldproxy.ogcapi.collections.app;

import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiCollectionsConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class CapabilityCollections implements ApiBuildingBlock {

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableOgcApiCollectionsConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableOgcApiCollectionsConfiguration.Builder().enabled(true)
                                                                    .build();
    }
}
