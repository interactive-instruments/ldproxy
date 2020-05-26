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
public class QueryParameterOffsetNearby implements OgcApiQueryParameter {

    @Override
    public String getId() {
        return "offsetNearby";
    }

    @Override
    public String getName() {
        return "offset";
    }

    @Override
    public String getDescription() {
        return "List of offsets for each related collection in the `relations` parameter.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items/{featureId}"));
    }

    private final Schema schema = new ArraySchema()
            .items(new IntegerSchema()._default(0).minimum(BigDecimal.valueOf(0)));

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, NearbyConfiguration.class);
    }
}
