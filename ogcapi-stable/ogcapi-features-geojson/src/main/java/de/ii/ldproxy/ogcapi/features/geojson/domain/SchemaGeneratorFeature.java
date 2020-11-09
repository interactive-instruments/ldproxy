package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.xtraplatform.features.domain.*;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
@Provides(specifications = {SchemaGeneratorFeature.class})
@Instantiate
public class SchemaGeneratorFeature {

    final static Schema GENERIC = new ObjectSchema()
            .required(ImmutableList.of("type", "geometry", "properties"))
            .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
            .addProperties("geometry", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON"))
            .addProperties("properties", new ObjectSchema())
            .addProperties("id", new StringSchema())
            .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")));

    final static String DEFINITIONS_TOKEN = "definitions";

    private final ConcurrentMap<Integer, ConcurrentMap<String, Schema>> schemaMapOpenApi = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, ConcurrentMap<String, Map<String, Object>>> schemaMapJson = new ConcurrentHashMap<>();

    @Requires
    FeaturesCoreProviders providers;

    public String getSchemaReferenceOpenApi() {
        return "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureGeoJSON";
    }

    public Schema getSchemaOpenApi() {
        return GENERIC;
    }

    public String getSchemaReferenceOpenApi(String collectionIdOrName) {
        return "#/components/schemas/featureGeoJson_" + collectionIdOrName;
    }

    public Schema getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMapOpenApi.containsKey(apiHashCode))
            schemaMapOpenApi.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMapOpenApi.get(apiHashCode).containsKey(collectionId)) {
            FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                                                                   .get(collectionId);
            String featureTypeId = apiData.getCollections()
                                          .get(collectionId)
                                          .getExtension(FeaturesCoreConfiguration.class)
                                          .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                          .orElse(collectionId);
            FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
            FeatureSchema featureType = featureProvider.getData()
                                                       .getTypes()
                                                       .get(featureTypeId);

            // TODO we should generalize this and move it to Features Core; this is not just about GeoJSON, but applies to other feature encodings
            Optional<GeoJsonConfiguration> geoJsonConfiguration = collectionData.getExtension(GeoJsonConfiguration.class);
            boolean flatten = geoJsonConfiguration.filter(geoJsonConfig -> geoJsonConfig.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN &&
                    geoJsonConfig.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                                  .isPresent();

            ContextOpenApi featureContext = processPropertiesOpenApi(featureType, true, flatten);
            schemaMapOpenApi.get(apiHashCode)
                            .put(collectionId, new ObjectSchema().title(featureType.getLabel().orElse(null))
                                                                 .description(featureType.getDescription().orElse(null))
                                                                 .required(ImmutableList.of("type", "geometry", "properties"))
                                                                 .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
                                                                 .addProperties("geometry", featureContext.geometry)
                                                                 .addProperties("properties", featureContext.properties)
                                                                 .addProperties("id", new StringSchema())
                                                                 .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link"))));
        }
        return schemaMapOpenApi.get(apiHashCode).get(collectionId);
    }

    public List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId) {
        return getPropertyNames(apiData, collectionId, false, false);
    }

    public List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId, boolean withSpatial, boolean withArrayBrackets) {
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, apiData.getCollections().get(collectionId));
        String featureTypeId = apiData.getCollections()
                                      .get(collectionId)
                                      .getExtension(FeaturesCoreConfiguration.class)
                                      .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                      .orElse(collectionId);
        return featureProvider.getData()
                .getTypes()
                .get(featureTypeId)
                .getProperties()
                .stream()
                .filter(featureProperty -> !featureProperty.isSpatial() || withSpatial)
                .map(featureProperty -> getPropertyNames(featureProperty, "", withArrayBrackets))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<String> getPropertyNames(FeatureSchema featureType, boolean withArrayBrackets) {
        return featureType.getProperties()
                .stream()
                .map(featureProperty -> getPropertyNames(featureProperty, "", withArrayBrackets))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private String getPropertyName(FeatureSchema property, String basePath, boolean withArrayBrackets) {
        return (basePath.isEmpty() ? "" : basePath+".") + property.getName().trim() + (property.isArray() && withArrayBrackets ? "[]" : "");
    }

    private List<String> getPropertyNames(FeatureSchema property, String basePath, boolean withArrayBrackets) {
        List<String> propertyNames = new Vector<>();
        if (property.isObject()) {
            property.getProperties()
                    .stream()
                    .forEach(subProperty -> propertyNames.addAll(getPropertyNames(subProperty, getPropertyName(property, basePath, withArrayBrackets), withArrayBrackets)));
        } else {
            propertyNames.add(getPropertyName(property, basePath, withArrayBrackets));
        }
        return propertyNames;
    }

    public Map<String,String> getNameTitleMap(FeatureSchema featureType) {
        ImmutableMap.Builder<String,String> nameTitleMapBuilder = new ImmutableMap.Builder<>();
        featureType.getProperties()
                .stream()
                .forEach(featureProperty -> addPropertyTitles(nameTitleMapBuilder, featureProperty, "", ""));
        return nameTitleMapBuilder.build();
    }

    private Map.Entry<String,String> getPropertyTitle(FeatureSchema property, String basePath, String baseTitle) {
        String name = (basePath.isEmpty() ? "" : basePath+".") + property.getName().trim();
        String title = (baseTitle.isEmpty() ? "" : baseTitle+"|") + property.getLabel().orElse(property.getName()).trim();
        return new AbstractMap.SimpleImmutableEntry<>(name, title);
    }

    private void addPropertyTitles(ImmutableMap.Builder<String,String> nameTitleMapBuilder, FeatureSchema property, String basePath, String baseTitle) {
        if (property.isObject()) {
            nameTitleMapBuilder.put(getPropertyTitle(property, basePath, baseTitle));
            property.getProperties()
                    .stream()
                    .forEach(subProperty -> {
                        Map.Entry<String, String> entry = getPropertyTitle(property, basePath, baseTitle);
                        addPropertyTitles(nameTitleMapBuilder, subProperty, entry.getKey(), entry.getValue());
                    });
        } else {
            nameTitleMapBuilder.put(getPropertyTitle(property, basePath, baseTitle));
        }
    }

    private class ContextOpenApi {
        Schema properties = new ObjectSchema();
        Schema geometry = null;
    }

    private Schema getSchemaForLiteralType(de.ii.xtraplatform.features.domain.SchemaBase.Type type) {
        switch (type) {
            case INTEGER:
                return new IntegerSchema();
            case FLOAT:
                return new NumberSchema();
            case BOOLEAN:
                return new BooleanSchema();
            case DATETIME:
                return new DateTimeSchema();
            case STRING:
                return new StringSchema();
        }
        return new Schema();
    }

    private ContextOpenApi processPropertiesOpenApi(FeatureSchema schema, boolean isFeature, boolean flatten) {

        ContextOpenApi context = new ContextOpenApi();
        AtomicBoolean hasGeometry = new AtomicBoolean(false);

        List<FeatureSchema> properties = flatten ? schema.getAllNestedProperties() : schema.getProperties();
        // maps from the dotted path name to the path name with array brackets
        Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() : getPropertyNames(schema,true).stream()
                .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String,String> nameTitleMap = !flatten ? ImmutableMap.of() : getNameTitleMap(schema);
        properties.stream()
                .forEachOrdered(property -> {
                    Schema pSchema = null;
                    SchemaBase.Type propType = property.getType();
                    String propertyPath = String.join(".", property.getFullPath());
                    String propertyName = !flatten || property.isObject() ? property.getName() : propertyNameMap.get(propertyPath);
                    if (flatten && propertyName!=null)
                        propertyName = propertyName.replace("[]",".1");
                    switch (propType) {
                        case FLOAT:
                        case INTEGER:
                        case STRING:
                        case BOOLEAN:
                        case DATETIME:
                            pSchema = getSchemaForLiteralType(propType);
                            break;
                        case OBJECT_ARRAY:
                        case OBJECT:
                            if (!flatten) {
                                // ignore intermediate objects in flattening mode, only process leaf properties
                                if (property.getObjectType().orElse("").equals("Link")) {
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link");
                                    break;
                                }
                                pSchema = processPropertiesOpenApi(property, false, flatten).properties;
                            }
                            break;
                        case VALUE_ARRAY:
                            pSchema = getSchemaForLiteralType(property.getValueType().orElse(SchemaBase.Type.UNKNOWN));
                            break;
                        case GEOMETRY:
                            switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                                case POINT:
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/pointGeoJSON");
                                    hasGeometry.set(true);
                                    break;
                                case MULTI_POINT:
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipointGeoJSON");
                                    hasGeometry.set(true);
                                    break;
                                case LINE_STRING:
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/linestringGeoJSON");
                                    hasGeometry.set(true);
                                    break;
                                case MULTI_LINE_STRING:
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multilinestringGeoJSON");
                                    hasGeometry.set(true);
                                    break;
                                case POLYGON:
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/polygonGeoJSON");
                                    hasGeometry.set(true);
                                    break;
                                case MULTI_POLYGON:
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipolygonGeoJSON");
                                    hasGeometry.set(true);
                                    break;
                                case GEOMETRY_COLLECTION:
                                case ANY:
                                default:
                                    pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON");
                                    hasGeometry.set(true);
                                    break;
                                case NONE:
                                    hasGeometry.set(true);
                                    break;
                            }
                            break;
                        case UNKNOWN:
                        default:
                            pSchema = new Schema();
                            break;
                    }

                    if (isFeature && hasGeometry.get()) {
                        // only one geometry per feature, last one wins
                        context.geometry = pSchema;
                    } else if (!flatten) {
                        if (property.isArray()) {
                            pSchema = new ArraySchema().items(pSchema);
                            if (property.getConstraints().isPresent()) {
                                SchemaConstraints constraints = property.getConstraints().get();
                                if (constraints.getMinOccurrence().isPresent())
                                    pSchema = pSchema.minItems(constraints.getMinOccurrence().get());
                                if (constraints.getMaxOccurrence().isPresent())
                                    pSchema = pSchema.maxItems(constraints.getMaxOccurrence().get());
                            }
                        }

                        if (property.getLabel().isPresent())
                            pSchema.title(property.getLabel().get());
                        if (property.getDescription().isPresent())
                            pSchema.description(property.getDescription().get());
                        if (property.getConstraints().isPresent()) {
                            SchemaConstraints constraints = property.getConstraints().get();
                            if (constraints.getRequired().isPresent() && constraints.getRequired().get())
                                context.properties.addRequiredItem(property.getName());
                        }

                        context.properties.addProperties(propertyName, pSchema);
                    } else if (flatten && pSchema!=null) {
                        // we have a leaf property in flattening mode
                        pSchema.title(nameTitleMap.get(propertyPath));
                        if (property.getDescription().isPresent())
                            pSchema.description(property.getDescription().get());

                        context.properties.addProperties(propertyName, pSchema);
                    }

                    if (pSchema != null) {
                        context.properties.addProperties(propertyName, pSchema);
                    }
                });

        if (!hasGeometry.get()) {
            context.geometry = new Schema().nullable(true);
        }

        return context;
    }

    /*
    public Schema generateBaseSchema(String name) {
        return new ObjectSchema()
                .required(ImmutableList.of("type", "geometry", "properties"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
                .addProperties("geometry", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON"))
                .addProperties("properties", new ObjectSchema()) // to be filled by caller
                .addProperties("id", new StringSchema())
                .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")));
    }
     */

    private class ContextJsonSchema {
        String objectKey = null;
        List<String> required = new Vector<>();
        Map<String, Object> properties = new TreeMap<>();
        Map<String, Object> geometry = ImmutableMap.of("type", "null");
        Set<FeatureSchema> definitions = new HashSet<>();
    }

    public Map<String, Object> getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMapJson.containsKey(apiHashCode))
            schemaMapJson.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMapJson.get(apiHashCode).containsKey(collectionId)) {

            FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                    .get(collectionId);
            String featureTypeId = apiData.getCollections()
                                          .get(collectionId)
                                          .getExtension(FeaturesCoreConfiguration.class)
                                          .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                          .orElse(collectionId);
            FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
            FeatureSchema featureType = featureProvider.getData()
                    .getTypes()
                    .get(featureTypeId);

            Optional<GeoJsonConfiguration> geoJsonConfiguration = collectionData.getExtension(GeoJsonConfiguration.class);
            boolean flatten = geoJsonConfiguration.filter(geoJsonConfig -> geoJsonConfig.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN &&
                                                                    geoJsonConfig.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                            .isPresent();

            ContextJsonSchema featureContext = processPropertiesJsonSchema(featureType, true, flatten);

            ImmutableMap.Builder<String, Object> definitionsMapBuilder = ImmutableMap.<String, Object>builder();
            Set<FeatureSchema> processed = new HashSet<>();
            Set<FeatureSchema> current = featureContext.definitions;

            while (!flatten && !current.isEmpty()) {
                Set<FeatureSchema> next = new HashSet<>();
                current.stream()
                        .filter(defObject -> !processed.contains(defObject))
                        .forEach(defObject -> {
                            ContextJsonSchema definitionContext = processPropertiesJsonSchema(defObject, false, false);
                            definitionsMapBuilder.put(definitionContext.objectKey, ImmutableMap.<String, Object>builder()
                                    .put("type", "object")
                                    .put("title", defObject.getLabel())
                                    .put("description", defObject.getDescription())
                                    .put("required", ImmutableList.builder()
                                            .addAll(definitionContext.required)
                                            .build())
                                    .put("properties", definitionContext.properties)
                                    .build());
                            next.addAll(definitionContext.definitions);
                            processed.add(defObject);
                        });
                current = next;
            }

            schemaMapJson.get(apiHashCode)
                         .put(collectionId, ImmutableMap.<String, Object>builder()
                                 .put("$schema", "http://json-schema.org/draft-07/schema#")
                                 .put("$id", schemaUri)
                                 .put("type", "object")
                                 .put("title", featureType.getLabel())
                                 .put("description", featureType.getDescription())
                                 .put("required", ImmutableList.builder()
                                                               .add("type", "geometry", "properties")
                                                               .build())
                                 .put("properties", ImmutableMap.builder()
                                                                .put("type", ImmutableMap.builder()
                                                                                         .put("type", "string")
                                                                                         .put("enum", ImmutableList.builder()
                                                                                                                   .add("Feature")
                                                                                                                   .build())
                                                                                         .build())
                                                                .put("id", ImmutableMap.builder()
                                                                                       .put("type", "string")
                                                                                       .build())
                                                                .put("links", ImmutableMap.builder()
                                                                                          .put("type", "array")
                                                                                          .put("items", ImmutableMap.builder().put("$ref", "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link").build())
                                                                                          .build())
                                                                .put("geometry", featureContext.geometry)
                                                                .put("properties", featureContext.properties)
                                                                .build())
                                 .put(DEFINITIONS_TOKEN, definitionsMapBuilder.build())
                                 .build());
        }
        return schemaMapJson.get(apiHashCode).get(collectionId);
    }

    private Map<String, Object> getJsonSchemaForLiteralType(de.ii.xtraplatform.features.domain.SchemaBase.Type type) {
        switch (type) {
            case INTEGER:
                return ImmutableMap.of("type", "integer");
            case FLOAT:
                return ImmutableMap.of("type", "number");
            case BOOLEAN:
                return ImmutableMap.of("type", "boolean");
            case DATETIME:
                return ImmutableMap.of("type", "string", "format", "date-time");
            case STRING:
                return ImmutableMap.of("type", "string");
        }
        return ImmutableMap.of();
    }

    private String getFallbackTypeName(FeatureSchema property) {
        return "type_"+Integer.toHexString(property.hashCode());
    }

    private ContextJsonSchema processPropertiesJsonSchema(FeatureSchema schema, boolean isFeature, boolean flatten) {

        ContextJsonSchema context = new ContextJsonSchema();
        context.objectKey = schema.getObjectType().orElse(getFallbackTypeName(schema));

        List<FeatureSchema> properties = flatten ? schema.getAllNestedProperties() : schema.getProperties();
        // maps from the dotted path name to the path name with array brackets
        Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() : getPropertyNames(schema,true).stream()
                .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String,String> nameTitleMap = !flatten ? ImmutableMap.of() : getNameTitleMap(schema);
        properties.stream()
                .forEachOrdered(property -> {
                    boolean geometry = false;
                    Map<String, Object> pSchema = null;
                    SchemaBase.Type propType = property.getType();
                    String propertyPath = String.join(".", property.getFullPath());
                    String propertyName = !flatten || property.isObject() ? property.getName() : propertyNameMap.get(propertyPath);
                    if (propertyName!=null)
                        propertyName = propertyName.replace("[]",".1");
                    switch (propType) {
                        case FLOAT:
                        case INTEGER:
                        case STRING:
                        case BOOLEAN:
                        case DATETIME:
                            pSchema = getJsonSchemaForLiteralType(propType);
                            break;
                        case VALUE_ARRAY:
                            pSchema = getJsonSchemaForLiteralType(property.getValueType().orElse(SchemaBase.Type.UNKNOWN));
                            break;
                        case OBJECT:
                        case OBJECT_ARRAY:
                            if (!flatten) {
                                // ignore intermediate objects in flattening mode, only process leaf properties
                                if (property.getObjectType().orElse("").equals("Link")) {
                                    pSchema = ImmutableMap.of("$ref", "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link");
                                    break;
                                }
                                pSchema = ImmutableMap.of("$ref", "#/"+DEFINITIONS_TOKEN+"/"+property.getObjectType().orElse(getFallbackTypeName(property)));
                                context.definitions.add(property);
                            }
                            break;
                        case GEOMETRY:
                            switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                                case POINT:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/Point.json");
                                    geometry = true;
                                    break;
                                case MULTI_POINT:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/MultiPoint.json");
                                    geometry = true;
                                    break;
                                case LINE_STRING:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/LineString.json");
                                    geometry = true;
                                    break;
                                case MULTI_LINE_STRING:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/MultiLineString.json");
                                    geometry = true;
                                    break;
                                case POLYGON:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/Polygon.json");
                                    geometry = true;
                                    break;
                                case MULTI_POLYGON:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/MultiPolygon.json");
                                    geometry = true;
                                    break;
                                case GEOMETRY_COLLECTION:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/GeometryCollection.json");
                                    geometry = true;
                                    break;
                                case NONE:
                                    pSchema = ImmutableMap.of("type", "null");
                                    geometry = true;
                                    break;
                                case ANY:
                                default:
                                    pSchema = ImmutableMap.of("$ref", "https://geojson.org/schema/Geometry.json");
                                    geometry = true;
                                    break;
                            }
                            break;
                        case UNKNOWN:
                        default:
                            pSchema = ImmutableMap.of();
                            break;
                    }

                    if (isFeature && geometry) {
                        // only one geometry per feature, last one wins
                        context.geometry = pSchema;
                    } else if (!flatten) {
                        ImmutableMap.Builder<String, Object> schemaBuilder = ImmutableMap.<String, Object>builder();
                        if (property.getLabel().isPresent())
                            schemaBuilder.put("title", property.getLabel().get());
                        if (property.getDescription().isPresent())
                            schemaBuilder.put("description", property.getDescription().get());
                        if (property.getConstraints().isPresent()) {
                            SchemaConstraints constraints = property.getConstraints().get();
                            if (constraints.getMinOccurrence().isPresent() && constraints.getRequired().get())
                                context.required.add(property.getName());
                        }

                        if (property.isArray()) {
                            schemaBuilder.put("type", "array")
                                    .put("items", pSchema);
                            if (property.getConstraints().isPresent()) {
                                SchemaConstraints constraints = property.getConstraints().get();
                                if (constraints.getMinOccurrence().isPresent())
                                    schemaBuilder.put("minItems", constraints.getMinOccurrence().get());
                                if (constraints.getMaxOccurrence().isPresent())
                                    schemaBuilder.put("maxItems", constraints.getMaxOccurrence().get());
                            }
                        } else {
                            schemaBuilder.putAll(pSchema);
                        }

                        context.properties.put(propertyName, schemaBuilder.build());
                    } else if (flatten && pSchema!=null) {
                        // we have a leaf property in flattening mode
                        ImmutableMap.Builder<String, Object> schemaBuilder = ImmutableMap.<String, Object>builder();
                        schemaBuilder.put("title", nameTitleMap.get(propertyPath));
                        if (property.getDescription().isPresent())
                            schemaBuilder.put("description", property.getDescription().get());
                        schemaBuilder.putAll(pSchema);

                        context.properties.put(propertyName, schemaBuilder.build());

                    }
                });

        return context;
    }
}
