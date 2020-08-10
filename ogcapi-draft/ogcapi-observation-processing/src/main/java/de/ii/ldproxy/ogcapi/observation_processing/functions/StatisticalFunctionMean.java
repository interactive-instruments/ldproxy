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
public class StatisticalFunctionMean implements ObservationProcessingStatisticalFunction {

    private final OgcApiExtensionRegistry extensionRegistry;

    public StatisticalFunctionMean(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "mean";
    }

    @Override
    public Float getValue(CopyOnWriteArrayList<Number> values) {
        return (float) values.parallelStream().mapToDouble(Number::doubleValue).average().orElse(Double.NaN);
    }

    @Override
    public Class getType() { return Float.class; }
}
