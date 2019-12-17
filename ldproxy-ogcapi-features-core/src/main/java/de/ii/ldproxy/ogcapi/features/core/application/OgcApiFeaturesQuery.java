/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;



@Component
@Provides(specifications = {OgcApiFeaturesQuery.class})
@Instantiate
public class OgcApiFeaturesQuery {

    private static final String TIMESTAMP_REGEX = "([0-9]+)-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])[Tt]([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9]|60)(\\.[0-9]+)?(([Zz])|([\\+|\\-]([01][0-9]|2[0-3]):[0-5][0-9]))";
    private static final String OPEN_REGEX = "(\\.\\.)?";

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiFeaturesQuery.class);

    private final OgcApiExtensionRegistry wfs3ExtensionRegistry;

    public OgcApiFeaturesQuery(@Requires OgcApiExtensionRegistry wfs3ExtensionRegistry) {
        this.wfs3ExtensionRegistry = wfs3ExtensionRegistry;
    }

    public FeatureQuery requestToFeatureQuery(OgcApiDataset api, String collectionId, Map<String, String> parameters, String featureId) {

        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            FeatureTypeConfigurationOgcApi featureTypeConfiguration = api.getData()
                    .getFeatureTypes()
                    .get(collectionId);
            // check, if the requested collection exists
            if (Objects.isNull(featureTypeConfiguration))
                throw new NotFoundException();

            parameters = parameterExtension.transformParameters(featureTypeConfiguration, parameters, api.getData());
        }

        final String filter = String.format("IN ('%s')", featureId);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(collectionId)
                                                                                .filter(filter);

        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            parameterExtension.transformQuery(api.getData()
                                                     .getFeatureTypes()
                                                     .get(collectionId), queryBuilder, parameters,api.getData());
        }

        return queryBuilder.build();
    }

    public FeatureQuery requestToFeatureQuery(OgcApiDataset api, String collectionId, int minimumPageSize, int defaultPageSize, int maxPageSize, Map<String, String> parameters) {

        final Map<String, String> filterableFields = api.getData()
                                                        .getFilterableFieldsForFeatureType(collectionId);

        Set<String> filterParameters = ImmutableSet.of();
        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            filterParameters = parameterExtension.getFilterParameters(filterParameters, api.getData());
        }

        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {

            FeatureTypeConfigurationOgcApi featureTypeConfiguration = api.getData()
                    .getFeatureTypes()
                    .get(collectionId);
            // check, if the requested collection exists
            if (Objects.isNull(featureTypeConfiguration))
                throw new NotFoundException();

            parameters = parameterExtension.transformParameters(featureTypeConfiguration, parameters, api.getData());
        }

        final Map<String, String> filters = getFiltersFromQuery(parameters, filterableFields, filterParameters);

        boolean hitsOnly = parameters.containsKey("resultType") && parameters.get("resultType")
                                                                             .toLowerCase()
                                                                             .equals("hits");

        /**
         * NOTE: OGC API and ldproxy do not use the HTTP "Range" header for limit/offset for the following reasons:
         * - We need to support some non-header mechanism anyhow to be able to mint URIs (links) to pages / partial responses.
         * - A request without a range header cannot return 206, so there is no way that a server could have a default limit.
         *   I.e. any request to a collection without a range header would have to return all features and it is important to
         *   enable servers to have a default page limit.
         * - There is no real need for multipart responses, but servers would have to support requests that lead to
         *   206 multipart responses.
         * - Developers do not seem to expect such an approach and since it uses a custom range unit anyhow (i.e. not bytes),
         *   it is unclear how much value it brings. Probably consistent with this: I have not seen much of range headers
         *   in Web APIs for paging.
         */
        final int limit = parseLimit(minimumPageSize, defaultPageSize, maxPageSize, parameters.get("limit"));
        final int offset = parseOffset(parameters.get("offset"));

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(collectionId)
                                                                                .limit(limit)
                                                                                .offset(offset)
                                                                                .hitsOnly(hitsOnly);

        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            parameterExtension.transformQuery(api.getData()
                                                     .getFeatureTypes()
                                                     .get(collectionId), queryBuilder, parameters,api.getData());
        }


        if (!filters.isEmpty()) {
            String cql = getCQLFromFilters(api, filters, filterableFields, filterParameters);
            LOGGER.debug("CQL {}", cql);
            queryBuilder.filter(cql);
        }

        return queryBuilder.build();

    }

    private Map<String, String> getFiltersFromQuery(Map<String, String> query, Map<String, String> filterableFields, Set<String> filterParameters) {

        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterParameters.contains(filterKey)) {
                String filterValue = query.get(filterKey);
                filters.put(filterKey, filterValue);
            } else if (filterableFields.containsKey(filterKey)) {
                String filterValue = query.get(filterKey);
                filters.put(filterKey, filterValue);
            }
        }

        return filters;
    }

    private String getCQLFromFilters(OgcApiDataset service, Map<String, String> filters, Map<String, String> filterableFields, Set<String> filterParameters) {
        return filters.entrySet()
                      .stream()
                      .map(f -> {
                          if (f.getKey()
                               .equals("bbox")) {
                              return bboxToCql(service, filterableFields.get(f.getKey()), f.getValue());
                          }
                          if (f.getKey()
                               .equals("datetime")) {
                              return timeToCql(filterableFields.get(f.getKey()), f.getValue());
                          }
                          if (filterParameters.contains(f.getKey())) {
                              return f.getValue();
                          }
                          if (f.getValue()
                               .contains("*")) {
                              return String.format("%s LIKE '%s'", filterableFields.get(f.getKey()), f.getValue());
                          }

                          return String.format("%s = '%s'", filterableFields.get(f.getKey()), f.getValue());
                      })
                      .filter(pred -> pred!=null)
                      .collect(Collectors.joining(" AND "));
    }

    private String bboxToCql(OgcApiDataset service, String geometryField, String bboxValue) {
        String[] bboxArray = bboxValue.split(",");

        if (bboxArray.length<4)
            throw new BadRequestException("The parameter 'bbox' has less than four values.");

        if (bboxArray.length>5)
            throw new BadRequestException("The parameter 'bbox' has more than four values.");

        EpsgCrs crs;
        try {
            String bboxCrs = bboxArray.length > 4 ? bboxArray[4] : null;
            crs = Optional.ofNullable(bboxCrs)
                    .map(EpsgCrs::new)
                    .orElse(OgcApiDatasetData.DEFAULT_CRS);
        } catch (NullPointerException e) {
            throw new BadRequestException("Error processing CRS of bounding box: '" + getBboxCrs(bboxArray) + "'");
        }

        try {
            double val1 = Double.valueOf(bboxArray[0]);
            double val2 = Double.valueOf(bboxArray[1]);
            double val3 = Double.valueOf(bboxArray[2]);
            double val4 = Double.valueOf(bboxArray[3]);
            if (bboxArray.length==4) {
                // check coordinate range of default CRS
                if (val1<-180 || val1>180 || val3<-180 || val3>180 || val2<-90 || val2>90 || val4<-90 || val4>90 || val2 > val4) {
                    // note that val1<val3 does not apply due to bboxes crossing the dateline
                    throw new BadRequestException("The coordinates of the bounding box '" + getBbox(bboxArray) + "' do not form a valid WGS 84 bounding box");
                }
            }
            BoundingBox bbox = new BoundingBox(val1, val2, val3, val4, crs);
            BoundingBox transformedBbox = service.transformBoundingBox(bbox);
            return String
                    .format(Locale.US,"BBOX(%s, %f, %f, %f, %f, '%s')", geometryField, transformedBbox.getXmin(), transformedBbox.getYmin(), transformedBbox.getXmax(), transformedBbox.getYmax(), transformedBbox.getEpsgCrs()
                    .getAsSimple());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Error processing the coordinates of the bounding box '" + getBbox(bboxArray) + "'");
        } catch (CrsTransformationException e) {
            throw new BadRequestException("Error transforming the bounding box '" + getBbox(bboxArray) + "' with CRS '" + getBboxCrs(bboxArray) +"'");
        } catch (NullPointerException e) {
            throw new BadRequestException("Error processing the bounding box '" + getBbox(bboxArray) + "' with CRS '" + getBboxCrs(bboxArray) +"'");
        }
    }

    private String getBbox(String[] bboxArray) {
        return String.format("%s,%s,%s,%s", bboxArray[0], bboxArray[1], bboxArray[2], bboxArray[3]);
    }

    private String getBboxCrs(String[] bboxArray) {
        return (bboxArray.length==5 ? bboxArray[4] : OgcApiDatasetData.DEFAULT_CRS.getAsUri());
    }

    private String timeToCql(String timeField, String timeValue) {
        // valid values: timestamp or time interval;
        // this includes open intervals indicated by ".." (see ISO 8601-2);
        // accept also unknown ("") with the same interpretation
        try {
            if (timeValue.matches("^"+TIMESTAMP_REGEX+"\\/"+TIMESTAMP_REGEX+"$")) {
                // the following parse accepts fully specified time intervals
                Interval fromIso8601Period = Interval.parse(timeValue);
                return String.format("%s DURING %s", timeField, fromIso8601Period);
            } else if (timeValue.matches("^"+TIMESTAMP_REGEX+"$")) {
                // a time instant
                Instant fromIso8601 = Instant.parse(timeValue);
                return String.format("%s TEQUALS %s", timeField, fromIso8601);
            } else if (timeValue.matches("^"+OPEN_REGEX+"\\/"+OPEN_REGEX+"$")) {
                // open start and end, nothing to do, all values match
                return null;
            } else if (timeValue.matches("^"+TIMESTAMP_REGEX+"\\/"+OPEN_REGEX+"$")) {
                // open end
                Instant fromIso8601 = Instant.parse(timeValue.substring(0,timeValue.indexOf("/")));
                return String.format("%s AFTER %s", timeField, fromIso8601.minusSeconds(1));
            } else if (timeValue.matches("^"+OPEN_REGEX+"\\/"+TIMESTAMP_REGEX+"$")) {
                // open start
                Instant fromIso8601 = Instant.parse(timeValue.substring(timeValue.indexOf("/")+1));
                return String.format("%s BEFORE %s", timeField, fromIso8601.plusSeconds(1));
            } else {
                LOGGER.error("TIME PARSER ERROR " + timeValue);
                throw new BadRequestException("Invalid value for query parameter '"+timeField+"'. Found: "+timeValue);
            }
        } catch (DateTimeParseException e) {
            LOGGER.error("TIME PARSER ERROR", e);
            throw new BadRequestException("Invalid value for query parameter '"+timeField+"'. Found: "+timeValue);
        }
    }

    private int parseLimit(int minimumPageSize, int defaultPageSize, int maxPageSize, String paramLimit) {
        int limit = defaultPageSize;
        if (paramLimit != null && !paramLimit.isEmpty()) {
            try {
                limit = Integer.parseInt(paramLimit);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid value for query parameter 'limit'. The value must be an integer. Found: "+paramLimit);
            }
            if (limit < Integer.max(minimumPageSize,1)) {
                throw new BadRequestException("Invalid value for query parameter 'limit'. The value must be at least " + minimumPageSize + ". Found: "+paramLimit);
            }
            if (limit > maxPageSize) {
                throw new BadRequestException("Invalid value for query parameter 'limit'. The value must be less than " + maxPageSize + ". Found: "+paramLimit);
            }
        }
        return limit;
    }

    private int parseOffset(String paramOffset) {
        int offset = 0;
        if (paramOffset != null && !paramOffset.isEmpty()) {
            try {
                offset = Integer.parseInt(paramOffset);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Invalid value for query parameter 'offset'. The value must be a non-negative integer. Found: "+paramOffset);
            }
            if (offset < 0) {
                throw new BadRequestException("Invalid value for query parameter 'offset'. The value must be a non-negative integer. Found: "+paramOffset);
            }
        }
        return offset;
    }
}
