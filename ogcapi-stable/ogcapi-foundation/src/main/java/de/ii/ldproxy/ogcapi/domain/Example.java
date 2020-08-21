package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableExample.Builder.class)
public interface Example {
        Optional<String> getSummary();
        Optional<String> getDescription();
        Optional<Object> getValue();
        Optional<String> getExternalValue();
}
