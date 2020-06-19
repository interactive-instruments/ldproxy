package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormatVariables;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFVariables extends QueryParameterF {

    private static final String DAPA_PATH_ELEMENT = "dapa";

    protected QueryParameterFVariables(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fVariables";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
                definitionPath.equals("/collections/{collectionId}/"+DAPA_PATH_ELEMENT+"/variables");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return ObservationProcessingOutputFormatVariables.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class);
    }

}
