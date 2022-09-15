/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.collect.ImmutableSortedSet;
import de.ii.ogcapi.collections.domain.CollectionsConfiguration;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.geojson.domain.FeatureEncoderGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
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
import javax.ws.rs.core.MediaType;

@AutoMultiBind
public interface FeaturesFormatJsonFgBase extends FeatureFormatExtension {

  ApiMediaType COLLECTION_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("JSON")
          .parameter("json")
          .build();

  @Override
  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return JsonFgConfiguration.class;
  }

  @Override
  default ValidationResult onStartup(OgcApi api, MODE apiValidation) {

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == MODE.NONE) return ValidationResult.of();

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    // TODO anything to validate?

    return builder.build();
  }

  Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId);

  Schema<?> getSchemaCollection(OgcApiDataV2 apiData, String collectionId);

  String getSchemaReference(String collectionId);

  String getSchemaReferenceCollection(String collectionId);

  @Override
  default ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
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
        schemaRef = getSchemaReferenceCollection(collectionId);
        schema = getSchemaCollection(apiData, collectionId);
      } else if (path.matches("/collections/[^//]+/items/[^//]+/?")) {
        schemaRef = getSchemaReference(collectionId);
        schema = getSchema(apiData, collectionId);
      }
    }
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(schemaRef)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  default ApiMediaTypeContent getRequestContent(
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
        schema = getSchema(apiData, collectionId);
        schemaRef = getSchemaReference(collectionId);
      }
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schema)
          .schemaRef(schemaRef)
          .ogcApiMediaType(getMediaType())
          .build();
    }

    return null;
  }

  @Override
  default ApiMediaType getCollectionMediaType() {
    return COLLECTION_MEDIA_TYPE;
  }

  @Override
  default boolean canEncodeFeatures() {
    return true;
  }

  List<GeoJsonWriter> getWriters();

  @Override
  default Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {

    // TODO support language
    ImmutableSortedSet<GeoJsonWriter> writers =
        getWriters().stream()
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
            .mediaType(getMediaType())
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
                    .isPresent())
            .suppressPrimaryGeometry(!includePrimaryGeometry(transformationContext))
            .forceDefaultCrs(true);

    return Optional.of(
        new FeatureEncoderGeoJson(transformationContextJsonFgBuilder.build(), writers));
  }

  boolean includePrimaryGeometry(FeatureTransformationContext transformationContext);

  static boolean hasSimpleFeatureGeometryType(FeatureSchema schema) {
    return schema.getProperties().stream()
        .noneMatch(
            p ->
                p.isPrimaryGeometry()
                    && SimpleFeatureGeometry.MULTI_POLYGON.equals(
                        p.getGeometryType().orElse(SimpleFeatureGeometry.NONE))
                    && p.getConstraints().map(SchemaConstraints::isComposite).orElse(false)
                    && p.getConstraints().map(SchemaConstraints::isClosed).orElse(false));
  }

  static Optional<Integer> getGeometryDimension(FeatureSchema schema) {
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
