/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.RangeHeader;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3Collection;
import de.ii.ldproxy.wfs3.api.Wfs3Collections;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3OutputFormatExtension;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private final Map<Wfs3MediaType, Wfs3OutputFormatExtension> wfs3OutputFormats;

    public Wfs3EndpointCore(@Requires Wfs3ExtensionRegistry wfs3ExtensionRegistry) {
        this.wfs3OutputFormats = wfs3ExtensionRegistry.getOutputFormats();
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
        Wfs3Service wfs3Service = (Wfs3Service) service;

        checkAuthorization(wfs3Service.getData(), optionalUser);

        Wfs3Collections collections = wfs3Core.createCollections(wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer());

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getDatasetResponse(collections, wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), wfs3Request.getStaticUrlPrefix(), true);
    }

    @Path("/{id}")
    @GET
    public Response getCollectionInfo(@Auth Optional<User> optionalUser, @PathParam("id") String id, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        Wfs3Service wfs3Service = (Wfs3Service) service;

        checkAuthorization(wfs3Service.getData(), optionalUser);

        wfs3Core.checkCollectionName(wfs3Service.getData(), id);

        Wfs3Collection wfs3Collection = wfs3Core.createCollection(wfs3Service.getData().getFeatureTypes()
                                                                  .get(id), new Wfs3LinksGenerator(), wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), false);

        return wfs3OutputFormats.get(wfs3Request.getMediaType())
                                .getCollectionResponse(wfs3Collection, wfs3Service.getData(), wfs3Request.getMediaType(), getAlternativeMediaTypes(wfs3Request.getMediaType()), wfs3Request.getUriCustomizer(), id);


    }

    @Path("/{id}/items")
    @GET
    public Response getItems(@Auth Optional<User> optionalUser, @PathParam("id") String id, @QueryParam("crs") String crs, @QueryParam("bbox-crs") String bboxCrs, @QueryParam("resultType") String resultType, @QueryParam("maxAllowableOffset") String maxAllowableOffset, @HeaderParam("Range") String range, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

        FeatureQuery query = getFeatureQuery(((Wfs3Service) service), id, range, crs, bboxCrs, maxAllowableOffset, uriInfo.getQueryParameters(), resultType != null && resultType.toLowerCase()
                                                                                                                                                                                 .equals("hits"));

        return ((Wfs3Service) service).getItemsResponse(wfs3Request, id, query);
    }

    @Path("/{id}/items/{featureid}")
    @GET
    public Response getItem(@Auth Optional<User> optionalUser, @PathParam("id") String id, @QueryParam("crs") String crs, @QueryParam("maxAllowableOffset") String maxAllowableOffset, @PathParam("featureid") final String featureId, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization(((Wfs3Service) service).getData(), optionalUser);

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

    private Wfs3MediaType[] getAlternativeMediaTypes(Wfs3MediaType mediaType) {
        return wfs3OutputFormats.keySet()
                                .stream()
                                .filter(wfs3MediaType -> !wfs3MediaType.equals(mediaType))
                                .toArray(Wfs3MediaType[]::new);
    }
}
