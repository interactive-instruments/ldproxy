/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorCollectionOpenApi;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ogcapi.features.geojson.domain.GeoJsonWriterRegistry;
import de.ii.ogcapi.features.jsonfg.domain.FeaturesFormatJsonFgBase;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class FeaturesFormatJsonFg implements FeaturesFormatJsonFgBase {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "vnd.ogc.fg+json"))
          .label("JSON-FG")
          .parameter("jsonfg")
          .fileExtension(".fg.json")
          .build();

  private final SchemaGeneratorOpenApi schemaGeneratorFeature;
  private final SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection;
  private final GeoJsonWriterRegistry geoJsonWriterRegistry;

  @Inject
  public FeaturesFormatJsonFg(
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      SchemaGeneratorCollectionOpenApi schemaGeneratorFeatureCollection,
      GeoJsonWriterRegistry geoJsonWriterRegistry) {
    this.schemaGeneratorFeature = schemaGeneratorFeature;
    this.schemaGeneratorFeatureCollection = schemaGeneratorFeatureCollection;
    this.geoJsonWriterRegistry = geoJsonWriterRegistry;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public List<GeoJsonWriter> getWriters() {
    return geoJsonWriterRegistry.getWriters();
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    return schemaGeneratorFeature.getSchema(apiData, collectionId);
  }

  @Override
  public Schema<?> getSchemaCollection(OgcApiDataV2 apiData, String collectionId) {
    return schemaGeneratorFeatureCollection.getSchema(apiData, collectionId);
  }

  @Override
  public String getSchemaReference(String collectionId) {
    return schemaGeneratorFeature.getSchemaReference(collectionId);
  }

  @Override
  public String getSchemaReferenceCollection(String collectionId) {
    return schemaGeneratorFeatureCollection.getSchemaReference(collectionId);
  }

  @Override
  public boolean includePrimaryGeometry(FeatureTransformationContext transformationContext) {
    return transformationContext.getTargetCrs().equals(transformationContext.getDefaultCrs())
        && transformationContext
            .getFeatureSchema()
            .map(FeaturesFormatJsonFgBase::hasSimpleFeatureGeometryType)
            .orElse(true);
  }
}
