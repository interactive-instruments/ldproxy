package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleLight.class)
public abstract class MbStyleLight {
    public enum Anchor { map, viewport }

    @Value.Default
    public Anchor getAnchor() {
        return Anchor.viewport;
    }

    public abstract Optional<List<Double>> getPosition(); // { return Optional.of(ImmutableList.of(1.15,210.0,30.0)); }

    public abstract Optional<String> getColor(); // { return Optional.of("#ffffff"); }

    // public abstract Optional<String> setColor(Optional<String> color); // { return color; }

    public abstract Optional<Double> getIntensity(); // { return Optional.of(0.5); }
}
