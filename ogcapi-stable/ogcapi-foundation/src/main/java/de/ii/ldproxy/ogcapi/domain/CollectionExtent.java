package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCollectionExtent.Builder.class)
public interface CollectionExtent {

    Optional<TemporalExtent> getTemporal();

    Optional<BoundingBox> getSpatial();

    Optional<Boolean> getSpatialComputed();

    Optional<Boolean> getTemporalComputed();
}
