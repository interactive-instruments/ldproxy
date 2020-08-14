package de.ii.ldproxy.ogcapi.observation_processing.functions;

import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Provides
@Instantiate
public class StatisticalFunctionCount implements ObservationProcessingStatisticalFunction {

    private final OgcApiExtensionRegistry extensionRegistry;

    public StatisticalFunctionCount(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "count";
    }

    @Override
    public Integer getValue(CopyOnWriteArrayList<Number> values) {
        return values.size();
    }

    @Override
    public Class getType() { return Integer.class; }

    @Override
    public boolean isDefault() {
        return false;
    }
}
