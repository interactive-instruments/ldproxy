package de.ii.ldproxy.ogcapi.collections.schema;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.QueryParameterProfile;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;

@Component
@Provides
@Instantiate
public class QueryParameterProfileSchema extends QueryParameterProfile {

    final static List<String> PROFILES = ImmutableList.of("2019-09", "07");

    public QueryParameterProfileSchema(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "profileSchema";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return super.isApplicable(apiData, definitionPath, method) &&
                definitionPath.equals("/collections/{collectionId}/schema");
    }

    @Override
    protected List<String> getProfiles(OgcApiDataV2 apiData) {
        return PROFILES;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return SchemaConfiguration.class;
    }

}
