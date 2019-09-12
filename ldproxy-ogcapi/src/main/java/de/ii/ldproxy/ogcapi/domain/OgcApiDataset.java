/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerService;
import de.ii.xtraplatform.service.api.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface OgcApiDataset extends FeatureTransformerService, Service {

    // TODO: move the following 3 methods to OgcApiApi, split generic parts of OgcApiDatasetData to OgcApiApiData (requires a change in xtraplatform)
    OgcApiDatasetData getData();
    <T extends FormatExtension> Optional<T> getOutputFormat(Class<T> extensionType, OgcApiMediaType mediaType, String path);
    <T extends FormatExtension> List<T> getAllOutputFormats(Class<T> extensionType, OgcApiMediaType mediaType, String path, Optional<T> excludeFormat);

    // TODO: all these go to OgcApiDataset (or OgcApiFeatureDataset?)
    Optional<CrsTransformer> getCrsTransformer(EpsgCrs crs);

    CrsTransformer getCrsReverseTransformer(EpsgCrs crs);

    BoundingBox transformBoundingBox(BoundingBox bbox) throws CrsTransformationException;

    List<List<Double>> transformCoordinates(List<List<Double>> coordinates,
                                            EpsgCrs crs) throws CrsTransformationException;
}
