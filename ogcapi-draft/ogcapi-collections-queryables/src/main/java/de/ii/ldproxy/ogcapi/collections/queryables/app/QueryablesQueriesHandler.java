/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorFeatureGeoJson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Instantiate
@Provides(specifications = {QueryablesQueriesHandler.class})
public class QueryablesQueriesHandler implements QueriesHandler<QueryablesQueriesHandler.Query> {

    public enum Query implements QueryIdentifier {QUERYABLES}

    @Value.Immutable
    public interface QueryInputQueryables extends QueryInput {
        String getCollectionId();

        boolean getIncludeLinkHeader();
    }

    @Requires
    SchemaGeneratorFeatureGeoJson schemaGeneratorFeature;

    private final I18n i18n;
    private final FeaturesCoreProviders providers;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;

    public QueryablesQueriesHandler(@Requires I18n i18n,
                                    @Requires FeaturesCoreProviders providers) {
        this.i18n = i18n;
        this.providers = providers;
        this.queryHandlers = ImmutableMap.of(
                Query.QUERYABLES, QueryHandler.with(QueryInputQueryables.class, this::getQueryablesResponse)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    public static void checkCollectionId(OgcApiDataV2 apiData, String collectionId) {
        if (!apiData.isCollectionEnabled(collectionId)) {
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
        }
    }

    // TODO consolidate code
    private Response getQueryablesResponse(QueryInputQueryables queryInput, ApiRequestContext requestContext) {

        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String collectionId = queryInput.getCollectionId();
        if (!apiData.isCollectionEnabled(collectionId))
            throw new NotFoundException(MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));

        QueryablesFormatExtension outputFormat = api.getOutputFormat(
                    QueryablesFormatExtension.class,
                    requestContext.getMediaType(),
                    "/collections/"+collectionId+"/queryables",
                    Optional.of(collectionId))
                                                    .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        checkCollectionId(api.getData(), collectionId);
        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links =
                new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

        /*
        ImmutableQueryables.Builder queryables = ImmutableQueryables.builder();

        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                .get(collectionId);
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
        Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class);

        List<String> featureTypeIds = featuresCoreConfiguration.get().getFeatureTypes();
        if (featureTypeIds.isEmpty())
            featureTypeIds = ImmutableList.of(collectionId);

        List<String> otherQueryables = featuresCoreConfiguration
                .flatMap(FeaturesCoreConfiguration::getQueryables)
                .map(FeaturesCollectionQueryables::getOther)
                .orElse(ImmutableList.of());

        List<String> temporalQueryables = featuresCoreConfiguration
                .flatMap(FeaturesCoreConfiguration::getQueryables)
                .map(FeaturesCollectionQueryables::getTemporal)
                .orElse(ImmutableList.of());

        List<String> spatialQueryables = featuresCoreConfiguration
                .flatMap(FeaturesCoreConfiguration::getQueryables)
                .map(FeaturesCollectionQueryables::getSpatial)
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
         */

        JsonSchemaObject jsonSchema = schemaGeneratorFeature.getSchemaJson(apiData, collectionId, links.stream()
                                                                                                       .filter(link -> link.getRel().equals("self"))
                                                                                                       .map(link -> link.getHref())
                                                                                                       .map(link -> link.indexOf("?") == -1 ? link : link.substring(0, link.indexOf("?")))
                                                                                                       .findAny(), SchemaGeneratorFeature.SCHEMA_TYPE.QUERYABLES);

        return prepareSuccessResponse(api, requestContext, queryInput.getIncludeLinkHeader() ? links : null)
                .entity(outputFormat.getEntity(jsonSchema, links, collectionId, api, requestContext))
                .build();
    }
}
