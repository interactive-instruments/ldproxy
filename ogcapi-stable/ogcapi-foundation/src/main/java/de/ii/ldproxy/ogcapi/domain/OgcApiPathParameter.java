package de.ii.ldproxy.ogcapi.domain;

import java.util.Optional;
import java.util.Set;

public interface OgcApiPathParameter extends ParameterExtension {
    default boolean getExplodeInOpenApi() { return false; }
    Set<String> getValues(OgcApiDataV2 apiData);
    String getPattern();

    boolean isApplicable(OgcApiDataV2 apiData, String definitionPath);
    default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) { return isApplicable(apiData, definitionPath); }
    default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, Optional<String> collectionId) {
        return collectionId.isPresent() ? isApplicable(apiData,definitionPath,collectionId.get()) : isApplicable(apiData,definitionPath);
    }

    @Override
    default boolean getRequired(OgcApiDataV2 apiData) { return true; }
}
