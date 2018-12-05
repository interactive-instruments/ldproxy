/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ParameterExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.ldproxy.wfs3.api.Wfs3Service2;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS_URI;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointCore implements Wfs3EndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointCore.class);

    @Requires
    private Wfs3Core wfs3Core;

    @Requires
    private Wfs3Query wfs3Query;

    private final Map<Wfs3MediaType, Wfs3OutputFormatExtension> wfs3OutputFormats;
    private final List<Wfs3ParameterExtension> wfs3ParameterExtensions;
    private final Wfs3ExtensionRegistry wfs3ExtensionRegistry;

    public Wfs3EndpointCore(@Requires Wfs3ExtensionRegistry wfs3ExtensionRegistry) {
        this.wfs3ExtensionRegistry = wfs3ExtensionRegistry;
        this.wfs3OutputFormats = wfs3ExtensionRegistry.getOutputFormats();
        this.wfs3ParameterExtensions = wfs3ExtensionRegistry.getWfs3Parameters();
    }


    @Override
    public String getPath() {
        return "collections";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/?(?:\\/\\w+\\/?(?:\\/items\\/?.*)?)?$";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("GET");
    }

    @Path("/")
    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3Service2 wfs3Service = (Wfs3Service2) service;

        checkAuthorization(wfs3Service.getData(), optionalUser);

        Wfs3Collections collections = wfs3Core.createCollections(wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer());

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getDatasetResponse(collections, wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), wfs3Request.getStaticUrlPrefix(), true);
    }

    @Path("/{id}")
    @GET
    public Response getCollectionInfo(@Auth Optional<User> optionalUser, @PathParam("id") String id, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3Service2 wfs3Service = (Wfs3Service2) service;

        checkAuthorization(wfs3Service.getData(), optionalUser);

        wfs3Core.checkCollectionName(wfs3Service.getData(), id);

        Wfs3Collection wfs3Collection = wfs3Core.createCollection(wfs3Service.getData()
                                                                             .getFeatureTypes()
                                                                             .get(id), new Wfs3LinksGenerator(), wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), false);

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getCollectionResponse(wfs3Collection, wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), id);


    }

    @Path("/{id}/items")
    @GET
    public Response getItems(@Auth Optional<User> optionalUser, @PathParam("id") String id, @HeaderParam("Range") String range, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization(((Wfs3Service2) service).getData(), optionalUser);

        FeatureQuery query = wfs3Query.requestToFeatureQuery(((Wfs3Service2) service), id, range, toFlatMap(uriInfo.getQueryParameters()));

        return ((Wfs3Service2) service).getItemsResponse(wfs3Request, id, query);
    }

    @Path("/{id}/items/{featureid}")
    @GET
    public Response getItem(@Auth Optional<User> optionalUser, @PathParam("id") String id, @PathParam("featureid") final String featureId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context UriInfo uriInfo) {
        checkAuthorization(((Wfs3Service2) service).getData(), optionalUser);

        FeatureQuery query = wfs3Query.requestToFeatureQuery(((Wfs3Service2) service), id, toFlatMap(uriInfo.getQueryParameters()), featureId);

        return ((Wfs3Service2) service).getItemsResponse(wfs3Request, id, query);
    }

    public static Map<String, String> toFlatMap(MultivaluedMap<String, String> queryParameters) {
        return queryParameters.entrySet()
                                                              .stream()
                                                              .map(entry -> {
                                                                  return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().isEmpty() ? "" : entry.getValue().get(0));
                                                              })
                                                              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, String> getFiltersFromQuery(Map<String, String> query, Map<String, String> filterableFields) {

        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterableFields.containsKey(filterKey.toLowerCase())) {
                String filterValue = query.get(filterKey);
                filters.put(filterKey.toLowerCase(), filterValue);
            }
        }

        return filters;
    }

    public Wfs3MediaType[] getAlternativeMediaTypes(Wfs3MediaType mediaType) {
        return wfs3OutputFormats.keySet()
                                .stream()
                                .filter(wfs3MediaType -> !wfs3MediaType.equals(mediaType))
                                .toArray(Wfs3MediaType[]::new);
    }


}
