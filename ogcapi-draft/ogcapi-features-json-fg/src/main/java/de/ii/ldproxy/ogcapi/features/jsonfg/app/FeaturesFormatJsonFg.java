/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.google.common.collect.ImmutableSortedSet;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureEncoderGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.WhereConfiguration;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class FeaturesFormatJsonFg implements FeatureFormatExtension {

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "vnd.ogc.fg+json"))
            .label("JSON-FG")
            .parameter("jsonfg")
            .build();
    public static final ApiMediaType COLLECTION_MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    private final SchemaGeneratorOpenApi schemaGeneratorFeature;
    private final SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection;
    private final GeoJsonWriterRegistry geoJsonWriterRegistry;
    private final CrsTransformerFactory crsTransformerFactory;

    public FeaturesFormatJsonFg(@Requires SchemaGeneratorOpenApi schemaGeneratorFeature,
                                @Requires SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection,
                                @Requires GeoJsonWriterRegistry geoJsonWriterRegistry,
                                @Requires CrsTransformerFactory crsTransformerFactory) {
        this.schemaGeneratorFeature = schemaGeneratorFeature;
        this.schemaGeneratorFeatureCollection = schemaGeneratorFeatureCollection;
        this.geoJsonWriterRegistry = geoJsonWriterRegistry;
        this.crsTransformerFactory = crsTransformerFactory;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return JsonFgConfiguration.class;
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

        // TODO anything to validate?

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
        // TODO support JSON-FG extensions
        if (!collectionId.equals("{collectionId}")) {
            if (path.matches("/collections/[^//]+/items/?")) {
                schemaRef = schemaGeneratorFeatureCollection.getSchemaReference(collectionId);
                schema = schemaGeneratorFeatureCollection.getSchema(apiData, collectionId);
            } else if (path.matches("/collections/[^//]+/items/[^//]+/?")) {
                schemaRef = schemaGeneratorFeature.getSchemaReference(collectionId);
                schema = schemaGeneratorFeature.getSchema(apiData, collectionId);
            }
        }
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
    public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(FeatureTransformationContext transformationContext,
                                                              Optional<Locale> language) {

        // TODO support language
        ImmutableSortedSet<GeoJsonWriter> geoJsonWriters = geoJsonWriterRegistry.getGeoJsonWriters()
                                                                                .stream()
                                                                                .map(GeoJsonWriter::create)
                                                                                .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.comparingInt(GeoJsonWriter::getSortPriority)));

        GeoJsonConfiguration geoJsonConfig = transformationContext.getApiData().getExtension(GeoJsonConfiguration.class, transformationContext.getCollectionId()).get();
        ImmutableFeatureTransformationContextGeoJson.Builder transformationContextJsonFgBuilder =
                ImmutableFeatureTransformationContextGeoJson.builder()
                                                            .from(transformationContext)
                                                            .geoJsonConfig(geoJsonConfig)
                                                            .mediaType(MEDIA_TYPE)
                                                            .prettify(Optional.ofNullable(transformationContext.getOgcApiRequest()
                                                                                                               .getParameters()
                                                                                                               .get("pretty"))
                                                                              .filter(value -> Objects.equals(value, "true"))
                                                                              .isPresent() ||
                                                                              (Objects.requireNonNullElse(geoJsonConfig.getUseFormattedJsonOutput(), false)))
                                                            .debugJson(Optional.ofNullable(transformationContext.getOgcApiRequest()
                                                                                                                .getParameters()
                                                                                                                .get("debug"))
                                                                               .filter(value -> Objects.equals(value, "true"))
                                                                               .isPresent());

        // the GeoJSON "geometry" member is included, if and only if
        // - the value of "where" and "geometry" are identical in which case "geometry" is used or
        // - the collection is configured to always include the GeoJSON "geometry" member
        boolean includePrimaryGeometry = transformationContext.getTargetCrs().equals(transformationContext.getDefaultCrs())
                || transformationContext.getApiData()
                                        .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
                                        .map(JsonFgConfiguration::getWhere)
                                        .map(WhereConfiguration::getAlwaysIncludeGeoJsonGeometry)
                                        .orElse(false);
        transformationContextJsonFgBuilder.suppressPrimaryGeometry(!includePrimaryGeometry)
                                          .forceDefaultCrs(true);

        return Optional.of(new FeatureEncoderGeoJson(transformationContextJsonFgBuilder.build(), geoJsonWriters));
    }

}
