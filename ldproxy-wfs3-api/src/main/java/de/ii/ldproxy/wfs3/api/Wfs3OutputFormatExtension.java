/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OutputFormatExtension;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;
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
public interface Wfs3OutputFormatExtension extends OutputFormatExtension {

    Response getCollectionResponse(Wfs3Collection wfs3Collection, OgcApiDatasetData datasetData,
                                   OgcApiMediaType mediaType,
                                   List<OgcApiMediaType> alternativeMediaTypes, URICustomizer uriCustomizer,
                                   String collectionName);

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

    default Optional<TargetMappingRefiner> getMappingRefiner() {
        return Optional.empty();
    }

}
