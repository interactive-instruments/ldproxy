package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleLayer.class)
public abstract class MbStyleLayer {
    public enum LayerType { background, fill, line, symbol, raster, circle, fillExtrusion, heatmap, hillshade }

    public abstract String getId();

    public abstract LayerType getType();

    public abstract Optional<Object> getMetadata();

    public abstract Optional<String> getSource();

    @JsonProperty("source-layer")
    public abstract Optional<String> getSourceLayer();

    public abstract Optional<Integer> getMinzoom();

    public abstract Optional<Integer> getMaxzoom();

    public abstract Optional<Object> getFilter(); // TODO proper handling of expressions

    public abstract Optional<Map<String, Object>> getLayout(); // TODO proper handling of layout properties

    public abstract Optional<Map<String, Object>> getPaint(); // TODO proper handling of paint properties
}
