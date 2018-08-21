/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceResource;
import io.dropwizard.auth.Auth;
import io.dropwizard.views.ViewRenderer;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import javax.annotation.security.PermitAll;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS_URI;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = ServiceResource.SERVICE_TYPE_KEY, type = "java.lang.String", value = "WFS3")
})
@Instantiate

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@PermitAll //TODO ???
@Produces(MediaType.WILDCARD)
public class Wfs3ServiceResource implements ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceResource.class);

    private Set<Wfs3MediaType> supportedMediaTypes;

    private Optional<URI> externalUri = Optional.empty();
    private List<Wfs3EndpointExtension> wfs3Endpoints;

    @Requires
    private Wfs3RequestInjectableContext wfs3RequestContext;


    Wfs3ServiceResource(@Requires Wfs3ExtensionRegistry wfs3ConformanceClassRegistry) {
        this.supportedMediaTypes = wfs3ConformanceClassRegistry.getOutputFormats()
                                                               .keySet();
        this.wfs3Endpoints = wfs3ConformanceClassRegistry.getEndpoints();
    }

    @Bind
    void setCore(CoreServerConfig coreServerConfig) {
        URI externalUri = null;
        try {
            externalUri = new URI(coreServerConfig.getExternalUrl());
        } catch (URISyntaxException e) {
            // ignore
        }

        this.externalUri = Optional.ofNullable(externalUri);
    }

    @Override
    public Service getService() {
        return null;
    }

    @Override
    public void setService(Service service) {

    }

    @Override
    public void init(ObjectMapper defaultObjectMapper, EntityRepository entityRepository, AuthorizationProvider permProvider) {

    }

    @Override
    public void setMustacheRenderer(ViewRenderer mustacheRenderer) {

    }

    @GET
    public Response getDataset(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization((Wfs3Service) service, optionalUser);

        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getDatasetResponse(wfs3Request, false);
    }

    @Path("/conformance")
    @GET
    public Response getConformanceClasses(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getConformanceResponse(wfs3Request);
    }

    //TODO: to oas30 bundle
    /*@Path("/api")
    public OpenApiResource getOpenApi() {
        return openApiResource;
    }
*/
    @Path("/collections")
    @GET
    public Response getCollections(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization((Wfs3Service) service, optionalUser);

        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getDatasetResponse(wfs3Request, true);
    }

    @Path("/collections/{id}")
    @GET
    public Response getCollectionInfo(@Auth Optional<User> optionalUser, @PathParam("id") String id, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization((Wfs3Service) service, optionalUser);

        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getCollectionResponse(wfs3Request, id);
    }

    @Path("/collections/{id}/items")
    @GET
    public Response getItems(@Auth Optional<User> optionalUser, @PathParam("id") String id, @QueryParam("crs") String crs, @QueryParam("bbox-crs") String bboxCrs, @QueryParam("resultType") String resultType, @QueryParam("maxAllowableOffset") String maxAllowableOffset, @HeaderParam("Range") String range, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization((Wfs3Service) service, optionalUser);

        FeatureQuery query = getFeatureQuery(((Wfs3Service) service), id, range, crs, bboxCrs, maxAllowableOffset, uriInfo.getQueryParameters(), resultType != null && resultType.toLowerCase()
                                                                                                                                                                                 .equals("hits"));

        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getItemsResponse(wfs3Request, id, query);
    }

    @Path("/collections/{id}/items/{featureid}")
    @GET
    public Response getItem(@Auth Optional<User> optionalUser, @PathParam("id") String id, @QueryParam("crs") String crs, @QueryParam("maxAllowableOffset") String maxAllowableOffset, @PathParam("featureid") final String featureId, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization((Wfs3Service) service, optionalUser);

        ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                          .type(id)
                                                                          .filter(String.format("IN ('%s')", featureId));

        if (Objects.nonNull(crs) && !isDefaultCrs(crs)) {
            EpsgCrs targetCrs = new EpsgCrs(crs);
            queryBuilder.crs(targetCrs);
        }

        if (Objects.nonNull(maxAllowableOffset)) {
            try {
                queryBuilder.maxAllowableOffset(Double.valueOf(maxAllowableOffset));
            } catch (NumberFormatException e) {
                //ignore
            }
        }

        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getItemsResponse(wfs3Request, id, queryBuilder.build());
    }

    //TODO: to transactional bundle, also the openapi operations

    @Path("/{path}")
    public Wfs3EndpointExtension dispatch(@Auth Optional<User> optionalUser, @PathParam("path") String path, @Context Service service, @Context HttpHeaders httpHeaders, @Context ContainerRequestContext request) {
        //checkAuthorization(optionalUser);

        //wfs3RequestContext.inject(request, new Wfs3Request(request.getUriInfo().getRequestUri(), externalUri, httpHeaders));

        return wfs3Endpoints.stream()
                            .filter(wfs3Endpoint -> wfs3Endpoint.matches(path, request.getMethod()))
                            .findFirst()
                            .orElseThrow(() -> new NotFoundException("catched " + path + " " + request.getMethod()));
    }


    private void checkAuthorization(Wfs3Service service, Optional<User> optionalUser) {
        if (service.getData()
                   .isSecured() && !optionalUser.isPresent()) {
            throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
            //throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
    }

    //TODO
    private MediaType contentNegotiation(List<MediaType> acceptableMediaTypes) {
        /*if (supportedMediaTypes.isEmpty() || acceptableMediaTypes.isEmpty() || !supportedMediaTypes.contains(acceptableMediaTypes.get(0))) {
            throw new NotAcceptableException();
        }*/

        return acceptableMediaTypes.get(0);
    }

    private FeatureQuery getFeatureQuery(Wfs3Service service, String featureType, String range, String crs, String bboxCrs, String maxAllowableOffset, MultivaluedMap<String, String> queryParameters, boolean hitsOnly) {
        final Map<String, String> filterableFields = service.getData()
                                                            .getFilterableFieldsForFeatureType(featureType);

        final Map<String, String> filters = getFiltersFromQuery(queryParameters, filterableFields);

        final int[] countFrom = RangeHeader.parseRange(range);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(featureType)
                                                                                .limit(countFrom[0])
                                                                                .offset(countFrom[1])
                                                                                .hitsOnly(hitsOnly);

        if (Objects.nonNull(crs) && !isDefaultCrs(crs)) {
            EpsgCrs targetCrs = new EpsgCrs(crs);
            queryBuilder.crs(targetCrs);
        }

        if (Objects.nonNull(maxAllowableOffset)) {
            try {
                queryBuilder.maxAllowableOffset(Double.valueOf(maxAllowableOffset));
            } catch (NumberFormatException e) {
                //ignore
            }
        }

        if (!filters.isEmpty()) {
            String cql = getCQLFromFilters(service, filters, filterableFields, Optional.ofNullable(bboxCrs)
                                                                                       .filter(s -> !isDefaultCrs(s))
                                                                                       .map(EpsgCrs::new));
            LOGGER.debug("CQL {}", cql);
            queryBuilder.filter(cql);
        }

        return queryBuilder.build();
    }

    private boolean isDefaultCrs(String crs) {
        return Objects.equals(crs, DEFAULT_CRS_URI);
    }

    private Map<String, String> getFiltersFromQuery(MultivaluedMap<String, String> query, Map<String, String> filterableFields) {

        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterableFields.containsKey(filterKey.toLowerCase())) {
                String filterValue = query.getFirst(filterKey);
                filters.put(filterKey.toLowerCase(), filterValue);
            }
        }

        return filters;
    }

    private String getCQLFromFilters(Wfs3Service service, Map<String, String> filters, Map<String, String> filterableFields, Optional<EpsgCrs> bboxCrs) {
        return filters.entrySet()
                      .stream()
                      .map(f -> {
                          if (f.getKey()
                               .equals("bbox")) {
                              String[] bbox = f.getValue()
                                               .split(",");
                              EpsgCrs crs = bboxCrs.orElse(DEFAULT_CRS);
                              //String[] bbox2 = {bbox[1], bbox[0], bbox[3], bbox[2]};
                              BoundingBox bbox3 = new BoundingBox(Double.valueOf(bbox[0]), Double.valueOf(bbox[1]), Double.valueOf(bbox[2]), Double.valueOf(bbox[3]), crs);
                              BoundingBox bbox4 = null;
                              try {
                                  bbox4 = service.transformBoundingBox(bbox3);
                              } catch (CrsTransformationException e) {
                                  LOGGER.error("Error transforming bbox");
                              }

                              return String.format(Locale.US, "BBOX(%s, %.3f, %.3f, %.3f, %.3f, '%s')", filterableFields.get(f.getKey()), bbox4.getXmin(), bbox4.getYmin(), bbox4.getXmax(), bbox4.getYmax(), bbox4.getEpsgCrs()
                                                                                                                                                                                                                   .getAsSimple());
                          }
                          if (f.getKey()
                               .equals("time")) {
                              try {
                                  Interval fromIso8601Period = Interval.parse(f.getValue());
                                  return String.format("%s DURING %s", filterableFields.get(f.getKey()), fromIso8601Period);
                              } catch (DateTimeParseException ignore) {
                                  try {
                                      Instant fromIso8601 = Instant.parse(f.getValue());
                                      return String.format("%s TEQUALS %s", filterableFields.get(f.getKey()), fromIso8601);
                                  } catch (DateTimeParseException e) {
                                      LOGGER.debug("TIME PARSER ERROR", e);
                                      throw new BadRequestException();
                                  }
                              }
                          }
                          if (f.getValue()
                               .contains("*")) {
                              return String.format("%s LIKE '%s'", filterableFields.get(f.getKey()), f.getValue());
                          }

                          return String.format("%s = '%s'", filterableFields.get(f.getKey()), f.getValue());
                      })
                      .collect(Collectors.joining(" AND "));
    }
}
