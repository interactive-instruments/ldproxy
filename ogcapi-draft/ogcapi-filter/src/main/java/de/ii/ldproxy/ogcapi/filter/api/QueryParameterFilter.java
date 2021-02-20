package de.ii.ldproxy.ogcapi.filter.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableStartupResult;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.filter.domain.FilterConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public StartupResult onStartup(OgcApiDataV2 apiData, FeatureProviderDataV2.VALIDATION apiValidation) {
        if (apiValidation==FeatureProviderDataV2.VALIDATION.NONE)
            return StartupResult.of();

        ImmutableStartupResult.Builder builder = new ImmutableStartupResult.Builder()
                .mode(apiValidation);

        for (String collectionWithoutQueryables : apiData.getCollections()
                                                         .entrySet()
                                                         .stream()
                                                         .filter(entry ->  entry.getValue()
                                                                                .getExtension(getBuildingBlockConfigurationType())
                                                                                .map(ExtensionConfiguration::getEnabled)
                                                                                .orElse(false) &&
                                                                 !entry.getValue()
                                                                       .getExtension(QueryablesConfiguration.class)
                                                                       .map(ExtensionConfiguration::getEnabled)
                                                                       .orElse(false))
                                                         .map(entry -> entry.getKey())
                                                         .collect(Collectors.toUnmodifiableList())) {
            builder.addStrictErrors(MessageFormat.format("The FILTER module is enabled for collection ''{0}'', but the QUERYABLES module is not enabled.", collectionWithoutQueryables));
        }

        return builder.build();
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
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/cql-json");
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
