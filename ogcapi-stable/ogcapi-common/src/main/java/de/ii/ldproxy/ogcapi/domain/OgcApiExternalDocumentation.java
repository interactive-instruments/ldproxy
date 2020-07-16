package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableOgcApiExternalDocumentation.Builder.class)
public abstract class OgcApiExternalDocumentation {
    public abstract Optional<String> getDescription();
    public abstract String getUrl();
}
