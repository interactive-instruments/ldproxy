package de.ii.ldproxy.wfs3.transactional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableTransactionalConfiguration.class)
public abstract class TransactionalConfiguration implements ExtensionConfiguration {

    public static final String EXTENSION_KEY = "transactionalExtension";
    public static final String EXTENSION_TYPE = "TRANSACTIONAL";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
