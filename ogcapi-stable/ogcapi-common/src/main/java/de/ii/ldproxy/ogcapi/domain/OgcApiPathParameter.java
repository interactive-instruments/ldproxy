package de.ii.ldproxy.ogcapi.domain;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public interface OgcApiPathParameter extends OgcApiParameter {
    default boolean getExplodeInOpenApi() { return false; }
    Set<String> getValues(OgcApiApiDataV2 apiData);
    String getPattern();

    boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath);
    default boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, String collectionId) { return isApplicable(apiData, definitionPath); }
    default boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, Optional<String> collectionId) {
        return collectionId.isPresent() ? isApplicable(apiData,definitionPath,collectionId.get()) : isApplicable(apiData,definitionPath);
    }

    @Override
    default boolean getRequired(OgcApiApiDataV2 apiData) { return true; }
}
