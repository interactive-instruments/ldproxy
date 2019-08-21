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

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface OgcApiDataset extends FeatureTransformerService, Service {
    OgcApiDatasetData getData();

    Response getConformanceResponse(OgcApiRequestContext wfs3Request);

    Response getDatasetResponse(OgcApiRequestContext wfs3Request, boolean isCollections);

    Optional<CrsTransformer> getCrsTransformer(EpsgCrs crs);

    CrsTransformer getCrsReverseTransformer(EpsgCrs crs);

    /*
                Response getItemsResponse(Wfs3RequestContext wfs3Request, String collectionName, FeatureQuery query);

                Response getItemsResponse(Wfs3RequestContext wfs3Request, String collectionName, FeatureQuery query,
                                          boolean isCollection, Wfs3OutputFormatExtension outputFormat);
            */
    BoundingBox transformBoundingBox(BoundingBox bbox) throws CrsTransformationException;

    List<List<Double>> transformCoordinates(List<List<Double>> coordinates,
                                            EpsgCrs crs) throws CrsTransformationException;
}
