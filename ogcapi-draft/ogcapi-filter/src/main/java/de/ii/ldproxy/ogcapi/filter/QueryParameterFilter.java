package de.ii.ldproxy.ogcapi.filter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Set;

@Component
@Provides
@Instantiate
public class QueryParameterFilter implements OgcApiQueryParameter, ConformanceClass {

    @Override
    public String getName() {
        return "filter";
    }

    @Override
    public String getDescription() {
        return "Filter features in the collection using the query expression in the parameter value.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    private final Schema schema = new StringSchema();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/filter",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/features-filter",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/simple-cql",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/cql-text",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/cql-json",
                "http://www.opengis.net/spec/ogcapi-records-1/0.0/conf/cql-filter");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FilterConfiguration.class;
    }

    @Override
    public Set<String> getFilterParameters(Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) {
        if (!isEnabledForApi(apiData))
            return filterParameters;

        return new ImmutableSet.Builder<String>()
                .addAll(filterParameters)
                .add("filter")
                .build();
    }
}
