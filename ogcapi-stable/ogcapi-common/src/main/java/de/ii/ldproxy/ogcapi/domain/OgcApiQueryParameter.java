package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import java.util.Optional;
import java.util.Set;

public interface OgcApiQueryParameter extends OgcApiParameter {

    default String getStyle() { return "form"; }

    boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method);
    default boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, String collectionId, OgcApiContext.HttpMethods method) { return isApplicable(apiData, definitionPath, method); }
    default boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, Optional<String> collectionId, OgcApiContext.HttpMethods method) {
        return collectionId.isPresent() ? isApplicable(apiData,definitionPath,collectionId.get(),method) : isApplicable(apiData,definitionPath,method);
    }

    default Set<String> getFilterParameters(Set<String> filterParameters, OgcApiApiDataV2 apiData, String collectionId) { return filterParameters; };
}
