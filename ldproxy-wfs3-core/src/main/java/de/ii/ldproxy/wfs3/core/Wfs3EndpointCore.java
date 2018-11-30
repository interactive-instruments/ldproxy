/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import com.google.common.base.Splitter;
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
    public Response getItems(@Auth Optional<User> optionalUser, @PathParam("id") String id, @QueryParam("crs") String crs, @QueryParam("bbox-crs") String bboxCrs, @QueryParam("resultType") String resultType, @QueryParam("maxAllowableOffset") String maxAllowableOffset, @HeaderParam("Range") String range, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization(((Wfs3Service2) service).getData(), optionalUser);

        FeatureQuery query = getFeatureQuery(((Wfs3Service2) service), id, range, crs, bboxCrs, maxAllowableOffset, uriInfo.getQueryParameters(), resultType != null && resultType.toLowerCase()
                                                                                                                                                                                  .equals("hits"));

        return ((Wfs3Service2) service).getItemsResponse(wfs3Request, id, query);
    }

    @Path("/{id}/items/{featureid}")
    @GET
    public Response getItem(@Auth Optional<User> optionalUser, @PathParam("id") String id, @QueryParam("crs") String crs, @QueryParam("maxAllowableOffset") String maxAllowableOffset, @PathParam("featureid") final String featureId, @Context Service service, @Context Wfs3RequestContext wfs3Request, @Context UriInfo uriInfo) {
        checkAuthorization(((Wfs3Service2) service).getData(), optionalUser);

        List<String> propertiesList = getPropertiesList(uriInfo.getQueryParameters().getFirst("properties"));

        ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                          .type(id)
                                                                          .filter(String.format("IN ('%s')", featureId))
                                                                          .fields(propertiesList);

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

        return ((Wfs3Service2) service).getItemsResponse(wfs3Request, id, queryBuilder.build());
    }

    //TODO Wfs3Query class for parsing request and creating FeatureQuery
    private FeatureQuery getFeatureQuery(Wfs3Service2 service, String featureType, String range, String crs, String bboxCrs, String maxAllowableOffset, MultivaluedMap<String, String> queryParameters, boolean hitsOnly) {

        final Map<String, String> filterableFields = service.getData()
                                                            .getFilterableFieldsForFeatureType(featureType);

        Map<String, String> parameters = toFlatMap(queryParameters);

        for (Wfs3ParameterExtension parameterExtension : wfs3ExtensionRegistry.getWfs3Parameters()) {
            parameters = parameterExtension.transformParameters(service.getData().getFeatureTypes().get(featureType), parameters);
        }


        final Map<String, String> filters = getFiltersFromQuery(parameters, filterableFields);

        List<String> propertiesList = getPropertiesList(parameters.get("properties"));


        final int[] countFrom = RangeHeader.parseRange(range);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(featureType)
                                                                                .limit(countFrom[0])
                                                                                .offset(countFrom[1])
                                                                                .hitsOnly(hitsOnly)
                                                                                .fields(propertiesList);

        wfs3ExtensionRegistry.getWfs3Parameters().forEach(parameterExtension -> {
            parameterExtension.transformQuery(service.getData().getFeatureTypes().get(featureType), queryBuilder);
        });

        //TODO to extension
        if (Objects.nonNull(crs) && !isDefaultCrs(crs)) {
            EpsgCrs targetCrs = new EpsgCrs(crs);
            queryBuilder.crs(targetCrs);
        }

        //TODO to extension
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

    public static Map<String, String> toFlatMap(MultivaluedMap<String, String> queryParameters) {
        return queryParameters.entrySet()
                                                              .stream()
                                                              .map(entry -> {
                                                                  return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().isEmpty() ? "" : entry.getValue().get(0));
                                                              })
                                                              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isDefaultCrs(String crs) {
        return Objects.equals(crs, DEFAULT_CRS_URI);
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

    private String getCQLFromFilters(Wfs3Service2 service, Map<String, String> filters, Map<String, String> filterableFields, Optional<EpsgCrs> bboxCrs) {
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

    public Wfs3MediaType[] getAlternativeMediaTypes(Wfs3MediaType mediaType) {
        return wfs3OutputFormats.keySet()
                                .stream()
                                .filter(wfs3MediaType -> !wfs3MediaType.equals(mediaType))
                                .toArray(Wfs3MediaType[]::new);
    }

    public static List<String> getPropertiesList(String properties) {
       if (Objects.nonNull(properties)) {
            return Splitter.on(',').omitEmptyStrings().trimResults().splitToList(properties);
        } else {
            return ImmutableList.of("*");
        }
    }
}
