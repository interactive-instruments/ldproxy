package de.ii.ldproxy.ogcapi.styles.app;


import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Component
@Provides
@Instantiate
public class PathParameterStyleId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterStyleId.class);

    @Override
    public String getPattern() {
        return "[^/]+";
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        return ImmutableList.of();
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
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
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               (definitionPath.equals("/styles/{styleId}") ||
                definitionPath.equals("/styles/{styleId}/metadata"));
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }
}
