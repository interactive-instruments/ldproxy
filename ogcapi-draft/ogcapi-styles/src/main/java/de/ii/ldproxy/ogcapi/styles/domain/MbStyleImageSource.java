package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleImageSource.class)
public abstract class MbStyleImageSource extends MbStyleSource {
    public final String getType() {
        return "image";
    }

    public abstract String getUrl();

    public abstract List<List<Double>> getCoordinates();
}
