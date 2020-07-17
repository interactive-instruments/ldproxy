package de.ii.ldproxy.ogcapi.features.processing;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.OgcApiExternalDocumentation;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableProcessDocumentation.Builder.class)
public abstract class ProcessDocumentation {

    public abstract Optional<String> getSummary();
    public abstract Optional<String> getDescription();
    public abstract Optional<OgcApiExternalDocumentation> getExternalDocs();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
