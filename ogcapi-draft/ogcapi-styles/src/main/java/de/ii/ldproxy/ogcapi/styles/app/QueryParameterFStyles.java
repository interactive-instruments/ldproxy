package de.ii.ldproxy.ogcapi.styles.app;

import de.ii.ldproxy.ogcapi.common.domain.QueryParameterF;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StylesFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFStyles extends QueryParameterF {

    public QueryParameterFStyles(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fStyles";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
                (definitionPath.equals("/styles") || definitionPath.equals("/styles/{styleId}/metadata"));
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return StylesFormatExtension.class;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

}
