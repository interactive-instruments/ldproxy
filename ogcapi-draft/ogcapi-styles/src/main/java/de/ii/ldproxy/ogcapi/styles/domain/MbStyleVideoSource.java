package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleVideoSource.class)
public abstract class MbStyleVideoSource extends MbStyleSource {
    public final String getType() {
        return "video";
    }

    public abstract String getUrl();

    public abstract List<List<Double>> getCoordinates();
}
