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
