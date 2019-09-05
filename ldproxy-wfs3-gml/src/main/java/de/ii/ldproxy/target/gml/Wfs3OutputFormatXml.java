/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.api.Wfs3CollectionFormatExtension;
import org.apache.felix.ipojo.annotations.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3OutputFormatXml implements Wfs3CollectionFormatExtension, CommonFormatExtension {

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
        return "^\\/?(?:conformance|collections(/\\w+)?)?$";
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
    public Response getLandingPageResponse(Dataset dataset, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(new LandingPageXml(dataset.getLinks()));
    }

    @Override
    public Response getConformanceResponse(List<ConformanceClass> ocgApiConformanceClasses,
                                           OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(new Wfs3ConformanceClassesXml(new ConformanceClasses(ocgApiConformanceClasses.stream()
                                                                                                   .map(ConformanceClass::getConformanceClass)
                                                                                                   .collect(Collectors.toList()))));
    }

    @Override
    public Response getCollectionsResponse(Dataset dataset, OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(new Wfs3CollectionsXml(dataset));
    }

    @Override
    public Response getCollectionResponse(OgcApiCollection ogcApiCollection, String collectionName,
                                          OgcApiDataset api, OgcApiRequestContext requestContext) {
        return response(new Wfs3CollectionXml(ogcApiCollection));
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
