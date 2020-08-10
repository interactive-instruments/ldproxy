package de.ii.ldproxy.ogcapi.filter;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class QueryParameterFilterLang implements OgcApiQueryParameter {

    private static final String FILTER_LANG_CQL = "cql-text";
    private static final String FILTER_LANG_JSON = "cql-json";

    @Override
    public String getName() {
        return "filter-lang";
    }

    @Override
    public String getDescription() {
        return "Language of the query expression in the 'filter' parameter.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                 definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    private final Schema schema = new StringSchema()._enum(ImmutableList.of(FILTER_LANG_CQL, FILTER_LANG_JSON))._default(FILTER_LANG_CQL);

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, FilterConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), FilterConfiguration.class);
    }
}
