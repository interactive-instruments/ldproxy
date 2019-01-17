package de.ii.ldproxy.wfs3.sitemaps;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableSitemapsConfiguration.class)
public abstract class SitemapsConfiguration implements ExtensionConfiguration {

    public static final String EXTENSION_KEY = "sitemapsExtension";
    public static final String EXTENSION_TYPE = "SITEMAPS";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
