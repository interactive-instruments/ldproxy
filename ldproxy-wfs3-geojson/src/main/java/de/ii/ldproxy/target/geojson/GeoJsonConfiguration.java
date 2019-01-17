package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableGeoJsonConfiguration.class)
public abstract class GeoJsonConfiguration implements ExtensionConfiguration {

    public static final String EXTENSION_KEY = "geoJsonExtension";
    public static final String EXTENSION_TYPE = "GEOJSON";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
