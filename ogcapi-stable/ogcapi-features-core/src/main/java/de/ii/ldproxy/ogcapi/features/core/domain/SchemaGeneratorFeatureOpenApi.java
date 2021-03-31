package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class SchemaGeneratorFeatureOpenApi extends SchemaGeneratorFeature implements SchemaGeneratorOpenApi {

    private final ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<SCHEMA_TYPE, Schema>>> schemaMapOpenApi = new ConcurrentHashMap<>();
    private final SchemaInfo schemaInfo;
    private final FeaturesCoreProviders providers;
    private final EntityRegistry entityRegistry;

    public SchemaGeneratorFeatureOpenApi(@Requires FeaturesCoreProviders providers,
                                         @Requires EntityRegistry entityRegistry,
                                         @Requires SchemaInfo schemaInfo) {
        this.providers = providers;
        this.entityRegistry = entityRegistry;
        this.schemaInfo = schemaInfo;
    }

    @Override
    public String getSchemaReferenceOpenApi(String collectionIdOrName, SCHEMA_TYPE type) {
        return "#/components/schemas/featureGeoJson_" + collectionIdOrName + "_" + type.toString().toLowerCase();
    }

    @Override
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
            if (Objects.isNull(featureType))
                // Use an empty object schema as fallback, if we cannot get one from the provider
                featureType = new ImmutableFeatureSchema.Builder()
                                                        .name(featureTypeId)
                                                        .type(SchemaBase.Type.OBJECT)
                                                        .build();

            schemaMapOpenApi.get(apiHashCode)
                            .get(collectionId)
                            .put(type, getSchemaOpenApi(featureType, collectionData, type));
        }
        return schemaMapOpenApi.get(apiHashCode).get(collectionId).get(type);
    }

    @Override
    public Schema getSchemaOpenApi(FeatureSchema featureType, FeatureTypeConfigurationOgcApi collectionData, SCHEMA_TYPE type) {

        // TODO support mutables schema
        ContextOpenApi featureContext;
        Schema schema;
        if (type== SCHEMA_TYPE.QUERYABLES) {
            // the querables schema is always flattened and we only return the queryables
            Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(FeaturesCoreConfiguration.class);
            Optional<FeaturesCollectionQueryables> queryables = Optional.empty();
            if (featuresCoreConfiguration.isPresent()) {
                queryables = featuresCoreConfiguration.get().getQueryables();
            }
            List<String> allQueryables = queryables.map(FeaturesCollectionQueryables::getAll).orElse(ImmutableList.of());
            featureContext = processPropertiesOpenApi(featureType, type, true, allQueryables);
            schema = new ObjectSchema().title(collectionData.getLabel())
                                       .description(collectionData.getDescription().orElse(featureType.getDescription().orElse(null)))
                                       .properties(featureContext.properties.getProperties());
        } else {
            // the returnables schema
            featureContext = processPropertiesOpenApi(featureType, type, true, null);
            schema = new ObjectSchema().title(collectionData.getLabel())
                                       .description(collectionData.getDescription().orElse(featureType.getDescription().orElse(null)))
                                       .addProperties("properties", featureContext.properties);
        }

        if (type==SCHEMA_TYPE.RETURNABLES || type==SCHEMA_TYPE.RETURNABLES_FLAT) {
            schema.required(ImmutableList.of("type", "geometry", "properties"))
                  .addProperties("type", new StringSchema()._enum(ImmutableList.of("Feature")))
                  .addProperties("geometry", featureContext.geometry)
                  .addProperties("id", featureContext.id)
                  .addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/Link")));
        }

        return schema;
    }

    @Override
    public Optional<Schema> getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId, String propertyName) {
        Schema featureSchema = getSchemaOpenApi(apiData, collectionId, SCHEMA_TYPE.QUERYABLES);
        if (Objects.isNull(featureSchema))
            return Optional.empty();
        if (Objects.isNull(featureSchema.getProperties()))
            return Optional.empty();
        if (featureSchema.getProperties().containsKey(propertyName))
            return Optional.ofNullable((Schema) featureSchema.getProperties().get(propertyName));
        if (propertyName.contains("[")) {
            String propertyPath = propertyName.replaceAll("\\[[^\\]]*\\]", "");
            if (featureSchema.getProperties().containsKey(propertyPath))
                return Optional.ofNullable((Schema) featureSchema.getProperties().get(propertyPath));
        }

        return Optional.empty();
    }

    private class ContextOpenApi {
        Schema id = new Schema();
        Schema properties = new ObjectSchema();
        Schema geometry = new Schema().nullable(true);
    }

    private Schema getSchemaForLiteralType(SchemaBase.Type type) {
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
                                   .map(val -> Integer.parseInt(val))
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

    private ContextOpenApi processPropertiesOpenApi(FeatureSchema schema, SCHEMA_TYPE type, boolean isFeature, List<String> propertySubset) {

        boolean flatten = (type != SCHEMA_TYPE.RETURNABLES);
        ContextOpenApi context = new ContextOpenApi();

        // maps from the dotted path name to the path name with array brackets
        Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() : schemaInfo.getPropertyNames(schema,true).stream()
                                                                                                         .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                                                                                                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String,String> nameTitleMap = !flatten ? ImmutableMap.of() : schemaInfo.getNameTitleMap(schema);

        List<FeatureSchema> properties;
        switch (type) {
            case QUERYABLES:
                properties = Objects.nonNull(schema) ?
                        schema.getAllNestedProperties()
                              .stream()
                              .filter(property -> propertySubset.stream()
                                                                // queryables have been normalized during hydration and there are no square brackets
                                                                .anyMatch(queryableProperty -> queryableProperty.equals(String.join(".", property.getFullPath()))))
                              .collect(Collectors.toList()) :
                        ImmutableList.of();
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

        properties.forEach(property -> {
            boolean geometry = false;
            Schema pSchema = null;
            SchemaBase.Type propType = property.getType();
            String propertyPath = String.join(".", property.getFullPath());
            String propertyName = !flatten || property.isObject() ? property.getName() : propertyNameMap.get(propertyPath);
            if (Objects.nonNull(propertyName) && flatten) {
                if (type==SCHEMA_TYPE.RETURNABLES || type==SCHEMA_TYPE.RETURNABLES_FLAT)
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
                    if (type==SCHEMA_TYPE.RETURNABLES) {
                        // ignore intermediate objects in flattening mode, only process leaf properties
                        if (property.getObjectType().orElse("").equals("Link")) {
                            pSchema = new Schema().$ref("#/components/schemas/Link");
                            break;
                        }
                        pSchema = processPropertiesOpenApi(property, type, false, null).properties;
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

                if (property.isId() && (type==SCHEMA_TYPE.RETURNABLES || type==SCHEMA_TYPE.RETURNABLES_FLAT)) {
                    context.id = pSchema;
                    List<String> requiredProperties = context.properties.getRequired();
                    if (Objects.nonNull(requiredProperties) && requiredProperties.contains(property.getName())) {
                        context.properties.required(requiredProperties.stream()
                                                                      .filter(p -> !p.equals(property.getName()))
                                                                      .collect(Collectors.toList()));
                    }
                } else {
                    context.properties.addProperties(propertyName, pSchema);
                }
            } else if (flatten && Objects.nonNull(pSchema)) {
                // we have a leaf property in flattening mode
                pSchema.title(nameTitleMap.get(propertyPath));
                if (property.getDescription().isPresent())
                    pSchema.description(property.getDescription().get());
                processConstraintsOpenApi(property, context, pSchema);

                if (property.isId() && (type==SCHEMA_TYPE.RETURNABLES || type==SCHEMA_TYPE.RETURNABLES_FLAT)) {
                    context.id = pSchema;
                    List<String> requiredProperties = context.properties.getRequired();
                    if (Objects.nonNull(requiredProperties) && requiredProperties.contains(property.getName())) {
                        context.properties.required(requiredProperties.stream()
                                                                      .filter(p -> !p.equals(property.getName()))
                                                                      .collect(Collectors.toList()));
                    }
                } else {
                    context.properties.addProperties(propertyName, pSchema);
                }
            }
        });

        return context;
    }
}
