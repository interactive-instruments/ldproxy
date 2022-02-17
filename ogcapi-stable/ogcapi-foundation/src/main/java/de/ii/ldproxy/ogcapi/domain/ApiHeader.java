/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.infra.json.SchemaValidatorImpl;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AutoMultiBind
public interface ApiHeader extends ApiExtension {

    Schema SCHEMA = new StringSchema();

    String getId();
    String getDescription();
    default boolean isRequestHeader() { return false; }
    default boolean isResponseHeader() { return false; }
    default Schema getSchema(OgcApiDataV2 apiData) { return SCHEMA; }

    boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method);

    default Optional<String> validateSchema(OgcApiDataV2 apiData, String value) {
        try {
            SchemaValidator validator = new SchemaValidatorImpl();
            String schemaContent = Json.mapper().writeValueAsString(getSchema(apiData));
            Optional<String> result = validator.validate(schemaContent, "\""+value+"\"");
            if (!result.isPresent())
                return Optional.empty();
            return Optional.of(String.format("Value '%s' is invalid for header '%s': %s", value, getId(), result.get()));
        } catch (IOException e) {
            // TODO log an error
            return Optional.of(String.format("An exception occurred while validating the value '%s' for header '%s'", value, getId()));
        }
    };

    default ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureType,
                                                         ImmutableFeatureQuery.Builder queryBuilder,
                                                         Map<String, String> headers,
                                                         OgcApiDataV2 apiData) {
        return queryBuilder;
    }

    default Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                 Map<String, Object> context,
                                                 Map<String, String> headers,
                                                 OgcApiDataV2 apiData) {
        return context;
    }
}
