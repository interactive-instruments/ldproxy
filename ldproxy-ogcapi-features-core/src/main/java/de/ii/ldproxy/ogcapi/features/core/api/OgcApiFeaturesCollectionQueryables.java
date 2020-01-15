package de.ii.ldproxy.ogcapi.features.core.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableOgcApiFeaturesCollectionQueryables.Builder.class)
public interface OgcApiFeaturesCollectionQueryables {

    List<String> getSpatial();

    List<String> getTemporal();

    List<String> getOther();
}
