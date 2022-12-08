/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class SchemaGeneratorFeatureCollectionOpenApi implements SchemaGeneratorCollectionOpenApi {

  public static final String TYPE = "type";
  public static final String FEATURES = "features";
  public static final String NUMBER_RETURNED_REF =
      "https://schemas.opengis.net/ogcapi/features/part1/1.0/openapi/schemas/numberReturned.yaml";
  public static final String NUMBER_MATCHED_REF =
      "https://schemas.opengis.net/ogcapi/features/part1/1.0/openapi/schemas/numberMatched.yaml";
  public static final String TIME_STAMP_REF =
      "https://schemas.opengis.net/ogcapi/features/part1/1.0/openapi/schemas/timeStamp.yaml";
  public static final String LINK_REF = "#/components/schemas/Link";
  public static final String FEATURE_GEO_JSON =
      "https://schemas.opengis.net/ogcapi/features/part1/1.0/openapi/schemas/featureGeoJSON.yaml";
  public static final String COLLECTION_GEO_JSON_REF =
      "https://schemas.opengis.net/ogcapi/features/part1/1.0/openapi/schemas/featureCollectionGeoJSON.yaml";
  public static final String FEATURE_COLLECTION_GEO_JSON_REF_PREFIX =
      "#/components/schemas/featureCollectionGeoJson_";
  public static final String NUMBER_RETURNED = "numberReturned";
  public static final String NUMBER_MATCHED = "numberMatched";
  public static final String TIME_STAMP = "timeStamp";
  public static final String LINKS = "links";
  public static final String FEATURE_COLLECTION = "FeatureCollection";
  public static final Schema<?> GENERIC =
      new ObjectSchema()
          .required(ImmutableList.of(TYPE, FEATURES))
          .addProperties(TYPE, new StringSchema()._enum(ImmutableList.of(FEATURE_COLLECTION)))
          .addProperties(FEATURES, new ArraySchema().items(new Schema<>().$ref(FEATURE_GEO_JSON)))
          .addProperties(LINKS, new ArraySchema().items(new Schema<>().$ref(LINK_REF)))
          .addProperties(TIME_STAMP, new Schema<>().$ref(TIME_STAMP_REF))
          .addProperties(NUMBER_MATCHED, new Schema<>().$ref(NUMBER_MATCHED_REF))
          .addProperties(NUMBER_RETURNED, new Schema<>().$ref(NUMBER_RETURNED_REF));

  private final SchemaGeneratorOpenApi schemaGeneratorFeature;

  @Inject
  public SchemaGeneratorFeatureCollectionOpenApi(SchemaGeneratorOpenApi schemaGeneratorFeature) {
    this.schemaGeneratorFeature = schemaGeneratorFeature;
  }

  @Override
  public String getSchemaReference() {
    return COLLECTION_GEO_JSON_REF;
  }

  @Override
  public Schema<?> getSchema() {
    return GENERIC;
  }

  @Override
  public String getSchemaReference(String collectionId) {
    return FEATURE_COLLECTION_GEO_JSON_REF_PREFIX + collectionId;
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    return new ObjectSchema()
        .required(ImmutableList.of(TYPE, FEATURES))
        .addProperties(TYPE, new StringSchema()._enum(ImmutableList.of(FEATURE_COLLECTION)))
        .addProperties(
            FEATURES,
            new ArraySchema()
                .items(
                    new Schema<>().$ref(schemaGeneratorFeature.getSchemaReference(collectionId))))
        .addProperties(LINKS, new ArraySchema().items(new Schema<>().$ref(LINK_REF)))
        .addProperties(TIME_STAMP, new Schema<>().$ref(TIME_STAMP_REF))
        .addProperties(NUMBER_MATCHED, new Schema<>().$ref(NUMBER_MATCHED_REF))
        .addProperties(NUMBER_RETURNED, new Schema<>().$ref(NUMBER_RETURNED_REF));
  }

  @Override
  public String getSchemaReferenceForName(String name) {
    return FEATURE_COLLECTION_GEO_JSON_REF_PREFIX + name;
  }

  @Override
  public Schema<?> getSchemaForName(String name) {
    return new ObjectSchema()
        .required(ImmutableList.of(TYPE, FEATURES))
        .addProperties(TYPE, new StringSchema()._enum(ImmutableList.of(FEATURE_COLLECTION)))
        .addProperties(
            FEATURES,
            new ArraySchema()
                .items(new Schema<>().$ref(schemaGeneratorFeature.getSchemaReference(name)))
                .addProperties(LINKS, new ArraySchema().items(new Schema<>().$ref(LINK_REF)))
                .addProperties(TIME_STAMP, new Schema<>().$ref(TIME_STAMP_REF)))
        .addProperties(NUMBER_MATCHED, new Schema<>().$ref(NUMBER_MATCHED_REF))
        .addProperties(NUMBER_RETURNED, new Schema<>().$ref(NUMBER_RETURNED_REF));
  }
}
