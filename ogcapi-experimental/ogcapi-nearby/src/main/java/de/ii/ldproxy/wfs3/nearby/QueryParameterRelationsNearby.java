package de.ii.ldproxy.wfs3.nearby;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Provides
@Instantiate
public class QueryParameterRelationsNearby implements OgcApiQueryParameter {

    @Override
    public String getId(String collectionId) {
        return "relationsNearby_"+collectionId;
    }

    @Override
    public String getName() {
        return "relations";
    }

    @Override
    public String getDescription() {
        return "List of related collections that should be shown for this feature.";
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items/{featureId}"));
    }

    private Map<String,Schema> schemaMap = new ConcurrentHashMap();

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        String key = apiData.getId()+"__"+collectionId;
        if (!schemaMap.containsKey(key)) {
            Optional<NearbyConfiguration> config = apiData.getCollections()
                    .get(collectionId)
                    .getExtension(NearbyConfiguration.class);
            if (config.isPresent()) {
                ImmutableList<String> relations = config.get()
                        .getRelations()
                        .stream()
                        .map(relation -> relation.getId())
                        .collect(ImmutableList.toImmutableList());
                Schema schema = new ArraySchema().items(new StringSchema()._enum(relations));
                schemaMap.put(key, schema);
            } else {
                schemaMap.put(key, new ArraySchema().items(new StringSchema()._enum(ImmutableList.of())));
            }
        }
        return schemaMap.get(key);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, NearbyConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), NearbyConfiguration.class);
    }
}
