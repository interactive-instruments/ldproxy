package de.ii.ldproxy.ogcapi.features.core.application;


import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


@Component
@Provides
@Instantiate
public class PathParameterFeatureIdFeatures implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterFeatureIdFeatures.class);

    final OgcApiFeatureCoreProviders providers;

    public PathParameterFeatureIdFeatures(@Requires OgcApiFeatureCoreProviders providers) {
        this.providers = providers;
    };

    @Override
    public String getPattern() {
        return "[\\w\\-]+";
    }

    @Override
    public boolean getExplodeInOpenApi() {
        return false;
    }

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        return ImmutableSet.of();
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema().pattern(getPattern());
    }

    @Override
    public String getName() {
        return "featureId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a feature, unique within the feature collection.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                definitionPath.equals("/collections/{collectionId}/items/{featureId}");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return true;
    }
}
