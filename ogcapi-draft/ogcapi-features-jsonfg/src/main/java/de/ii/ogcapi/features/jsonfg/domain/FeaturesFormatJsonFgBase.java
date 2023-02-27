/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.collect.ImmutableSortedSet;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.geojson.domain.FeatureEncoderGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@AutoMultiBind
public interface FeaturesFormatJsonFgBase extends FeatureFormatExtension {

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
  default ApiMediaTypeContent getContent() {
    Schema<?> schema = new ObjectSchema();
    String schemaRef = "https://geojson.org/schema/FeatureCollection.json";
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(OBJECT_SCHEMA)
        .schemaRef(OBJECT_SCHEMA_REF)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  default ApiMediaTypeContent getFeatureContent(
      OgcApiDataV2 apiData, Optional<String> collectionId, boolean featureCollection) {
    if (collectionId.isEmpty()) {
      return getContent();
    }

    // TODO support JSON-FG extensions
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(
            featureCollection
                ? getSchemaCollection(apiData, collectionId.get())
                : getSchema(apiData, collectionId.get()))
        .schemaRef(
            featureCollection
                ? getSchemaReferenceCollection(collectionId.get())
                : getSchemaReference(collectionId.get()))
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  default ApiMediaType getCollectionMediaType() {
    return ApiMediaType.JSON_MEDIA_TYPE;
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

  static Optional<Integer> getGeometryDimension(FeatureSchema schema) {
    return schema.getProperties().stream()
        .filter(p -> p.isPrimaryGeometry() || p.isSecondaryGeometry())
        .map(
            p ->
                JsonFgGeometryType.getGeometryDimension(
                    p.getGeometryType().orElse(SimpleFeatureGeometry.NONE),
                    p.getConstraints().map(SchemaConstraints::isComposite).orElse(false),
                    p.getConstraints().map(SchemaConstraints::isClosed).orElse(false)))
        .flatMap(Optional::stream)
        .max(Comparator.naturalOrder());
  }

  @Override
  default boolean supportsHitsOnly() {
    return true;
  }

  @Override
  default Optional<Long> getNumberMatched(Object content) {
    return getMetadata(content, "numberMatched");
  }

  @Override
  default Optional<Long> getNumberReturned(Object content) {
    return getMetadata(content, "numberReturned");
  }

  private Optional<Long> getMetadata(Object content, String key) {
    if (content instanceof byte[]) {
      JsonNode jsonNode;
      ObjectMapper mapper = new ObjectMapper();
      try {
        jsonNode = mapper.readTree((byte[]) content);
      } catch (IOException e) {
        throw new IllegalStateException(
            String.format("Could not parse GeoJSON object: %s", e.getMessage()), e);
      }
      if (jsonNode.isObject()) {
        jsonNode = jsonNode.get(key);
        if (jsonNode.isNumber()) {
          return Optional.of(jsonNode.longValue());
        }
      }
    }

    return Optional.empty();
  }
}
