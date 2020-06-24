package de.ii.ldproxy.ogcapi.observation_processing.functions;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Provides
@Instantiate
public class StatisticalFunctionSum implements ObservationProcessingStatisticalFunction {

    private final OgcApiExtensionRegistry extensionRegistry;

    public StatisticalFunctionSum(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
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
