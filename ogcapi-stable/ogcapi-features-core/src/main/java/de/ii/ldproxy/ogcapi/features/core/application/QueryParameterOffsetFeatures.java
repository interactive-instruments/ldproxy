package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class QueryParameterOffsetFeatures implements OgcApiQueryParameter {

    @Override
    public String getId() {
        return "offsetFeatures";
    }

    @Override
    public String getName() {
        return "offset";
    }

    @Override
    public String getDescription() {
        return "The optional offset parameter identifies the index of the first feature in the response in the overall " +
                "result set.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items");
    }

    private Schema schema = null;

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        if (schema==null) {
            schema = new IntegerSchema()._default(0).minimum(BigDecimal.ZERO);
        }
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

}
