package de.ii.ldproxy.wfs3.projections;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableProjectionsConfiguration.class)
public abstract class ProjectionsConfiguration implements ExtensionConfiguration {
    public static final String EXTENSION_KEY = "projectionsExtension";
    public static final String EXTENSION_TYPE = "PROJECTIONS";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
