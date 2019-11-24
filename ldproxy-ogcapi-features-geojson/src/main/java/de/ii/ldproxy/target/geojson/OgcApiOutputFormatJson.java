/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author zahnen
 */
@Component
@Provides(specifications = {OgcApiOutputFormatJson.class, CollectionsFormatExtension.class, CommonFormatExtension.class, FormatExtension.class, OgcApiExtension.class})
@Instantiate
public class OgcApiOutputFormatJson implements CollectionsFormatExtension, CommonFormatExtension {

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

    @Override
    public String getPathPattern() {
        return "^\\/?(?:conformance|collections(/[\\w\\-]+)?)?$";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, GeoJsonConfiguration.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Response getLandingPageResponse(LandingPage apiLandingPage, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(apiLandingPage);
    }

    @Override
    public Response getConformanceResponse(ConformanceDeclaration conformanceDeclaration,
                                           OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(conformanceDeclaration);
    }

    @Override
    public Response getCollectionsResponse(Collections collections, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(collections);
    }

    @Override
    public Response getCollectionResponse(OgcApiCollection ogcApiCollection,
                                          OgcApiDataset api,
                                          OgcApiRequestContext requestContext) {
        return response(ogcApiCollection);
    }

    private Response response(Object entity) {
        return response(entity, null);
    }

    private Response response(Object entity, String type) {
        Response.ResponseBuilder response = Response.ok()
                                                    .entity(entity);
        if (type != null) {
            response.type(type);
        }

        return response.build();
    }
}
