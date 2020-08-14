package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormat;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.ArrayList;
import java.util.List;

import static de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormat.DAPA_PATH_ELEMENT;

@Component
@Provides
@Instantiate
public class QueryParameterFProcessesPosition extends QueryParameterF {

    @Requires
    FeatureProcessInfo featureProcessInfo;

    protected QueryParameterFProcessesPosition(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fProcesses-position";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"position");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return ObservationProcessingOutputFormat.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(featureType -> featureType.getEnabled())
                        .filter(featureType -> isEnabledForApi(apiData, featureType.getId()))
                        .findAny()
                        .isPresent();
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        String key = apiData.getId()+"_*";
        if (!schemaMap.containsKey(key)) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                    .stream()
                    .filter(f -> f.isEnabledForApi(apiData))
                    .filter(f -> !f.getMediaType().parameter().equals("*"))
                    .filter(f -> f.getContent(apiData, "/collections/{collectionId}/"+DAPA_PATH_ELEMENT+"/position")!=null)
                    .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.put(key, new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(key);
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        String key = apiData.getId()+"_"+collectionId;
        if (!schemaMap.containsKey(key)) {
            List<String> fEnum = new ArrayList<>();
            extensionRegistry.getExtensionsForType(getFormatClass())
                    .stream()
                    .filter(f -> f.isEnabledForApi(apiData, collectionId))
                    .filter(f -> !f.getMediaType().parameter().equals("*"))
                    .filter(f -> f.getContent(apiData, "/collections/"+collectionId+"/"+DAPA_PATH_ELEMENT+"/position")!=null)
                    .forEach(f -> fEnum.add(f.getMediaType().parameter()));
            schemaMap.put(key, new StringSchema()._enum(fEnum));
        }
        return schemaMap.get(key);
    }
}
