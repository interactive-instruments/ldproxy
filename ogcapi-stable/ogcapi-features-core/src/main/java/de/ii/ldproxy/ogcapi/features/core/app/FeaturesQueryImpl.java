/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.Eq;
import de.ii.xtraplatform.cql.domain.Function;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.Intersects;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.TEquals;
import de.ii.xtraplatform.cql.domain.TOverlaps;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.DATETIME_INTERVAL_SEPARATOR;
import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.PARAMETER_BBOX;
import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.PARAMETER_DATETIME;


@Component
@Provides
@Instantiate
public class FeaturesQueryImpl implements FeaturesQuery {

    private static final Splitter ARRAY_SPLITTER = Splitter.on(',')
                                                           .trimResults();

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesQueryImpl.class);

    private final ExtensionRegistry extensionRegistry;
    private final CrsTransformerFactory crsTransformerFactory;
    private final FeaturesCoreProviders providers;
    private final Cql cql;

    public FeaturesQueryImpl(@Requires ExtensionRegistry extensionRegistry,
                             @Requires CrsTransformerFactory crsTransformerFactory,
                             @Requires FeaturesCoreProviders providers,
                             @Requires Cql cql) {
        this.extensionRegistry = extensionRegistry;
        this.crsTransformerFactory = crsTransformerFactory;
        this.providers = providers;
        this.cql = cql;
    }

    private String urldecode(String segment) {
        try {
            return URLDecoder.decode(segment, Charsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(String.format("Exception while decoding feature id '%s' for querying. Trying with the undecoded id.", segment));
            return segment;
        }
    }

    @Override
    public FeatureQuery requestToFeatureQuery(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                              FeaturesCoreConfiguration coreConfiguration,
                                              Map<String, String> parameters, List<OgcApiQueryParameter> allowedParameters,
                                              String featureId) {

        for (OgcApiQueryParameter parameter : allowedParameters) {
            parameters = parameter.transformParameters(collectionData, parameters, apiData);
        }

        final CqlFilter filter = CqlFilter.of(In.of(ScalarLiteral.of(urldecode(featureId))));

        final String collectionId = collectionData.getId();
        final String featureTypeId = apiData.getCollections()
                                      .get(collectionId)
                                      .getExtension(FeaturesCoreConfiguration.class)
                                      .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                      .orElse(collectionId);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(featureTypeId)
                                                                                .filter(filter)
                                                                                .crs(coreConfiguration.getDefaultEpsgCrs());

        for (OgcApiQueryParameter parameter : allowedParameters) {
            parameter.transformQuery(collectionData, queryBuilder, parameters, apiData);
        }

        return queryBuilder.build();
    }

    @Override
    public FeatureQuery requestToFeatureQuery(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                              FeaturesCoreConfiguration coreConfiguration,
                                              int minimumPageSize,
                                              int defaultPageSize, int maxPageSize, Map<String, String> parameters,
                                              List<OgcApiQueryParameter> allowedParameters) {
        final Map<String, String> filterableFields = collectionData.getExtension(FeaturesCoreConfiguration.class)
                                                                   .map(FeaturesCoreConfiguration::getAllFilterParameters)
                                                                   .orElse(ImmutableMap.of());

        Set<String> filterParameters = ImmutableSet.of();
        for (OgcApiQueryParameter parameter : allowedParameters) {
            filterParameters = parameter.getFilterParameters(filterParameters, apiData, collectionData.getId());
            parameters = parameter.transformParameters(collectionData, parameters, apiData);
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
        // TODO detailed checks should no longer be necessary
        final int limit = parseLimit(minimumPageSize, defaultPageSize, maxPageSize, parameters.get("limit"));
        final int offset = parseOffset(parameters.get("offset"));

        final String collectionId = collectionData.getId();
        String featureTypeId = apiData.getCollections()
                                      .get(collectionId)
                                      .getExtension(FeaturesCoreConfiguration.class)
                                      .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                      .orElse(collectionId);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(featureTypeId)
                                                                                .crs(coreConfiguration.getDefaultEpsgCrs())
                                                                                .limit(limit)
                                                                                .offset(offset)
                                                                                .hitsOnly(hitsOnly);

        for (OgcApiQueryParameter parameter : allowedParameters) {
            parameter.transformQuery(collectionData, queryBuilder, parameters, apiData);
        }

        if (!filters.isEmpty()) {
            Cql.Format cqlFormat = Cql.Format.TEXT;
            if (parameters.containsKey("filter-lang") && "cql-json".equals(parameters.get("filter-lang"))) {
                cqlFormat = Cql.Format.JSON;
            }
            Optional<CqlFilter> cql = getCQLFromFilters(filters, filterableFields, filterParameters, cqlFormat);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Filter: {}", cql);
            }

            queryBuilder.filter(cql);
        }

        return queryBuilder.build();

    }

    @Override
    public Optional<CqlFilter> getFilterFromQuery(Map<String, String> query, Map<String, String> filterableFields,
                                                  Set<String> filterParameters, Cql.Format cqlFormat) {

        Map<String, String> filtersFromQuery = getFiltersFromQuery(query, filterableFields, filterParameters);

        if (!filtersFromQuery.isEmpty()) {

            return getCQLFromFilters(filtersFromQuery, filterableFields, filterParameters, cqlFormat);
        }

        return Optional.empty();
    }

    private Map<String, String> getFiltersFromQuery(Map<String, String> query, Map<String, String> filterableFields,
                                                    Set<String> filterParameters) {

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

    private Optional<CqlFilter> getCQLFromFilters(Map<String, String> filters,
                                                  Map<String, String> filterableFields, Set<String> filterParameters,
                                                  Cql.Format cqlFormat) {

        List<CqlPredicate> predicates = filters.entrySet()
                                               .stream()
                                               .map(filter -> {
                                                   if (filter.getKey()
                                                             .equals(PARAMETER_BBOX)) {
                                                       return bboxToCql(filterableFields.get(filter.getKey()), filter.getValue());
                                                   }
                                                   if (filter.getKey()
                                                             .equals(PARAMETER_DATETIME)) {
                                                       return timeToCql(filterableFields.get(filter.getKey()), filter.getValue()).orElse(null);
                                                   }
                                                   if (filterParameters.contains(filter.getKey())) {
                                                       CqlPredicate cqlPredicate;
                                                       try {
                                                           cqlPredicate = cql.read(filter.getValue(), cqlFormat);
                                                       } catch (Throwable e) {
                                                           throw new IllegalArgumentException(String.format("The parameter '%s' is invalid.", filter.getKey()), e);
                                                       }

                                                       List<String> invalidProperties = cql.findInvalidProperties(cqlPredicate, filterableFields.keySet());

                                                       if (invalidProperties.isEmpty()) {
                                                           return cqlPredicate;
                                                       } else {
                                                           throw new IllegalArgumentException(String.format("The parameter '%s' is invalid. Unknown or forbidden properties used: %s.", filter.getKey(), String.join(", ", invalidProperties)));
                                                       }
                                                   }
                                                   if (filter.getValue()
                                                             .contains("*")) {
                                                       return CqlPredicate.of(Like.of(filterableFields.get(filter.getKey()), ScalarLiteral.of(filter.getValue()), "*"));
                                                   }

                                                   return CqlPredicate.of(Eq.of(filterableFields.get(filter.getKey()), ScalarLiteral.of(filter.getValue())));
                                               })
                                               .filter(Objects::nonNull)
                                               .collect(Collectors.toList());

        return predicates.isEmpty() ? Optional.empty() : Optional.of(predicates.size() == 1 ? CqlFilter.of(predicates.get(0)) : CqlFilter.of(And.of(predicates)));
    }

    private CqlPredicate bboxToCql(String geometryField, String bboxValue) {
        List<String> values = ARRAY_SPLITTER.splitToList(bboxValue);
        EpsgCrs sourceCrs = OgcCrs.CRS84;

        if (values.size() == 5) {
            try {
                sourceCrs = EpsgCrs.fromString(values.get(4));
                values = values.subList(0, 4);
            } catch (Throwable e) {
                //continue, fifth value is not from bbox-crs, as that is already validated in OgcApiParameterCrs
            }
        }

        if (values.size() != 4) {
            throw new IllegalArgumentException(String.format("The parameter 'bbox' is invalid: it must have exactly four values, found %d.", values.size()));
        }

        List<Double> coordinates;
        try {
            coordinates = values.stream()
                                .map(Double::valueOf)
                                .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("The parameter 'bbox' is invalid: the coordinates are not valid numbers '%s'", getBboxAsString(values)));
        }

        checkCoordinateRange(coordinates, sourceCrs);

        Envelope envelope = Envelope.of(coordinates.get(0), coordinates.get(1), coordinates.get(2), coordinates.get(3), sourceCrs);

        return CqlPredicate.of(Intersects.of(geometryField, SpatialLiteral.of(envelope)));
    }

    private void checkCoordinateRange(List<Double> coordinates, EpsgCrs crs) {
        if (Objects.equals(crs, OgcCrs.CRS84)) {
            double val1 = coordinates.get(0);
            double val2 = coordinates.get(1);
            double val3 = coordinates.get(2);
            double val4 = coordinates.get(3);
            // check coordinate range of default CRS
            if (val1 < -180 || val1 > 180 || val3 < -180 || val3 > 180 || val2 < -90 || val2 > 90 || val4 < -90 || val4 > 90 || val2 > val4) {
                // note that val1<val3 does not apply due to bboxes crossing the dateline
                throw new IllegalArgumentException(String.format("The parameter 'bbox' is invalid: the coordinates of the bounding box '%s' do not form a valid WGS 84 bounding box.", getCoordinatesAsString(coordinates)));
            }
        }
    }

    private String getBboxAsString(List<String> bboxArray) {
        return String.format("%s,%s,%s,%s", bboxArray.get(0), bboxArray.get(1), bboxArray.get(2), bboxArray.get(3));
    }

    private String getCoordinatesAsString(List<Double> bboxArray) {
        return String.format(Locale.US, "%f,%f,%f,%f", bboxArray.get(0), bboxArray.get(1), bboxArray.get(2), bboxArray.get(3));
    }

    private Optional<CqlPredicate> timeToCql(String timeField, String timeValue) {
        // valid values: timestamp or time interval;
        // this includes open intervals indicated by ".." (see ISO 8601-2);
        // accept also unknown ("") with the same interpretation

        TemporalLiteral temporalLiteral;
        try {
            temporalLiteral = TemporalLiteral.of(timeValue);
        } catch (Throwable e) {
            throw new IllegalArgumentException("Invalid value for query parameter '" + PARAMETER_DATETIME + "'.", e);
        }

        boolean atLeastOneInterval = timeField.contains(DATETIME_INTERVAL_SEPARATOR) || temporalLiteral.getType() == Interval.class;

        if (atLeastOneInterval) {
            Function intervalFunction = timeField.contains(DATETIME_INTERVAL_SEPARATOR)
                    ? Function.of("interval", Splitter.on(DATETIME_INTERVAL_SEPARATOR)
                                                      .splitToList(timeField)
                                                      .stream()
                                                      .map(Property::of)
                                                      .collect(Collectors.toList()))
                    : Function.of("interval", ImmutableList.of(Property.of(timeField), Property.of(timeField)));

            return Optional.of(CqlPredicate.of(TOverlaps.of(intervalFunction, temporalLiteral)));
        }

        return Optional.of(CqlPredicate.of(TEquals.of(timeField, temporalLiteral)));
    }

    private int parseLimit(int minimumPageSize, int defaultPageSize, int maxPageSize, String paramLimit) {
        int limit = defaultPageSize;
        if (paramLimit != null && !paramLimit.isEmpty()) {
            try {
                limit = Integer.parseInt(paramLimit);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid value for query parameter 'limit'. The value must be an integer. Found: " + paramLimit);
            }
            if (limit < Integer.max(minimumPageSize, 1)) {
                throw new IllegalArgumentException("Invalid value for query parameter 'limit'. The value must be at least " + minimumPageSize + ". Found: " + paramLimit);
            }
            if (limit > maxPageSize) {
                throw new IllegalArgumentException("Invalid value for query parameter 'limit'. The value must be less than " + maxPageSize + ". Found: " + paramLimit);
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
                throw new IllegalArgumentException("Invalid value for query parameter 'offset'. The value must be a non-negative integer. Found: " + paramOffset);
            }
            if (offset < 0) {
                throw new IllegalArgumentException("Invalid value for query parameter 'offset'. The value must be a non-negative integer. Found: " + paramOffset);
            }
        }
        return offset;
    }
}
