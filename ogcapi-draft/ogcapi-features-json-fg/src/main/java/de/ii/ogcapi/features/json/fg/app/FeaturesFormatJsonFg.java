/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.json.fg.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableSortedSet;
import de.ii.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.features.geojson.domain.FeatureEncoderGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ogcapi.features.json.fg.domain.JsonFgConfiguration;
import de.ii.ogcapi.features.json.fg.domain.JsonFgGeometryType;
import de.ii.ogcapi.features.json.fg.domain.PlaceConfiguration;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class FeaturesFormatJsonFg implements FeatureFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "vnd.ogc.fg+json"))
          .label("JSON-FG")
          .parameter("jsonfg")
          .build();
  public static final ApiMediaType COLLECTION_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("JSON")
          .parameter("json")
          .build();

  private final SchemaGeneratorOpenApi schemaGeneratorFeature;
  private final SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection;
  private final GeoJsonWriterRegistry geoJsonWriterRegistry;
  private final CrsTransformerFactory crsTransformerFactory;

  @Inject
  public FeaturesFormatJsonFg(
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection,
      GeoJsonWriterRegistry geoJsonWriterRegistry,
      CrsTransformerFactory crsTransformerFactory) {
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
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == MODE.NONE) return ValidationResult.of();

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    // TODO anything to validate?

    return builder.build();
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    String schemaRef = "#/components/schemas/anyObject";
    Schema schema = new ObjectSchema();
    String collectionId = path.split("/", 4)[2];
    if (collectionId.equals("{collectionId}")
        && apiData
            .getExtension(CollectionsConfiguration.class)
            .filter(config -> config.getCollectionDefinitionsAreIdentical().orElse(false))
            .isPresent()) {
      collectionId = apiData.getCollections().keySet().iterator().next();
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
  public ApiMediaTypeContent getRequestContent(
      OgcApiDataV2 apiData, String path, HttpMethods method) {
    String schemaRef = "#/components/schemas/anyObject";
    Schema schema = new ObjectSchema();
    String collectionId = path.split("/", 4)[2];
    if ((path.matches("/collections/[^//]+/items/[^//]+/?") && method == HttpMethods.PUT)
        || (path.matches("/collections/[^//]+/items/?") && method == HttpMethods.POST)) {

      if (collectionId.equals("{collectionId}")
          && apiData
              .getExtension(CollectionsConfiguration.class)
              .filter(config -> config.getCollectionDefinitionsAreIdentical().orElse(false))
              .isPresent()) {
        collectionId = apiData.getCollections().keySet().iterator().next();
      }
      if (!collectionId.equals("{collectionId}")) {
        // TODO: implement getMutablesSchema with SchemaDeriverOpenApiMutables
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
      FeatureTransformationContext transformationContext, Optional<Locale> language) {

    // TODO support language
    ImmutableSortedSet<GeoJsonWriter> geoJsonWriters =
        geoJsonWriterRegistry.getWriters().stream()
            .map(GeoJsonWriter::create)
            .collect(
                ImmutableSortedSet.toImmutableSortedSet(
                    Comparator.comparingInt(GeoJsonWriter::getSortPriority)));

    GeoJsonConfiguration geoJsonConfig =
        transformationContext
            .getApiData()
            .getExtension(GeoJsonConfiguration.class, transformationContext.getCollectionId())
            .get();
    ImmutableFeatureTransformationContextGeoJson.Builder transformationContextJsonFgBuilder =
        ImmutableFeatureTransformationContextGeoJson.builder()
            .from(transformationContext)
            .geoJsonConfig(geoJsonConfig)
            .mediaType(MEDIA_TYPE)
            .prettify(
                Optional.ofNullable(
                            transformationContext.getOgcApiRequest().getParameters().get("pretty"))
                        .filter(value -> Objects.equals(value, "true"))
                        .isPresent()
                    || (Objects.requireNonNullElse(
                        geoJsonConfig.getUseFormattedJsonOutput(), false)))
            .debugJson(
                Optional.ofNullable(
                        transformationContext.getOgcApiRequest().getParameters().get("debug"))
                    .filter(value -> Objects.equals(value, "true"))
                    .isPresent());

    // the GeoJSON "geometry" member is included, if and only if
    // - the value of "place" and "geometry" are identical in which case "geometry" is used or
    // - the collection is configured to always include the GeoJSON "geometry" member
    boolean includePrimaryGeometry =
        (transformationContext.getTargetCrs().equals(transformationContext.getDefaultCrs())
                && transformationContext
                    .getFeatureSchema()
                    .map(FeaturesFormatJsonFg::hasSimpleFeatureGeometryType)
                    .orElse(true))
            || transformationContext
                .getApiData()
                .getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId())
                .map(JsonFgConfiguration::getPlace)
                .map(PlaceConfiguration::getAlwaysIncludeGeoJsonGeometry)
                .orElse(false);
    transformationContextJsonFgBuilder
        .suppressPrimaryGeometry(!includePrimaryGeometry)
        .forceDefaultCrs(true);

    return Optional.of(
        new FeatureEncoderGeoJson(transformationContextJsonFgBuilder.build(), geoJsonWriters));
  }

  public static boolean hasSimpleFeatureGeometryType(FeatureSchema schema) {
    return schema.getProperties().stream()
        .noneMatch(
            p ->
                p.isPrimaryGeometry()
                    && SimpleFeatureGeometry.MULTI_POLYGON.equals(
                        p.getGeometryType().orElse(SimpleFeatureGeometry.NONE))
                    && p.getConstraints().map(SchemaConstraints::isComposite).orElse(false)
                    && p.getConstraints().map(SchemaConstraints::isClosed).orElse(false));
  }

  public static Optional<Integer> getGeometryDimension(FeatureSchema schema) {
    List<Integer> dimensions =
        schema.getProperties().stream()
            .filter(SchemaBase::isPrimaryGeometry)
            .map(
                p ->
                    JsonFgGeometryType.getGeometryDimension(
                        p.getGeometryType().orElse(SimpleFeatureGeometry.NONE),
                        p.getConstraints().map(SchemaConstraints::isComposite).orElse(false),
                        p.getConstraints().map(SchemaConstraints::isComposite).orElse(false)))
            .flatMap(Optional::stream)
            .distinct()
            .collect(Collectors.toUnmodifiableList());
    if (dimensions.size() != 1) {
      return Optional.empty();
    }
    return Optional.of(dimensions.get(0));
  }
}
