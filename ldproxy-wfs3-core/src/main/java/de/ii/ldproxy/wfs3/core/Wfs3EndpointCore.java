/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.DatasetLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext.HttpMethods;
import de.ii.ldproxy.wfs3.api.Wfs3CollectionFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3FeatureFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ParameterExtension;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class Wfs3EndpointCore implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointCore.class);
    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^/?(?:/\\w+/?(?:/items/?\\w*)?)?$")
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

    private Map<OgcApiMediaType, Wfs3FeatureFormatExtension> getFeatureFormats() {
        return extensionRegistry.getExtensionsForType(Wfs3FeatureFormatExtension.class)
                                .stream()
                                .map(outputFormatExtension -> new AbstractMap.SimpleEntry<>(outputFormatExtension.getMediaType(), outputFormatExtension))
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<OgcApiMediaType, Wfs3CollectionFormatExtension> getCollectionFormats() {
        return extensionRegistry.getExtensionsForType(Wfs3CollectionFormatExtension.class)
                .stream()
                .map(outputFormatExtension -> new AbstractMap.SimpleEntry<>(outputFormatExtension.getMediaType(), outputFormatExtension))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        if (subPath.matches("^/?\\w+/items(?:/?\\w+)?$"))
            // feature collection or feature
            return extensionRegistry.getExtensionsForType(Wfs3FeatureFormatExtension.class)
                                    .stream()
                                    .filter(formatExtension -> formatExtension.isEnabledForApi(dataset))
                                    .map(Wfs3FeatureFormatExtension::getMediaType)
                                    .collect(ImmutableSet.toImmutableSet());
        else if (subPath.matches("^/?(?:\\w+)?$"))
            // collections or collection
            return extensionRegistry.getExtensionsForType(Wfs3CollectionFormatExtension.class)
                    .stream()
                    .filter(formatExtension -> formatExtension.isEnabledForApi(dataset))
                    .map(Wfs3CollectionFormatExtension::getMediaType)
                    .collect(ImmutableSet.toImmutableSet());

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, Wfs3CoreConfiguration.class);
    }

    @Path("/")
    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context OgcApiDataset service,
                                   @Context OgcApiRequestContext ogcApiRequest) {
        OgcApiDatasetData apiData = service.getData();
        checkAuthorization(apiData, optionalUser);

        final DatasetLinksGenerator linksGenerator = new DatasetLinksGenerator();

        //TODO: to crs extension, remove duplicates
        ImmutableList<String> crs = ImmutableList.<String>builder()
                .add(apiData.getFeatureProvider()
                        .getNativeCrs()
                        .getAsUri())
                .add(OgcApiDatasetData.DEFAULT_CRS_URI)
                .addAll(apiData.getAdditionalCrs()
                        .stream()
                        .map(EpsgCrs::getAsUri)
                        .collect(Collectors.toList()))
                .build();

        Wfs3CollectionFormatExtension format = service.getOutputFormat(Wfs3CollectionFormatExtension.class, ogcApiRequest.getMediaType(), "/").orElseThrow(NotAcceptableException::new);
        List<CommonFormatExtension> alternateFormats = service.getAllOutputFormats(CommonFormatExtension.class, ogcApiRequest.getMediaType(), "/", Optional.of(format));
        List<OgcApiMediaType> alternateMediaTypes = alternateFormats.stream()
                .map(alternateFormat -> alternateFormat.getMediaType())
                .collect(Collectors.toList());

        List<OgcApiLink> ogcApiLinks = linksGenerator.generateDatasetLinks(ogcApiRequest.getUriCustomizer().copy(), Optional.empty(), ogcApiRequest.getMediaType(), alternateMediaTypes);

        ImmutableDataset.Builder dataset = new ImmutableDataset.Builder()
                .crs(crs)
                .links(ogcApiLinks);

        for (OgcApiLandingPageExtension ogcApiLandingPageExtension : getDatasetExtenders()) {
            dataset = ogcApiLandingPageExtension.process(dataset, apiData, ogcApiRequest.getUriCustomizer().copy(), ogcApiRequest.getMediaType(), alternateMediaTypes);
        }

        Response response =  format.getCollectionsResponse(dataset.build(), service, ogcApiRequest);

        return Response.ok()
                .entity(response.getEntity())
                .type(format.getMediaType().type())
                .build();
    }

    @Path("/{collectionId}")
    @GET
    public Response getCollection(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                                  @Context OgcApiDataset service, @Context OgcApiRequestContext wfs3Request) {
        OgcApiDatasetData apiData = service.getData();
        checkAuthorization(service.getData(), optionalUser);

        wfs3Core.checkCollectionName(apiData, collectionId);

        OgcApiCollection ogcApiCollection = wfs3Core.createCollection(apiData.getFeatureTypes()
                                                                             .get(collectionId), service.getData(), wfs3Request.getMediaType(), wfs3Request.getAlternateMediaTypes(), wfs3Request.getUriCustomizer(), false);

        Wfs3CollectionFormatExtension format = service.getOutputFormat(Wfs3CollectionFormatExtension.class, wfs3Request.getMediaType(), "/collections/"+collectionId).orElseThrow(NotAcceptableException::new);

        Response response = format.getCollectionResponse(ogcApiCollection, collectionId, service, wfs3Request);

        return Response.ok()
                .entity(response.getEntity())
                .type(format.getMediaType().type())
                .build();
    }

    @Path("/{collectionId}/items")
    @GET
    public Response getItems(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                             @HeaderParam("Range") String range, @Context OgcApiDataset service,
                             @Context UriInfo uriInfo, @Context OgcApiRequestContext wfs3Request) {
        checkAuthorization(service.getData(), optionalUser);

        FeatureQuery query = wfs3Query.requestToFeatureQuery(service, collectionId, range, toFlatMap(uriInfo.getQueryParameters()));

        return wfs3Core.getItemsResponse(service, wfs3Request, collectionId, query, getFeatureFormats().get(wfs3Request.getMediaType()), wfs3Request.getAlternateMediaTypes(), false);
    }

    @Path("/{collectionId}/items/{featureId}")
    @GET
    public Response getItem(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                            @PathParam("featureId") final String featureId, @Context OgcApiDataset service,
                            @Context OgcApiRequestContext wfs3Request, @Context UriInfo uriInfo) {
        checkAuthorization(service.getData(), optionalUser);

        FeatureQuery query = wfs3Query.requestToFeatureQuery(service, collectionId, toFlatMap(uriInfo.getQueryParameters()), featureId);

        return wfs3Core.getItemResponse(service, wfs3Request, collectionId, query, getFeatureFormats().get(wfs3Request.getMediaType()), wfs3Request.getAlternateMediaTypes());
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

    private List<OgcApiLandingPageExtension> getDatasetExtenders() {
        return extensionRegistry.getExtensionsForType(OgcApiLandingPageExtension.class);
    }
}
