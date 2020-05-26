package de.ii.ldproxy.ogcapi.feature_processing.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiResponse;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ImmutableProcess.Builder.class)
public abstract class Process extends PageRepresentation {

    public abstract String getInputCollectionId();
    public abstract String getDescriptionUri();

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();
}
