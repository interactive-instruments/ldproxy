package de.ii.ldproxy.ogcapi.features.html.app;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.runtime.FelixRuntime;
import de.ii.xtraplatform.runtime.domain.Constants;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

//TODO: this was not meant for debugging but is needed for the nearby functionality, so it could be moved to community
@Component
@Provides
@Instantiate
public class QueryParameterBareHtml implements OgcApiQueryParameter {

    private final Schema schema = new BooleanSchema()._default(false);
    private final boolean allowDebug;

    public QueryParameterBareHtml(@Context BundleContext context) {
        this.allowDebug = Constants.ENV.valueOf(context.getProperty(Constants.ENV_KEY)) == Constants.ENV.DEVELOPMENT;
    }

    @Override
    public String getName() {
        return "bare";
    }

    @Override
    public String getDescription() {
        return "Debug option in development environments: Bare HTML output for feature pages.";
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
        return isExtensionEnabled(apiData, HtmlConfiguration.class) && allowDebug;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), HtmlConfiguration.class) && allowDebug;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return HtmlConfiguration.class;
    }
}
