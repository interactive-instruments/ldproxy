
package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableFields.class)
public abstract class Fields
{
    @JsonAnyGetter
    public abstract Map<String, String> getAdditionalProperties();
}
