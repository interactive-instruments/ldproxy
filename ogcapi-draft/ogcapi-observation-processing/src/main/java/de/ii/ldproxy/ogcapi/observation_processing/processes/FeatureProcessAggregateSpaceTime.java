package de.ii.ldproxy.ogcapi.observation_processing.processes;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.application.*;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionArea;
import de.ii.ldproxy.ogcapi.observation_processing.data.ObservationCollectionPointTimeSeriesList;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureProcessAggregateSpaceTime implements ObservationProcess {

    private final OgcApiExtensionRegistry extensionRegistry;

    public FeatureProcessAggregateSpaceTime(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public List<FeatureProcess> getSupportedProcesses(OgcApiApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(FeatureProcess.class).stream()
                .filter(param -> param.getOutputType()== ObservationCollectionPointTimeSeriesList.class)
                .collect(Collectors.toList());
    }

    @Override
    public void validateProcessingParameters(Map<String, Object> processingParameters) throws ServerErrorException {
        Object obj = processingParameters.get("area");
        if (obj==null || !(obj instanceof GeometryMultiPolygon)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No area has been provided.", 500);
        }
        obj = processingParameters.get("interval");
        if (obj==null || !(obj instanceof TemporalInterval)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No time interval has been provided.", 500);
        }
        obj = processingParameters.get("functions");
        if (obj==null || !(obj instanceof List) ||((List)obj).isEmpty() || !(((List)obj).get(0) instanceof ObservationProcessingStatisticalFunction)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No statistical functions for the aggregation has been provided.", 500);
        }
    }

    @Override
    public Object execute(Object data, Map<String, Object> processingParameters) {
        validateProcessingParameters(processingParameters);
        if (!(data instanceof ObservationCollectionPointTimeSeriesList)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No time series data has been provided.", 500);
        }
        ObservationCollectionPointTimeSeriesList timeSeriesPoints = (ObservationCollectionPointTimeSeriesList) data;
        GeometryMultiPolygon area = (GeometryMultiPolygon) processingParameters.get("area");
        TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");
        List<ObservationProcessingStatisticalFunction> functions = (List<ObservationProcessingStatisticalFunction>) processingParameters.get("functions");

        ObservationCollectionArea obsColArea = new ObservationCollectionArea(area, interval);

        ConcurrentMap<String, CopyOnWriteArrayList<Number>> values = new ConcurrentHashMap<>();
        timeSeriesPoints.stream()
                .map(pos -> pos.getValues().values())
                .flatMap(Collection::parallelStream)
                .map(entry -> entry.keySet())
                .flatMap(Set::parallelStream)
                .distinct()
                .forEach(variable -> {
                    CopyOnWriteArrayList<Number> vs = timeSeriesPoints.parallelStream()
                            .map(pos -> pos.getValues().values())
                            .flatMap(Collection::parallelStream)
                            .map(entry -> entry.get(variable))
                            .filter(v -> Objects.nonNull(v))
                            .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                    if (!vs.isEmpty()) {
                        functions.stream()
                            .filter(f -> Number.class.isAssignableFrom(f.getType()))
                            .forEach(f -> obsColArea.put(
                                    variable + "_" + f.getName(),
                                    f.getValue(vs)));
                    }
                });

        return obsColArea;
    }

    @Override
    public Class<?> getOutputType() {
        return null;
    }

    @Override
    public String getName() {
        return "aggregate-space-time";
    }

    @Override
    public String getSummary() {
        return "retrieve information about observations and compute values aggregated over time";
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Observation features with a point geometry at the selected location or " +
                "within the selected area. " +
                "Each feature includes a property for each combination of a variable (`variables`) for which " +
                "a value can be interpolated and a statistical function (`functions`), separated by an underscore. " +
                "The property value is the function applied to the interpolated values for each time step " +
                "in the selected time interval (`datetime`).");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }
}
