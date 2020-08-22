package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.runtime.domain.Constants;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

@Component
@Provides
@Instantiate
public class QueryParameterPrettyFeaturesGeoJson implements OgcApiQueryParameter {

    private final Schema schema = new BooleanSchema()._default(false);
    private final boolean allowDebug;

    public QueryParameterPrettyFeaturesGeoJson(@Context BundleContext context) {
        this.allowDebug = Constants.ENV.valueOf(context.getProperty(Constants.ENV_KEY)) == Constants.ENV.DEVELOPMENT;
    }

    @Override
    public String getName() {
        return "pretty";
    }

    @Override
    public String getDescription() {
        return "Debug option in development environments: Pretty print the GeoJSON output.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}"));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, GeoJsonConfiguration.class) && allowDebug;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), GeoJsonConfiguration.class) && allowDebug;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonConfiguration.class;
    }
}
