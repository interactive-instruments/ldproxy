package de.ii.ldproxy.wfs3.oas30;


import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Component
@Provides
@Instantiate
public class PathParameterResourceApiDefinition implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterResourceApiDefinition.class);
    Map<String,Set<String>> apiCollectionMap;

    public PathParameterResourceApiDefinition() {
        apiCollectionMap = new HashMap<>();
    };

    @Override
    public String getPattern() {
        return "[^/]+";
    }

    @Override
    public Set<String> getValues(OgcApiApiDataV2 apiData) {
        return ImmutableSet.of();
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return new StringSchema();
    }

    @Override
    public String getName() {
        return "resource";
    }

    @Override
    public String getDescription() {
        return "The filename of a file used by Swagger UI.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                definitionPath.matches("/api/[^/]+/?");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return true;
    }
}
