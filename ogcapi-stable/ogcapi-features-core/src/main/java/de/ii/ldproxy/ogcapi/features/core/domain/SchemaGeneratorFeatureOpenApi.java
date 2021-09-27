/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation.Builder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Provides
@Instantiate
public class SchemaGeneratorFeatureOpenApi implements SchemaGeneratorOpenApi {

    private static final String DEFAULT_FLATTENING_SEPARATOR = ".";

    private final ConcurrentMap<Integer, ConcurrentMap<String, Schema<?>>> schemaCache = new ConcurrentHashMap<>();
    private final FeaturesCoreProviders providers;
    private final EntityRegistry entityRegistry;

    public SchemaGeneratorFeatureOpenApi(@Requires FeaturesCoreProviders providers,
                                         @Requires EntityRegistry entityRegistry) {
        this.providers = providers;
        this.entityRegistry = entityRegistry;
    }

    @Override
    public String getSchemaReference(String collectionIdOrName) {
        return "#/components/schemas/featureGeoJson_" + collectionIdOrName;
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaCache.containsKey(apiHashCode))
            schemaCache.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaCache.get(apiHashCode).containsKey(collectionId)) {
            FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                                                                   .get(collectionId);
            String featureTypeId = apiData.getCollections()
                                          .get(collectionId)
                                          .getExtension(FeaturesCoreConfiguration.class)
                                          .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                          .orElse(collectionId);
            FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
            FeatureSchema featureType = featureProvider.getData()
                                                       .getTypes()
                                                       .get(featureTypeId);
            if (Objects.isNull(featureType))
                // Use an empty object schema as fallback, if we cannot get one from the provider
                featureType = new ImmutableFeatureSchema.Builder()
                                                        .name(featureTypeId)
                                                        .type(SchemaBase.Type.OBJECT)
                                                        .build();

            schemaCache.get(apiHashCode)
                            .put(collectionId, getSchema(featureType, collectionData));
        }
        return schemaCache.get(apiHashCode).get(collectionId);
    }

    @Override
    public Schema<?> getSchema(FeatureSchema featureSchema,
        FeatureTypeConfigurationOgcApi collectionData) {
        SchemaDeriverOpenApi schemaDeriverQueryables = new SchemaDeriverOpenApiReturnables(collectionData.getLabel(), collectionData.getDescription(), entityRegistry.getEntitiesForType(Codelist.class));

        Schema<?> schema = featureSchema.accept(new WithTransformationsApplied()).accept(schemaDeriverQueryables);

        return schema;
    }

    @Override
    public Optional<Schema<?>> getQueryable(FeatureSchema featureSchema,
        FeatureTypeConfigurationOgcApi collectionData, String propertyName) {
        WithTransformationsApplied schemaFlattener = new WithTransformationsApplied(
            ImmutableMap.of(PropertyTransformations.WILDCARD, new Builder().flatten(DEFAULT_FLATTENING_SEPARATOR).build()));

        String flatteningSeparator = schemaFlattener.getFlatteningSeparator(featureSchema).orElse(DEFAULT_FLATTENING_SEPARATOR);

        String queryableWithSeparator = Objects.equals(flatteningSeparator, DEFAULT_FLATTENING_SEPARATOR)
            ? propertyName
            : propertyName.replaceAll(Pattern.quote(DEFAULT_FLATTENING_SEPARATOR), flatteningSeparator);

        SchemaDeriverOpenApi schemaDeriverQueryables = new SchemaDeriverOpenApiQueryables(collectionData.getLabel(), collectionData.getDescription(), entityRegistry.getEntitiesForType(Codelist.class), ImmutableList.of(queryableWithSeparator));

        Schema<?> schema = featureSchema.accept(schemaFlattener).accept(schemaDeriverQueryables);

        if (schema.getProperties().containsKey(queryableWithSeparator)) {
            return Optional.ofNullable((Schema<?>) schema.getProperties().get(queryableWithSeparator));
        }

        return Optional.empty();
    }

}
