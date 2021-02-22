/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
public interface FeatureWriterGeoJson<T extends FeatureTransformationContext> {

    int getSortPriority();

    default void onEvent(T transformationContext, Consumer<T> next) throws IOException {
        switch (transformationContext.getState().getEvent()) {
            case START:
                onStart(transformationContext, next);
                break;
            case END:
                onEnd(transformationContext, next);
                break;
            case FEATURE_START:
                onFeatureStart(transformationContext, next);
                break;
            case FEATURE_END:
                // first close the properties object and then the feature object
                onPropertiesEnd(transformationContext, next);
                onFeatureEnd(transformationContext, next);
                break;
            case PROPERTY:
                onProperty(transformationContext, next);
                break;
            case COORDINATES:
                onCoordinates(transformationContext, next);
                break;
            case GEOMETRY_END:
                onGeometryEnd(transformationContext, next);
                break;
        }
    }

    default void onStart(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}

    default void onEnd(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}

    default void onFeatureStart(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}

    default void onPropertiesEnd(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}

    default void onFeatureEnd(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}

    default void onProperty(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}

    default void onCoordinates(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}

    default void onGeometryEnd(T transformationContext, Consumer<T> next) throws IOException {next.accept(transformationContext);}
}
