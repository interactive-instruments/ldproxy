/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoderDefault;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureEncoderHits extends FeatureTokenEncoderDefault<EncodingAwareContextHits> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderHits.class);

  private final FeatureTransformationContext transformationContext;

  public FeatureEncoderHits(FeatureTransformationContext transformationContext) {
    this.transformationContext = transformationContext;
  }

  @Override
  public void onStart(EncodingAwareContextHits context) {
    if (transformationContext.getOutputStream() instanceof OutputStreamToByteConsumer) {
      ((OutputStreamToByteConsumer) transformationContext.getOutputStream())
          .setByteConsumer(this::push);
    }

    try {
      transformationContext
          .getOutputStream()
          .write(String.valueOf(context.metadata().getNumberReturned().orElse(-1L)).getBytes());
    } catch (IOException e) {
      throw new IllegalStateException("Error writing to output stream.", e);
    }
  }

  @Override
  public void onEnd(EncodingAwareContextHits context) {}

  @Override
  public void onFeatureStart(EncodingAwareContextHits context) {}

  @Override
  public void onFeatureEnd(EncodingAwareContextHits context) {}

  @Override
  public void onObjectStart(EncodingAwareContextHits context) {}

  @Override
  public void onObjectEnd(EncodingAwareContextHits context) {}

  @Override
  public void onArrayStart(EncodingAwareContextHits context) {}

  @Override
  public void onArrayEnd(EncodingAwareContextHits context) {}

  @Override
  public void onValue(EncodingAwareContextHits context) {}

  @Override
  public Class<EncodingAwareContextHits> getContextInterface() {
    return EncodingAwareContextHits.class;
  }

  @Override
  public EncodingAwareContextHits createContext() {
    return ModifiableEncodingAwareContextHits.create().setEncoding(transformationContext);
  }
}
