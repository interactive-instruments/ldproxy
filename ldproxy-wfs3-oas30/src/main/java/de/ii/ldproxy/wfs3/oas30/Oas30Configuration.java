package de.ii.ldproxy.wfs3.oas30;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableOas30Configuration.class)
public abstract class Oas30Configuration implements ExtensionConfiguration {
    public static final String EXTENSION_KEY = "oas30Extension";
    public static final String EXTENSION_TYPE = "OAS30";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
