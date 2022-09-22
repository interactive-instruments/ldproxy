/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.csv.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeatureSfFlat;
import de.ii.ogcapi.features.core.domain.ModifiableFeatureSfFlat;
import de.ii.ogcapi.features.core.domain.ModifiablePropertySfFlat;
import de.ii.ogcapi.features.core.domain.PropertySfFlat;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.SortedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureEncoderCsv extends FeatureObjectEncoder<PropertySfFlat, FeatureSfFlat> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderCsv.class);

  private final String collectionId;
  private final List<String> fields;
  private final boolean allProperties;
  private final FeatureSchema featureSchema;
  private final OutputStream outputStream;

  private final long transformerStart;
  private long processingStart;
  private Long featureDuration = 0L;
  private long written = 0;
  private List<String> headers;

  public FeatureEncoderCsv(FeatureTransformationContextCsv encodingContext) {
    this.collectionId = encodingContext.getCollectionId();
    this.outputStream = encodingContext.getOutputStream();
    this.fields = encodingContext.getFields();
    this.allProperties = fields.contains("*");
    this.featureSchema = encodingContext.getSchema();
    this.transformerStart = System.nanoTime();
  }

  @Override
  public FeatureSfFlat createFeature() {
    return ModifiableFeatureSfFlat.create();
  }

  @Override
  public PropertySfFlat createProperty() {
    return ModifiablePropertySfFlat.create();
  }

  @Override
  public void onStart(ModifiableContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Start generating CSV file for collection {}.", collectionId);
    }
    this.processingStart = System.nanoTime();

    if (outputStream instanceof OutputStreamToByteConsumer) {
      ((OutputStreamToByteConsumer) outputStream).setByteConsumer(this::push);
    }

    headers = getHeaders();
    try {
      writeRecord(headers);
    } catch (Exception e) {
      throw new IllegalStateException("Could not write to CSV output stream: " + e.getMessage(), e);
    }
  }

  private String processValue(String value) {
    String escapedValue = value.replaceAll("\\R", " ");
    if (value.contains(",") || value.contains("\"") || value.contains("'")) {
      value = value.replace("\"", "\"\"");
      escapedValue = "\"" + value + "\"";
    }
    return escapedValue;
  }

  private List<String> getHeaders() {
    ImmutableList.Builder<String> columns = ImmutableList.builder();
    for (FeatureSchema schema : featureSchema.getProperties()) {
      if (schema.getType() != SchemaBase.Type.GEOMETRY
          && (allProperties || fields.contains(schema.getFullPathAsString()))) {
        columns.add(schema.getName());
      }
    }
    return columns.build();
  }

  @Override
  public void onFeature(FeatureSfFlat feature) {
    long startFeature = System.nanoTime();

    writeRecord(feature.getPropertiesAsMap());
    written++;

    featureDuration += System.nanoTime() - startFeature;
  }

  private void writeRecord(SortedMap<String, Object> properties) {
    ImmutableList.Builder<String> values = ImmutableList.builder();
    for (String column : headers) {
      if (properties.containsKey(column)) {
        values.add(processValue(properties.get(column).toString()));
      } else {
        values.add(processValue(""));
      }
    }
    writeRecord(values.build());
  }

  private void writeRecord(List<String> record) {
    push(String.join(",", record).getBytes(StandardCharsets.UTF_8));
    push("\n".getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void onEnd(ModifiableContext context) {
    if (LOGGER.isTraceEnabled()) {
      long transformerDuration = (System.nanoTime() - transformerStart) / 1000000;
      long processingDuration = (System.nanoTime() - processingStart) / 1000000;
      LOGGER.trace(
          String.format(
              "Collection %s, features returned: %d, written: %d, total duration: %dms, processing: %dms, feature processing: %dms.",
              collectionId,
              context.metadata().getNumberReturned().orElse(0),
              written,
              transformerDuration,
              processingDuration,
              featureDuration / 1000000));
    }
  }
}
