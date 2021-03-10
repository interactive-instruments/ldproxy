package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTemporalExtent.Builder.class)
public interface TemporalExtent {

    @Value.Default
    @Nullable
    default Long getStart() {
        return null;
    }

    @Value.Default
    @Nullable
    default Long getEnd() {
        return null;
    }
}
