package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableOgcApiExample.Builder.class)
public interface OgcApiExample {
        Optional<String> getSummary();
        Optional<String> getDescription();
        Optional<Object> getValue();
        Optional<String> getExternalValue();
}
