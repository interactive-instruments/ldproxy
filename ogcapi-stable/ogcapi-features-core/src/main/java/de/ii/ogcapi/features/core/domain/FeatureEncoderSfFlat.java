/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import java.util.List;

public abstract class FeatureEncoderSfFlat
    extends FeatureObjectEncoder<PropertySfFlat, FeatureSfFlat> {

  protected final String collectionId;
  protected final List<String> properties;
  protected final boolean allProperties;
  protected final long transformerStart;
  protected long processingStart;
  protected Long featureDuration = 0L;
  protected long written;

  protected FeatureEncoderSfFlat(EncodingContextSfFlat encodingContext) {
    super();
    this.collectionId = encodingContext.getCollectionId();
    this.properties =
        encodingContext.getFields().values().stream().findFirst().orElse(ImmutableList.of("*"));
    this.allProperties = properties.contains("*");
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
}
