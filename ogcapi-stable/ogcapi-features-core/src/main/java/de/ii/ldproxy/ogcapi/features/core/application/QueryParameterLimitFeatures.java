package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class QueryParameterLimitFeatures implements OgcApiQueryParameter {

    @Override
    public String getId() {
        return "limitFeatures";
    }

    @Override
    public String getName() {
        return "limit";
    }

    @Override
    public String getDescription() {
        return "The optional limit parameter limits the number of items that are presented in the response document. " +
                "Only items are counted that are on the first level of the collection in the response document. " +
                "Nested objects contained within the explicitly requested items shall not be counted.";
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
            schema = new IntegerSchema();

            Optional<Integer> minimumPageSize = getExtensionConfiguration(apiData, OgcApiFeaturesCoreConfiguration.class)
                    .map(OgcApiFeaturesCoreConfiguration::getMinimumPageSize);
            if (minimumPageSize.isPresent())
                schema.minimum(BigDecimal.valueOf(minimumPageSize.get()));

            Optional<Integer> defaultPageSize = getExtensionConfiguration(apiData, OgcApiFeaturesCoreConfiguration.class)
                    .map(OgcApiFeaturesCoreConfiguration::getDefaultPageSize);
            if (defaultPageSize.isPresent())
                schema.setDefault(BigDecimal.valueOf(defaultPageSize.get()));

            Optional<Integer> maxPageSize = getExtensionConfiguration(apiData, OgcApiFeaturesCoreConfiguration.class)
                    .map(OgcApiFeaturesCoreConfiguration::getMaxPageSize);
            if (maxPageSize.isPresent())
                schema.maximum(BigDecimal.valueOf(maxPageSize.get()));
        }
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

}
