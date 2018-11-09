package de.ii.ldproxy.wfs3.api;

import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;

/**
 * @author zahnen
 */
public interface FeatureTransformerMiddleware<T extends FeatureTransformationContext> extends FeatureTransformer {

    void setContext(T transformationContext);
    
    /*O execute(I value);

    default <R> FeatureTransformerMiddleware<I, R> pipe(FeatureTransformerMiddleware<O, R> source) {
        return value -> source.execute(execute(value));
    }

    static <I, O> FeatureTransformerMiddleware<I, O> of(FeatureTransformerMiddleware<I, O> source) {
        return source;
    }*/
}
