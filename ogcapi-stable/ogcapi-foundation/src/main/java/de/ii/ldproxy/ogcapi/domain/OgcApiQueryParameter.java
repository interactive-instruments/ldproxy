package de.ii.ldproxy.ogcapi.domain;

import java.util.Optional;
import java.util.Set;

public interface OgcApiQueryParameter extends ParameterExtension {

    default String getStyle() { return "form"; }

    boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method);
    default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) { return isApplicable(apiData, definitionPath, method); }
    default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, Optional<String> collectionId, HttpMethods method) {
        return collectionId.isPresent() ? isApplicable(apiData,definitionPath,collectionId.get(),method) : isApplicable(apiData,definitionPath,method);
    }

    default Set<String> getFilterParameters(Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) { return filterParameters; };
}
