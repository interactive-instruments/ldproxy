/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiEndpointExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ParameterExtension;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointCore implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointCore.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^\\/?(?:\\/\\w+\\/?(?:\\/items\\/?.*)?)?$")
            .addMethods(HttpMethods.GET)
            .build();

    @Requires
    private Wfs3Core wfs3Core;

    @Requires
    private Wfs3Query wfs3Query;

    private final OgcApiExtensionRegistry extensionRegistry;

    public Wfs3EndpointCore(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    private List<Wfs3ParameterExtension> getParameterExtensions() {
        return extensionRegistry.getExtensionsForType(Wfs3ParameterExtension.class);
    }

    private Map<OgcApiMediaType, Wfs3OutputFormatExtension> getOutputFormats() {
        return extensionRegistry.getExtensionsForType(Wfs3OutputFormatExtension.class)
                                .stream()
                                .map(outputFormatExtension -> new AbstractMap.SimpleEntry<>(outputFormatExtension.getMediaType(), outputFormatExtension))
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset) {
        return extensionRegistry.getExtensionsForType(Wfs3OutputFormatExtension.class)
                                .stream()
                                .filter(wfs3OutputFormatExtension -> wfs3OutputFormatExtension.isEnabledForDataset(dataset))
                                .map(Wfs3OutputFormatExtension::getMediaType)
                                .collect(ImmutableSet.toImmutableSet());
    }

    //TODO: config for wfs3 core, what about dependencies between extensions?
    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData serviceData) {
        return true;
    }

    @Path("/")
    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context OgcApiDataset service,
                                   @Context OgcApiRequestContext wfs3Request) {
        checkAuthorization(service.getData(), optionalUser);

        return service.getDatasetResponse(wfs3Request, true);
    }

    @Path("/{id}")
    @GET
    public Response getCollectionInfo(@Auth Optional<User> optionalUser, @PathParam("id") String id,
                                      @Context OgcApiDataset service, @Context OgcApiRequestContext wfs3Request) {
        checkAuthorization(service.getData(), optionalUser);

        wfs3Core.checkCollectionName(service.getData(), id);

        Wfs3Collection wfs3Collection = wfs3Core.createCollection(service.getData()
                                                                         .getFeatureTypes()
                                                                         .get(id), service.getData(), wfs3Request.getMediaType(), wfs3Request.getAlternativeMediaTypes(), wfs3Request.getUriCustomizer(), false);

        return getOutputFormats().get(wfs3Request.getMediaType())
                                 .getCollectionResponse(wfs3Collection, service.getData(), wfs3Request.getMediaType(), wfs3Request.getAlternativeMediaTypes(), wfs3Request.getUriCustomizer(), id);


    }

    @Path("/{id}/items")
    @GET
    public Response getItems(@Auth Optional<User> optionalUser, @PathParam("id") String id,
                             @HeaderParam("Range") String range, @Context OgcApiDataset service,
                             @Context UriInfo uriInfo, @Context OgcApiRequestContext wfs3Request) {
        checkAuthorization(service.getData(), optionalUser);

        FeatureQuery query = wfs3Query.requestToFeatureQuery(service, id, range, toFlatMap(uriInfo.getQueryParameters()));

        return wfs3Core.getItemsResponse(service, wfs3Request, id, query, getOutputFormats().get(wfs3Request.getMediaType()), wfs3Request.getAlternativeMediaTypes(), false);
    }

    @Path("/{id}/items/{featureid}")
    @GET
    public Response getItem(@Auth Optional<User> optionalUser, @PathParam("id") String id,
                            @PathParam("featureid") final String featureId, @Context OgcApiDataset service,
                            @Context OgcApiRequestContext wfs3Request, @Context UriInfo uriInfo) {
        checkAuthorization(service.getData(), optionalUser);

        FeatureQuery query = wfs3Query.requestToFeatureQuery(service, id, toFlatMap(uriInfo.getQueryParameters()), featureId);

        return wfs3Core.getItemResponse(service, wfs3Request, id, query, getOutputFormats().get(wfs3Request.getMediaType()), wfs3Request.getAlternativeMediaTypes());
    }

    public static Map<String, String> toFlatMap(MultivaluedMap<String, String> queryParameters) {
        return toFlatMap(queryParameters, false);
    }

    public static Map<String, String> toFlatMap(MultivaluedMap<String, String> queryParameters,
                                                boolean keysToLowerCase) {
        return queryParameters.entrySet()
                              .stream()
                              .map(entry -> {
                                  String key = keysToLowerCase ? entry.getKey()
                                                                      .toLowerCase() : entry.getKey();
                                  return new AbstractMap.SimpleImmutableEntry<>(key, entry.getValue()
                                                                                          .isEmpty() ? "" : entry.getValue()
                                                                                                                 .get(0));
                              })
                              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, String> getFiltersFromQuery(Map<String, String> query,
                                                          Map<String, String> filterableFields) {

        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterableFields.containsKey(filterKey.toLowerCase())) {
                String filterValue = query.get(filterKey);
                filters.put(filterKey.toLowerCase(), filterValue);
            }
        }

        return filters;
    }


}
