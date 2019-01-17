package de.ii.ldproxy.target.gml;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableGmlConfiguration.class)
public abstract class GmlConfiguration implements ExtensionConfiguration {

    public static final String EXTENSION_KEY = "gmlExtension";
    public static final String EXTENSION_TYPE = "GML";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
