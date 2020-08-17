package de.ii.ldproxy.ogcapi.collections.queryables;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFQueryables extends QueryParameterF {

    public QueryParameterFQueryables(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fQueryables";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
                definitionPath.equals("/collections/{collectionId}/queryables");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return OgcApiQueryablesFormatExtension.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, QueryablesConfiguration.class);
    }

}
