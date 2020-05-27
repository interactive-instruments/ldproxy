package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.SchemaObject;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.features.domain.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class SchemaGeneratorFeature {

    final static Schema GENERIC = new ObjectSchema()
            .required(ImmutableList.of("type","geometry","properties"))
            .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
            .addProperties("geometry", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON"))
            .addProperties("properties", new ObjectSchema())
            .addProperties("id", new StringSchema())
            .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")));

    @Requires
    OgcApiFeatureCoreProviders providers;

    @Requires
    GeoJsonConfig geoJsonConfig;

    public static String referenceGeneric() { return "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureGeoJSON"; }

    public static Schema getGeneric() {
        return GENERIC;
    }

    public static String referenceForCollection(String collectionId) { return "#/components/schemas/featureGeoJson_"+collectionId; }

    public Schema generateForCollection(OgcApiApiDataV2 apiData, String collectionId) {

        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                                                               .get(collectionId);
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
        FeatureSchema featureTypeProvider = featureProvider.getData()
                                                           .getTypes()
                                                           .get(collectionId);
        SchemaObject schemaObject = apiData.getSchema(featureTypeProvider,
                geoJsonConfig.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN &&
                        geoJsonConfig.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX);

        Context featureContext = processPropertiesOpenApi(schemaObject, true);
        return new ObjectSchema()
                .title(schemaObject.title.orElse(null))
                .description(schemaObject.description.orElse(null))
                .required(ImmutableList.of("type","geometry","properties"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
                .addProperties("geometry", featureContext.geometry)
                .addProperties("properties", featureContext.properties)
                .addProperties("id", new StringSchema())
                .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")));
    }

    public List<String> getPropertyNames(OgcApiApiDataV2 apiData, String collectionId) {
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, apiData.getCollections().get(collectionId));
        return featureProvider.getData()
                .getTypes()
                .get(collectionId)
                .getProperties()
                .stream()
                .filter(featureProperty -> !featureProperty.isSpatial())
                .map(featureProperty -> featureProperty.getName().replaceAll("\\[[^\\]]+\\]", "[]"))
                .collect(Collectors.toList());
    }

    private class Context {
        Schema properties = new ObjectSchema();
        Schema geometry = null;
    }

    private Context processPropertiesOpenApi(SchemaObject schemaObject, boolean isFeature) {

        Context schema = new Context();
        AtomicBoolean hasGeometry = new AtomicBoolean(false);

        schemaObject.properties.stream()
                .forEachOrdered(prop -> {
                    boolean geometry = false;
                    Schema pSchema = null;
                    if (prop.literalType.isPresent()) {
                        String type = prop.literalType.get();
                        if (type.equalsIgnoreCase("datetime"))
                            pSchema = new DateTimeSchema();
                        else
                            pSchema = new Schema().type(type);

                    } else if (prop.wellknownType.isPresent()) {
                        switch (prop.wellknownType.get()) {
                            case "Link":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link");
                                break;
                            case "Point":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/pointGeoJSON");
                                geometry = true;
                                break;
                            case "MultiPoint":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipointGeoJSON");
                                geometry = true;
                                break;
                            case "LineString":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/linestringGeoJSON");
                                geometry = true;
                                break;
                            case "MultiLineString":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multilinestringGeoJSON");
                                geometry = true;
                                break;
                            case "Polygon":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/polygonGeoJSON");
                                geometry = true;
                                break;
                            case "MultiPolygon":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipolygonGeoJSON");
                                geometry = true;
                                break;
                            case "Geometry":
                                pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON");
                                geometry = true;
                                break;
                            default:
                                pSchema = new ObjectSchema();
                                break;
                        }
                    } else if (prop.objectType.isPresent()) {
                        Schema pContext = processPropertiesOpenApi(prop.objectType.get(), false).properties;

                    }
                    if (!isFeature || !geometry) {
                        if (prop.maxItems > 1) {
                            pSchema = new ArraySchema().items(pSchema);
                            if (prop.minItems > 0)
                                pSchema.minItems(prop.minItems);
                            if (prop.maxItems < Integer.MAX_VALUE)
                                pSchema.maxItems(prop.maxItems);
                        }
                    } else {
                        // only one geometry per feature
                        if (!hasGeometry.get()) {
                            schema.geometry = pSchema;
                            pSchema = null;
                            hasGeometry.set(true);
                        }
                    }

                    if (pSchema!=null) {
                        if (prop.title.isPresent())
                            pSchema.title(prop.title.get());
                        if (prop.description.isPresent())
                            pSchema.description(prop.description.get());
                        if (prop.minItems > 0)
                            schema.properties.addRequiredItem(prop.id);
                        schema.properties.addProperties(prop.id, pSchema);
                    }
                });

        if (!hasGeometry.get()) {
            schema.geometry = new Schema().nullable(true);
        }

        return schema;
    }


    public static String referenceByName(String name) { return "#/components/schemas/featureGeoJson_"+name; }

    public Schema generateBaseSchema(String name) {
        return new ObjectSchema()
                .required(ImmutableList.of("type","geometry","properties"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
                .addProperties("geometry", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON"))
                .addProperties("properties", new ObjectSchema()) // to be filled by caller
                .addProperties("id", new StringSchema())
                .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")));
    }

    /* TODO
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
                                .put( "items", ImmutableMap.builder().put( "$ref", "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link" ).build())
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
                                typeOrRefBuilder.put("$ref", "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link");
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
     */

}
