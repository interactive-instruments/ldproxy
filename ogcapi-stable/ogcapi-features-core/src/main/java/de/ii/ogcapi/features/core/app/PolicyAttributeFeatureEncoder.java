/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import static de.ii.ogcapi.features.core.domain.PolicyAttributeKeys.KEY_GEOMETRY;
import static de.ii.ogcapi.features.core.domain.PolicyAttributeKeys.KEY_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.domain.transform.FeatureEncoderSfFlat;
import de.ii.xtraplatform.features.domain.transform.FeatureSfFlat;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyAttributeFeatureEncoder extends FeatureEncoderSfFlat {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyAttributeFeatureEncoder.class);

  private final ObjectMapper objectMapper;
  private final GeometryFactory geometryFactory;
  private final WKTWriter wktWriter;

  public PolicyAttributeFeatureEncoder(FeatureTransformationContext encodingContext) {
    super(encodingContext);
    this.objectMapper = new ObjectMapper();
    this.geometryFactory = new GeometryFactory();
    this.wktWriter = new WKTWriter();
  }

  @Override
  public void onStart(ModifiableContext context) {
    push("[".getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void onFeature(FeatureSfFlat feature) {
    try {
      Map<String, Object> props = new LinkedHashMap<>();
      feature.getId().ifPresent(id -> props.put(KEY_ID, id.getValue()));
      feature
          .getJtsGeometry(geometryFactory)
          .ifPresent(geometry -> props.put(KEY_GEOMETRY, wktWriter.write(geometry)));
      props.putAll(feature.getPropertiesAsMap());

      push(objectMapper.writeValueAsBytes(props));
      push(",".getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    push("{}".getBytes(StandardCharsets.UTF_8));
    push("]".getBytes(StandardCharsets.UTF_8));
  }
}
