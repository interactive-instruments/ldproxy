/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.core;

import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3ParameterExtension;
import de.ii.ldproxy.wfs3.api.Wfs3Service;
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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;


/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3Query {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3Query.class);

    private final Wfs3ExtensionRegistry wfs3ExtensionRegistry;

    public Wfs3Query(@Requires Wfs3ExtensionRegistry wfs3ExtensionRegistry) {
        this.wfs3ExtensionRegistry = wfs3ExtensionRegistry;
    }

    public FeatureQuery requestToFeatureQuery(Wfs3Service service, String featureType, Map<String, String> parameters, String featureId) {

        for (Wfs3ParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(Wfs3ParameterExtension.class)) {
            parameters = parameterExtension.transformParameters(service.getData()
                                                                       .getFeatureTypes()
                                                                       .get(featureType), parameters,service.getData());
        }

        final String filter = String.format("IN ('%s')", featureId);


        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(featureType)
                                                                                .filter(filter);

        for (Wfs3ParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(Wfs3ParameterExtension.class)) {
            parameterExtension.transformQuery(service.getData()
                                                     .getFeatureTypes()
                                                     .get(featureType), queryBuilder, parameters,service.getData());
        }

        return queryBuilder.build();
    }

    public FeatureQuery requestToFeatureQuery(Wfs3Service service, String featureType, String range, Map<String, String> parameters) {

        final Map<String, String> filterableFields = service.getData()
                                                            .getFilterableFieldsForFeatureType(featureType);

        for (Wfs3ParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(Wfs3ParameterExtension.class)) {
            parameters = parameterExtension.transformParameters(service.getData()
                                                                       .getFeatureTypes()
                                                                       .get(featureType), parameters,service.getData());
        }


        final Map<String, String> filters = getFiltersFromQuery(parameters, filterableFields);

        boolean hitsOnly = parameters.containsKey("resultType") && parameters.get("resultType")
                                                                             .toLowerCase()
                                                                             .equals("hits");


        final int[] countFrom = RangeHeader.parseRange(range);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(featureType)
                                                                                .limit(countFrom[0])
                                                                                .offset(countFrom[1])
                                                                                .hitsOnly(hitsOnly);

        for (Wfs3ParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(Wfs3ParameterExtension.class)) {
            parameterExtension.transformQuery(service.getData()
                                                     .getFeatureTypes()
                                                     .get(featureType), queryBuilder, parameters,service.getData());
        }


        if (!filters.isEmpty()) {
            String cql = getCQLFromFilters(service, filters, filterableFields);
            LOGGER.debug("CQL {}", cql);
            queryBuilder.filter(cql);
        }

        return queryBuilder.build();

    }

    private Map<String, String> getFiltersFromQuery(Map<String, String> query, Map<String, String> filterableFields) {

        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterableFields.containsKey(filterKey.toLowerCase())) {
                String filterValue = query.get(filterKey);
                filters.put(filterKey.toLowerCase(), filterValue);
            }
        }

        return filters;
    }

    private String getCQLFromFilters(Wfs3Service service, Map<String, String> filters, Map<String, String> filterableFields) {
        return filters.entrySet()
                      .stream()
                      .map(f -> {
                          if (f.getKey()
                               .equals("bbox")) {
                              return bboxToCql(service, filterableFields.get(f.getKey()), f.getValue());
                          }
                          if (f.getKey()
                               .equals("time")) {
                              return timeToCql(filterableFields.get(f.getKey()), f.getValue());
                          }
                          if (f.getValue()
                               .contains("*")) {
                              return String.format("%s LIKE '%s'", filterableFields.get(f.getKey()), f.getValue());
                          }

                          return String.format("%s = '%s'", filterableFields.get(f.getKey()), f.getValue());
                      })
                      .collect(Collectors.joining(" AND "));
    }

    private String bboxToCql(Wfs3Service service, String geometryField, String bboxValue) {
        String[] bboxArray = bboxValue.split(",");

        String bboxCrs = bboxArray.length > 4 ? bboxArray[4] : null;
        EpsgCrs crs = Optional.ofNullable(bboxCrs)
                              .map(EpsgCrs::new)
                              .orElse(DEFAULT_CRS);

        BoundingBox bbox = new BoundingBox(Double.valueOf(bboxArray[0]), Double.valueOf(bboxArray[1]), Double.valueOf(bboxArray[2]), Double.valueOf(bboxArray[3]), crs);
        BoundingBox transformedBbox = null;
        try {
            transformedBbox = service.transformBoundingBox(bbox);
        } catch (CrsTransformationException e) {
            LOGGER.error("Error transforming bbox");
            transformedBbox = bbox;
        }

        return String.format(Locale.US, "BBOX(%s, %.3f, %.3f, %.3f, %.3f, '%s')", geometryField, transformedBbox.getXmin(), transformedBbox.getYmin(), transformedBbox.getXmax(), transformedBbox.getYmax(), transformedBbox.getEpsgCrs()
                                                                                                                                                                          .getAsSimple());
    }

    private String timeToCql(String timeField, String timeValue) {
        try {
            Interval fromIso8601Period = Interval.parse(timeValue);
            return String.format("%s DURING %s", timeField, fromIso8601Period);
        } catch (DateTimeParseException ignore) {
            try {
                Instant fromIso8601 = Instant.parse(timeValue);
                return String.format("%s TEQUALS %s", timeField, fromIso8601);
            } catch (DateTimeParseException e) {
                LOGGER.debug("TIME PARSER ERROR", e);
                throw new BadRequestException();
            }
        }
    }
}
