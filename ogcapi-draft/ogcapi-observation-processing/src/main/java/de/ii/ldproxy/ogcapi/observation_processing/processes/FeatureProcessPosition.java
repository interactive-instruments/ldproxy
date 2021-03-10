package de.ii.ldproxy.ogcapi.observation_processing.processes;

import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointTimeSeries;
import de.ii.ldproxy.ogcapi.observation_processing.data.Observations;
import de.ii.ldproxy.ogcapi.observation_processing.parameters.PathParameterCollectionIdProcess;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureProcessPosition implements ObservationProcess {

    private final ExtensionRegistry extensionRegistry;

    public FeatureProcessPosition(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Set<String> getSupportedCollections(OgcApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(PathParameterCollectionIdProcess.class).stream()
                .map(param -> param.getValues(apiData))
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void validateProcessingParameters(Map<String, Object> processingParameters) {
        Object obj = processingParameters.get("point");
        if (obj==null || !(obj instanceof GeometryPoint)) {
            throw new RuntimeException("Missing information for executing '" + getName() + "': No point has been provided.");
        }
        obj = processingParameters.get("interval");
        if (obj==null || !(obj instanceof TemporalInterval)) {
            throw new RuntimeException("Missing information for executing '" + getName() + "': No time interval has been provided.");
        }
        obj = processingParameters.get("apiData");
        if (obj==null || !(obj instanceof OgcApiDataV2))
            throw new RuntimeException("Missing information for executing '"+getName()+"': No API information has been provided.");
        obj = processingParameters.get("collectionId");
        if (obj==null || !(obj instanceof String))
            throw new RuntimeException("Missing information for executing '"+getName()+"': No collection identifier has been provided.");
    }

    @Override
    public Object execute(Object data, Map<String, Object> processingParameters) {
        validateProcessingParameters(processingParameters);
        if (!(data instanceof Observations)) {
            throw new RuntimeException("Missing information for executing '" + getName() + "': No observation data has been provided.");
        }
        Observations observations = (Observations) data;
        GeometryPoint point = (GeometryPoint) processingParameters.get("point");
        TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");
        OgcApiDataV2 apiData = (OgcApiDataV2) processingParameters.get("apiData");
        String collectionId = (String) processingParameters.get("collectionId");

        ObservationProcessingConfiguration config =
                apiData.getCollections().get(collectionId).getExtension(ObservationProcessingConfiguration.class).get();

        ObservationCollectionPointTimeSeries position = observations.interpolate(point, interval,
                config.getIdwCount(), config.getIdwDistanceKm(), config.getIdwPower());

        return position;
    }

    @Override
    public String getName() {
        return "position";
    }

    @Override
    public String getSummary() {
        return "retrieve information about observations at a position";
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("A point observation feature with a point geometry at the selected location (`coords`) " +
                "at the selected time or for each time step in the selected time interval (`datetime`). " +
                "The feature contains a property for each selected variable (`variables`) for which " +
                "a value can be interpolated. " +
                "The time steps are determined from the information in the original data.");
    }

    @Override
    public Class<?> getOutputType() {
        return ObservationCollectionPointTimeSeries.class;
    }

    @Override
    public boolean isNeverTerminal() {
        return true;
    }
}
