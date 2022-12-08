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
import de.ii.ogcapi.features.core.domain.FeatureQueryTransformer;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ImmutableQueryValidationInputCoordinates;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
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
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass", "PMD.CyclomaticComplexity"})
public class FeaturesQueryImpl implements FeaturesQuery {

  private static final Splitter ARRAY_SPLITTER = Splitter.on(',').trimResults();
  private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesQueryImpl.class);
  private static final double BUFFER_DEGREE = 0.000_01;
  private static final double BUFFER_METRE = 10.0;
  public static final String RESULT_TYPE = "resultType";
  public static final String HITS = "hits";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";
  public static final String FILTER_LANG = "filter-lang";
  public static final String CQL2_JSON = "cql2-json";
  public static final String FILTER_CRS = "filter-crs";
  public static final String BBOX_CRS = "bbox-crs";
  public static final String ASTERISK = "*";
  public static final String PERCENT = "%";
  public static final String METER = "meter";
  public static final String METRE = "metre";
  public static final String DEGREE = "degree";

  private final CrsTransformerFactory crsTransformerFactory;
  private final CrsInfo crsInfo;
  private final SchemaInfo schemaInfo;
  private final FeaturesCoreProviders providers;
  private final Cql cql;

  @Inject
  public FeaturesQueryImpl(
      CrsTransformerFactory crsTransformerFactory,
      CrsInfo crsInfo,
      SchemaInfo schemaInfo,
      FeaturesCoreProviders providers,
      Cql cql) {
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

    Map<String, String> effectiveParameters = parameters;
    for (OgcApiQueryParameter parameter : allowedParameters) {
      effectiveParameters =
          parameter.transformParameters(collectionData, effectiveParameters, apiData);
    }

    final Cql2Expression filter = In.of(ScalarLiteral.of(featureId));

    final String featureTypeId =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .flatMap(FeaturesCoreConfiguration::getFeatureType)
            .orElse(collectionData.getId());

    final ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureTypeId)
            .filter(filter)
            .returnsSingleFeature(true)
            .crs(defaultCrs)
            .eTag(withEtag);

    for (OgcApiQueryParameter parameter : allowedParameters) {
      if (parameter instanceof FeatureQueryTransformer) {
        ((FeatureQueryTransformer) parameter)
            .transformQuery(queryBuilder, effectiveParameters, apiData, collectionData);
      }
    }

    return processCoordinatePrecision(queryBuilder, coordinatePrecision).build();
  }

  @Override
  public FeatureQuery requestToFeaturesQuery(
      OgcApi api,
      FeatureTypeConfigurationOgcApi collectionData,
      EpsgCrs defaultCrs,
      Map<String, Integer> coordinatePrecision,
      int defaultPageSize,
      Map<String, String> parameters,
      List<OgcApiQueryParameter> allowedParameters) {
    final OgcApiDataV2 apiData = api.getData();
    final Map<String, String> filterableFields = getFilterableFields(apiData, collectionData);
    final Map<String, String> queryableTypes = getQueryableTypes(apiData, collectionData);

    Set<String> filterParameters = ImmutableSet.of();
    Map<String, String> effectiveParameters = parameters;
    for (OgcApiQueryParameter parameter : allowedParameters) {
      filterParameters =
          parameter.getFilterParameters(filterParameters, apiData, collectionData.getId());
      effectiveParameters =
          parameter.transformParameters(collectionData, effectiveParameters, apiData);
    }

    final Map<String, String> filters =
        getFiltersFromQuery(effectiveParameters, filterableFields, filterParameters);

    boolean hitsOnly =
        parameters.containsKey(RESULT_TYPE)
            && HITS.equalsIgnoreCase(effectiveParameters.get(RESULT_TYPE));

    /*
     * NOTE: OGC API and ldproxy do not use the HTTP "Range" header for limit/offset for the
     * following reasons:
     *
     * <p>- We need to support some non-header mechanism anyhow to be able to mint URIs (links) to
     * pages / partial responses.
     *
     * <p>- A request without a range header cannot return 206, so there is no way that a server
     * could have a default limit. I.e. any request to a collection without a range header would
     * have to return all features and it is important to enable servers to have a default page
     * limit.
     *
     * <p>- There is no real need for multipart responses, but servers would have to support
     * requests that lead to 206 multipart responses.
     *
     * <p>- Developers do not seem to expect such an approach and since it uses a custom range unit
     * anyhow (i.e. not bytes), it is unclear how much value it brings. Probably consistent with
     * this: I have not seen much of range headers in Web APIs for paging.
     */
    final int limit =
        effectiveParameters.containsKey(LIMIT)
            ? Integer.parseInt(effectiveParameters.get(LIMIT))
            : defaultPageSize;
    final int offset =
        effectiveParameters.containsKey(OFFSET)
            ? Integer.parseInt(effectiveParameters.get(OFFSET))
            : 0;

    final ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(getFeatureTypeId(collectionData))
            .crs(defaultCrs)
            .limit(limit)
            .offset(offset)
            .hitsOnly(hitsOnly);

    processParameters(collectionData, parameters, allowedParameters, apiData, queryBuilder);

    processFilters(
        api,
        collectionData,
        parameters,
        filterableFields,
        queryableTypes,
        filterParameters,
        filters,
        queryBuilder);

    return processCoordinatePrecision(queryBuilder, coordinatePrecision).build();
  }

  private void processFilters(
      OgcApi api,
      FeatureTypeConfigurationOgcApi collectionData,
      Map<String, String> parameters,
      Map<String, String> filterableFields,
      Map<String, String> queryableTypes,
      Set<String> filterParameters,
      Map<String, String> filters,
      ImmutableFeatureQuery.Builder queryBuilder) {
    if (!filters.isEmpty()) {
      OgcApiDataV2 apiData = api.getData();

      Format cqlFormat =
          parameters.containsKey(FILTER_LANG) && CQL2_JSON.equals(parameters.get(FILTER_LANG))
              ? Format.JSON
              : Format.TEXT;

      EpsgCrs crs =
          parameters.containsKey(FILTER_CRS)
              ? EpsgCrs.fromString(parameters.get(FILTER_CRS))
              : OgcCrs.CRS84;

      QueryValidationInputCoordinates queryValidationInput =
          apiData
                  .getExtension(FeaturesCoreConfiguration.class, collectionData.getId())
                  .map(FeaturesCoreConfiguration::getValidateCoordinatesInQueries)
                  .orElse(false)
              ? new ImmutableQueryValidationInputCoordinates.Builder()
                  .enabled(true)
                  .bboxCrs(
                      parameters.containsKey(BBOX_CRS)
                          ? EpsgCrs.fromString(parameters.get(BBOX_CRS))
                          : OgcCrs.CRS84)
                  .filterCrs(crs)
                  .nativeCrs(
                      providers
                          .getFeatureProvider(apiData, collectionData)
                          .map(FeatureProvider2::getData)
                          .flatMap(FeatureProviderDataV2::getNativeCrs))
                  .build()
              : QueryValidationInputCoordinates.none();

      Optional<Cql2Expression> cql =
          getCQLFromFilters(
              filters,
              filterableFields,
              filterParameters,
              queryableTypes,
              cqlFormat,
              crs,
              queryValidationInput,
              getBoundingBox(api, parameters, collectionData.getId()));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Filter: {}", cql);
      }

      queryBuilder.filter(cql);
    }
  }

  private Optional<BoundingBox> getBoundingBox(
      OgcApi api, Map<String, String> parameters, String collectionId) {
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
        parameters.containsKey(BBOX_CRS)
            ? EpsgCrs.fromString(parameters.get(BBOX_CRS))
            : OgcCrs.CRS84;
    List<Unit<?>> units = crsInfo.getAxisUnits(bboxCrs);
    double buffer =
        !units.isEmpty() && Units.DEGREE.equals(units.get(0)) ? BUFFER_DEGREE : BUFFER_METRE;
    return api.getSpatialExtent(collectionId, bboxCrs)
        .map(
            bbox ->
                new ImmutableBoundingBox.Builder()
                    .from(bbox)
                    .xmin(bbox.getXmin() - buffer)
                    .xmax(bbox.getXmax() + buffer)
                    .ymin(bbox.getYmin() - buffer)
                    .ymax(bbox.getYmax() + buffer)
                    .build());
  }

  private String getFeatureTypeId(FeatureTypeConfigurationOgcApi collection) {
    return collection
        .getExtension(FeaturesCoreConfiguration.class)
        .flatMap(FeaturesCoreConfiguration::getFeatureType)
        .orElse(collection.getId());
  }

  private void processParameters(
      FeatureTypeConfigurationOgcApi collectionData,
      Map<String, String> parameters,
      List<OgcApiQueryParameter> allowedParameters,
      OgcApiDataV2 apiData,
      ImmutableFeatureQuery.Builder queryBuilder) {
    for (OgcApiQueryParameter parameter : allowedParameters) {
      if (parameter instanceof FeatureQueryTransformer) {
        ((FeatureQueryTransformer) parameter).transformQuery(queryBuilder, parameters, apiData);
        ((FeatureQueryTransformer) parameter)
            .transformQuery(queryBuilder, parameters, apiData, collectionData);
      }
    }
  }

  @Override
  public FeatureQuery requestToBareFeaturesQuery(
      OgcApiDataV2 apiData,
      String featureTypeId,
      EpsgCrs crs,
      Map<String, Integer> coordinatePrecision,
      int defaultPageSize,
      Map<String, String> parameters,
      List<OgcApiQueryParameter> allowedParameters) {

    final int limit =
        parameters.containsKey(LIMIT) ? Integer.parseInt(parameters.get(LIMIT)) : defaultPageSize;
    final int offset =
        parameters.containsKey(OFFSET) ? Integer.parseInt(parameters.get(OFFSET)) : 0;
    final ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder().type(featureTypeId).crs(crs).limit(limit).offset(offset);

    for (OgcApiQueryParameter parameter : allowedParameters) {
      if (parameter instanceof FeatureQueryTransformer) {
        ((FeatureQueryTransformer) parameter).transformQuery(queryBuilder, parameters, apiData);
      }
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

    //noinspection ConstantConditions
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

    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

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
                  if (PARAMETER_BBOX.equals(filter.getKey())) {
                    return processBbox(
                        filterableFields, queryValidationInput, bbox, filter.getValue());
                  }
                  if (PARAMETER_DATETIME.equals(filter.getKey())) {
                    return processDatetime(filterableFields, filter.getValue());
                  }
                  if (filterParameters.contains(filter.getKey())) {
                    return processFilterParameter(
                        filterableFields,
                        queryableTypes,
                        cqlFormat,
                        crs,
                        queryValidationInput,
                        filter);
                  }
                  if (filter.getValue().contains(ASTERISK)) {
                    return Like.of(
                        filterableFields.get(filter.getKey()),
                        ScalarLiteral.of(filter.getValue().replace(ASTERISK, PERCENT)));
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

  private Cql2Expression processFilterParameter(
      Map<String, String> filterableFields,
      Map<String, String> queryableTypes,
      Format cqlFormat,
      EpsgCrs crs,
      QueryValidationInputCoordinates queryValidationInput,
      Map.Entry<String, String> filter) {
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

  private Cql2Expression processDatetime(Map<String, String> filterableFields, String value) {
    if (FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE.equals(
        filterableFields.get(PARAMETER_DATETIME))) {
      return null;
    }
    return timeToCql(filterableFields.get(PARAMETER_DATETIME), value).orElse(null);
  }

  private Cql2Expression processBbox(
      Map<String, String> filterableFields,
      QueryValidationInputCoordinates queryValidationInput,
      Optional<BoundingBox> bbox,
      String value) {
    if (FeatureQueryEncoder.PROPERTY_NOT_AVAILABLE.equals(filterableFields.get(PARAMETER_BBOX))) {
      return null;
    }

    Cql2Expression cqlPredicate = bboxToCql(filterableFields.get(PARAMETER_BBOX), value, bbox);

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

  private Cql2Expression bboxToCql(
      String geometryField, String bboxValue, Optional<BoundingBox> optionalSpatialExtent) {
    List<String> values = ARRAY_SPLITTER.splitToList(bboxValue);
    EpsgCrs sourceCrs = OgcCrs.CRS84;

    if (values.size() == 5) {
      try {
        sourceCrs = EpsgCrs.fromString(values.get(values.size() - 1));
        values = values.subList(0, values.size() - 1);
      } catch (Throwable e) {
        // ignore, the value is not from bbox-crs, as that is already validated in
        // OgcApiParameterCrs
      }
    }

    if (values.size() != 4) {
      throw new IllegalArgumentException(
          String.format(
              "The parameter 'bbox' is invalid: it must have exactly four values, found %d.",
              values.size()));
    }

    List<Double> bboxCoordinates =
        values.stream().map(Double::valueOf).collect(Collectors.toList());

    return getCql2Expression(geometryField, optionalSpatialExtent, sourceCrs, bboxCoordinates);
  }

  private Cql2Expression getCql2Expression(
      String geometryField,
      Optional<BoundingBox> optionalSpatialExtent,
      EpsgCrs sourceCrs,
      List<Double> bboxCoordinates) {
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

  private ImmutableFeatureQuery.Builder processCoordinatePrecision(
      ImmutableFeatureQuery.Builder queryBuilder, Map<String, Integer> coordinatePrecision) {
    // check, if we need to add a precision value; for this we need the target CRS,
    // so we need to build the query to get the CRS
    ImmutableFeatureQuery query = queryBuilder.build();
    if (!coordinatePrecision.isEmpty() && query.getCrs().isPresent()) {
      ImmutableList.Builder<Integer> precisionListBuilder = new ImmutableList.Builder<>();
      for (Unit<?> unit : crsInfo.getAxisUnits(query.getCrs().get())) {
        processUnit(precisionListBuilder, coordinatePrecision, unit);
      }
      List<Integer> precisionList = precisionListBuilder.build();
      if (!precisionList.isEmpty()) {
        queryBuilder.geometryPrecision(precisionList);
      }
    }
    return queryBuilder;
  }

  private void processUnit(
      ImmutableList.Builder<Integer> precisionListBuilder,
      Map<String, Integer> coordinatePrecision,
      Unit<?> unit) {
    Integer precision;
    if (unit.equals(Units.METRE)) {
      precision = coordinatePrecision.get(METER);
      if (Objects.isNull(precision)) {
        precision = coordinatePrecision.get(METRE);
      }
    } else if (unit.equals(Units.DEGREE)) {
      precision = coordinatePrecision.get(DEGREE);
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Coordinate precision, unrecognised unit found: '{}'. Using degree as fallback unit.",
            unit.getName());
      }
      precision = coordinatePrecision.get(DEGREE);
    }
    precisionListBuilder.add(precision);
  }
}
