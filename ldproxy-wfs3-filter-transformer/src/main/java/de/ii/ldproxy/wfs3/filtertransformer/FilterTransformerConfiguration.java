package de.ii.ldproxy.wfs3.filtertransformer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationExtension;
import org.immutables.value.Value;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableFilterTransformerConfiguration.class)

//TODO: also allow on global level (could we just use the same configuration there?)
public abstract class FilterTransformerConfiguration implements FeatureTypeConfigurationExtension {

    public static final String EXTENSION_KEY = "filterTransformer";
    public static final String EXTENSION_TYPE = "FILTER_TRANSFORMER";



}