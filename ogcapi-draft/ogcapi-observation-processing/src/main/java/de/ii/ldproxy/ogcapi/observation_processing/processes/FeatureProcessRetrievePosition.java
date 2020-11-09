package de.ii.ldproxy.ogcapi.observation_processing.processes;

import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointTimeSeries;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureProcessRetrievePosition implements ObservationProcess {

    private final ExtensionRegistry extensionRegistry;

    public FeatureProcessRetrievePosition(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public List<FeatureProcess> getSupportedProcesses(OgcApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(FeatureProcess.class).stream()
                                .filter(proc -> proc.getOutputType() == getOutputType() && !proc.isAlwaysTerminal())
                                .collect(Collectors.toList());
    }

    @Override
    public void validateProcessingParameters(Map<String, Object> processingParameters) {
    }

    @Override
    public Object execute(Object data, Map<String, Object> processingParameters) {
        validateProcessingParameters(processingParameters);
        if (!(data instanceof ObservationCollectionPointTimeSeries)) {
            throw new RuntimeException("Missing information for executing '" + getName() + "': No observation data has been provided.");
        }
        return data;
    }

    @Override
    public String getName() {
        return "retrieve";
    }

    @Override
    public String getSummary() {
        return "retrieve data without aggregation";
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("TODO");
    }

    @Override
    public Class<?> getOutputType() {
        return ObservationCollectionPointTimeSeries.class;
    }

    @Override
    public boolean isAlwaysTerminal() { return true; }
}
