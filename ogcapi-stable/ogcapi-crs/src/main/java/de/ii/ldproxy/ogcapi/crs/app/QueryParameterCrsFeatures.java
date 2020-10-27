package de.ii.ldproxy.ogcapi.crs.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Provides
@Instantiate
public class QueryParameterCrsFeatures implements OgcApiQueryParameter, ConformanceClass {

    public static final String CRS = "crs";
    public static final String CRS84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public static final String CRS84H = "http://www.opengis.net/def/crs/OGC/0/CRS84h";

    private final CrsSupport crsSupport;

    public QueryParameterCrsFeatures(@Requires CrsSupport crsSupport) {
        this.crsSupport = crsSupport;
    }

    @Override
    public String getId(String collectionId) {
        return CRS+"Features_"+collectionId;
    }

    @Override
    public String getName() {
        return CRS;
    }

    @Override
    public String getDescription() {
        return "The coordinate reference system of the response geometries. Default is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84).";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}"));
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            List<String> crsList = crsSupport.getSupportedCrsList(apiData, apiData.getCollections().get(collectionId))
                                             .stream()
                                             .map(EpsgCrs::toUriString)
                                             .collect(ImmutableList.toImmutableList());
            schemaMap.get(apiHashCode).put(collectionId, new StringSchema()._enum(crsList)._default(CRS84));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CrsConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 apiData) {

        if (isEnabledForApi(apiData) && parameters.containsKey(CRS)) {
            EpsgCrs targetCrs;
            try {
                targetCrs = EpsgCrs.fromString(parameters.get(CRS));
            } catch (Throwable e) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: %s", CRS, e.getMessage()), e);
            }
            if (!crsSupport.isSupported(apiData, featureTypeConfiguration, targetCrs)) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: the crs '%s' is not supported", CRS, targetCrs.toUriString()));
            }

            queryBuilder.crs(targetCrs);
        }

        return queryBuilder;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs");
    }
}
