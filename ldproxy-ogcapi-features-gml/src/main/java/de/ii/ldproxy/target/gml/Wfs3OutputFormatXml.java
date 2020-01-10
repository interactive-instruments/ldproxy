/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.*;
import org.apache.felix.ipojo.annotations.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatXml implements CollectionsFormatExtension, CommonFormatExtension {

    private static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "xml"))
            .label("XML")
            .parameter("xml")
            .build();

    @Requires
    private GmlConfig gmlConfig;

    @ServiceController(value = false)
    private boolean enable;

    @Validate
    private void onStart() {
        this.enable = gmlConfig.isEnabled();
    }

    @Override
    public String getPathPattern() {
        return "^\\/?(?:conformance|collections(/[\\w\\-]+)?)?$";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, GmlConfiguration.class);
    }

    @Override
    public Response getLandingPageResponse(LandingPage apiLandingPage, OgcApiDataset api, OgcApiRequestContext requestContext) {
        String title = requestContext.getApi().getData().getLabel();
        String description = requestContext.getApi().getData().getDescription().orElse(null);
        return response(new LandingPageXml(apiLandingPage.getLinks(), title, description));
    }

    @Override
    public Response getConformanceResponse(ConformanceDeclaration conformanceDeclaration,
                                           OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(new Wfs3ConformanceClassesXml(conformanceDeclaration));
    }

    @Override
    public Response getCollectionsResponse(Collections collections, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(new Wfs3CollectionsXml(collections));
    }

    @Override
    public Response getCollectionResponse(OgcApiCollection ogcApiCollection,
                                          OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(new Wfs3CollectionsXml(new ImmutableCollections.Builder()
                .addCollections(ogcApiCollection)
                .build()));
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
