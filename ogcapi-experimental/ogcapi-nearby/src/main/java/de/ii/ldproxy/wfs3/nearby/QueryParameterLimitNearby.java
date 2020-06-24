package de.ii.ldproxy.wfs3.nearby;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;

@Component
@Provides
@Instantiate
public class QueryParameterLimitNearby implements OgcApiQueryParameter {

    @Override
    public String getId() {
        return "limitNearby";
    }
    @Override
    public String getName() {
        return "limit";
    }

    @Override
    public String getDescription() {
        return "List of limits for each related collection in the `relations` parameter.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items/{featureId}"));
    }

    private final Schema schema = new ArraySchema().items(new IntegerSchema()._default(5)
            .minimum(BigDecimal.valueOf(0))
            .maximum(BigDecimal.valueOf(10000)));

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, NearbyConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), NearbyConfiguration.class);
    }
}
