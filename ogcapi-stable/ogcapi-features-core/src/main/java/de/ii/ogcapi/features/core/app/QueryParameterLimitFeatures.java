/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtendableConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn TODO
 * @langDe TODO
 * @name limit
 * @endpoints Features
 */

@Singleton
@AutoBind
public class QueryParameterLimitFeatures extends ApiExtensionCache implements OgcApiQueryParameter {

    @Inject
    QueryParameterLimitFeatures() {
    }

    @Override
    public String getId(String collectionId) {
        return "limitFeatures_"+collectionId;
    }

    @Override
    public String getName() {
        return "limit";
    }

    @Override
    public String getDescription() {
        return "The optional limit parameter limits the number of items that are presented in the response document. " +
                "Only items are counted that are on the first level of the collection in the response document. " +
                "Nested objects contained within the explicitly requested items are not counted.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items"));
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        String collectionId = "*";
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            Schema schema = new IntegerSchema();
            Optional<Integer> minimumPageSize = apiData.getExtension(FeaturesCoreConfiguration.class)
                                                       .map(FeaturesCoreConfiguration::getMinimumPageSize);
            if (minimumPageSize.isPresent())
                schema.minimum(BigDecimal.valueOf(minimumPageSize.get()));

            Optional<Integer> maxPageSize = apiData.getExtension(FeaturesCoreConfiguration.class)
                                                   .map(FeaturesCoreConfiguration::getMaximumPageSize);
            if (maxPageSize.isPresent())
                schema.maximum(BigDecimal.valueOf(maxPageSize.get()));

            Optional<Integer> defaultPageSize = apiData.getExtension(FeaturesCoreConfiguration.class)
                                                       .map(FeaturesCoreConfiguration::getDefaultPageSize);
            if (defaultPageSize.isPresent())
                schema.setDefault(BigDecimal.valueOf(defaultPageSize.get()));

            schemaMap.get(apiHashCode).put(collectionId, schema);
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            Schema schema = new IntegerSchema();
            FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);

            Optional<Integer> minimumPageSize = featureType.getExtension(FeaturesCoreConfiguration.class)
                                                           .map(FeaturesCoreConfiguration::getMinimumPageSize);
            if (minimumPageSize.isPresent())
                schema.minimum(BigDecimal.valueOf(minimumPageSize.get()));

            Optional<Integer> maxPageSize = featureType.getExtension(FeaturesCoreConfiguration.class)
                                                       .map(FeaturesCoreConfiguration::getMaximumPageSize);
            if (maxPageSize.isPresent())
                schema.maximum(BigDecimal.valueOf(maxPageSize.get()));

            Optional<Integer> defaultPageSize = featureType.getExtension(FeaturesCoreConfiguration.class)
                                                           .map(FeaturesCoreConfiguration::getDefaultPageSize);
            if (defaultPageSize.isPresent())
                schema.setDefault(BigDecimal.valueOf(defaultPageSize.get()));

            schemaMap.get(apiHashCode).put(collectionId, schema);
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Optional<String> validateSchema(OgcApiDataV2 apiData, Optional<String> collectionId, List<String> values) {
        // special validation to support limit values higher than the maximum,
        // the limit will later be reduced to the maximum

        if (values.size()!=1)
            return Optional.of(String.format("Parameter value '%s' is invalid for parameter '%s': The must be a single value.", values, getName()));

        int limit;
        try {
            limit = Integer.parseInt(values.get(0));
        } catch (NumberFormatException exception) {
            return Optional.of(String.format("Parameter value '%s' is invalid for parameter '%s': The value is not an integer.", values, getName()));
        }

        ExtendableConfiguration context = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : apiData;
        Optional<Integer> minimumPageSize = context.getExtension(FeaturesCoreConfiguration.class)
                                                   .map(FeaturesCoreConfiguration::getMinimumPageSize);
        if (minimumPageSize.isPresent() && limit < minimumPageSize.get())
            return Optional.of(String.format("Parameter value '%s' is invalid for parameter '%s': The value is smaller than the minimum value '%d'.", values, getName(), minimumPageSize.get()));

        return Optional.empty();
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters,
                                                   OgcApiDataV2 apiData) {
        if (parameters.containsKey(getName())) {
            // the parameter has been validated, so it must be an integer
            int limit = Integer.parseInt(parameters.get(getName()));
            Optional<Integer> maxPageSize = featureType.getExtension(FeaturesCoreConfiguration.class)
                                                       .map(FeaturesCoreConfiguration::getMaximumPageSize);
            if (maxPageSize.isPresent() && limit > maxPageSize.get()) {
                parameters = new ImmutableMap.Builder<String, String>().putAll(parameters.entrySet()
                                                                                         .stream()
                                                                                         .filter(entry -> !entry.getKey().equals(getName()))
                                                                                         .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)))
                                                                       .put(getName(), String.valueOf(maxPageSize.get()))
                                                                       .build();
            }
        }
        return parameters;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }

}
