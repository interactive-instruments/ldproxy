package de.ii.ldproxy.wfs3.crs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableCrsConfiguration.class)
public abstract class CrsConfiguration implements ExtensionConfiguration {

    public static final String EXTENSION_KEY = "crsExtension";
    public static final String EXTENSION_TYPE = "CRS";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
