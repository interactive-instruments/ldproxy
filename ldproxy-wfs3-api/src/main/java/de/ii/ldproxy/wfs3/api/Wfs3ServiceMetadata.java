package de.ii.ldproxy.wfs3.api;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableWfs3ServiceMetadata.class)
public abstract class Wfs3ServiceMetadata {

    public abstract Optional<String> getContactName();

    public abstract Optional<String> getContactUrl();

    public abstract Optional<String> getContactEmail();

    public abstract Optional<String> getLicenseName();

    public abstract Optional<String> getLicenseUrl();

    public abstract List<String> getKeywords();
}
