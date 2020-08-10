package de.ii.ldproxy.ogcapi.domain;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.infra.json.SchemaValidator;
import de.ii.ldproxy.ogcapi.infra.json.SchemaValidatorImpl;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OgcApiParameter extends OgcApiExtension {

    Schema SCHEMA = new StringSchema();

    default String getId() { return getName(); }
    default String getId(String collectionId) { return getId(); }
    default String getId(Optional<String> collectionId) {
        return collectionId.isPresent() ? getId(collectionId.get()) : getId();
    }
    String getName();
    String getDescription();
    default boolean getRequired(OgcApiApiDataV2 apiData) { return false; }
    default boolean getRequired(OgcApiApiDataV2 apiData, String collectionId) { return getRequired(apiData); }
    default boolean getRequired(OgcApiApiDataV2 apiData, Optional<String> collectionId) {
        return collectionId.isPresent() ? getRequired(apiData,collectionId.get()) : getRequired(apiData);
    }
    default Schema getSchema(OgcApiApiDataV2 apiData) { return SCHEMA; }
    default Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) { return getSchema(apiData); }
    default Schema getSchema(OgcApiApiDataV2 apiData, Optional<String> collectionId) {
        return collectionId.isPresent() ? getSchema(apiData,collectionId.get()) : getSchema(apiData);
    }
    default boolean getExplode() { return false; }

    default Optional<String> validate(OgcApiApiDataV2 apiData, Optional<String> collectionId, List<String> values) {
        return validateSchema(apiData, collectionId, values);
    }

    default Optional<String> validateSchema(OgcApiApiDataV2 apiData, Optional<String> collectionId, List<String> values) {
        try {
            SchemaValidator validator = new SchemaValidatorImpl();
            String schemaContent = Json.mapper().writeValueAsString(getSchema(apiData, collectionId));
            Optional<String> result1 = Optional.empty();
            if (values.size()==1) {
                // try non-array variant first
                result1 = validator.validate(schemaContent, "\""+values.get(0)+"\"");
                if (!result1.isPresent())
                    return Optional.empty();
                if (!getExplode() && values.get(0).contains(",")) {
                    values = Splitter.on(",")
                            .trimResults()
                            .omitEmptyStrings()
                            .splitToList(values.get(0));
                }
            }
            Optional<String> resultn = validator.validate(schemaContent, "[\"" + String.join("\",\"", values) + "\"]");
            if (resultn.isPresent()) {
                if (result1.isPresent())
                    return Optional.of(String.format("Parameter value '%s' is invalid for parameter '%s': %s", values, getName(), result1.get()));
                else
                    return Optional.of(String.format("Parameter value '%s' is invalid for parameter '%s': %s", values, getName(), resultn.get()));
            }
        } catch (IOException e) {
            // TODO log an error
            return Optional.of(String.format("An exception occurred while validating the parameter value '%s' for parameter '%s'", values, getName()));
        }

        return Optional.empty();
    };

    default Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                    Map<String, String> parameters,
                                                    OgcApiApiDataV2 apiData) {
        return parameters;
    }

    default ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureType,
                                                         ImmutableFeatureQuery.Builder queryBuilder,
                                                         Map<String, String> parameters,
                                                         OgcApiApiDataV2 apiData) {
        return queryBuilder;
    }

    default Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                 Map<String, Object> context,
                                                 Map<String, String> parameters,
                                                 OgcApiApiDataV2 apiData) {
        return context;
    }
}
