package de.ii.ldproxy.wfs3.jsonld;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.aroundrelations.AroundRelationConfiguration;
import de.ii.xsf.dropwizard.api.JacksonSubTypeIds;
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
public class TODOAroundRelationsSubtypeIds implements JacksonSubTypeIds {
    @Override
    public Map<Class<?>, String> getMapping() {
        return new ImmutableMap.Builder<Class<?>, String>()
                .put(AroundRelationConfiguration.class, "AROUND_RELATIONS")
                .build();
    }
}
