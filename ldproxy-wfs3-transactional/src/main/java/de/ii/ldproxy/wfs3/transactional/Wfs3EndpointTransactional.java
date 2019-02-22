/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.transactional;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;


/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTransactional implements Wfs3EndpointExtension {

    @Override
    public String getPath() {
        return "collections";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/(?:\\w+)\\/items\\/?.*$";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("POST", "PUT", "DELETE");
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData) {
        if (!isExtensionEnabled(serviceData, TransactionalConfiguration.class)) {
            throw new NotFoundException();
        }
        return true;
    }

    @Path("/{id}/items")
    @POST
    @Consumes(Wfs3MediaTypes.GEO_JSON)
    public Response postItems(@Auth Optional<User> optionalUser, @PathParam("id") String id, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {
        checkTransactional((Wfs3Service) service);

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        return ((Wfs3Service) service).postItemsResponse(wfs3Request.getMediaType(), wfs3Request.getUriCustomizer()
                                                                                                .copy(), id, requestBody);
    }

    @Path("/{id}/items/{featureid}")
    @PUT
    @Consumes(Wfs3MediaTypes.GEO_JSON)
    public Response putItem(@Auth Optional<User> optionalUser, @PathParam("id") String id, @PathParam("featureid") final String featureId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context HttpServletRequest request, InputStream requestBody) {
        checkTransactional((Wfs3Service) service);

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        return ((Wfs3Service) service).putItemResponse(wfs3Request.getMediaType(), id, featureId, requestBody);
    }

    @Path("/{id}/items/{featureid}")
    @DELETE
    public Response deleteItem(@Auth Optional<User> optionalUser, @Context Service service, @PathParam("id") String id, @PathParam("featureid") final String featureId) {
        checkTransactional((Wfs3Service) service);

        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        return ((Wfs3Service) service).deleteItemResponse(id, featureId);
    }

    private void checkTransactional(Wfs3Service wfs3Service) {
        if (!wfs3Service.getData()
                        .getFeatureProvider()
                        .supportsTransactions()) {
            throw new NotFoundException();
        }
    }
}
