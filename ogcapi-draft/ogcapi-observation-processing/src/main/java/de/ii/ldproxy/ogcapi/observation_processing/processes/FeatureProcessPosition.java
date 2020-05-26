package de.ii.ldproxy.ogcapi.observation_processing.processes;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.application.*;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryPoint;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointTimeSeries;
import de.ii.ldproxy.ogcapi.observation_processing.data.Observations;
import de.ii.ldproxy.ogcapi.observation_processing.parameters.PathParameterCollectionIdProcess;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureProcessPosition implements ObservationProcess {

    private final OgcApiExtensionRegistry extensionRegistry;

    public FeatureProcessPosition(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public Set<String> getSupportedCollections(OgcApiApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(PathParameterCollectionIdProcess.class).stream()
                .map(param -> param.getValues(apiData))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public void validateProcessingParameters(Map<String, Object> processingParameters) throws ServerErrorException {
        Object obj = processingParameters.get("point");
        if (obj==null || !(obj instanceof GeometryPoint)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No point has been provided.", 500);
        }
        obj = processingParameters.get("interval");
        if (obj==null || !(obj instanceof TemporalInterval)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No time interval has been provided.", 500);
        }
    }

    @Override
    public Object execute(Object data, Map<String, Object> processingParameters) {
        validateProcessingParameters(processingParameters);
        if (!(data instanceof Observations)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No observation data has been provided.", 500);
        }
        Observations observations = (Observations) data;
        GeometryPoint point = (GeometryPoint) processingParameters.get("point");
        TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");

        ObservationCollectionPointTimeSeries position = observations.interpolate(point, interval);

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
        return Optional.of("A point observation feature with a point geometry at the selected location (`coord`) " +
                "at the selected time or for each time step in the selected time interval (`datetime`). " +
                "The feature contains a property for each selected variable (`variables`) for which " +
                "a value can be interpolated. " +
                "The time steps are determined from the information in the original data.");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

    public Class<?> getOutputType() {
        return ObservationCollectionPointTimeSeries.class;
    }
}
