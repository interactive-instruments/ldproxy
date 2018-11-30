package de.ii.ldproxy.wfs3.filtertransformer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableRequestGeoJsonBboxConfiguration.class)
public abstract class RequestGeoJsonBboxConfiguration implements FilterTransformerConfiguration {

    public static final String TRANSFORMER_TYPE = "REQUEST_GEOJSON_BBOX";

    public abstract String getId();

    public abstract String getLabel();

    public abstract String getUrlTemplate();

    public abstract List<String> getParameters();
}
