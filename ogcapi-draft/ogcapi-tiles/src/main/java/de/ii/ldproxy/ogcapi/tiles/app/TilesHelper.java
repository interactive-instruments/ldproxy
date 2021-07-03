/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorGeoJson;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableFields;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTileLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTilePoint;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableVectorLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.TileLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.TilePoint;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.VectorLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.ImmutableTilesBoundingBox;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TilesBoundingBox;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TilesHelper {

    /**
     * generate the tile set metadata according to the OGC Tile Matrix Set standard (version 2.0.0, draft from June 2021)
     * @param apiData the API
     * @param tileMatrixSet the tile matrix set
     * @param zoomLevels the range of zoom levels
     * @param center the center point
     * @param collectionId the collection, empty = all collections in the dataset
     * @param links links to include in the object
     * @param uriCustomizer optional URI of the resource
     * @param limitsGenerator helper to generate the limits for each zoom level based on the bbox of the data
     * @param schemaGeneratorFeature helper to generate the JSON Schema of the features
     * @return the tile set metadata
     */
    public static TileSet buildTileSet(OgcApiDataV2 apiData,
                                       TileMatrixSet tileMatrixSet,
                                       MinMax zoomLevels,
                                       List<Double> center,
                                       Optional<String> collectionId,
                                       List<Link> links,
                                       Optional<URICustomizer> uriCustomizer,
                                       TileMatrixSetLimitsGenerator limitsGenerator,
                                       SchemaGeneratorGeoJson schemaGeneratorFeature) {

        ImmutableTileSet.Builder builder = ImmutableTileSet.builder()
                                                           .dataType(TileSet.DataType.vector);

        builder.tileMatrixSetId(tileMatrixSet.getId());

        if (tileMatrixSet.getURI().isPresent())
            builder.tileMatrixSetURI(tileMatrixSet.getURI().get().toString());
        else
            builder.tileMatrixSet(tileMatrixSet.getTileMatrixSetData());

        uriCustomizer.ifPresent(uriCustomizer1 -> builder.tileMatrixSetDefinition(uriCustomizer1.removeLastPathSegments(collectionId.isPresent() ? 3 : 1)
                                                                                                .clearParameters()
                                                                                                .ensureLastPathSegments("tileMatrixSets", tileMatrixSet.getId())
                                                                                                .toString()));
        builder.tileMatrixSetLimits(collectionId.isPresent()
                                            ? limitsGenerator.getCollectionTileMatrixSetLimits(apiData, collectionId.get(), tileMatrixSet, zoomLevels)
                                            : limitsGenerator.getTileMatrixSetLimits(apiData, tileMatrixSet, zoomLevels));

        Optional<BoundingBox> bbox = collectionId.isPresent() ? apiData.getSpatialExtent(collectionId.get()) : apiData.getSpatialExtent();
        bbox.ifPresent(boundingBox -> builder.boundingBox(ImmutableTilesBoundingBox.builder()
                                                                                   .lowerLeft(BigDecimal.valueOf(boundingBox.getXmin()).setScale(7, RoundingMode.HALF_UP),
                                                                                              BigDecimal.valueOf(boundingBox.getYmin()).setScale(7, RoundingMode.HALF_UP))
                                                                                   .upperRight(BigDecimal.valueOf(boundingBox.getXmax()).setScale(7, RoundingMode.HALF_UP),
                                                                                               BigDecimal.valueOf(boundingBox.getYmax()).setScale(7, RoundingMode.HALF_UP))
                                                                                   .crsEpsg(OgcCrs.CRS84)
                                                                                   .build()));

        if (zoomLevels.getDefault().isPresent() || Objects.nonNull(center)) {
            ImmutableTilePoint.Builder builder2 = new ImmutableTilePoint.Builder();
            zoomLevels.getDefault().ifPresent(def -> builder2.tileMatrix(String.valueOf(def)));
            if (Objects.nonNull(center))
                builder2.coordinates(center);
            builder.centerPoint(builder2.build());
        }

        // prepare a map with the JSON schemas of the feature collections used in the style
        SchemaGeneratorFeature.SCHEMA_TYPE schemaType = SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT;
        Map<String, JsonSchemaObject> schemaMap = collectionId.isPresent()
                ? apiData.getCollections()
                         .get(collectionId.get())
                         .getExtension(TilesConfiguration.class)
                         .filter(ExtensionConfiguration::isEnabled)
                         .map(config -> ImmutableMap.of(collectionId.get(), schemaGeneratorFeature.getSchemaJson(apiData, collectionId.get(), Optional.empty(), schemaType)))
                         .orElse(ImmutableMap.of())
                : apiData.getCollections()
                         .entrySet()
                         .stream()
                         .filter(entry -> {
                             Optional<TilesConfiguration> config = entry.getValue().getExtension(TilesConfiguration.class);
                             return entry.getValue().getEnabled() &&
                                     config.isPresent() &&
                                     config.get().getMultiCollectionEnabledDerived();
                         })
                         .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), schemaGeneratorFeature.getSchemaJson(apiData, entry.getKey(), Optional.empty(), schemaType)))
                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        schemaMap.entrySet()
                 .stream()
                 .forEach(entry -> {
                     String collectionId2 = entry.getKey();
                     FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                                                                            .get(collectionId2);

                     JsonSchemaObject schema = entry.getValue();
                     ImmutableTileLayer.Builder builder2 = ImmutableTileLayer.builder()
                                                                             .id(collectionId2)
                                                                             .title(collectionData.getLabel())
                                                                             .description(collectionData.getDescription())
                                                                             .dataType(TileSet.DataType.vector);

                     collectionData.getExtension(TilesConfiguration.class)
                                   .map(config -> config.getZoomLevelsDerived().get(tileMatrixSet.getId()))
                                   .ifPresent(minmax -> builder2.minTileMatrix(String.valueOf(minmax.getMin()))
                                                                .maxTileMatrix(String.valueOf(minmax.getMax())));

                     final JsonSchema geometry = schema.getProperties().get("geometry");
                     if (Objects.nonNull(geometry)) {
                         String geomAsString = geometry.toString();
                         boolean point = geomAsString.contains("GeoJSON Point") || geomAsString.contains("GeoJSON MultiPoint");
                         boolean line = geomAsString.contains("GeoJSON LineString") || geomAsString.contains("GeoJSON MultiLineString");
                         boolean polygon = geomAsString.contains("GeoJSON Polygon") || geomAsString.contains("GeoJSON MultiPolygon");
                         if (point && !line && !polygon)
                             builder2.geometryType(TileLayer.GeometryType.points);
                         else if (!point && line && !polygon)
                             builder2.geometryType(TileLayer.GeometryType.lines);
                         else if (!point && !line && polygon)
                             builder2.geometryType(TileLayer.GeometryType.polygons);
                     }

                     final JsonSchemaObject properties = (JsonSchemaObject) schema.getProperties().get("properties");
                     builder2.propertiesSchema(ImmutableJsonSchemaObject.builder()
                                                                        .required(properties.getRequired())
                                                                        .properties(properties.getProperties())
                                                                        .patternProperties(properties.getPatternProperties())
                                                                        .build());
                     builder.addLayers(builder2.build());
                 });

        builder.links(links);

        return builder.build();
    }

    /**
     * derive the bbox as a sequence left, bottom, right, upper
     * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
     * @return the bbox
     */
    public static List<Double> getBounds(TileSet tileset) {
        TilesBoundingBox bbox = tileset.getBoundingBox();
        return ImmutableList.of(bbox.getLowerLeft()[0].doubleValue(),
                                bbox.getLowerLeft()[1].doubleValue(),
                                bbox.getUpperRight()[0].doubleValue(),
                                bbox.getUpperRight()[1].doubleValue());
    }

    /**
     * derive the minimum zoom level
     * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
     * @return the zoom level
     */
    public static Optional<Integer> getMinzoom(TileSet tileset) {
        return tileset.getTileMatrixSetLimits()
                      .stream()
                      .map(TileMatrixSetLimits::getTileMatrix)
                      .map(Integer::valueOf)
                      .min(Integer::compareTo);
    }

    /**
     * derive the maximum zoom level
     * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
     * @return the zoom level
     */
    public static Optional<Integer> getMaxzoom(TileSet tileset) {
        return tileset.getTileMatrixSetLimits()
                      .stream()
                      .map(TileMatrixSetLimits::getTileMatrix)
                      .map(Integer::valueOf)
                      .max(Integer::compareTo);
    }

    /**
     * derive the default view as longitude, latitude, zoom level
     * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
     * @return the default view
     */
    public static List<Number> getCenter(TileSet tileset) {
        TilesBoundingBox bbox = tileset.getBoundingBox();
        double centerLon = tileset.getCenterPoint()
                                  .map(TilePoint::getCoordinates)
                                  .filter(coord -> coord.size() >= 2)
                                  .map(coord -> coord.get(0))
                                  .orElse(bbox.getLowerLeft()[0].doubleValue()+(bbox.getUpperRight()[0].doubleValue()-bbox.getLowerLeft()[0].doubleValue())*0.5);
        double centerLat = tileset.getCenterPoint()
                                  .map(TilePoint::getCoordinates)
                                  .filter(coord -> coord.size() >= 2)
                                  .map(coord -> coord.get(1))
                                  .orElse(bbox.getLowerLeft()[1].doubleValue()+(bbox.getUpperRight()[1].doubleValue()-bbox.getLowerLeft()[1].doubleValue())*0.5);
        int defaultZoomLevel = tileset.getCenterPoint()
                                      .map(TilePoint::getTileMatrix)
                                      .flatMap(level -> level)
                                      .map(Integer::valueOf)
                                      .orElse(0);
        return ImmutableList.of(centerLon, centerLat, defaultZoomLevel);
    }

    /**
     * generate the tile set metadata according to the TileJSON spec
     * @param apiData the API
     * @param collectionId the collection, empty = all collections in the dataset
     * @param tileMatrixSetId the well-known code of the tile matrix set
     * @param providers helper to access feature provide information
     * @param schemaInfo helper to derive the schema information
     * @return the tile set metadata
     */
    public static List<VectorLayer> getVectorLayers(OgcApiDataV2 apiData,
                                             Optional<String> collectionId,
                                             String tileMatrixSetId,
                                             FeaturesCoreProviders providers,
                                             SchemaInfo schemaInfo) {
        Map<String, FeatureTypeConfigurationOgcApi> featureTypesApi = apiData.getCollections();
        return featureTypesApi.values()
                              .stream()
                              .filter(featureTypeApi -> collectionId.isEmpty() || featureTypeApi.getId().equals(collectionId.get()))
                              .map(featureTypeApi -> {
                                  String featureTypeId = apiData.getCollections()
                                                                .get(featureTypeApi.getId())
                                                                .getExtension(FeaturesCoreConfiguration.class)
                                                                .map(cfg -> cfg.getFeatureType().orElse(featureTypeApi.getId()))
                                                                .orElse(featureTypeApi.getId());
                                  FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, featureTypeApi);
                                  FeatureSchema featureType = featureProvider.getData()
                                                                             .getTypes()
                                                                             .get(featureTypeId);
                                  Optional<GeoJsonConfiguration> geoJsonConfiguration = featureTypeApi.getExtension(GeoJsonConfiguration.class);
                                  boolean flatten = geoJsonConfiguration.filter(cfg -> cfg.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN && cfg.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                                                        .isPresent();
                                  List<FeatureSchema> properties = flatten ? featureType.getAllNestedProperties() : featureType.getProperties();
                                  // maps from the dotted path name to the path name with array brackets
                                  Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() :
                                          schemaInfo.getPropertyNames(apiData, featureTypeApi.getId(), false, true).stream()
                                                    .map(name -> new AbstractMap.SimpleImmutableEntry<>(name.replace("[]", ""), name))
                                                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
                                  ImmutableFields.Builder fieldsBuilder = new ImmutableFields.Builder();
                                  AtomicReference<String> geometryType = new AtomicReference<>("unknown");
                                  properties.forEach(property -> {
                                      SchemaBase.Type propType = property.getType();
                                      String propertyName = !flatten || property.isObject() ? property.getName() : propertyNameMap.get(String.join(".", property.getFullPath()));
                                      if (flatten && propertyName!=null)
                                          propertyName = propertyName.replace("[]",".1");
                                      switch (propType) {
                                          case FLOAT:
                                          case INTEGER:
                                          case STRING:
                                          case BOOLEAN:
                                          case DATETIME:
                                              fieldsBuilder.putAdditionalProperties(propertyName, getType(propType));
                                              break;
                                          case OBJECT:
                                          case OBJECT_ARRAY:
                                              if (!flatten) {
                                                  if (property.getObjectType().orElse("").equals("Link")) {
                                                      fieldsBuilder.putAdditionalProperties(propertyName, "link");
                                                      break;
                                                  }
                                                  fieldsBuilder.putAdditionalProperties(propertyName, "object");
                                              }
                                              break;
                                          case VALUE_ARRAY:
                                              fieldsBuilder.putAdditionalProperties(propertyName, getType(property.getValueType().orElse(SchemaBase.Type.UNKNOWN)));
                                              break;
                                          case GEOMETRY:
                                              switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                                                  case POINT:
                                                  case MULTI_POINT:
                                                      geometryType.set("points");
                                                      break;
                                                  case LINE_STRING:
                                                  case MULTI_LINE_STRING:
                                                      geometryType.set("lines");
                                                      break;
                                                  case POLYGON:
                                                  case MULTI_POLYGON:
                                                      geometryType.set("polygons");
                                                      break;
                                                  case GEOMETRY_COLLECTION:
                                                  case ANY:
                                                  case NONE:
                                                  default:
                                                      geometryType.set("unknown");
                                                      break;
                                              }
                                              break;
                                          case UNKNOWN:
                                          default:
                                              fieldsBuilder.putAdditionalProperties(propertyName, "unknown");
                                              break;
                                      }
                                  });

                                  ImmutableVectorLayer.Builder builder = ImmutableVectorLayer.builder()
                                                                                              .id(featureTypeApi.getId())
                                                                                              .description(featureTypeApi.getDescription().orElse(""))
                                                                                              .geometryType(geometryType.get())
                                                                                              .fields(fieldsBuilder.build());
                                  apiData.getExtension(TilesConfiguration.class, featureTypeApi.getId())
                                         .map(config -> config.getZoomLevelsDerived().get(tileMatrixSetId))
                                         .ifPresent(minmax -> builder.minzoom(minmax.getMin())
                                                                     .maxzoom(minmax.getMax()));
                                  return builder.build();
                              })
                              .collect(Collectors.toList());
    }

    /**
     * map the provider types to the TileJSON/Mbtiles types
     * @param type the provider type
     * @return the TileJSON/Mbtiles type
     */
    private static String getType(de.ii.xtraplatform.features.domain.SchemaBase.Type type) {
        switch (type) {
            case INTEGER:
            case FLOAT:
                return "Number";
            case BOOLEAN:
                return "Boolean";
            default:
            case DATETIME:
            case STRING:
                return "String";
        }
    }

}
