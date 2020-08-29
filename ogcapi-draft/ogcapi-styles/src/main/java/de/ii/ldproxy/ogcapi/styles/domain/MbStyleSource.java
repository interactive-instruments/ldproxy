package de.ii.ldproxy.ogcapi.styles.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MbStyleVectorSource.class, name = "vector"),
        @JsonSubTypes.Type(value = MbStyleRasterSource.class, name = "raster"),
        @JsonSubTypes.Type(value = MbStyleRasterDemSource.class, name = "raster-dem"),
        @JsonSubTypes.Type(value = MbStyleGeojsonSource.class, name = "geojson"),
        @JsonSubTypes.Type(value = MbStyleImageSource.class, name = "image"),
        @JsonSubTypes.Type(value = MbStyleVideoSource.class, name = "video")
})
public abstract class MbStyleSource {
    public enum Scheme { xyz, tms }
}
