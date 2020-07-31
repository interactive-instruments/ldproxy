package de.ii.ldproxy.resources;


import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.wfs3.styles.StylesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;


@Component
@Provides
@Instantiate
public class PathParameterResourceId implements OgcApiPathParameter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParameterResourceId.class);

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
        return new StringSchema().pattern(getPattern());
    }

    @Override
    public String getName() {
        return "resourceId";
    }

    @Override
    public String getDescription() {
        return "The file name of the file resource.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) && definitionPath.equals("/resources/{resourceId}");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if (stylesExtension.isPresent() && stylesExtension.get()
                                                          .getResourcesEnabled()) {
            return true;
        }
        return false;
    }
}
