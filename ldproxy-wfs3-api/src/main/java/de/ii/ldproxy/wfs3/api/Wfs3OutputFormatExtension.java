/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
//TODO: split into metadata and features; might mean to also split Wfs3MediaType
public interface Wfs3OutputFormatExtension extends Wfs3Extension, Wfs3FormatMetadataExtension {
    Wfs3MediaType getMediaType();

    default boolean canPassThroughFeatures() {
        return false;
    }

    default boolean canTransformFeatures() {
        return false;
    }

    default Optional<GmlConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.empty();
    }

    default Optional<FeatureTransformer> getFeatureTransformer(FeatureTransformationContext transformationContext) {
        return Optional.empty();
    }

    default Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.empty();
    }

    default boolean isEnabledForService(Wfs3ServiceData serviceData){return true;}

}
