
package de.ii.ldproxy.ogcapi.tiles;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableVectorLayer.class)
@JsonPropertyOrder({
    "id",
    "fields",
    "description",
    "maxzoom",
    "minzoom"
})
public abstract class VectorLayer
{

    @JsonProperty("id")
    public abstract String getId();

    @JsonProperty("fields")
    public abstract Fields getFields();

    @JsonProperty("description")
    public abstract Optional<String> getDescription();

    @JsonProperty("geometry_type")
    public abstract Optional<String> getGeometryType();

    @JsonProperty("maxzoom")
    public abstract Optional<Integer> getMaxzoom();

    @JsonProperty("minzoom")
    public abstract Optional<Integer> getMinzoom();

    @JsonAnyGetter
    public abstract Map<String, Object> getAdditionalProperties();

}
