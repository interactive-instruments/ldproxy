package de.ii.ldproxy.output.geojson;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.jackson.dynamic.JacksonSubTypeIds;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Map;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonTargetMappingSubTypeIds implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(GeoJsonPropertyMapping.class, "GEO_JSON_PROPERTY")
                .put(GeoJsonGeometryMapping.class, "GEO_JSON_GEOMETRY")
                .build();
    }
}
