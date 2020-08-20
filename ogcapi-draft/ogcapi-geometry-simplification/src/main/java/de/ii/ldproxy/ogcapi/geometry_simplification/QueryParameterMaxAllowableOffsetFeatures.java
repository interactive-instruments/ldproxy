package de.ii.ldproxy.ogcapi.geometry_simplification;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Provides
@Instantiate
public class QueryParameterMaxAllowableOffsetFeatures implements OgcApiQueryParameter {

    @Override
    public String getName() {
        return "maxAllowableOffset";
    }

    @Override
    public String getDescription() {
        return "This option can be used to specify the maxAllowableOffset to be used for simplifying the geometries in the response. " +
                "The maxAllowableOffset is in the units of the response coordinate reference system.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}"));
    }

    private final Schema schema = new NumberSchema()._default(BigDecimal.valueOf(0)).example(0.05);

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, GeometrySimplificationConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), GeometrySimplificationConfiguration.class);
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiApiDataV2 datasetData) {
        if (!isExtensionEnabled(datasetData, GeometrySimplificationConfiguration.class)) {
            return queryBuilder;
        }
        if (parameters.containsKey("maxAllowableOffset")) {
            try {
                queryBuilder.maxAllowableOffset(Double.valueOf(parameters.get("maxAllowableOffset")));
            } catch (NumberFormatException e) {
                //ignore
            }
        }

        return queryBuilder;
    }
}
