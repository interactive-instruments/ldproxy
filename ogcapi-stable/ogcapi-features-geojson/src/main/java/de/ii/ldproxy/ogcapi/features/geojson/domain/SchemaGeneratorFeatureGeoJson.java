package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

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
@Provides(specifications = {SchemaGeneratorFeatureGeoJson.class})
@Instantiate
public class SchemaGeneratorFeatureGeoJson extends SchemaGeneratorFeature {

    final static String DEFINITIONS_TOKEN = "$defs";

    final static JsonSchemaObject LINK_JSON = ImmutableJsonSchemaObject.builder()
                                                                       .putProperties("href", ImmutableJsonSchemaString.builder()
                                                                                                                       .format("uri-reference")
                                                                                                                       .build())
                                                                       .putProperties("rel", ImmutableJsonSchemaString.builder().build())
                                                                       .putProperties("type", ImmutableJsonSchemaString.builder().build())
                                                                       .putProperties("title", ImmutableJsonSchemaString.builder().build())
                                                                       .addRequired("href")
                                                                       .build();

    private final ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<SCHEMA_TYPE, JsonSchemaObject>>> schemaMapJson = new ConcurrentHashMap<>();

    @Requires
    SchemaInfo schemaInfo;

    @Requires
    FeaturesCoreProviders providers;

    @Requires
    EntityRegistry entityRegistry;

    private class ContextJsonSchema {
        String objectKey = null;
        List<String> required = new Vector<>();
        JsonSchema id = null;
        Map<String, JsonSchema> properties = new TreeMap<>();
        Map<String, JsonSchema> patternProperties = new TreeMap<>();
        JsonSchema geometry = ImmutableJsonSchemaNull.builder()
                                                     .build();
        Set<FeatureSchema> definitions = new HashSet<>();
    }

    public JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SCHEMA_TYPE type) {
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

            schemaMapJson.get(apiHashCode)
                         .get(collectionId)
                         .put(type, getSchemaJson(featureType, collectionData, schemaUri, type));
        }
        return schemaMapJson.get(apiHashCode).get(collectionId).get(type);
    }

    public JsonSchemaObject getSchemaJson(FeatureSchema featureSchema, FeatureTypeConfigurationOgcApi collectionData, Optional<String> schemaUri, SCHEMA_TYPE type) {
        // TODO support mutables schema
        ContextJsonSchema featureContext;
        boolean flatten = false;
        if (type== SCHEMA_TYPE.QUERYABLES) {
            // the querables schema is always flattened and we only return the queryables
            flatten = true;
            Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class);
            Optional<FeaturesCollectionQueryables> queryables = Optional.empty();
            if (featuresCoreConfiguration.isPresent()) {
                queryables = featuresCoreConfiguration.get().getQueryables();
            }
            List<String> allQueryables = queryables.map(FeaturesCollectionQueryables::getAll).orElse(ImmutableList.of());
            featureContext = processPropertiesJsonSchema(featureSchema, type, true, allQueryables);
        } else {
            // the returnables schema
            // flattening depends on the GeoJSON configuration
            flatten = type==SCHEMA_TYPE.RETURNABLES_FLAT;

            featureContext = processPropertiesJsonSchema(featureSchema, type,true, null);
        }

        ImmutableMap.Builder<String, JsonSchema> definitionsMapBuilder = ImmutableMap.builder();
        Set<FeatureSchema> processed = new HashSet<>();
        Set<FeatureSchema> current = featureContext.definitions;

        final boolean[] linkAdded = {false};
        if (type==SCHEMA_TYPE.RETURNABLES || type==SCHEMA_TYPE.RETURNABLES_FLAT) {
            // we know, we will reference links
            definitionsMapBuilder.put("Link", LINK_JSON);
            linkAdded[0] = true;
        }

        while (!flatten && !current.isEmpty()) {
            Set<FeatureSchema> next = new HashSet<>();
            current.stream()
                    .filter(defObject -> !processed.contains(defObject))
                    .forEach(defObject -> {
                        if (defObject.getObjectType().isPresent() && defObject.getObjectType().get().equals("Link")) {
                            if (!linkAdded[0]) {
                                definitionsMapBuilder.put("Link", LINK_JSON);
                                linkAdded[0] = true;
                            }
                        } else {
                            ContextJsonSchema definitionContext = processPropertiesJsonSchema(defObject, type, false, null);
                            definitionsMapBuilder.put(definitionContext.objectKey, ImmutableJsonSchemaObject.builder()
                                    .title(defObject.getLabel())
                                    .description(defObject.getDescription())
                                    .required(ImmutableList.<String>builder()
                                            .addAll(definitionContext.required)
                                            .build())
                                    .properties(definitionContext.properties)
                                    .build());
                            next.addAll(definitionContext.definitions);
                        }
                        processed.add(defObject);
                    });
            current = next;
        }

        return ImmutableJsonSchemaObject.builder()
                .schema("https://json-schema.org/draft/2019-09/schema")
                .id(schemaUri)
                .title(collectionData.getLabel())
                .description(collectionData.getDescription().orElse(featureSchema.getDescription().orElse("")))
                .required(type==SCHEMA_TYPE.RETURNABLES || type==SCHEMA_TYPE.RETURNABLES_FLAT ?
                        ImmutableList.<String>builder()
                                .add("type", "geometry", "properties")
                                .build() :
                        ImmutableList.of())
                .properties(type==SCHEMA_TYPE.RETURNABLES || type==SCHEMA_TYPE.RETURNABLES_FLAT ?
                        ImmutableMap.<String,JsonSchema>builder()
                                .put("type", ImmutableJsonSchemaString.builder()
                                        .enums(ImmutableList.<String>builder()
                                                .add("Feature")
                                                .build())
                                        .build())
                                .put("id", featureContext.id)
                                .put("links", ImmutableJsonSchemaArray.builder()
                                        .items(ImmutableJsonSchemaRef.builder()
                                                .ref("#/"+DEFINITIONS_TOKEN+"/Link")
                                                .build())
                                        .build())
                                .put("geometry", featureContext.geometry)
                                .put("properties", ImmutableJsonSchemaObject.builder()
                                        .required(ImmutableList.<String>builder()
                                                .addAll(featureContext.required)
                                                .build())
                                        .properties(featureContext.properties)
                                        .patternProperties(featureContext.patternProperties)
                                        .build())
                                .build() :
                        ImmutableMap.<String,JsonSchema>builder()
                                .putAll(featureContext.properties)
                                .build())
                .defs(definitionsMapBuilder.build())
                .build();
    }

    private JsonSchema getJsonSchemaForLiteralType(SchemaBase.Type type, Optional<String> label, Optional<String> description) {
        switch (type) {
            case INTEGER:
                return ImmutableJsonSchemaInteger.builder()
                                                 .title(label)
                                                 .description(description)
                                                 .build();
            case FLOAT:
                return ImmutableJsonSchemaNumber.builder()
                                                .title(label)
                                                .description(description)
                                                .build();
            case BOOLEAN:
                return ImmutableJsonSchemaBoolean.builder()
                                                 .title(label)
                                                 .description(description)
                                                 .build();

            // TODO state either date or date-time, but we need a way to determine what it is
            //      validators will ignore this informaton as it isn't a well-known format value
            case DATETIME:
                return ImmutableJsonSchemaString.builder()
                                                .format("date-time,date")
                                                .title(label)
                                                .description(description)
                                                .build();

            case STRING:
                return ImmutableJsonSchemaString.builder()
                                                .title(label)
                                                .description(description)
                                                .build();
        }
        return ImmutableJsonSchemaString.builder()
                                        .title(label)
                                        .description(description)
                                        .build();
    }

    private String getFallbackTypeName(FeatureSchema property) {
        return "type_"+Integer.toHexString(property.hashCode());
    }

    private JsonSchema processConstraintsJsonSchema(FeatureSchema property,
                                                    ContextJsonSchema context,
                                                    JsonSchema jsonSchema,
                                                    boolean setRequired) {
        JsonSchema result = jsonSchema;
        if (property.getConstraints().isPresent()) {
            SchemaConstraints constraints = property.getConstraints().get();
            if (setRequired && constraints.getRequired().isPresent() && constraints.getRequired().get())
                context.required.add(property.getName());
            if (!constraints.getEnumValues().isEmpty()) {
                // if enum is specified in the configuration, it wins over codelist
                boolean string = property.isArray() ?
                        property.getValueType().orElse(SchemaBase.Type.UNKNOWN)!=SchemaBase.Type.INTEGER :
                        property.getType()!=SchemaBase.Type.INTEGER;
                result = string ?
                        ImmutableJsonSchemaString.builder()
                                                 .from(result)
                                                 .enums(constraints.getEnumValues())
                                                 .build() :
                        ImmutableJsonSchemaInteger.builder()
                                                  .from(result)
                                                  .enums(constraints.getEnumValues()
                                                                    .stream()
                                                                    .map(val -> Integer.getInteger(val))
                                                                    .collect(Collectors.toList()))
                                                  .build();
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
                    result = string ?
                            ImmutableJsonSchemaString.builder()
                                                     .from(result)
                                                     .enums(values)
                                                     .build() :
                            ImmutableJsonSchemaInteger.builder()
                                                      .from(result)
                                                      .enums(values.stream()
                                                                   .map(val -> Integer.valueOf(val))
                                                                   .sorted()
                                                                   .collect(Collectors.toList()))
                                                      .build();
                }
            }
            if (constraints.getRegex().isPresent() && result instanceof ImmutableJsonSchemaString) {
                result = ImmutableJsonSchemaString.builder()
                                                  .from(result)
                                                  .pattern(constraints.getRegex().get())
                                                  .build();
            }
            if (constraints.getMin().isPresent() || constraints.getMax().isPresent()) {
                if (result instanceof ImmutableJsonSchemaInteger) {
                    ImmutableJsonSchemaInteger.Builder builder = ImmutableJsonSchemaInteger.builder()
                                                                                           .from(result);
                    if (constraints.getMin().isPresent())
                        builder.minimum(Math.round(constraints.getMin().get()));
                    if (constraints.getMax().isPresent())
                        builder.minimum(Math.round(constraints.getMax().get()));
                    result = builder.build();
                } else if (result instanceof ImmutableJsonSchemaNumber) {
                    ImmutableJsonSchemaNumber.Builder builder = ImmutableJsonSchemaNumber.builder()
                                                                                         .from(result);
                    if (constraints.getMin().isPresent())
                        builder.minimum(constraints.getMin().get());
                    if (constraints.getMax().isPresent())
                        builder.minimum(constraints.getMax().get());
                    result = builder.build();
                }
            }
        }

        return result;
    }

    private ContextJsonSchema processPropertiesJsonSchema(FeatureSchema schema, SCHEMA_TYPE type, boolean isFeature, List<String> propertySubset) {

        boolean flatten = (type==SCHEMA_TYPE.RETURNABLES_FLAT || type==SCHEMA_TYPE.QUERYABLES);
        ContextJsonSchema context = new ContextJsonSchema();
        context.objectKey = schema.getObjectType().orElse(getFallbackTypeName(schema));

        // maps from the dotted path name to the path name with array brackets
        Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() : schemaInfo.getPropertyNames(schema,true).stream()
                                                                                                         .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                                                                                                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String,String> nameTitleMap = !flatten ? ImmutableMap.of() : schemaInfo.getNameTitleMap(schema);

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

            case RETURNABLES_FLAT:
                properties = schema.getAllNestedProperties();
                break;

            case RETURNABLES:
            default:
                properties = schema.getProperties();
                break;
        }

        properties.stream()
                  .forEachOrdered(property -> {
                      boolean geometry = false;
                      JsonSchema jsonSchema = null;
                      SchemaBase.Type propType = property.getType();
                      String propertyPath = String.join(".", property.getFullPath());
                      String propertyName = !flatten || property.isObject() ? property.getName() : propertyNameMap.get(propertyPath);
                      if (Objects.nonNull(propertyName)) {
                          if (type==SCHEMA_TYPE.RETURNABLES_FLAT)
                              propertyName = propertyName.replace("[]","\\.\\d+");
                          else if (type==SCHEMA_TYPE.QUERYABLES)
                              propertyName = propertyName.replace("[]","");
                      }
                      Optional<String> label = flatten ? Optional.of(nameTitleMap.get(propertyPath)) : property.getLabel();
                      Optional<String> description = flatten ? property.getDescription() : property.getDescription() ;
                      switch (propType) {
                          case FLOAT:
                          case INTEGER:
                          case STRING:
                          case BOOLEAN:
                          case DATETIME:
                              jsonSchema = getJsonSchemaForLiteralType(propType, label, description);
                              break;
                          case VALUE_ARRAY:
                              jsonSchema = getJsonSchemaForLiteralType(property.getValueType().orElse(SchemaBase.Type.UNKNOWN), label, description);
                              break;
                          case OBJECT:
                          case OBJECT_ARRAY:
                              // ignore intermediate objects in flattening mode, only process leaf properties
                              if (type==SCHEMA_TYPE.RETURNABLES) {
                                  jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                     .ref("#/"+DEFINITIONS_TOKEN+"/"+property.getObjectType().orElse(getFallbackTypeName(property)))
                                                                     .build();
                                  context.definitions.add(property);
                              }
                              break;
                          case GEOMETRY:
                              switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                                  case POINT:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/Point.json")
                                                                         .build();
                                      geometry = true;
                                      break;
                                  case MULTI_POINT:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/MultiPoint.json")
                                                                         .build();
                                      geometry = true;
                                      break;
                                  case LINE_STRING:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/LineString.json")
                                                                         .build();
                                      geometry = true;
                                      break;
                                  case MULTI_LINE_STRING:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/MultiLineString.json")
                                                                         .build();
                                      geometry = true;
                                      break;
                                  case POLYGON:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/Polygon.json")
                                                                         .build();
                                      geometry = true;
                                      break;
                                  case MULTI_POLYGON:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/MultiPolygon.json")
                                                                         .build();
                                      geometry = true;
                                      break;
                                  case GEOMETRY_COLLECTION:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/GeometryCollection.json")
                                                                         .build();
                                      geometry = true;
                                      break;
                                  case NONE:
                                      jsonSchema = ImmutableJsonSchemaNull.builder()
                                                                          .build();
                                      geometry = true;
                                      break;
                                  case ANY:
                                  default:
                                      jsonSchema = ImmutableJsonSchemaRef.builder()
                                                                         .ref("https://geojson.org/schema/Geometry.json")
                                                                         .build();
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
                          context.geometry = jsonSchema;
                      } else if (!flatten) {
                          jsonSchema = processConstraintsJsonSchema(property, context, jsonSchema, true);
                          if (property.isArray()) {
                              ImmutableJsonSchemaArray.Builder builder = ImmutableJsonSchemaArray.builder()
                                                                                                 .items(jsonSchema);
                              if (property.getConstraints().isPresent()) {
                                  SchemaConstraints constraints = property.getConstraints().get();
                                  builder.minItems(constraints.getMinOccurrence())
                                          .maxItems(constraints.getMaxOccurrence());
                              }
                              jsonSchema = builder.build();
                          }

                          if (property.isId() && type==SCHEMA_TYPE.RETURNABLES) {
                              context.id = jsonSchema;
                              context.required.remove(property.getName());
                          } else {
                              context.properties.put(propertyName, jsonSchema);
                          }
                      } else if (flatten && Objects.nonNull(jsonSchema)) {
                          // we have a leaf property in flattening mode
                          // leaf properties can only be required, if all nodes in the path are required; in general,
                          // we do not have this information at hand, so required properties are not set, unless the
                          // leaf is on level 1
                          jsonSchema = processConstraintsJsonSchema(property, context, jsonSchema, property.getParentPath().isEmpty());
                          if (property.isId() && type==SCHEMA_TYPE.RETURNABLES_FLAT) {
                              context.id = jsonSchema;
                              context.required.remove(property.getName());
                          } else if (propertyName.contains("\\.\\d+") && type==SCHEMA_TYPE.RETURNABLES_FLAT) {
                              context.patternProperties.put("^"+propertyName+"$", jsonSchema);
                          } else {
                              context.properties.put(propertyName, jsonSchema);
                          }
                      }
                  });

        return context;
    }
}
