package de.ii.ldproxy.ogcapi.observation_processing.functions;

import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Provides
@Instantiate
public class StatisticalFunctionMin implements ObservationProcessingStatisticalFunction {

    private final OgcApiExtensionRegistry extensionRegistry;

    public StatisticalFunctionMin(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "min";
    }

    @Override
    public Number getValue(CopyOnWriteArrayList<Number> values) {
        return Collections.min(values, Comparator.comparing(v -> v.floatValue()));
    }

    @Override
    public Class getType() { return Float.class; }
}
