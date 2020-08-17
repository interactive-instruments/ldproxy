package de.ii.ldproxy.ogcapi.collections.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.Schema;
import org.immutables.value.Value;

import java.util.Set;

@Value.Immutable
public abstract class OgcApiQueryParameterTemplateQueryable implements OgcApiQueryParameter {

    public abstract String getApiId();
    public abstract String getCollectionId();
    public abstract Schema getSchema();

    @Override
    @Value.Default
    public String getId() { return getName()+"_"+getCollectionId(); }

    @Override
    public abstract String getName();

    @Override
    public abstract String getDescription();

    @Override
    @Value.Default
    public boolean getExplode() { return false; }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, String collectionId, OgcApiContext.HttpMethods method) {
        return apiData.getId().equals(getApiId()) &&
                method== OgcApiContext.HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}") &&
                collectionId.equals(getCollectionId());
    }

    @Override
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return false;
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return getSchema();
    }

    @Override
    public Set<String> getFilterParameters(Set<String> filterParameters, OgcApiApiDataV2 apiData, String collectionId) {
        if (!isEnabledForApi(apiData))
            return filterParameters;

        return ImmutableSet.<String>builder()
                .addAll(filterParameters)
                .add(getId(collectionId))
                .build();
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return true;
    }
}
