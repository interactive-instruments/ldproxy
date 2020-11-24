package de.ii.ldproxy.ogcapi.tiles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableMvtFeature.Builder.class)
public abstract class MvtFeature implements Comparable {
    public abstract Geometry getGeometry();

    public abstract Map<String, Object> getProperties();

    public abstract Long getId();

    @Override
    public int compareTo(Object o) {
        return o instanceof MvtFeature ?
                ((MvtFeature) o).getId().compareTo(getId()) :
                0;
    }
}
