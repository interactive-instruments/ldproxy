/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.sorting;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class QueryParameterSortbyFeatures implements OgcApiQueryParameter, ConformanceClass {

    final static Splitter KEYS_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-records-1/0.0/conf/sorting");
    }

    @Override
    public String getId(String collectionId) {
        return "sortby_"+collectionId;
    }

    @Override
    public String getName() {
        return "sortby";
    }

    @Override
    public String getDescription() {
        return "Sort the results based on the properties identified by this parameter. " +
                "The parameter value is a comma-separated list of property names, where each parameter name " +
                "may be preceeded by a '+' (ascending, default) or '-' (descending).";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return apiData.getCollections()
                      .entrySet()
                      .stream()
                      .anyMatch(entry -> isEnabledForApi(apiData, entry.getKey())) &&
                method==HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items");
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        return isEnabledForApi(apiData, collectionId) &&
                method== HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items");
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            List<String> sortables = apiData.getCollections()
                                            .get(collectionId)
                                            .getExtension(SortingConfiguration.class)
                                            .map(cfg -> cfg.getSortables())
                                            .orElse(ImmutableList.of());
            schemaMap.get(apiHashCode)
                     .put(collectionId, new ArraySchema().items(new StringSchema()._enum(sortables.stream()
                                                                                                  .map(p -> ImmutableList.of(p, "+"+p, "-"+p))
                                                                                                  .flatMap(Collection::stream)
                                                                                                  .collect(Collectors.toUnmodifiableList()))));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return SortingConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 datasetData) {
        if (!isExtensionEnabled(datasetData, SortingConfiguration.class)) {
            return queryBuilder;
        }
        if (parameters.containsKey("sortby")) {
            // the validation against the schema has verified that only valid properties are listed
            for (String key: KEYS_SPLITTER.split(parameters.get("sortby"))) {
                if (key.startsWith("-")) {
                    queryBuilder.addSortKeys(SortKey.of(key.substring(1), SortKey.Direction.DESCENDING));
                } else {
                    if (key.startsWith("+"))
                        key = key.substring(1);
                    queryBuilder.addSortKeys(SortKey.of(key));
                }
            }
        }

        return queryBuilder;
    }
}
