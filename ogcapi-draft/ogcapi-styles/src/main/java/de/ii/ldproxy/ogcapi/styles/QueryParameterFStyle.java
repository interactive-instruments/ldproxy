package de.ii.ldproxy.ogcapi.styles;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFStyle extends QueryParameterF {

    public QueryParameterFStyle(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fStyle";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
                definitionPath.equals("/styles/{styleId}");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return StyleFormatExtension.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
    }

}
