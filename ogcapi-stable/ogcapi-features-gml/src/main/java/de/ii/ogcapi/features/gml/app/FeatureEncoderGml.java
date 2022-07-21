/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import com.google.common.collect.ImmutableCollection;
import de.ii.ogcapi.features.core.domain.FeatureTransformationContext.Event;
import de.ii.ogcapi.features.gml.domain.EncodingAwareContextGml;
import de.ii.ogcapi.features.gml.domain.FeatureTransformationContextGml;
import de.ii.ogcapi.features.gml.domain.GmlWriter;
import de.ii.ogcapi.features.gml.domain.ModifiableEncodingAwareContextGml;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoderDefault;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import java.util.Iterator;
import java.util.function.Consumer;

@SuppressWarnings({
  "PMD.TooManyMethods"
}) // this class needs that many methods, a refactoring makes no sense
public class FeatureEncoderGml extends FeatureTokenEncoderDefault<EncodingAwareContextGml> {
  private final ImmutableCollection<GmlWriter> featureWriters;
  private final FeatureTransformationContextGml transformationContext;

  public FeatureEncoderGml(
      FeatureTransformationContextGml transformationContext,
      ImmutableCollection<GmlWriter> featureWriters) {
    super();
    this.transformationContext = transformationContext;
    this.featureWriters = featureWriters;
  }

  private Consumer<EncodingAwareContextGml> executePipeline(
      final Iterator<GmlWriter> featureWriterIterator) {
    return consumerMayThrow(
        nextTransformationContext -> {
          if (featureWriterIterator.hasNext()) {
            featureWriterIterator
                .next()
                .onEvent(nextTransformationContext, this.executePipeline(featureWriterIterator));
          }
        });
  }

  @Override
  public void onStart(EncodingAwareContextGml context) {
    if (transformationContext.getOutputStream() instanceof OutputStreamToByteConsumer) {
      ((OutputStreamToByteConsumer) transformationContext.getOutputStream())
          .setByteConsumer(this::push);
    }

    transformationContext.getState().setNumberReturned(context.metadata().getNumberReturned());
    transformationContext.getState().setNumberMatched(context.metadata().getNumberMatched());

    transformationContext.getState().setEvent(Event.START);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onEnd(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.END);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onFeatureStart(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.FEATURE_START);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onFeatureEnd(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.FEATURE_END);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onObjectStart(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.OBJECT_START);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onObjectEnd(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.OBJECT_END);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onArrayStart(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.ARRAY_START);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onArrayEnd(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.ARRAY_END);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public void onValue(EncodingAwareContextGml context) {
    transformationContext.getState().setEvent(Event.PROPERTY);
    executePipeline(featureWriters.iterator()).accept(context);
  }

  @Override
  public Class<? extends EncodingAwareContextGml> getContextInterface() {
    return EncodingAwareContextGml.class;
  }

  @Override
  public EncodingAwareContextGml createContext() {
    return ModifiableEncodingAwareContextGml.create().setEncoding(transformationContext);
  }
}
