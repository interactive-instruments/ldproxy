package de.ii.ldproxy.wfs3.filtertransformer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationExtension;
import org.immutables.value.Value;

import java.util.List;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableFilterTransformersConfiguration.class)

//TODO: also allow on global level (could we just use the same configuration there?)
public abstract class FilterTransformersConfiguration implements FeatureTypeConfigurationExtension {

    public static final String EXTENSION_KEY = "filterTransformer";
    public static final String EXTENSION_TYPE = "FILTER_TRANSFORMER";


    public abstract List<FilterTransformerConfiguration> getTransformers();
}