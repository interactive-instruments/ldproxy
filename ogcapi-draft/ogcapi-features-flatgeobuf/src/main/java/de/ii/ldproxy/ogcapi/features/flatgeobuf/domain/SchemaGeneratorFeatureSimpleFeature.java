package de.ii.ldproxy.ogcapi.features.flatgeobuf.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Provides(specifications = {SchemaGeneratorFeatureSimpleFeature.class})
@Instantiate
public class SchemaGeneratorFeatureSimpleFeature extends SchemaGeneratorFeature {

    private final ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<SCHEMA_TYPE, Map<String, Class>>>> schemaMap = new ConcurrentHashMap<>();

    @Requires
    SchemaInfo schemaInfo;

    @Requires
    FeaturesCoreProviders providers;

    // TODO currently only for RETURNABLES_FLAT
    public Map<String, Class> getSchemaSimpleFeature(OgcApiDataV2 apiData, String collectionId, int maxMultiplicity, CoordinateReferenceSystem crs, List<String> propertySubset, SCHEMA_TYPE type) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId))
            schemaMap.get(apiHashCode).put(collectionId, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).get(collectionId).containsKey(type)) {

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

            Map<String, Class> properties = processPropertiesJsonSchema(featureType, type,true, propertySubset, maxMultiplicity);
            // SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            // builder.setName(collectionId);
            // builder.setCRS(crs);

            schemaMap.get(apiHashCode)
                     .get(collectionId)
                     .put(type, properties);
        }
        return schemaMap.get(apiHashCode).get(collectionId).get(type);
    }

    private void addProperty(ImmutableMap.Builder<String, Class> builder, String propertyName, Class clazz, int maxMultiplicity) {
        if (Objects.nonNull(propertyName)) {
            if (propertyName.contains("[]")) {
                for (int i=1; i<=maxMultiplicity; i++) {
                    String attName = propertyName.replaceFirst("\\[\\]", "." + i);
                    addProperty(builder, attName, clazz, maxMultiplicity);
                }
            } else {
                builder.put(propertyName, clazz);
            }
        }
    }

    private Map<String, Class> processPropertiesJsonSchema(FeatureSchema schema, SCHEMA_TYPE type, boolean isFeature, List<String> propertySubset, int maxMultiplicity) {

        ImmutableMap.Builder<String, Class> builder = ImmutableMap.builder();
        // maps from the dotted path name to the path name with array brackets
        Map<String,String> propertyNameMap = schemaInfo.getPropertyNames(schema,true).stream()
                                                       .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                                                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String,String> nameTitleMap = schemaInfo.getNameTitleMap(schema);

        List<FeatureSchema> properties = schema.getAllNestedProperties();
        final boolean[] geometry = {false};
        properties.stream()
                  .forEachOrdered(property -> {
                      JsonSchema jsonSchema = null;
                      SchemaBase.Type propType = property.getType();
                      String propertyPath = String.join(".", property.getFullPath());
                      String propertyName = property.isObject() ? property.getName() : propertyNameMap.get(propertyPath);
                      Optional<String> label = Optional.of(nameTitleMap.get(propertyPath));
                      String description = property.getDescription().orElse(null);
                      switch (propType) {
                          case FLOAT:
                              addProperty(builder, propertyName, Double.class, maxMultiplicity);
                              break;
                          case INTEGER:
                              addProperty(builder, propertyName, Integer.class, maxMultiplicity);
                              break;
                          case BOOLEAN:
                              addProperty(builder, propertyName, Boolean.class, maxMultiplicity);
                              break;
                          default:
                          case UNKNOWN:
                          case DATETIME:
                          case STRING:
                              addProperty(builder, propertyName, String.class, maxMultiplicity);
                              break;
                          case VALUE_ARRAY:
                              switch (property.getValueType().orElse(SchemaBase.Type.UNKNOWN)) {
                                  case FLOAT:
                                      addProperty(builder, propertyName, Double.class, maxMultiplicity);
                                      break;
                                  case INTEGER:
                                      addProperty(builder, propertyName, Integer.class, maxMultiplicity);
                                      break;
                                  case BOOLEAN:
                                      addProperty(builder, propertyName, Boolean.class, maxMultiplicity);
                                      break;
                                  default:
                                  case UNKNOWN:
                                  case DATETIME:
                                  case STRING:
                                      addProperty(builder, propertyName, String.class, maxMultiplicity);
                                      break;
                              }
                              break;
                          case OBJECT:
                          case OBJECT_ARRAY:
                              // ignore intermediate objects in flattening mode, only process leaf properties
                              break;
                          case GEOMETRY:
                              if (!geometry[0]) {
                                  // only use the first geometry
                                  switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                                      case POINT:
                                          addProperty(builder, "geometry", Point.class, 1);
                                          geometry[0] = true;
                                          break;
                                      case MULTI_POINT:
                                          addProperty(builder, "geometry", MultiPoint.class, 1);
                                          geometry[0] = true;
                                          break;
                                      case LINE_STRING:
                                          addProperty(builder, "geometry", LineString.class, 1);
                                          geometry[0] = true;
                                          break;
                                      case MULTI_LINE_STRING:
                                          addProperty(builder, "geometry", MultiLineString.class, 1);
                                          geometry[0] = true;
                                          break;
                                      case POLYGON:
                                          addProperty(builder, "geometry", Polygon.class, 1);
                                          geometry[0] = true;
                                          break;
                                      case MULTI_POLYGON:
                                          addProperty(builder, "geometry", MultiPolygon.class, 1);
                                          geometry[0] = true;
                                          break;
                                      case GEOMETRY_COLLECTION:
                                          addProperty(builder, "geometry", GeometryCollection.class, 1);
                                          geometry[0] = true;
                                          break;
                                      case NONE:
                                      case ANY:
                                      default:
                                          addProperty(builder, "geometry", Geometry.class, 1);
                                          geometry[0] = true;
                                          break;
                                  }
                              }

                              break;
                      }
                  });
        return builder.build();
    }
}
