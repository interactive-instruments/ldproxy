/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;

import javax.ws.rs.core.Response;

/**
 * @author zahnen
 */
public interface Wfs3Service2 {
    Wfs3ServiceData getData();

    Response getItemsResponse(Wfs3RequestContext wfs3Request, String collectionName, FeatureQuery query);

    BoundingBox transformBoundingBox(BoundingBox bbox) throws CrsTransformationException;
}
