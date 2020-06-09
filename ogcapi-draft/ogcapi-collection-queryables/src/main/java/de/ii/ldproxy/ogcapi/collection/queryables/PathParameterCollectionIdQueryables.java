package de.ii.ldproxy.ogcapi.collection.queryables;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.PathParameterCollectionIdFeatures;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@Provides
@Instantiate
public class PathParameterCollectionIdQueryables extends PathParameterCollectionIdFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterCollectionIdQueryables.class);

    public PathParameterCollectionIdQueryables(@Requires OgcApiFeatureCoreProviders providers) {
        super(providers);
    };

    @Override
    public String getPattern() {
        return "[\\w\\-]+/queryables";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                (definitionPath.matches("/collections/\\{collectionId\\}/schema") ||
                 definitionPath.matches("/collections/\\{collectionId\\}/queryables"));
    }
}
