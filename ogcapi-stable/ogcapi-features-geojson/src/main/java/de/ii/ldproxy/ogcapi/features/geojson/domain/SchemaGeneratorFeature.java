package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    public enum SCHEMA_TYPE {QUERYABLES, RETURNABLES, MUTABLES}

    private final ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<SCHEMA_TYPE, Schema>>> schemaMapOpenApi = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<SCHEMA_TYPE, Map<String, Object>>>> schemaMapJson = new ConcurrentHashMap<>();

    @Requires
    FeaturesCoreProviders providers;

    @Requires
    EntityRegistry entityRegistry;

    public String getSchemaReferenceOpenApi() {
        return "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureGeoJSON";
    }

    public Schema getSchemaOpenApi() {
        return GENERIC;
    }

    public String getSchemaReferenceOpenApi(String collectionIdOrName) {
        return getSchemaReferenceOpenApi(collectionIdOrName, SCHEMA_TYPE.RETURNABLES);
    }

    public String getSchemaReferenceOpenApi(String collectionIdOrName, SCHEMA_TYPE type) {
        return "#/components/schemas/featureGeoJson_" + collectionIdOrName + "_" + type.toString().toLowerCase();
    }

    public Schema getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId) {
        return getSchemaOpenApi(apiData, collectionId, SCHEMA_TYPE.RETURNABLES);
    }

    public Schema getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId, SCHEMA_TYPE type) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMapOpenApi.containsKey(apiHashCode))
            schemaMapOpenApi.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMapOpenApi.get(apiHashCode).containsKey(collectionId))
            schemaMapOpenApi.get(apiHashCode).put(collectionId, new ConcurrentHashMap<>());
        if (!schemaMapOpenApi.get(apiHashCode).get(collectionId).containsKey(type)) {
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

            // TODO support mutables schema
            ContextOpenApi featureContext;
            if (type==SCHEMA_TYPE.QUERYABLES) {
                // the querables schema is always flattened and we only return the queryables
                Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class);
                Optional<FeaturesCollectionQueryables> queryables = Optional.empty();
                if (featuresCoreConfiguration.isPresent()) {
                    queryables = featuresCoreConfiguration.get().getQueryables();
                }
                List<String> allQueryables = queryables.map(FeaturesCollectionQueryables::getAll).orElse(ImmutableList.of());
                featureContext = processPropertiesOpenApi(featureType, type, true, true, allQueryables);
            } else {
                // the returnables schema
                // flattening depends on the GeoJSON configuration
                // TODO we should generalize this and move it to Features Core; this is not just about GeoJSON, but applies to other feature encodings
                Optional<GeoJsonConfiguration> geoJsonConfiguration = collectionData.getExtension(GeoJsonConfiguration.class);
                boolean flatten = geoJsonConfiguration.filter(config -> config.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN && config.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                                      .isPresent();
                featureContext = processPropertiesOpenApi(featureType, type, true, flatten, null);
            }

            Schema schema = new ObjectSchema().title(collectionData.getLabel())
                                              .description(collectionData.getDescription().orElse(featureType.getDescription().orElse(null)))
                                              .addProperties("properties", featureContext.properties);
            if (type==SCHEMA_TYPE.RETURNABLES) {
                boolean integerId = featureType.getProperties().stream().anyMatch(prop -> prop.isId() && prop.getType()== SchemaBase.Type.INTEGER);
                schema.required(ImmutableList.of("type", "geometry", "properties"))
                      .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
                      .addProperties("geometry", featureContext.geometry)
                      .addProperties("id", integerId ? new IntegerSchema() : new StringSchema())
                      .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")));
            }
            schemaMapOpenApi.get(apiHashCode)
                            .get(collectionId)
                            .put(type, schema);
        }
        return schemaMapOpenApi.get(apiHashCode).get(collectionId).get(type);
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
        return (basePath.isEmpty() ? "" : basePath + ".") + property.getName().trim() + (property.isArray() && withArrayBrackets ? "[]" : "");
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

    public Map<String, String> getNameTitleMap(FeatureSchema featureType) {
        ImmutableMap.Builder<String, String> nameTitleMapBuilder = new ImmutableMap.Builder<>();
        featureType.getProperties()
                   .stream()
                   .forEach(featureProperty -> addPropertyTitles(nameTitleMapBuilder, featureProperty, "", ""));
        return nameTitleMapBuilder.build();
    }

    private Map.Entry<String, String> getPropertyTitle(FeatureSchema property, String basePath, String baseTitle) {
        String name = (basePath.isEmpty() ? "" : basePath + ".") + property.getName().trim();
        String title = (baseTitle.isEmpty() ? "" : baseTitle + " > ") + property.getLabel().orElse(property.getName()).trim();
        return new AbstractMap.SimpleImmutableEntry<>(name, title);
    }

    private void addPropertyTitles(ImmutableMap.Builder<String, String> nameTitleMapBuilder, FeatureSchema property, String basePath, String baseTitle) {
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
        Schema geometry = new Schema().nullable(true);
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

    private void processConstraintsOpenApi(FeatureSchema property,
                                           ContextOpenApi context,
                                           Schema pSchema) {
        if (property.getConstraints().isPresent()) {
            SchemaConstraints constraints = property.getConstraints().get();
            if (constraints.getRequired().isPresent() && constraints.getRequired().get())
                context.properties.addRequiredItem(property.getName());
            if (!constraints.getEnumValues().isEmpty()) {
                // if enum is specified in the configuration, it wins over codelist
                boolean string = property.isArray() ?
                        property.getValueType().orElse(SchemaBase.Type.UNKNOWN)!=SchemaBase.Type.INTEGER :
                        property.getType()!=SchemaBase.Type.INTEGER;
                pSchema.setEnum(string ?
                        constraints.getEnumValues() :
                        constraints.getEnumValues()
                                   .stream()
                                   .map(val -> Integer.getInteger(val))
                                   .collect(Collectors.toList()));
            } else if (constraints.getCodelist().isPresent()) {
                Optional<Codelist> codelist = entityRegistry.getEntitiesForType(Codelist.class)
                                                            .stream()
                                                            .filter(cl -> cl.getId().equals(constraints.getCodelist().get()))
                                                            .findAny();
                if (codelist.isPresent() && !codelist.get().getData().getFallback().isPresent()) {
                    boolean string = property.isArray() ?
                            property.getValueType().orElse(SchemaBase.Type.UNKNOWN)!=SchemaBase.Type.INTEGER :
                            property.getType()!=SchemaBase.Type.INTEGER;
                    Set<String> values = codelist.get().getData().getEntries().keySet();
                    pSchema.setEnum(string ?
                            values.stream().sorted().collect(Collectors.toList()) :
                            values.stream()
                                  .map(val -> Integer.valueOf(val))
                                  .sorted()
                                  .collect(Collectors.toList()));
                }
            }
            if (constraints.getRegex().isPresent())
                pSchema.setPattern(constraints.getRegex().get());
            if (constraints.getMin().isPresent())
                pSchema.setMinimum(BigDecimal.valueOf(constraints.getMin().get()));
            if (constraints.getMax().isPresent())
                pSchema.setMaximum(BigDecimal.valueOf(constraints.getMax().get()));
        }
    }

    private ContextOpenApi processPropertiesOpenApi(FeatureSchema schema, SCHEMA_TYPE type, boolean isFeature, boolean flatten, List<String> propertySubset) {

        ContextOpenApi context = new ContextOpenApi();

        // maps from the dotted path name to the path name with array brackets
        Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() : getPropertyNames(schema,true).stream()
                                                                                                         .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                                                                                                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String,String> nameTitleMap = !flatten ? ImmutableMap.of() : getNameTitleMap(schema);

        List<FeatureSchema> properties;
        switch (type) {
            case QUERYABLES:
                properties = schema.getAllNestedProperties()
                                   .stream()
                                   .filter(property -> propertySubset.stream()
                                                                     .anyMatch(queryableProperty -> queryableProperty.equals(propertyNameMap.get(String.join(".", property.getFullPath())))))
                                   .collect(Collectors.toList());
                break;

            case MUTABLES:
                // TODO
                properties = ImmutableList.of();
                break;

            case RETURNABLES:
            default:
                properties = flatten ? schema.getAllNestedProperties() : schema.getProperties();
                break;
        }

        properties.forEach(property -> {
            boolean geometry = false;
            Schema pSchema = null;
            SchemaBase.Type propType = property.getType();
            String propertyPath = String.join(".", property.getFullPath());
            String propertyName = !flatten || property.isObject() ? property.getName() : propertyNameMap.get(propertyPath);
            if (Objects.nonNull(propertyName) && flatten) {
                if (type==SCHEMA_TYPE.RETURNABLES)
                    propertyName = propertyName.replace("[]",".1");
                else if (type==SCHEMA_TYPE.QUERYABLES)
                    propertyName = propertyName.replace("[]","");
            }
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
                    if (type==SCHEMA_TYPE.RETURNABLES && !flatten) {
                        // ignore intermediate objects in flattening mode, only process leaf properties
                        if (property.getObjectType().orElse("").equals("Link")) {
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link");
                            break;
                        }
                        pSchema = processPropertiesOpenApi(property, type, false, flatten, null).properties;
                    }
                    break;
                case VALUE_ARRAY:
                    pSchema = getSchemaForLiteralType(property.getValueType().orElse(SchemaBase.Type.UNKNOWN));
                    break;
                case GEOMETRY:
                    switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                        case POINT:
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/pointGeoJSON");
                            geometry = true;;
                            break;
                        case MULTI_POINT:
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipointGeoJSON");
                            geometry = true;;
                            break;
                        case LINE_STRING:
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/linestringGeoJSON");
                            geometry = true;;
                            break;
                        case MULTI_LINE_STRING:
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multilinestringGeoJSON");
                            geometry = true;;
                            break;
                        case POLYGON:
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/polygonGeoJSON");
                            geometry = true;;
                            break;
                        case MULTI_POLYGON:
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/multipolygonGeoJSON");
                            geometry = true;;
                            break;
                        case GEOMETRY_COLLECTION:
                        case ANY:
                        default:
                            pSchema = new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/geometryGeoJSON");
                            geometry = true;;
                            break;
                        case NONE:
                            geometry = true;;
                            break;
                    }
                    break;
                case UNKNOWN:
                default:
                    pSchema = new Schema();
                    break;
            }

            if (isFeature && geometry) {
                // only one geometry per feature, last one wins
                context.geometry = pSchema;
            } else if (!flatten) {
                if (property.getLabel().isPresent())
                    pSchema.title(property.getLabel().get());
                if (property.getDescription().isPresent())
                    pSchema.description(property.getDescription().get());
                processConstraintsOpenApi(property, context, pSchema);

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

                context.properties.addProperties(propertyName, pSchema);
            } else if (flatten && pSchema != null) {
                // we have a leaf property in flattening mode
                pSchema.title(nameTitleMap.get(propertyPath));
                if (property.getDescription().isPresent())
                    pSchema.description(property.getDescription().get());
                processConstraintsOpenApi(property, context, pSchema);

                context.properties.addProperties(propertyName, pSchema);
            }
        });

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
        return getSchemaJson(apiData, collectionId, schemaUri, SCHEMA_TYPE.RETURNABLES);
    }

    public Map<String, Object> getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SCHEMA_TYPE type) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMapJson.containsKey(apiHashCode))
            schemaMapJson.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMapJson.get(apiHashCode).containsKey(collectionId))
            schemaMapJson.get(apiHashCode).put(collectionId, new ConcurrentHashMap<>());
        if (!schemaMapJson.get(apiHashCode).get(collectionId).containsKey(type)) {

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

            // TODO support mutables schema
            ContextJsonSchema featureContext;
            boolean flatten = false;
            if (type==SCHEMA_TYPE.QUERYABLES) {
                // the querables schema is always flattened and we only return the queryables
                flatten = true;
                Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class);
                Optional<FeaturesCollectionQueryables> queryables = Optional.empty();
                if (featuresCoreConfiguration.isPresent()) {
                    queryables = featuresCoreConfiguration.get().getQueryables();
                }
                List<String> allQueryables = queryables.map(FeaturesCollectionQueryables::getAll).orElse(ImmutableList.of());
                featureContext = processPropertiesJsonSchema(featureType, type, true, true, allQueryables);
            } else {
                // the returnables schema
                // flattening depends on the GeoJSON configuration
                Optional<GeoJsonConfiguration> geoJsonConfiguration = collectionData.getExtension(GeoJsonConfiguration.class);
                flatten = geoJsonConfiguration.filter(geoJsonConfig -> geoJsonConfig.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN &&
                        geoJsonConfig.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                                      .isPresent();

                featureContext = processPropertiesJsonSchema(featureType, type,true, flatten, null);
            }

            ImmutableMap.Builder<String, Object> definitionsMapBuilder = ImmutableMap.<String, Object>builder();
            Set<FeatureSchema> processed = new HashSet<>();
            Set<FeatureSchema> current = featureContext.definitions;

            while (!flatten && !current.isEmpty()) {
                Set<FeatureSchema> next = new HashSet<>();
                current.stream()
                        .filter(defObject -> !processed.contains(defObject))
                        .forEach(defObject -> {
                            ContextJsonSchema definitionContext = processPropertiesJsonSchema(defObject, type,false, false, null);
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

            String idType = featureType.getProperties().stream().anyMatch(prop -> prop.isId() && prop.getType()== SchemaBase.Type.INTEGER) ? "integer" : "string";
            schemaMapJson.get(apiHashCode)
                         .get(collectionId)
                         .put(type, ImmutableMap.<String, Object>builder()
                                 .put("$schema", "http://json-schema.org/draft-07/schema#")
                                 .put("$id", schemaUri)
                                 .put("type", "object")
                                 .put("title", collectionData.getLabel())
                                 .put("description", collectionData.getDescription().orElse(featureType.getDescription().orElse("")))
                                 .put("properties", type==SCHEMA_TYPE.RETURNABLES ?
                                         ImmutableMap.builder()
                                                     .put("required", ImmutableList.builder()
                                                                                   .add("type", "geometry", "properties")
                                                                                   .build())
                                                     .put("type", ImmutableMap.builder()
                                                                              .put("type", "string")
                                                                              .put("enum", ImmutableList.builder()
                                                                                                        .add("Feature")
                                                                                                        .build())
                                                                              .build())
                                                     .put("id", ImmutableMap.builder()
                                                                            .put("type", idType)
                                                                            .build())
                                                     .put("links", ImmutableMap.builder()
                                                                               .put("type", "array")
                                                                               .put("items", ImmutableMap.builder().put("$ref", "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link").build())
                                                                               .build())
                                                     .put("geometry", featureContext.geometry)
                                                     .put("properties", featureContext.properties)
                                                     .build() :
                                        ImmutableMap.builder()
                                                    .putAll(featureContext.properties)
                                                    .build())
                                 .put(DEFINITIONS_TOKEN, definitionsMapBuilder.build())
                                 .build());
        }
        return schemaMapJson.get(apiHashCode).get(collectionId).get(type);
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

    private void processConstrainsJsonSchema(FeatureSchema property,
                                             ContextJsonSchema context,
                                             ImmutableMap.Builder<String, Object> pSchemaBuilder) {
        if (property.getConstraints().isPresent()) {
            SchemaConstraints constraints = property.getConstraints().get();
            if (constraints.getRequired().isPresent() && constraints.getRequired().get())
                context.required.add(property.getName());
            if (!constraints.getEnumValues().isEmpty()) {
                // if enum is specified in the configuration, it wins over codelist
                boolean string = property.isArray() ?
                        property.getValueType().orElse(SchemaBase.Type.UNKNOWN)!=SchemaBase.Type.INTEGER :
                        property.getType()!=SchemaBase.Type.INTEGER;
                pSchemaBuilder.put("enum", string ?
                        constraints.getEnumValues() :
                        constraints.getEnumValues()
                                   .stream()
                                   .map(val -> Integer.getInteger(val))
                                   .collect(Collectors.toList()));
            } else if (constraints.getCodelist().isPresent()) {
                Optional<Codelist> codelist = entityRegistry.getEntitiesForType(Codelist.class)
                                                            .stream()
                                                            .filter(cl -> cl.getId().equals(constraints.getCodelist().get()))
                                                            .findAny();
                if (codelist.isPresent() && !codelist.get().getData().getFallback().isPresent()) {
                    boolean string = property.isArray() ?
                            property.getValueType().orElse(SchemaBase.Type.UNKNOWN)!=SchemaBase.Type.INTEGER :
                            property.getType()!=SchemaBase.Type.INTEGER;
                    Set<String> values = codelist.get().getData().getEntries().keySet();
                    pSchemaBuilder.put("enum", string ?
                            values :
                            values.stream()
                                  .map(val -> Integer.valueOf(val))
                                  .sorted()
                                  .collect(Collectors.toList()));
                }
            }
            if (constraints.getRegex().isPresent())
                pSchemaBuilder.put("pattern", constraints.getRegex().get());
            if (constraints.getMin().isPresent())
                pSchemaBuilder.put("minimum", constraints.getMin().get());
            if (constraints.getMax().isPresent())
                pSchemaBuilder.put("maximum", constraints.getMax().get());
        }
    }

    private ContextJsonSchema processPropertiesJsonSchema(FeatureSchema schema, SCHEMA_TYPE type, boolean isFeature, boolean flatten, List<String> propertySubset) {

        ContextJsonSchema context = new ContextJsonSchema();
        context.objectKey = schema.getObjectType().orElse(getFallbackTypeName(schema));

        // maps from the dotted path name to the path name with array brackets
        Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() : getPropertyNames(schema,true).stream()
                                                                                                         .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                                                                                                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String,String> nameTitleMap = !flatten ? ImmutableMap.of() : getNameTitleMap(schema);

        List<FeatureSchema> properties;
        switch (type) {
            case QUERYABLES:
                properties = schema.getAllNestedProperties()
                                   .stream()
                                   .filter(property -> propertySubset.stream()
                                                                     .anyMatch(queryableProperty -> queryableProperty.equals(propertyNameMap.get(String.join(".", property.getFullPath())))))
                                   .collect(Collectors.toList());
                break;

            case MUTABLES:
                // TODO
                properties = ImmutableList.of();
                break;

            case RETURNABLES:
            default:
                properties = flatten ? schema.getAllNestedProperties() : schema.getProperties();
                break;
        }

        properties.stream()
                  .forEachOrdered(property -> {
                      boolean geometry = false;
                      ImmutableMap.Builder<String, Object> pSchemaBuilder = ImmutableMap.builder();
                      SchemaBase.Type propType = property.getType();
                      String propertyPath = String.join(".", property.getFullPath());
                      String propertyName = !flatten || property.isObject() ? property.getName() : propertyNameMap.get(propertyPath);
                      if (Objects.nonNull(propertyName)) {
                          if (type==SCHEMA_TYPE.RETURNABLES)
                              propertyName = propertyName.replace("[]",".1");
                          else if (type==SCHEMA_TYPE.QUERYABLES)
                              propertyName = propertyName.replace("[]","");
                      }
                      switch (propType) {
                          case FLOAT:
                          case INTEGER:
                          case STRING:
                          case BOOLEAN:
                          case DATETIME:
                              pSchemaBuilder.putAll(getJsonSchemaForLiteralType(propType));
                              break;
                          case VALUE_ARRAY:
                              pSchemaBuilder.putAll(getJsonSchemaForLiteralType(property.getValueType().orElse(SchemaBase.Type.UNKNOWN)));
                              break;
                          case OBJECT:
                          case OBJECT_ARRAY:
                              if (type==SCHEMA_TYPE.RETURNABLES && !flatten) {
                                  // ignore intermediate objects in flattening mode, only process leaf properties
                                  if (property.getObjectType().orElse("").equals("Link")) {
                                      pSchemaBuilder.put("$ref", "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link");
                                      break;
                                  }
                                  pSchemaBuilder.put("$ref", "#/"+DEFINITIONS_TOKEN+"/"+property.getObjectType().orElse(getFallbackTypeName(property)));
                                  context.definitions.add(property);
                              }
                              break;
                          case GEOMETRY:
                              switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                                  case POINT:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/Point.json");
                                      geometry = true;
                                      break;
                                  case MULTI_POINT:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/MultiPoint.json");
                                      geometry = true;
                                      break;
                                  case LINE_STRING:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/LineString.json");
                                      geometry = true;
                                      break;
                                  case MULTI_LINE_STRING:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/MultiLineString.json");
                                      geometry = true;
                                      break;
                                  case POLYGON:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/Polygon.json");
                                      geometry = true;
                                      break;
                                  case MULTI_POLYGON:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/MultiPolygon.json");
                                      geometry = true;
                                      break;
                                  case GEOMETRY_COLLECTION:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/GeometryCollection.json");
                                      geometry = true;
                                      break;
                                  case NONE:
                                      pSchemaBuilder.put("type", "null");
                                      geometry = true;
                                      break;
                                  case ANY:
                                  default:
                                      pSchemaBuilder.put("$ref", "https://geojson.org/schema/Geometry.json");
                                      geometry = true;
                                      break;
                              }
                              break;
                          case UNKNOWN:
                          default:
                              break;
                      }

                      if (isFeature && geometry && type != SCHEMA_TYPE.QUERYABLES) {
                          // only one geometry per feature, last one wins
                          context.geometry = pSchemaBuilder.build();
                      } else if (!flatten) {
                          ImmutableMap.Builder<String, Object> schemaBuilder = ImmutableMap.builder();
                          if (property.getLabel().isPresent())
                              schemaBuilder.put("title", property.getLabel().get());
                          if (property.getDescription().isPresent())
                              schemaBuilder.put("description", property.getDescription().get());
                          processConstrainsJsonSchema(property, context, pSchemaBuilder);

                          if (property.isArray()) {
                              schemaBuilder.put("type", "array")
                                           .put("items", pSchemaBuilder.build());
                              if (property.getConstraints().isPresent()) {
                                  SchemaConstraints constraints = property.getConstraints().get();
                                  if (constraints.getMinOccurrence().isPresent())
                                      schemaBuilder.put("minItems", constraints.getMinOccurrence().get());
                                  if (constraints.getMaxOccurrence().isPresent())
                                      schemaBuilder.put("maxItems", constraints.getMaxOccurrence().get());
                              }
                          } else {
                              schemaBuilder.putAll(pSchemaBuilder.build());
                          }

                          context.properties.put(propertyName, schemaBuilder.build());
                      } else if (flatten) {
                          // we have a leaf property in flattening mode
                          processConstrainsJsonSchema(property, context, pSchemaBuilder);
                          ImmutableMap<String, Object> pSchema = pSchemaBuilder.build();
                          if (!pSchema.isEmpty()) {
                              ImmutableMap.Builder<String, Object> schemaBuilder = ImmutableMap.<String, Object>builder();
                              schemaBuilder.put("title", nameTitleMap.get(propertyPath));
                              if (property.getDescription().isPresent())
                                  schemaBuilder.put("description", property.getDescription().get());
                              schemaBuilder.putAll(pSchema);

                              context.properties.put(propertyName, schemaBuilder.build());
                          }
                      }
                  });

        return context;
    }
}
