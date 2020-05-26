package de.ii.ldproxy.ogcapi.observation_processing.processes;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.feature_processing.api.FeatureProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.application.*;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXy;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXyt;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureProcessAggregateTimeGrid implements ObservationProcess {

    private final OgcApiExtensionRegistry extensionRegistry;

    public FeatureProcessAggregateTimeGrid(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public List<FeatureProcess> getSupportedProcesses(OgcApiApiDataV2 apiData) {
        return extensionRegistry.getExtensionsForType(FeatureProcess.class).stream()
                .filter(param -> param.getOutputType()== DataArrayXyt.class)
                .collect(Collectors.toList());
    }

    @Override
    public void validateProcessingParameters(Map<String, Object> processingParameters) throws ServerErrorException {
        Object obj = processingParameters.get("functions");
        if (obj==null || !(obj instanceof List) ||((List)obj).isEmpty() || !(((List)obj).get(0) instanceof ObservationProcessingStatisticalFunction)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No statistical functions for the aggregation has been provided.", 500);
        }
        obj = processingParameters.get("interval");
        if (obj==null || !(obj instanceof TemporalInterval)) {
            throw new ServerErrorException("Missing information for executing '"+getName()+"': No time interval has been provided.", 500);
        }
    }

    @Override
    public Object execute(Object data, Map<String, Object> processingParameters) {
        validateProcessingParameters(processingParameters);
        if (!(data instanceof DataArrayXyt)) {
            throw new ServerErrorException("Missing information for executing '" + getName() + "': No grid data has been provided.", 500);
        }
        DataArrayXyt array = (DataArrayXyt) data;
        TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");
        List<ObservationProcessingStatisticalFunction> functions = (List<ObservationProcessingStatisticalFunction>) processingParameters.get("functions");
        int functionCount = functions.size();

        Vector<String> vars = array.getVars();
        Vector<String> newVars = new Vector<>();
        for (String var: vars) {
            for (ObservationProcessingStatisticalFunction f : functions) {
                if (Number.class.isAssignableFrom(f.getType())) {
                    String variable_function = String.join("_", var, f.getName());
                    newVars.add(variable_function);
                }
            }
        }

        DataArrayXy newArray = new DataArrayXy(array.getWidth(), array.getHeight(), newVars,
                array.lon(0), array.lat(0), array.lon(array.getWidth()-1), array.lat(array.getHeight()-1),
                interval);

        for (int i0=0; i0<array.getWidth(); i0++)
            for (int i1=0; i1<array.getHeight(); i1++) {
                for (int i3 = 0; i3 < vars.size(); i3++) {
                    CopyOnWriteArrayList<Number> vals = new CopyOnWriteArrayList<>();
                    for (int i2 = 0; i2 < array.getSteps(); i2++)
                        if (!Float.isNaN(array.array[i2][i1][i0][i3]))
                            vals.add(array.array[i2][i1][i0][i3]);
                    for (int i4 = 0; i4 < functionCount; i4++) {
                        ObservationProcessingStatisticalFunction f = functions.get(i4);
                        if (Number.class.isAssignableFrom(f.getType()) && !vals.isEmpty())
                            newArray.array[i1][i0][i3*functionCount+i4] = f.getValue(vals).floatValue();
                        else
                            newArray.array[i1][i0][i3*functionCount+i4] = Float.NaN;
                    }
                }
            }

        return newArray;
    }

    @Override
    public Class<?> getOutputType() {
        return DataArrayXy.class;
    }

    @Override
    public String getName() {
        return "aggregate-time";
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
