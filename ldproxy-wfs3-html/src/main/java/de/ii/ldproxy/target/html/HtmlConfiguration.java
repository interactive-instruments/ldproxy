package de.ii.ldproxy.target.html;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableHtmlConfiguration.class)
public abstract class HtmlConfiguration implements ExtensionConfiguration {
    public static final String EXTENSION_KEY = "htmlExtension";
    public static final String EXTENSION_TYPE = "HTML";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
