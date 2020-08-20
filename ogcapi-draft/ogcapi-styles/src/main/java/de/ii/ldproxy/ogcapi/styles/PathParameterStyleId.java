package de.ii.ldproxy.ogcapi.styles;


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

import java.util.Set;


@Component
@Provides
@Instantiate
public class PathParameterStyleId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterStyleId.class);

    @Override
    public String getPattern() {
        return "[\\w]+";
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
        return "styleId";
    }

    @Override
    public String getDescription() {
        return "The local identifier of a style, unique within the API.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               (definitionPath.equals("/styles/{styleId}") ||
                definitionPath.equals("/styles/{styleId}/metadata"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
    }
}
