package de.ii.ldproxy.ogcapi.features.processing;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.OgcApiExternalDocumentation;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableProcess.Builder.class)
public abstract class Process extends PageRepresentation {

    public abstract String getId();
    public abstract String getInputCollectionId();
    public abstract String getDefinitionUri();
    public abstract String getDescriptionUri();
    public abstract List<String> getMediaTypes();
    public abstract Optional<OgcApiExternalDocumentation> getExternalDocs();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
