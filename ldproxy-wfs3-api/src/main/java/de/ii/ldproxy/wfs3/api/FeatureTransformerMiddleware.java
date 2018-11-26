/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
