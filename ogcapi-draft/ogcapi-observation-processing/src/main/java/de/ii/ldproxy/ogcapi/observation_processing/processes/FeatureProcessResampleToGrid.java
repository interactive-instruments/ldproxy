package de.ii.ldproxy.ogcapi.observation_processing.processes;

import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXyt;
import de.ii.ldproxy.ogcapi.observation_processing.data.GeometryMultiPolygon;
import de.ii.ldproxy.ogcapi.observation_processing.data.Observations;
import de.ii.ldproxy.ogcapi.observation_processing.parameters.PathParameterCollectionIdProcess;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class FeatureProcessResampleToGrid implements ObservationProcess {

    private final ExtensionRegistry extensionRegistry;

    public FeatureProcessResampleToGrid(@Requires ExtensionRegistry extensionRegistry) {
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
        if (obj==null || !(obj instanceof GeometryMultiPolygon))
            throw new RuntimeException("Missing information for executing '" + getName() + "': No area has been provided.");
        obj = processingParameters.get("interval");
        if (obj==null || !(obj instanceof TemporalInterval))
            throw new RuntimeException("Missing information for executing '" + getName() + "': No time interval has been provided.");
        obj = processingParameters.get("width");
        if (obj==null && obj instanceof OptionalInt && ((OptionalInt) obj).isPresent())
            throw new IllegalArgumentException("No grid width has been provided.");
        obj = processingParameters.get("height");
        if (obj==null && obj instanceof OptionalInt && ((OptionalInt) obj).isPresent())
            throw new IllegalArgumentException("No grid height has been provided.");
        obj = processingParameters.get("apiData");
        if (obj==null || !(obj instanceof OgcApiDataV2))
            throw new IllegalArgumentException("Missing information for executing '"+getName()+"': No API information has been provided.");
        obj = processingParameters.get("collectionId");
        if (obj==null || !(obj instanceof String))
            throw new IllegalArgumentException("Missing information for executing '"+getName()+"': No collection identifier has been provided.");
    }

    @Override
    public Object execute(Object data, Map<String, Object> processingParameters) {
        validateProcessingParameters(processingParameters);
        if (!(data instanceof Observations)) {
            throw new RuntimeException("Missing information for executing '"+getName()+"': No observation data has been provided.");
        }
        Observations observations = (Observations) data;
        GeometryMultiPolygon area = (GeometryMultiPolygon) processingParameters.get("area");
        TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");
        OptionalInt gridWidth = (OptionalInt) processingParameters.get("width");
        OptionalInt gridHeight = (OptionalInt) processingParameters.get("height");
        OgcApiDataV2 apiData = (OgcApiDataV2) processingParameters.get("apiData");
        String collectionId = (String) processingParameters.get("collectionId");

        ObservationProcessingConfiguration config =
                apiData.getCollections().get(collectionId).getExtension(ObservationProcessingConfiguration.class).get();

        DataArrayXyt dataArray = observations.resampleToGrid(area.getBbox(), interval, gridWidth, gridHeight, OptionalInt.empty(),
                config.getIdwCount(), config.getIdwDistanceKm(), config.getIdwPower());
        return dataArray;
    }

    @Override
    public Class<?> getOutputType() {
        return DataArrayXyt.class;
    }

    @Override
    public String getName() {
        return "resample-to-grid";
    }

    @Override
    public String getSummary() {
        return "TODO retrieve information about observations in an area";
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
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
}

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

    private float[] getMultiPolygonBbox(List<List<List<List<Float>>>> multiPolygon) {
        final float[] bbox = {180f,90f,-180f,-90f};
        multiPolygon.stream().forEachOrdered(polygon -> {
            polygon.stream().forEachOrdered(ring -> {
                ring.stream().forEachOrdered(pos -> {
                    float lon = pos.get(0);
                    if (lon < bbox[0])
                        bbox[0] = lon;
                    if (lon > bbox[2])
                        bbox[2] = lon;
                    float lat = pos.get(1);
                    if (lat < bbox[1])
                        bbox[1] = lat;
                    if (lat > bbox[3])
                        bbox[3] = lat;
                });
            });
        });
        return bbox;
    }
}
