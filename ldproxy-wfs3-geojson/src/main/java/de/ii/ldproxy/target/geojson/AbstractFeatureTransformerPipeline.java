/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * @author zahnen
 */
public abstract class AbstractFeatureTransformerPipeline implements FeatureTransformer {

    private final List<FeatureTransformer> transformers;

    protected AbstractFeatureTransformerPipeline() {
        this.transformers = new ArrayList<>();
    }

    @Override
    public void onStart(OptionalLong optionalLong, OptionalLong optionalLong1) throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onStart(optionalLong, optionalLong1);
        }
    }

    @Override
    public void onEnd() throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onEnd();
        }
    }

    @Override
    public void onFeatureStart(TargetMapping targetMapping) throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onFeatureStart(targetMapping);
        }
    }

    @Override
    public void onFeatureEnd() throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onFeatureEnd();
        }
    }

    @Override
    public void onPropertyStart(TargetMapping targetMapping, List<Integer> list) throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onPropertyStart(targetMapping, list);
        }
    }

    @Override
    public void onPropertyText(String s) throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onPropertyText(s);
        }
    }

    @Override
    public void onPropertyEnd() throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onPropertyEnd();
        }
    }

    @Override
    public void onGeometryStart(TargetMapping targetMapping, SimpleFeatureGeometry simpleFeatureGeometry, Integer integer) throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onGeometryStart(targetMapping, simpleFeatureGeometry, integer);
        }
    }

    @Override
    public void onGeometryNestedStart() throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onGeometryNestedStart();
        }
    }

    @Override
    public void onGeometryCoordinates(String s) throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onGeometryCoordinates(s);
        }
    }

    @Override
    public void onGeometryNestedEnd() throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onGeometryNestedEnd();
        }
    }

    @Override
    public void onGeometryEnd() throws Exception {
        for (FeatureTransformer transformer : transformers) {
            transformer.onGeometryEnd();
        }
    }
}
