package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleTransition.class)
public abstract class MbStyleTransition {
    public abstract Optional<Integer> getDuration(); // { return Optional.of(300); }

    public abstract Optional<Integer> getDelay(); // { return Optional.of(0); }
}
