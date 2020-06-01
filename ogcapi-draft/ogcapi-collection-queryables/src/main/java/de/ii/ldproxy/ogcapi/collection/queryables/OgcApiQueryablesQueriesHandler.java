/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.target.geojson.GeoJsonConfig;
import de.ii.ldproxy.target.geojson.SchemaGeneratorFeature;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.*;

@Component
@Instantiate
@Provides(specifications = {OgcApiQueryablesQueriesHandler.class})
public class OgcApiQueryablesQueriesHandler implements OgcApiQueriesHandler<OgcApiQueryablesQueriesHandler.Query> {

    public enum Query implements OgcApiQueryIdentifier {QUERYABLES, SCHEMA}

    @Value.Immutable
    public interface OgcApiQueryInputQueryables extends OgcApiQueryInput {
        String getCollectionId();

        boolean getIncludeHomeLink();

        boolean getIncludeLinkHeader();
    }

    @Requires
    SchemaGeneratorFeature schemaGeneratorFeature;

    private final I18n i18n;
    private final OgcApiFeatureCoreProviders providers;
    private final GeoJsonConfig geoJsonConfig;
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;

    public OgcApiQueryablesQueriesHandler(@Requires I18n i18n,
                                          @Requires OgcApiFeatureCoreProviders providers,
                                          @Requires GeoJsonConfig geoJsonConfig) {
        this.i18n = i18n;
        this.providers = providers;
        this.geoJsonConfig = geoJsonConfig;

        this.queryHandlers = ImmutableMap.of(
                Query.QUERYABLES, OgcApiQueryHandler.with(OgcApiQueryInputQueryables.class, this::getQueryablesResponse),
                Query.SCHEMA, OgcApiQueryHandler.with(OgcApiQueryInputQueryables.class, this::getSchemaResponse)
        );
    }

    @Override
    public Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public static void checkCollectionId(OgcApiApiDataV2 apiData, String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException();
        }
    }

    // TODO consolidate code
    private Response getQueryablesResponse(OgcApiQueryInputQueryables queryInput, OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException();

        OgcApiQueryablesFormatExtension outputFormat = api.getOutputFormat(
                    OgcApiQueryablesFormatExtension.class,
                    requestContext.getMediaType(),
                    "/collections/"+collectionId+"/queryables")
                .orElseThrow(NotAcceptableException::new);

        checkCollectionId(api.getData(), collectionId);
        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<OgcApiLink> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        ImmutableQueryables.Builder queryables = ImmutableQueryables.builder();

        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                .get(collectionId);
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
        Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(OgcApiFeaturesCoreConfiguration.class);

        List<String> featureTypeIds = featuresCoreConfiguration.get().getFeatureTypes();
        if (featureTypeIds.isEmpty())
            featureTypeIds = ImmutableList.of(collectionId);

        List<String> otherQueryables = featuresCoreConfiguration
                .flatMap(OgcApiFeaturesCoreConfiguration::getQueryables)
                .map(OgcApiFeaturesCollectionQueryables::getOther)
                .orElse(ImmutableList.of());

        List<String> temporalQueryables = featuresCoreConfiguration
                .flatMap(OgcApiFeaturesCoreConfiguration::getQueryables)
                .map(OgcApiFeaturesCollectionQueryables::getTemporal)
                .orElse(ImmutableList.of());

        List<String> spatialQueryables = featuresCoreConfiguration
                .flatMap(OgcApiFeaturesCoreConfiguration::getQueryables)
                .map(OgcApiFeaturesCollectionQueryables::getSpatial)
                .orElse(ImmutableList.of());

        List<String> visitedProperties = new ArrayList<>();

        featureTypeIds.forEach(featureTypeId -> {
            FeatureSchema featureType = featureProvider.getData()
                                                       .getTypes()
                                                       .get(featureTypeId);

            Optional<Locale> language = featureProvider.getData().getDefaultLanguage().isPresent() ?
                    Optional.of(Locale.forLanguageTag(featureProvider.getData().getDefaultLanguage().get())) :
                    Optional.empty();

            if (Objects.nonNull(featureType)) {
                featureType.getAllNestedProperties()
                           .forEach(featureProperty -> {
                               if (featureProperty.isObject()) {
                                   return;
                               }

                               String nameInFilters = String.join(".", featureProperty.getFullPath());

                               if (visitedProperties.contains(nameInFilters)) {
                                   return;
                               }

                               Optional<Boolean> required = featureProperty.getConstraints().isPresent() ?
                                       featureProperty.getConstraints().get().getRequired() : Optional.empty();

                               Optional<String> pattern = featureProperty.getConstraints().isPresent() ?
                                       featureProperty.getConstraints().get().getRegex() : Optional.empty();
                               Optional<Double> min = featureProperty.getConstraints().isPresent() ?
                                       featureProperty.getConstraints().get().getMin() : Optional.empty();
                               Optional<Double> max = featureProperty.getConstraints().isPresent() ?
                                       featureProperty.getConstraints().get().getMax() : Optional.empty();
                               List<String> values = featureProperty.getConstraints().isPresent() ?
                                       featureProperty.getConstraints().get().getEnumValues() : ImmutableList.of();


                               // TODO: add more information to the configuration and include it in the Queryable values

                               if (otherQueryables.contains(nameInFilters)) {
                                   String type;
                                   switch (featureProperty.getType()) {
                                       case INTEGER:
                                           type = "integer";
                                    break;
                                       case FLOAT:
                                           type = "number";
                                    break;
                                       case STRING:
                                           type = "string";
                                    break;
                                       case BOOLEAN:
                                           type = "boolean";
                                    break;
                                       default:
                                           return;
                                   }

                                   queryables.addQueryables(ImmutableQueryable.builder()
                                                                              .id(nameInFilters)
                                                                              .type(type)
                                                                              .title(featureProperty.getLabel())
                                                                              .description(featureProperty.getDescription())
                                                                              .values(values)
                                                                              .pattern(pattern)
                                                                              .min(min)
                                                                              .max(max)
                                                                              .required(required)
                                                                              .language(language)
                                                                              .build());
                                   visitedProperties.add(nameInFilters);
                               } else if (temporalQueryables.contains(nameInFilters)) {
                                   queryables.addQueryables(ImmutableQueryable.builder()
                                                                              .id(nameInFilters)
                                                                              .type("dateTime")
                                                                              .title(featureProperty.getLabel())
                                                                              .description(featureProperty.getDescription())
                                                                              .required(required)
                                                                              .language(language)
                                                                              .build());
                                   visitedProperties.add(nameInFilters);
                               } else if (spatialQueryables.contains(nameInFilters)) {
                                   queryables.addQueryables(ImmutableQueryable.builder()
                                                                              .id(nameInFilters)
                                                                              .type("geometry")
                                                                              .title(featureProperty.getLabel())
                                                                              .description(featureProperty.getDescription())
                                                                              .required(required)
                                                                              .language(language)
                                                                              .build());
                                   visitedProperties.add(nameInFilters);
                               }
                           });
            }
        });

        queryables.links(links);

        Response queryablesResponse = outputFormat.getResponse(queryables.build(), collectionId, api, requestContext);

        Response.ResponseBuilder response = Response.ok()
                .entity(queryablesResponse.getEntity())
                .type(requestContext
                        .getMediaType()
                        .type());

        Optional<Locale> language = requestContext.getLanguage();
        if (language.isPresent())
            response.language(language.get());

        if (queryInput.getIncludeLinkHeader() && links != null)
            links.stream()
                    .forEach(link -> response.links(link.getLink()));

        return response.build();
    }

    private Response getSchemaResponse(OgcApiQueryInputQueryables queryInput, OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException();

        OgcApiSchemaFormatExtension outputFormat = api.getOutputFormat(
                OgcApiSchemaFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/"+collectionId+"/schema")
                .orElseThrow(NotAcceptableException::new);

        checkCollectionId(api.getData(), collectionId);
        List<OgcApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<OgcApiLink> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        Map<String,Object> jsonSchema = schemaGeneratorFeature.getSchemaJson(apiData, collectionId, links.stream()
                .filter(link -> link.getRel().equals("self"))
                .map(link -> link.getHref())
                .findAny());

        Optional<OgcApiSchemaFormatExtension> outputFormatExtension = api.getOutputFormat(
                OgcApiSchemaFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/"+collectionId+"/schema");

        if (outputFormatExtension.isPresent()) {
            Response schemaResponse = outputFormatExtension.get()
                    .getResponse(jsonSchema, collectionId, api, requestContext);

            Response.ResponseBuilder response = Response.ok()
                    .entity(schemaResponse.getEntity())
                    .type(requestContext
                            .getMediaType()
                            .type());

            Optional<Locale> language = requestContext.getLanguage();
            if (language.isPresent())
                response.language(language.get());

            if (queryInput.getIncludeLinkHeader() && links != null)
                links.stream()
                        .forEach(link -> response.links(link.getLink()));

            return response.build();
        }

        throw new NotAcceptableException();
    }
}
