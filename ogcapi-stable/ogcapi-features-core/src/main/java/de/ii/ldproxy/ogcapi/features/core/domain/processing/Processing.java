package de.ii.ldproxy.ogcapi.features.core.domain.processing;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableProcessing.Builder.class)
public abstract class Processing extends PageRepresentation {

    public abstract List<Process> getEndpoints();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
