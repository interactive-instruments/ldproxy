/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.google.common.collect.ImmutableSortedSet;
import de.ii.ldproxy.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.FeatureTransformationContextJsonFgExtension;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.ImmutableFeatureTransformationContextJsonFgExtension;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.WhereConfiguration;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
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
        if (!collectionId.equals("{collectionId}")) {
            Optional<GeoJsonConfiguration> geoJsonConfiguration = apiData.getCollections()
                                                                         .get(collectionId)
                                                                         .getExtension(GeoJsonConfiguration.class);
            boolean flatten = geoJsonConfiguration.filter(config -> config.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN && config.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                                  .isPresent();
            SchemaGeneratorFeature.SCHEMA_TYPE type = flatten ? SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT : SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES;
            if (path.matches("/collections/[^//]+/items/?")) {
                schemaRef = schemaGeneratorFeatureCollection.getSchemaReferenceOpenApi(collectionId, type);
                schema = schemaGeneratorFeatureCollection.getSchemaOpenApi(apiData, collectionId, type);
            } else if (path.matches("/collections/[^//]+/items/[^//]+/?")) {
                schemaRef = schemaGeneratorFeature.getSchemaReferenceOpenApi(collectionId, type);
                schema = schemaGeneratorFeature.getSchemaOpenApi(apiData, collectionId, type);
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
            // TODO change to MUTABLES
            Optional<GeoJsonConfiguration> geoJsonConfiguration = apiData.getCollections()
                                                                         .get(collectionId)
                                                                         .getExtension(GeoJsonConfiguration.class);
            boolean flatten = geoJsonConfiguration.filter(config -> config.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN && config.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                                  .isPresent();
            SchemaGeneratorFeature.SCHEMA_TYPE type = flatten ? SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT : SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES;
                schema = schemaGeneratorFeature.getSchemaOpenApi(apiData, collectionId, type);
                schemaRef = schemaGeneratorFeature.getSchemaReferenceOpenApi(collectionId, type);
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
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext,
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

        Optional<CrsTransformer> crsTransformerWhere = transformationContext.getCrsTransformer();
        FeatureTransformationContextJsonFgExtension extension = ImmutableFeatureTransformationContextJsonFgExtension.builder()
                                                                                                                    .crsTransformerWhere(crsTransformerWhere)
                                                                                                                    .geometryPrecisionWhere(transformationContext.getGeometryPrecision())
                                                                                                                    .maxAllowableOffsetWhere(transformationContext.getMaxAllowableOffset())
                                                                                                                    .shouldSwapCoordinatesWhere(transformationContext.shouldSwapCoordinates())
                                                                                                                    .suppressWhere(transformationContext.getTargetCrs().equals(transformationContext.getDefaultCrs()))
                                                                                                                    .crs(transformationContext.getTargetCrs())
                                                                                                                    .build();
        transformationContextJsonFgBuilder.putExtensions("jsonfg", extension);

        Optional<CrsTransformer> crsTransformerGeometry = Optional.empty();
        boolean swapCoordinatesGeometry = false;
        if (transformationContext.getSourceCrs().isPresent()) {
            EpsgCrs sourceCrsGeometry = transformationContext.getSourceCrs().get();
            EpsgCrs targetCrsGeometry = transformationContext.getDefaultCrs();
            crsTransformerGeometry = crsTransformerFactory.getTransformer(sourceCrsGeometry, targetCrsGeometry);
            swapCoordinatesGeometry = crsTransformerGeometry.isPresent() && crsTransformerGeometry.get()
                                                                                                  .needsCoordinateSwap();
        }

        int precisionGeometry = transformationContext.getApiData()
                                                     .getExtension(FeaturesCoreConfiguration.class, transformationContext.getCollectionId())
                                                     .map(cfg -> Objects.requireNonNullElse(cfg.getCoordinatePrecision()
                                                                                               .get("degree"), 7))
                                                     .orElse(7);

        Double maxAllowableOffsetGeometry = transformationContext.getApiData()
                                                                 .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
                                                                 .map(cfg -> cfg.getWhere().getMaxAllowableOffsetGeometry())
                                                                 .orElse(null);

        // the GeoJSON "geometry" member is included, if and only if
        // - the value of "where" and "geometry" are identical in which case "geometry" is used or
        // - the collection is configured to always include the GeoJSON "geometry" member
        boolean includeGeometry = transformationContext.getTargetCrs().equals(transformationContext.getDefaultCrs());
        if (!includeGeometry) {
            Optional<WhereConfiguration> whereConfig = transformationContext.getApiData()
                                                                            .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
                                                                            .map(JsonFgConfiguration::getWhere);
            if (whereConfig.isPresent()) {
                includeGeometry = Objects.requireNonNullElse(whereConfig.get().getAlwaysIncludeGeoJsonGeometry(), false);
            }
        }


        if (Objects.isNull(maxAllowableOffsetGeometry)) {
            maxAllowableOffsetGeometry = transformationContext.getMaxAllowableOffset();
            Unit<?> unit = crsTransformerFactory.getCrsUnit(transformationContext.getTargetCrs());
            if (unit.equals(SI.METRE)) {
                // metre per degree at the equator
                maxAllowableOffsetGeometry /= 111319.4908;
            }
        }

        transformationContextJsonFgBuilder.crsTransformer(crsTransformerGeometry)
                                          .geometryPrecision(precisionGeometry)
                                          .maxAllowableOffset(maxAllowableOffsetGeometry)
                                          .shouldSwapCoordinates(swapCoordinatesGeometry)
                                          .suppressGeometry(!includeGeometry);

        return Optional.of(new FeatureTransformerGeoJson(transformationContextJsonFgBuilder.build(), geoJsonWriters));
    }

}
