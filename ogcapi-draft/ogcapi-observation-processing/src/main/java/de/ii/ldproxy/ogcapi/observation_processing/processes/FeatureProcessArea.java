package de.ii.ldproxy.ogcapi.observation_processing.processes;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointTimeSeriesList;
import de.ii.ldproxy.ogcapi.observation_processing.data.Observations;
import de.ii.ldproxy.ogcapi.observation_processing.parameters.PathParameterCollectionIdProcess;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureProcessArea implements ObservationProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProcessArea.class);

    private final ExtensionRegistry extensionRegistry;

    public FeatureProcessArea(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Set<String> getSupportedCollections(OgcApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(PathParameterCollectionIdProcess.class).stream()
                .map(param -> param.getValues(apiData))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public void validateProcessingParameters(Map<String, Object> processingParameters) {
        Object obj = processingParameters.get("area");
        if (obj==null || !(obj instanceof GeometryMultiPolygon)) {
            throw new RuntimeException("Missing information for executing '" + getName() + "': No area has been provided.");
        }
        obj = processingParameters.get("interval");
        if (obj==null || !(obj instanceof TemporalInterval)) {
            throw new RuntimeException("Missing information for executing '" + getName() + "': No time interval has been provided.");
        }
    }

    @Override
    public Object execute(Object data, Map<String, Object> processingParameters) {
        validateProcessingParameters(processingParameters);
        if (!(data instanceof Observations)) {
            throw new RuntimeException("Missing information for executing '" + getName() + "': No observation data has been provided.");
        }
        Observations observations = (Observations) data;
        TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");

        ObservationCollectionPointTimeSeriesList positions = observations.interpolate(interval);
        LOGGER.debug("{} distinct locations.", positions.size());

        return positions;
    }

    @Override
    public String getName() {
        return "area";
    }

    @Override
    public String getSummary() {
        return "retrieve information about observations in an area";
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("TODO" +
                "A point observation feature with a point geometry at the selected location (`coord`) " +
                "at the selected time or for each time step in the selected time interval (`datetime`). " +
                "The feature contains a property for each selected variable (`variables`) for which " +
                "a value can be interpolated. " +
                "The time steps are determined from the information in the original data.");
    }

    @Override
    public Class<?> getOutputType() {
        return ObservationCollectionPointTimeSeriesList.class;
    }
}
