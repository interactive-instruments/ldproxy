package de.ii.ldproxy.ogcapi.observation_processing.functions;

import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Provides
@Instantiate
public class StatisticalFunctionSum implements ObservationProcessingStatisticalFunction {

    private final ExtensionRegistry extensionRegistry;

    public StatisticalFunctionSum(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "sum";
    }

    @Override
    public Number getValue(CopyOnWriteArrayList<Number> values) {
        return (float) values.stream().mapToDouble(Number::doubleValue).sum();
    }

    @Override
    public Class getType() { return Float.class; }

    @Override
    public boolean isDefault() {
        return false;
    }
}
