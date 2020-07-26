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
public class StatisticalFunctionStandardDeviation implements ObservationProcessingStatisticalFunction {

    private final OgcApiExtensionRegistry extensionRegistry;

    public StatisticalFunctionStandardDeviation(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "std-dev";
    }

    @Override
    public Float getValue(CopyOnWriteArrayList<Number> values) {
        double mean = values.parallelStream().mapToDouble(Number::doubleValue).average().orElse(Double.NaN);
        double variance = values.parallelStream()
                .map(val -> (val.doubleValue()-mean)*(val.doubleValue()-mean))
                .reduce(0.0, Double::sum) / (values.size()-1);
        return (float) Math.sqrt(variance);
    }

    @Override
    public Class getType() { return Float.class; }

    @Override
    public boolean isDefault() {
        return false;
    }
}
