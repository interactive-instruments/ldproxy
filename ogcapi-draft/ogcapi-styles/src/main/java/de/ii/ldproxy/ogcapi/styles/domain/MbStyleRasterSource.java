package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleRasterSource.class)
public abstract class MbStyleRasterSource extends MbStyleSource {
    public final String getType() {
        return "raster";
    }

    public abstract Optional<String> getUrl();

    public abstract Optional<List<String>> getTiles();

    public abstract Optional<List<Double>> getBounds(); // { return Optional.of(ImmutableList.of(-180.0,-85.051129,180.0,85.051129)); }

    @Value.Default
    public Scheme getScheme() {
        return Scheme.xyz;
    }

    public abstract Optional<Integer> getTilesize(); // { return Optional.of(512); }

    public abstract Optional<Integer> getMinzoom(); // { return Optional.of(0); }

    public abstract Optional<Integer> getMaxzoom(); // { return Optional.of(22); }

    public abstract Optional<String> getAttribution();
}
