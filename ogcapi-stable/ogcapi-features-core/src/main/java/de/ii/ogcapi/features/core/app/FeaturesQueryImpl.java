/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.DATETIME_INTERVAL_SEPARATOR;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.PARAMETER_BBOX;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.PARAMETER_DATETIME;
import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ImmutableQueryValidationInputCoordinates;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.BooleanValue2;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.Eq;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.Interval;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.Or;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.SIntersects;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.TIntersects;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableBoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.web.domain.ETag;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Unit;
import org.kortforsyningen.proj.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeaturesQueryImpl implements FeaturesQuery {

  private static final Splitter ARRAY_SPLITTER = Splitter.on(',').trimResults();

  private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesQueryImpl.class);
  private static final double BUFFER_DEGREE = 0.00001;
  private static final double BUFFER_METRE = 10.0;

  private final ExtensionRegistry extensionRegistry;
  private final CrsTransformerFactory crsTransformerFactory;
  private final CrsInfo crsInfo;
  private final SchemaInfo schemaInfo;
  private final FeaturesCoreProviders providers;
  private final Cql cql;

  @Inject
  public FeaturesQueryImpl(
      ExtensionRegistry extensionRegistry,
      CrsTransformerFactory crsTransformerFactory,
      CrsInfo crsInfo,
      SchemaInfo schemaInfo,
      FeaturesCoreProviders providers,
      Cql cql) {
    this.extensionRegistry = extensionRegistry;
    this.crsTransformerFactory = crsTransformerFactory;
    this.crsInfo = crsInfo;
    this.schemaInfo = schemaInfo;
    this.providers = providers;
    this.cql = cql;
  }

  @Override
  public FeatureQuery requestToFeatureQuery(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      EpsgCrs defaultCrs,
      Map<String, Integer> coordinatePrecision,
      Map<String, String> parameters,
      List<OgcApiQueryParameter> allowedParameters,
      String featureId,
      Optional<ETag.Type> withEtag) {

    for (OgcApiQueryParameter parameter : allowedParameters) {
      parameters = parameter.transformParameters(collectionData, parameters, apiData);
    }

    final Cql2Expression filter = In.of(ScalarLiteral.of(featureId));

    final String collectionId = collectionData.getId();
    final String featureTypeId =
        apiData
            .getCollections()
            .get(collectionId)
            .getExtension(FeaturesCoreConfiguration.class)
            .map(cfg -> cfg.getFeatureType().orElse(collectionId))
            .orElse(collectionId);

    final ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureTypeId)
            .filter(filter)
            .returnsSingleFeature(true)
            .crs(defaultCrs)
            .eTag(withEtag);

    for (OgcApiQueryParameter parameter : allowedParameters) {
      parameter.transformQuery(collectionData, queryBuilder, parameters, apiData);
    }

    return processCoordinatePrecision(queryBuilder, coordinatePrecision).build();
  }

  @Override
  public FeatureQuery requestToFeatureQuery(
      OgcApi api,
      FeatureTypeConfigurationOgcApi collectionData,
      EpsgCrs defaultCrs,
      Map<String, Integer> coordinatePrecision,
      int minimumPageSize,
      int defaultPageSize,
      int maxPageSize,
      Map<String, String> parameters,
      List<OgcApiQueryParameter> allowedParameters) {
    final OgcApiDataV2 apiData = api.getData();
    final Map<String, String> filterableFields = getFilterableFields(apiData, collectionData);
    final Map<String, String> queryableTypes = getQueryableTypes(apiData, collectionData);

    Set<String> filterParameters = ImmutableSet.of();
    for (OgcApiQueryParameter parameter : allowedParameters) {
      filterParameters =
          parameter.getFilterParameters(filterParameters, apiData, collectionData.getId());
      parameters = parameter.transformParameters(collectionData, parameters, apiData);
    }

    final Map<String, String> filters =
        getFiltersFromQuery(parameters, filterableFields, filterParameters);

    boolean hitsOnly =
        parameters.containsKey("resultType")
            && parameters.get("resultType").toLowerCase().equals("hits");

    /**
     * NOTE: OGC API and ldproxy do not use the HTTP "Range" header for limit/offset for the
     * following reasons: - We need to support some non-header mechanism anyhow to be able to mint
     * URIs (links) to pages / partial responses. - A request without a range header cannot return
     * 206, so there is no way that a server could have a default limit. I.e. any request to a
     * collection without a range header would have to return all features and it is important to
     * enable servers to have a default page limit. - There is no real need for multipart responses,
     * but servers would have to support requests that lead to 206 multipart responses. - Developers
     * do not seem to expect such an approach and since it uses a custom range unit anyhow (i.e. not
     * bytes), it is unclear how much value it brings. Probably consistent with this: I have not
     * seen much of range headers in Web APIs for paging.
     */
    // TODO detailed checks should no longer be necessary
    final int limit =
        parseLimit(minimumPageSize, defaultPageSize, maxPageSize, parameters.get("limit"));
    final int offset = parseOffset(parameters.get("offset"));

    final String collectionId = collectionData.getId();
    String featureTypeId =
        apiData
            .getCollections()
            .get(collectionId)
            .getExtension(FeaturesCoreConfiguration.class)
            .map(cfg -> cfg.getFeatureType().orElse(collectionId))
            .orElse(collectionId);

    final ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureTypeId)
            .crs(defaultCrs)
            .limit(limit)
            .offset(offset)
            .hitsOnly(hitsOnly);

    for (OgcApiQueryParameter parameter : allowedParameters) {
      parameter.transformQuery(queryBuilder, parameters, apiData);
      parameter.transformQuery(collectionData, queryBuilder, parameters, apiData);
    }

    if (!filters.isEmpty()) {
      Cql.Format cqlFormat = Cql.Format.TEXT;
      EpsgCrs crs = OgcCrs.CRS84;
      if (parameters.containsKey("filter-lang")
          && "cql2-json".equals(parameters.get("filter-lang"))) {
        cqlFormat = Cql.Format.JSON;
      }
      if (parameters.containsKey("filter-crs")) {
        crs = EpsgCrs.fromString(parameters.get("filter-crs"));
      }
      QueryValidationInputCoordinates queryValidationInput;
      if (apiData
          .getExtension(FeaturesCoreConfiguration.class, collectionId)
          .map(FeaturesCoreConfiguration::getValidateCoordinatesInQueries)
          .orElse(false)) {
        queryValidationInput =
            new ImmutableQueryValidationInputCoordinates.Builder()
                .enabled(true)
                .bboxCrs(
                    parameters.containsKey("bbox-crs")
                        ? EpsgCrs.fromString(parameters.get("bbox-crs"))
                        : OgcCrs.CRS84)
                .filterCrs(crs)
                .nativeCrs(
                    providers
                        .getFeatureProvider(apiData)
                        .map(FeatureProvider2::getData)
                        .flatMap(FeatureProviderDataV2::getNativeCrs))
                .build();
      } else {
        queryValidationInput = QueryValidationInputCoordinates.none();
      }
      // We are using the spatial extent of the data to avoid
      // coordinate transformation errors when a bbox parameter
      // is completely outside of the domain of a projected CRS
      // in which the data is stored. Using the minimal bounding
      // box can lead to surprising results in particular with
      // point features and queries in other CRSs where features
      // on the boundary of the spatial extent are suddenly no
      // longer included in the result. For the purpose of the
      // filter, we do not need the minimal bounding rectangle,
      // but we can use a small buffer to avoid those issues.
      EpsgCrs bboxCrs =
          parameters.containsKey("bbox-crs")
              ? EpsgCrs.fromString(parameters.get("bbox-crs"))
              : OgcCrs.CRS84;
      double buffer = getBuffer(bboxCrs);
      Optional<BoundingBox> spatialExtentForBboxParameter =
          api.getSpatialExtent(collectionId, bboxCrs)
              .map(
                  bbox ->
                      new ImmutableBoundingBox.Builder()
                          .from(bbox)
                          .xmin(bbox.getXmin() - buffer)
                          .xmax(bbox.getXmax() + buffer)
                          .ymin(bbox.getYmin() - buffer)
                          .ymax(bbox.getYmax() + buffer)
                          .build());
      Optional<Cql2Expression> cql =
          getCQLFromFilters(
              filters,
              filterableFields,
              filterParameters,
              queryableTypes,
              cqlFormat,
              crs,
              queryValidationInput,
              spatialExtentForBboxParameter);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Filter: {}", cql);
      }

      queryBuilder.filter(cql);
    }

    return processCoordinatePrecision(queryBuilder, coordinatePrecision).build();
  }

  private double getBuffer(EpsgCrs crs) {
    List<Unit<?>> units = crsInfo.getAxisUnits(crs);
    if (!units.isEmpty()) {
      return Units.METRE.equals(units.get(0)) ? BUFFER_METRE : BUFFER_DEGREE;
    }
    // fallback to meters
    return BUFFER_METRE;
  }

  public FeatureQuery requestToBareFeatureQuery(
      OgcApiDataV2 apiData,
      String featureTypeId,
      EpsgCrs defaultCrs,
      Map<String, Integer> coordinatePrecision,
      int minimumPageSize,
      int defaultPageSize,
      int maxPageSize,
      Map<String, String> parameters,
      List<OgcApiQueryParameter> allowedParameters) {

    // TODO detailed checks should no longer be necessary
    final int limit =
        parseLimit(minimumPageSize, defaultPageSize, maxPageSize, parameters.get("limit"));
    final int offset = parseOffset(parameters.get("offset"));

    final ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureTypeId)
            .crs(defaultCrs)
            .limit(limit)
            .offset(offset)
            .hitsOnly(false);

    for (OgcApiQueryParameter parameter : allowedParameters) {
      parameter.transformQuery(queryBuilder, parameters, apiData);
    }

    return processCoordinatePrecision(queryBuilder, coordinatePrecision).build();
  }

  @Override
  public Map<String, String> getFilterableFields(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    Map<String, String> queryables =
        new LinkedHashMap<>(
            collectionData
                .getExtension(FeaturesCoreConfiguration.class)
                .map(FeaturesCoreConfiguration::getAllFilterParameters)
                .orElse(ImmutableMap.of()));

    Optional<FeatureSchema> featureSchema = providers.getFeatureSchema(apiData, collectionData);

    featureSchema
        .flatMap(SchemaBase::getPrimaryGeometry)
        .ifPresent(geometry -> queryables.put(PARAMETER_BBOX, geometry.getFullPathAsString()));

    featureSchema
        .flatMap(SchemaBase::getPrimaryInterval)
        .ifPresentOrElse(
            interval ->
                queryables.put(
                    PARAMETER_DATETIME,
                    String.format(
                        "%s%s%s",
                        interval.first().getFullPathAsString(),
                        DATETIME_INTERVAL_SEPARATOR,
                        interval.second().getFullPathAsString())),
            () ->
                featureSchema
                    .flatMap(SchemaBase::getPrimaryInstant)
                    .ifPresent(
                        instant ->
                            queryables.put(PARAMETER_DATETIME, instant.getFullPathAsString())));

    return ImmutableMap.<String, String>builder().putAll(queryables).build();
  }

  @Override
  public Map<String, String> getQueryableTypes(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {

    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();

    Optional<FeatureSchema> featureSchema = providers.getFeatureSchema(apiData, collectionData);
    if (featureSchema.isPresent()) {
      List<String> queryables =
          collectionData
              .getExtension(FeaturesCoreConfiguration.class)
              .flatMap(FeaturesCoreConfiguration::getQueryables)
              .map(FeaturesCollectionQueryables::getAll)
              .orElse(ImmutableList.of());

      Map<String, SchemaBase.Type> propertyMap =
          schemaInfo.getPropertyTypes(featureSchema.get(), false);

      queryables.forEach(
          queryable ->
              Optional.ofNullable(propertyMap.get(queryable))
                  .ifPresent(type -> builder.put(queryable, type.name())));

      // also add the ID property
      featureSchema.get().getProperties().stream()
          .filter(p -> p.getRole().isPresent() && p.getRole().get() == SchemaBase.Role.ID)
          .findFirst()
          .map(FeatureSchema::getType)
          .ifPresent(type -> builder.put(ID_PLACEHOLDER, type.name()));
    }

    return builder.build();
  }

  @Override
  public Optional<Cql2Expression> getFilterFromQuery(
      Map<String, String> query,
      Map<String, String> filterableFields,
      Set<String> filterParameters,
      Map<String, String> queryableTypes,
      Format cqlFormat) {

    Map<String, String> filtersFromQuery =
        getFiltersFromQuery(query, filterableFields, filterParameters);

    if (!filtersFromQuery.isEmpty()) {

      return getCQLFromFilters(
          filtersFromQuery,
          filterableFields,
          filterParameters,
          queryableTypes,
          cqlFormat,
          OgcCrs.CRS84,
          QueryValidationInputCoordinates.none(),
          Optional.empty());
    }

    return Optional.empty();
  }

  private Map<String, String> getFiltersFromQuery(
      Map<String, String> query,
      Map<String, String> filterableFields,
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

  private Optional<Cql2Expression> getCQLFromFilters(
      Map<String, String> filters,
      Map<String, String> filterableFields,
      Set<String> filterParameters,
      Map<String, String> queryableTypes,
      Cql.Format cqlFormat,
      EpsgCrs crs,
      QueryValidationInputCoordinates queryValidationInput,
      Optional<BoundingBox> bbox) {

    List<Cql2Expression> predicates =
        filters.entrySet().stream()
            .map(
                filter -> {
                  if (filter.getKey().equals(PARAMETER_BBOX)) {
                    if (filterableFields
                        .get(filter.getKey())
                        .equals(FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE)) return null;

                    Cql2Expression cqlPredicate =
                        bboxToCql(filterableFields.get(filter.getKey()), filter.getValue(), bbox);

                    if (queryValidationInput.getEnabled()) {
                      queryValidationInput
                          .getBboxCrs()
                          .ifPresent(
                              bboxCrs ->
                                  cql.checkCoordinates(
                                      cqlPredicate,
                                      crsTransformerFactory,
                                      crsInfo,
                                      bboxCrs,
                                      queryValidationInput.getNativeCrs().orElse(null)));
                    }

                    return cqlPredicate;
                  }
                  if (filter.getKey().equals(PARAMETER_DATETIME)) {
                    if (filterableFields
                        .get(filter.getKey())
                        .equals(FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE)) return null;
                    return timeToCql(filterableFields.get(filter.getKey()), filter.getValue())
                        .orElse(null);
                  }
                  if (filterParameters.contains(filter.getKey())) {
                    Cql2Expression cqlPredicate;
                    try {
                      cqlPredicate = cql.read(filter.getValue(), cqlFormat, crs);
                    } catch (Throwable e) {
                      throw new IllegalArgumentException(
                          String.format("The parameter '%s' is invalid", filter.getKey()), e);
                    }

                    List<String> invalidProperties =
                        cql.findInvalidProperties(cqlPredicate, filterableFields.keySet());

                    if (!invalidProperties.isEmpty()) {
                      throw new IllegalArgumentException(
                          String.format(
                              "The parameter '%s' is invalid. Unknown or forbidden properties used: %s.",
                              filter.getKey(), String.join(", ", invalidProperties)));
                    }
                    // will throw an error
                    cql.checkTypes(cqlPredicate, queryableTypes);

                    if (queryValidationInput.getEnabled()) {
                      queryValidationInput
                          .getFilterCrs()
                          .ifPresent(
                              filterCrs ->
                                  cql.checkCoordinates(
                                      cqlPredicate,
                                      crsTransformerFactory,
                                      crsInfo,
                                      filterCrs,
                                      queryValidationInput.getNativeCrs().orElse(null)));
                    }

                    return cqlPredicate;
                  }
                  if (filter.getValue().contains("*")) {
                    return Like.of(
                        filterableFields.get(filter.getKey()),
                        ScalarLiteral.of(filter.getValue().replace("*", "%")));
                  }

                  return Eq.of(
                      filterableFields.get(filter.getKey()), ScalarLiteral.of(filter.getValue()));
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return predicates.isEmpty()
        ? Optional.empty()
        : Optional.of(predicates.size() == 1 ? predicates.get(0) : And.of(predicates));
  }

  private Cql2Expression bboxToCql(
      String geometryField, String bboxValue, Optional<BoundingBox> optionalSpatialExtent) {
    List<String> values = ARRAY_SPLITTER.splitToList(bboxValue);
    EpsgCrs sourceCrs = OgcCrs.CRS84;

    if (values.size() == 5) {
      try {
        sourceCrs = EpsgCrs.fromString(values.get(4));
        values = values.subList(0, 4);
      } catch (Throwable e) {
        // continue, fifth value is not from bbox-crs, as that is already validated in
        // OgcApiParameterCrs
      }
    }

    if (values.size() != 4) {
      throw new IllegalArgumentException(
          String.format(
              "The parameter 'bbox' is invalid: it must have exactly four values, found %d.",
              values.size()));
    }

    List<Double> bboxCoordinates;
    try {
      bboxCoordinates = values.stream().map(Double::valueOf).collect(Collectors.toList());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          String.format(
              "The parameter 'bbox' is invalid: the coordinates are not valid numbers '%s'",
              getBboxAsString(values)));
    }

    Envelope envelope =
        Envelope.of(
            bboxCoordinates.get(0),
            bboxCoordinates.get(1),
            bboxCoordinates.get(2),
            bboxCoordinates.get(3),
            sourceCrs);

    if (optionalSpatialExtent.isPresent()
        && optionalSpatialExtent.get().getEpsgCrs().equals(sourceCrs)) {
      BoundingBox spatialExtent = optionalSpatialExtent.get();
      if (bboxCoordinates.get(0) > spatialExtent.getXmax()
          || bboxCoordinates.get(1) > spatialExtent.getYmax()
          || bboxCoordinates.get(2) < spatialExtent.getXmin()
          || bboxCoordinates.get(3) < spatialExtent.getYmin()) {
        // bounding box does not overlap with spatial extent of the data, no match;
        // detecting this is also important to avoid errors when converting bbox coordinates
        // that are outside of the range of the native CRS
        return BooleanValue2.of(false);
      } else if (bboxCoordinates.get(0) < spatialExtent.getXmin()
          || bboxCoordinates.get(1) < spatialExtent.getYmin()
          || bboxCoordinates.get(2) > spatialExtent.getXmax()
          || bboxCoordinates.get(3) > spatialExtent.getYmax()) {
        // bounding box extends beyond the spatial extent of the data, reduce to overlapping area;
        // again, detecting this is important to avoid errors when converting bbox coordinates
        // that are outside of the range of the native CRS
        envelope =
            Envelope.of(
                Math.max(bboxCoordinates.get(0), spatialExtent.getXmin()),
                Math.max(bboxCoordinates.get(1), spatialExtent.getYmin()),
                Math.min(bboxCoordinates.get(2), spatialExtent.getXmax()),
                Math.min(bboxCoordinates.get(3), spatialExtent.getYmax()),
                sourceCrs);
      }
    }

    return SIntersects.of(Property.of(geometryField), SpatialLiteral.of(envelope));
  }

  private String getBboxAsString(List<String> bboxArray) {
    return String.format(
        "%s,%s,%s,%s", bboxArray.get(0), bboxArray.get(1), bboxArray.get(2), bboxArray.get(3));
  }

  private Optional<Cql2Expression> timeToCql(String timeField, String timeValue) {
    // valid values: timestamp or time interval;
    // this includes open intervals indicated by ".." (see ISO 8601-2);
    // accept also unknown ("") with the same interpretation;
    // in addition, "now" is accepted for the current time

    TemporalLiteral temporalLiteral;
    try {
      if (timeValue.contains(DATETIME_INTERVAL_SEPARATOR)) {
        temporalLiteral =
            TemporalLiteral.of(Splitter.on(DATETIME_INTERVAL_SEPARATOR).splitToList(timeValue));
      } else {
        temporalLiteral = TemporalLiteral.of(timeValue);
      }
    } catch (Throwable e) {
      throw new IllegalArgumentException(
          "Invalid value for query parameter '" + PARAMETER_DATETIME + "'.", e);
    }

    if (timeField.contains(DATETIME_INTERVAL_SEPARATOR)) {
      Interval interval =
          Interval.of(
              Splitter.on(DATETIME_INTERVAL_SEPARATOR).splitToList(timeField).stream()
                  .map(Property::of)
                  .collect(Collectors.toList()));

      return Optional.of(TIntersects.of(interval, temporalLiteral));
    }

    return Optional.of(TIntersects.of(Property.of(timeField), temporalLiteral));
  }

  private Optional<Cql2Expression> qToCql(List<String> qFields, String qValue) {
    // predicate that ORs LIKE operators of all values;
    List<String> qValues = Splitter.on(",").trimResults().splitToList(qValue);

    return qFields.size() > 1 || qValues.size() > 1
        ? Optional.of(
            Or.of(
                qFields.stream()
                    .map(
                        qField ->
                            qValues.stream()
                                .map(word -> Like.of(qField, ScalarLiteral.of("%" + word + "%")))
                                .collect(Collectors.toUnmodifiableList()))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableList())))
        : Optional.of(Like.of(qFields.get(0), ScalarLiteral.of("%" + qValues.get(0) + "%")));
  }

  private int parseLimit(
      int minimumPageSize, int defaultPageSize, int maxPageSize, String paramLimit) {
    int limit = defaultPageSize;
    if (paramLimit != null && !paramLimit.isEmpty()) {
      try {
        limit = Integer.parseInt(paramLimit);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException(
            "Invalid value for query parameter 'limit'. The value must be an integer. Found: "
                + paramLimit);
      }
      if (limit < Integer.max(minimumPageSize, 1)) {
        throw new IllegalArgumentException(
            "Invalid value for query parameter 'limit'. The value must be at least "
                + minimumPageSize
                + ". Found: "
                + paramLimit);
      }
      if (limit > maxPageSize) {
        throw new IllegalArgumentException(
            "Invalid value for query parameter 'limit'. The value must be less than "
                + maxPageSize
                + ". Found: "
                + paramLimit);
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
        throw new IllegalArgumentException(
            "Invalid value for query parameter 'offset'. The value must be a non-negative integer. Found: "
                + paramOffset);
      }
      if (offset < 0) {
        throw new IllegalArgumentException(
            "Invalid value for query parameter 'offset'. The value must be a non-negative integer. Found: "
                + paramOffset);
      }
    }
    return offset;
  }

  private ImmutableFeatureQuery.Builder processCoordinatePrecision(
      ImmutableFeatureQuery.Builder queryBuilder, Map<String, Integer> coordinatePrecision) {
    // check, if we need to add a precision value; for this we need the target CRS,
    // so we need to build the query to get the CRS
    ImmutableFeatureQuery query = queryBuilder.build();
    if (!coordinatePrecision.isEmpty() && query.getCrs().isPresent()) {
      Integer precision;
      List<Unit<?>> units = crsInfo.getAxisUnits(query.getCrs().get());
      ImmutableList.Builder<Integer> precisionListBuilder = new ImmutableList.Builder<>();
      for (Unit<?> unit : units) {
        if (unit.equals(Units.METRE)) {
          precision = coordinatePrecision.get("meter");
          if (Objects.isNull(precision)) precision = coordinatePrecision.get("metre");
        } else if (unit.equals(Units.DEGREE)) {
          precision = coordinatePrecision.get("degree");
        } else {
          LOGGER.debug(
              "Coordinate precision could not be set, unrecognised unit found: '{}'.",
              unit.getName());
          return queryBuilder;
        }
        precisionListBuilder.add(precision);
      }
      List<Integer> precisionList = precisionListBuilder.build();
      if (!precisionList.isEmpty()) {
        queryBuilder.geometryPrecision(precisionList);
      }
    }
    return queryBuilder;
  }
}
