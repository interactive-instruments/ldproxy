/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureEncoderGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class FeaturesFormatGeoJson implements ConformanceClass, FeatureFormatExtension {

    private static final String CONFORMANCE_CLASS_FEATURES = "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson";
    private static final String CONFORMANCE_CLASS_RECORDS = "http://www.opengis.net/spec/ogcapi-records-1/0.0/conf/json";
    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "geo+json"))
            .label("GeoJSON")
            .parameter("json")
            .build();
    public static final ApiMediaType COLLECTION_MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final FeaturesCoreProviders providers;
    private final EntityRegistry entityRegistry;
    private final FeaturesCoreValidation featuresCoreValidator;
    private final SchemaGeneratorOpenApi schemaGeneratorFeature;
    private final SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection;
    private final GeoJsonWriterRegistry geoJsonWriterRegistry;

    @Inject
    public FeaturesFormatGeoJson(FeaturesCoreProviders providers,
                                 EntityRegistry entityRegistry,
                                 FeaturesCoreValidation featuresCoreValidator,
                                 SchemaGeneratorOpenApi schemaGeneratorFeature,
                                 SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection,
                                 GeoJsonWriterRegistry geoJsonWriterRegistry) {
        this.providers = providers;
        this.entityRegistry = entityRegistry;
        this.featuresCoreValidator = featuresCoreValidator;
        this.schemaGeneratorFeature = schemaGeneratorFeature;
        this.schemaGeneratorFeatureCollection = schemaGeneratorFeatureCollection;
        this.geoJsonWriterRegistry = geoJsonWriterRegistry;
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of(CONFORMANCE_CLASS_FEATURES, CONFORMANCE_CLASS_RECORDS);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeoJsonConfiguration.class;
    }

    @Override
    public boolean canSupportTransactions() {
        return true;
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {

        // no additional operational checks for now, only validation; we can stop, if no validation is requested
        if (apiValidation== MODE.NONE)
            return ValidationResult.of();

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .mode(apiValidation);

        Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(apiData);

        // get GeoJSON configurations to process
        Map<String, GeoJsonConfiguration> geoJsonConfigurationMap = apiData.getCollections()
                                                                    .entrySet()
                                                                    .stream()
                                                                    .map(entry -> {
                                                                        final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                                                                        final GeoJsonConfiguration config = collectionData.getExtension(GeoJsonConfiguration.class).orElse(null);
                                                                        if (Objects.isNull(config))
                                                                            return null;
                                                                        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
                                                                    })
                                                                    .filter(Objects::nonNull)
                                                                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, GeoJsonConfiguration> entry : geoJsonConfigurationMap.entrySet()) {
            String collectionId = entry.getKey();
            GeoJsonConfiguration config = entry.getValue();

            if (config.getNestedObjectStrategy() == GeoJsonConfiguration.NESTED_OBJECTS.FLATTEN && config.getMultiplicityStrategy() != GeoJsonConfiguration.MULTIPLICITY.SUFFIX) {
                builder.addStrictErrors(MessageFormat.format("The GeoJSON Nested Object Strategy ''FLATTEN'' in collection ''{0}'' cannot be combined with the Multiplicity Strategy ''{1}''.", collectionId, config.getMultiplicityStrategy()));
            } else if (config.getNestedObjectStrategy() == GeoJsonConfiguration.NESTED_OBJECTS.NEST && config.getMultiplicityStrategy() != GeoJsonConfiguration.MULTIPLICITY.ARRAY) {
                builder.addStrictErrors(MessageFormat.format("The GeoJSON Nested Object Strategy ''FLATTEN'' in collection ''{0}'' cannot be combined with the Multiplicity Strategy ''{1}''.", collectionId, config.getMultiplicityStrategy()));
            }

            List<String> separators = ImmutableList.of(".","_",":","/");
            if (!separators.contains(config.getSeparator())) {
                builder.addStrictErrors(MessageFormat.format("The separator ''{0}'' in collection ''{1}'' is invalid, it must be one of {2}.", config.getSeparator(), collectionId, separators));
            }
        }

        Map<String, Collection<String>> keyMap = geoJsonConfigurationMap.entrySet()
                                                          .stream()
                                                          .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                                                    .getTransformations()
                                                                                                                                    .keySet()))
                                                          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, Collection<String>> stringCollectionEntry : featuresCoreValidator.getInvalidPropertyKeys(keyMap, featureSchemas).entrySet()) {
            for (String property : stringCollectionEntry.getValue()) {
                builder.addStrictErrors(MessageFormat.format("A transformation for property ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.", property, stringCollectionEntry.getKey()));
            }
        }

        Set<String> codelists = entityRegistry.getEntitiesForType(Codelist.class)
                                              .stream()
                                              .map(Codelist::getId)
                                              .collect(Collectors.toUnmodifiableSet());
        for (Map.Entry<String, GeoJsonConfiguration> entry : geoJsonConfigurationMap.entrySet()) {
            String collectionId = entry.getKey();
            for (Map.Entry<String, List<PropertyTransformation>> entry2 : entry.getValue().getTransformations().entrySet()) {
                String property = entry2.getKey();
                for (PropertyTransformation transformation: entry2.getValue()) {
                    builder = transformation.validate(builder, collectionId, property, codelists);
                }
            }
        }

        return builder.build();
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        String schemaRef = "#/components/schemas/anyObject";
        Schema schema = new ObjectSchema();
        String collectionId = path.split("/", 4)[2];
        if (collectionId.equals("{collectionId}") && apiData.getExtension(CollectionsConfiguration.class)
                                                            .filter(config -> config.getCollectionDefinitionsAreIdentical()
                                                                                    .orElse(false))
                                                            .isPresent()) {
            collectionId = apiData.getCollections()
                                  .keySet()
                                  .iterator()
                                  .next();
        }
        if (!collectionId.equals("{collectionId}")) {
            if (path.matches("/collections/[^//]+/items/?")) {
                schemaRef = schemaGeneratorFeatureCollection.getSchemaReference(collectionId);
                schema = schemaGeneratorFeatureCollection.getSchema(apiData, collectionId);
            } else if (path.matches("/collections/[^//]+/items/[^//]+/?")) {
                schemaRef = schemaGeneratorFeature.getSchemaReference(collectionId);
                schema = schemaGeneratorFeature.getSchema(apiData, collectionId);
            }
        }
        // TODO example
        return new ImmutableApiMediaTypeContent.Builder()
                .schema(schema)
                .schemaRef(schemaRef)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        String schemaRef = "#/components/schemas/anyObject";
        Schema schema = new ObjectSchema();
        String collectionId = path.split("/", 4)[2];
        if ((path.matches("/collections/[^//]+/items/[^//]+/?") && method== HttpMethods.PUT) ||
            (path.matches("/collections/[^//]+/items/?") && method== HttpMethods.POST)) {

            if (collectionId.equals("{collectionId}") && apiData.getExtension(CollectionsConfiguration.class)
                                                                .filter(config -> config.getCollectionDefinitionsAreIdentical()
                                                                                        .orElse(false))
                                                                .isPresent()) {
                collectionId = apiData.getCollections()
                                      .keySet()
                                      .iterator()
                                      .next();
            }
            if (!collectionId.equals("{collectionId}")) {
            //TODO: implement getMutablesSchema with SchemaDeriverOpenApiMutables
                schema = schemaGeneratorFeature.getSchema(apiData, collectionId);
                schemaRef = schemaGeneratorFeature.getSchemaReference(collectionId);
            }
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schema)
                    .schemaRef(schemaRef)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();
        }

        return null;
    }

    @Override
    public ApiMediaType getCollectionMediaType() {
        return COLLECTION_MEDIA_TYPE;
    }

    @Override
    public boolean canEncodeFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
        FeatureTransformationContext transformationContext,
        Optional<Locale> language) {

        // TODO support language
        ImmutableSortedSet<GeoJsonWriter> geoJsonWriters = geoJsonWriterRegistry.getGeoJsonWriters()
            .stream()
            .map(GeoJsonWriter::create)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.comparingInt(GeoJsonWriter::getSortPriority)));

        ImmutableFeatureTransformationContextGeoJson transformationContextGeoJson = ImmutableFeatureTransformationContextGeoJson
            .builder()
            .from(transformationContext)
            .geoJsonConfig(transformationContext.getApiData().getCollections()
                .get(transformationContext.getCollectionId())
                .getExtension(GeoJsonConfiguration.class).get())
            .prettify(Optional.ofNullable(transformationContext.getOgcApiRequest()
                .getParameters()
                .get("pretty"))
                .filter(value -> Objects.equals(value, "true"))
                .isPresent() ||
                (transformationContext.getApiData().getCollections()
                    .get(transformationContext.getCollectionId())
                    .getExtension(GeoJsonConfiguration.class).get().getUseFormattedJsonOutput()))
            .debugJson(Optional.ofNullable(transformationContext.getOgcApiRequest()
                .getParameters()
                .get("debug"))
                .filter(value -> Objects.equals(value, "true"))
                .isPresent())
            .build();

        return Optional.of(new FeatureEncoderGeoJson(transformationContextGeoJson, geoJsonWriters));
    }

}
