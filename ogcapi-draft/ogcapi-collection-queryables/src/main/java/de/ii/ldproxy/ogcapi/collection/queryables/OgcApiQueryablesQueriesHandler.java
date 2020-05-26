/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import com.google.common.base.Splitter;
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
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureType;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.immutables.value.Value;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

        List<String> featureTypeIds = featuresCoreConfiguration
                .map(OgcApiFeaturesCoreConfiguration::getFeatureTypes)
                .orElse(ImmutableList.of());

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

            if (Objects.nonNull(featureType)) {
                featureType.getProperties()
                           .forEach(featureProperty -> {
                               String name = featureProperty.getName();

                               if (visitedProperties.contains(name)) {
                                   return;
                               }

                               // no "[...]" in filters, just dots to separate the segments
                               String nameInFilters = name.replaceAll("\\[[^\\]]*\\]", "");

                               if (otherQueryables.contains(name)) {
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

                                   // TODO: add more information to the configuration and include it in the Queryable values

                                   queryables.addQueryables(ImmutableQueryable.builder()
                                                                              .id(nameInFilters)
                                                                              .type(type)
                                                                              .build());
                                   visitedProperties.add(name);
                               } else if (temporalQueryables.contains(name)) {
                                   queryables.addQueryables(ImmutableQueryable.builder()
                                                                              .id(nameInFilters)
                                                                              .type("dateTime")
                                                                              .build());
                                   visitedProperties.add(name);
                               } else if (spatialQueryables.contains(name)) {
                                   queryables.addQueryables(ImmutableQueryable.builder()
                                                                              .id(nameInFilters)
                                                                              .type("geometry")
                                                                              .build());
                                   visitedProperties.add(name);
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

        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                .get(collectionId);
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
        Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(OgcApiFeaturesCoreConfiguration.class);

        FeatureSchema featureTypeProvider = featureProvider.getData()
                                                           .getTypes()
                                                           .get(collectionId);

        FeatureTransformerGeoJson.NESTED_OBJECTS nestingStrategy = geoJsonConfig.getNestedObjectStrategy();
        FeatureTransformerGeoJson.MULTIPLICITY multiplicityStrategy = geoJsonConfig.getMultiplicityStrategy();

        SchemaObject schemaObject = apiData.getSchema(featureTypeProvider,
                nestingStrategy == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN &&
                multiplicityStrategy == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX);

        Map<String,Object> jsonSchema = getJsonSchema(schemaObject, links);

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

    private class Context {
        Set<SchemaObject> definitions = new HashSet<>();
        ImmutableMap<String, Object> properties;
        ImmutableMap<String, Object> geometry;
    }

    private Context processProperties(SchemaObject schemaObject, boolean isFeature) {

        Context result = new Context();
        ImmutableMap.Builder<String, Object> propertiesMapBuilder = ImmutableMap.<String, Object>builder();
        ImmutableMap.Builder<String, Object> geometryMapBuilder = ImmutableMap.<String, Object>builder();
        AtomicBoolean hasGeometry = new AtomicBoolean(false);

        schemaObject.properties.stream()
                .forEachOrdered(prop -> {
                    ImmutableMap.Builder<String, Object> typeOrRefBuilder = ImmutableMap.<String,Object>builder();
                    boolean geometry = false;
                    if (prop.literalType.isPresent()) {
                        String type = prop.literalType.get();
                        if (type.equalsIgnoreCase("datetime")) {
                            typeOrRefBuilder.put("type", "string")
                                    .put("format","date-time");
                        } else {
                            typeOrRefBuilder.put("type", type);
                        }

                    } else if (prop.wellknownType.isPresent()) {
                        switch (prop.wellknownType.get()) {
                            case "Link":
                                typeOrRefBuilder.put("$ref", "https://api.swaggerhub.com/domains/cportele/ogcapi-features-1/1.0.0#/components/schemas/link");
                                break;
                            case "Point":
                                typeOrRefBuilder.put("$ref", "https://geojson.org/schema/Point.json");
                                geometry = true;
                                break;
                            case "MultiPoint":
                                typeOrRefBuilder.put("$ref", "https://geojson.org/schema/MultiPoint.json");
                                geometry = true;
                                break;
                            case "LineString":
                                typeOrRefBuilder.put("$ref", "https://geojson.org/schema/LineString.json");
                                geometry = true;
                                break;
                            case "MultiLineString":
                                typeOrRefBuilder.put("$ref", "https://geojson.org/schema/MultiLineString.json");
                                geometry = true;
                                break;
                            case "Polygon":
                                typeOrRefBuilder.put("$ref", "https://geojson.org/schema/Polygon.json");
                                geometry = true;
                                break;
                            case "MultiPolygon":
                                typeOrRefBuilder.put("$ref", "https://geojson.org/schema/MultiPolygon.json");
                                geometry = true;
                                break;
                            case "Geometry":
                                typeOrRefBuilder.put("$ref", "https://geojson.org/schema/Geometry.json");
                                geometry = true;
                                break;
                            default:
                                typeOrRefBuilder.put("type", "object");
                                break;
                        }
                    } else if (prop.objectType.isPresent()) {
                        typeOrRefBuilder.put("$ref", "#/definitions/"+prop.objectType.get().id);
                        result.definitions.add(prop.objectType.get());

                    }
                    if (!isFeature || !geometry) {
                        if (prop.maxItems == 1) {
                            propertiesMapBuilder.put(prop.id, typeOrRefBuilder.build());
                        } else {
                            propertiesMapBuilder.put(prop.id, ImmutableMap.<String, Object>builder()
                                    .put("type", "array")
                                    .put("items", typeOrRefBuilder.build())
                                    .build());
                        }
                    } else {
                        // only one geometry per feature
                        if (!hasGeometry.get()) {
                            geometryMapBuilder.put("oneOf", ImmutableList.builder()
                                    .add(typeOrRefBuilder.build(), ImmutableMap.builder().put("type", "null").build())
                                    .build());
                            hasGeometry.set(true);
                        }
                    }
                });

        if (!hasGeometry.get()) {
            geometryMapBuilder.put("type", "null");
        }

        result.properties =  ImmutableMap.<String, Object>builder()
                .put( "type", "object" )
                .put( "properties", propertiesMapBuilder.build() )
                .build();
        result.geometry = geometryMapBuilder.build();
        return result;
    }

    // TODO support also nullable (as in OpenAPI 3.0), not only null (as in JSON Schema)
    private Map<String, Object> getJsonSchema(SchemaObject schemaObject, List<OgcApiLink> links) {

        Context featureContext = processProperties(schemaObject, true);

        ImmutableMap.Builder<String, Object> definitionsMapBuilder = ImmutableMap.<String, Object>builder();
        Set<SchemaObject> processed = new HashSet<>();
        Set<SchemaObject> current = featureContext.definitions;

        while (!current.isEmpty()) {
            Set<SchemaObject> next = new HashSet<>();
            current.stream()
                    .filter(defObject -> !processed.contains(defObject))
                    .forEach(defObject -> {
                        Context definitionContext = processProperties(defObject, false);
                        definitionsMapBuilder.put(defObject.id, definitionContext.properties);
                        next.addAll(definitionContext.definitions);
                        processed.add(defObject);
                    });
            current = next;
        }

        return ImmutableMap.<String,Object>builder()
                .put( "$schema", "http://json-schema.org/draft-07/schema#" )
                .put( "$id", links.stream()
                        .filter(link -> link.getRel().equalsIgnoreCase("self"))
                        .findFirst()
                        .map(link -> link.getHref()))
                .put( "type", "object" )
                .put( "title", schemaObject.title.orElse("") )
                .put( "description", schemaObject.description.orElse("") )
                .put( "required", ImmutableList.builder()
                        .add( "type", "geometry", "properties" )
                        .build() )
                .put( "properties", ImmutableMap.builder()
                        .put( "type", ImmutableMap.builder()
                                .put( "type", "string" )
                                .put( "enum", ImmutableList.builder()
                                        .add( "Feature" )
                                        .build())
                                .build())
                        .put( "id", ImmutableMap.builder()
                                .put( "oneOf", ImmutableList.builder()
                                        .add( ImmutableMap.builder().put( "type", "string" ).build(), ImmutableMap.builder().put( "type", "integer" ).build())
                                        .build())
                                .build())
                        .put( "links", ImmutableMap.builder()
                                .put( "type", "array" )
                                .put( "items", ImmutableMap.builder().put( "$ref", "https://api.swaggerhub.com/domains/cportele/ogcapi-features-1/1.0.0#/components/schemas/link" ).build())
                                .build())
                        .put( "geometry", featureContext.geometry )
                        .put( "properties", ImmutableMap.builder()
                                .put( "oneOf", ImmutableList.builder()
                                        .add( featureContext.properties, ImmutableMap.builder().put( "type", "null" ).build())
                                        .build())
                                .build())
                        .build())
                .put( "definitions", definitionsMapBuilder.build() )
                .build();
    }

    private SchemaObject xxxgetSchema(FeatureType featureType, FeatureTypeConfigurationOgcApi featureTypeApi) {
        SchemaObject featureTypeObject = new SchemaObject();

        if (Objects.nonNull(featureType) && Objects.nonNull(featureTypeApi)) {
            ArrayList<FeatureProperty> featureProperties = new ArrayList<>(featureType.getProperties().values());
            Optional<OgcApiFeaturesCoreConfiguration> coreConfig = featureTypeApi.getExtension(OgcApiFeaturesCoreConfiguration.class);
            featureTypeObject.id = featureType.getName();
            featureTypeObject.title = Optional.of(featureTypeApi.getLabel());
            featureTypeObject.description = featureTypeApi.getDescription();
            AtomicInteger typeIdx = new AtomicInteger(1);

            featureType.getProperties()
                    .forEach((name, featureProperty) -> {
                        // remove content in square brackets, just in case
                        String nameNormalized = name.replaceAll("\\[[^\\]]+\\]", "[]");

                        String baseName = featureProperty.getName();
                        List<String> baseNameSections = Splitter.on('.').splitToList(baseName);
                        Map<String, String> addInfo = featureProperty.getAdditionalInfo();
                        boolean link = false;
                        List<String> htmlNameSections = ImmutableList.of();
                        String geometryType = null;
                        if (Objects.nonNull(addInfo)) {
                            if (addInfo.containsKey("role")) {
                                link = addInfo.get("role").startsWith("LINK");
                            }
                            if (addInfo.containsKey("title")) {
                                htmlNameSections = Splitter.on('|').splitToList(addInfo.get("title"));
                            }
                            if (addInfo.containsKey("geometryType")) {
                                geometryType = addInfo.get("geometryType");
                            }
                        }

                        // determine context in the properties of this feature
                        String curPath = null;
                        SchemaObject valueContext = featureTypeObject;
                        int arrays = 0;
                        int objectLevel = 0;
                        for (String nameComponent : baseNameSections) {
                            curPath = Objects.isNull(curPath) ? nameComponent : curPath.concat("."+nameComponent);
                            if (link && curPath.equals(baseName)) {
                                // already processed
                                continue;
                            }
                            boolean isArray = nameComponent.endsWith("]");
                            SchemaProperty property = valueContext.get(curPath);
                            if (Objects.isNull(property)) {
                                property = new SchemaProperty();
                                property.id = nameComponent.replace("[]", "");
                                property.maxItems = isArray ? Integer.MAX_VALUE : 1;
                                property.path = curPath;
                                valueContext.properties.add(property);
                            }
                            if (curPath.equals(baseName) ||
                                    link && (curPath.concat(".href").equals(baseName) || curPath.concat(".title").equals(baseName))) {
                                // we are at the end of the path;
                                // this includes the special case of a link object that is mapped to a single value in the HTML
                                if (!htmlNameSections.isEmpty())
                                    property.title = Optional.ofNullable(htmlNameSections.get(Math.min(objectLevel,htmlNameSections.size()-1)));
                                if (link) {
                                    property.wellknownType = Optional.of("Link");
                                } else {
                                    switch (featureProperty.getType()) {
                                        case GEOMETRY:
                                            property.wellknownType = Objects.nonNull(geometryType) ? Optional.of(geometryType) : Optional.of("Geometry");
                                            break;
                                        case INTEGER:
                                            property.literalType = Optional.of("integer");
                                            break;
                                        case FLOAT:
                                            property.literalType = Optional.of("number");
                                            break;
                                        case STRING:
                                            property.literalType = Optional.of("string");
                                            break;
                                        case BOOLEAN:
                                            property.literalType = Optional.of("boolean");
                                            break;
                                        case DATETIME:
                                            property.literalType = Optional.of("dateTime");
                                            break;
                                        default:
                                            return;
                                    }
                                }
                            } else {
                                // we have an object, either the latest object in the existing list or a new object
                                if (property.objectType.isPresent()) {
                                    valueContext = property.objectType.get();
                                } else {
                                    valueContext = new SchemaObject();
                                    valueContext.id = "type_" + (typeIdx.getAndIncrement()); // TODO how can we get proper type names?
                                    property.objectType = Optional.of(valueContext);
                                    if (!htmlNameSections.isEmpty())
                                        property.title = Optional.ofNullable(htmlNameSections.get(Math.min(objectLevel, htmlNameSections.size() - 1)));
                                }
                                objectLevel++;
                            }
                        }
                    });
        }
        return featureTypeObject;
    }
}
