/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueriesHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryInput;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private final Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> queryHandlers;

    public OgcApiQueryablesQueriesHandler(@Requires I18n i18n,
                                          @Requires OgcApiFeatureCoreProviders providers) {
        this.i18n = i18n;
        this.providers = providers;
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
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
    }

    // TODO consolidate code
    private Response getQueryablesResponse(OgcApiQueryInputQueryables queryInput, OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));

        OgcApiQueryablesFormatExtension outputFormat = api.getOutputFormat(
                    OgcApiQueryablesFormatExtension.class,
                    requestContext.getMediaType(),
                    "/collections/"+collectionId+"/queryables")
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

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

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getEntity(queryables.build(), collectionId, api, requestContext))
                .build();
    }

    private Response getSchemaResponse(OgcApiQueryInputQueryables queryInput, OgcApiRequestContext requestContext) {

        OgcApiApi api = requestContext.getApi();
        OgcApiApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));

        OgcApiSchemaFormatExtension outputFormat = api.getOutputFormat(
                OgcApiSchemaFormatExtension.class,
                requestContext.getMediaType(),
                "/collections/"+collectionId+"/schema")
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

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

        if (!outputFormatExtension.isPresent())
            throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type()));

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormatExtension.get().getEntity(jsonSchema, collectionId, api, requestContext))
                .build();
    }
}
