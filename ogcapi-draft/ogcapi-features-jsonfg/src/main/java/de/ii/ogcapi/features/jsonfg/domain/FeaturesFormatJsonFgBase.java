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
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.geojson.domain.FeatureEncoderGeoJson;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.entities.domain.ImmutableValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult.MODE;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@AutoMultiBind
public abstract class FeaturesFormatJsonFgBase extends FeatureFormatExtension {

  protected FeaturesFormatJsonFgBase(
      ExtensionRegistry extensionRegistry, FeaturesCoreProviders providers) {
    super(extensionRegistry, providers);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return JsonFgConfiguration.class;
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

  public abstract Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId);

  public abstract Schema<?> getSchemaCollection(OgcApiDataV2 apiData, String collectionId);

  public abstract String getSchemaReference(String collectionId);

  public abstract String getSchemaReferenceCollection(String collectionId);

  @Override
  public ApiMediaTypeContent getContent() {
    Schema<?> schema = new ObjectSchema();
    String schemaRef = "https://geojson.org/schema/FeatureCollection.json";
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(OBJECT_SCHEMA)
        .schemaRef(OBJECT_SCHEMA_REF)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public ApiMediaTypeContent getFeatureContent(
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
  public ApiMediaType getCollectionMediaType() {
    return ApiMediaType.JSON_MEDIA_TYPE;
  }

  @Override
  public boolean canEncodeFeatures() {
    return true;
  }

  public abstract List<GeoJsonWriter> getWriters();

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
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
            .prettify(transformationContext.getPrettify())
            .suppressPrimaryGeometry(!includePrimaryGeometry(transformationContext))
            .forceDefaultCrs(true);

    return Optional.of(
        new FeatureEncoderGeoJson(transformationContextJsonFgBuilder.build(), writers));
  }

  public abstract boolean includePrimaryGeometry(
      FeatureTransformationContext transformationContext);

  public static Optional<Integer> getGeometryDimension(FeatureSchema schema) {
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
  public boolean supportsHitsOnly() {
    return true;
  }

  @Override
  public Optional<Long> getNumberMatched(Object content) {
    return getMetadata(content, "numberMatched");
  }

  @Override
  public Optional<Long> getNumberReturned(Object content) {
    return getMetadata(content, "numberReturned");
  }

  @Override
  public boolean isComplex() {
    return true;
  }

  @Override
  public boolean supportsRootConcat() {
    return true;
  }

  private Optional<Long> getMetadata(Object content, String key) {
    if (content instanceof byte[]) {
      JsonNode jsonNode;
      ObjectMapper mapper = new ObjectMapper();
      try {
        jsonNode = mapper.readTree((byte[]) content);
        if (Objects.nonNull(jsonNode) && jsonNode.isObject()) {
          jsonNode = jsonNode.get(key);
          if (Objects.nonNull(jsonNode) && jsonNode.isNumber()) {
            return Optional.of(jsonNode.longValue());
          }
        }
      } catch (IOException e) {
        // ignore
      }
    }

    return Optional.empty();
  }
}
