/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.domain.SchemaGenerator;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.MediaType;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class TileSetFormatTileJson implements TileSetFormatExtension {

    @Requires
    SchemaGenerator schemaGenerator;

    @Requires
    SchemaGeneratorFeature schemaGeneratorFeature;

    @Requires
    FeaturesCoreProviders providers;

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .label("TileJSON")
            .parameter("json")
            .build();

    private final Schema schemaTileJson;
    public final static String SCHEMA_REF_TILE_JSON = "#/components/schemas/TileJson";

    public TileSetFormatTileJson() {
        schemaTileJson = schemaGenerator.getSchema(TileJson.class);
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        if (path.endsWith("/tiles/{tileMatrixSetId}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaTileJson)
                    .schemaRef(SCHEMA_REF_TILE_JSON)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public Object getTileSetEntity(OgcApiDataV2 apiData, ApiRequestContext requestContext,
                                   Optional<String> collectionId,
                                   TileMatrixSet tileMatrixSet, MinMax zoomLevels, double[] center,
                                   List<Link> links) {

        String tilesUriTemplate = getTilesUriTemplate(links, tileMatrixSet);

        ImmutableTileJson.Builder tileJsonBuilder = ImmutableTileJson.builder();

        tileJsonBuilder.tilejson("3.0.0")
                .name(apiData.getLabel())
                .description(apiData.getDescription())
                .tiles(ImmutableList.of(tilesUriTemplate));

        // TODO: add support for attribution and version (manage revisions to the data)

        Optional<BoundingBox> bbox = collectionId.isPresent() ? apiData.getSpatialExtent(collectionId.get()) : apiData.getSpatialExtent();
        if (bbox.isPresent()) {
            tileJsonBuilder.bounds(ImmutableList.of(bbox.get().getXmin(), bbox.get().getYmin(), bbox.get().getXmax(), bbox.get().getYmax()));
        }

        List<Integer> minMaxZoom = getMinMaxZoom(zoomLevels, tileMatrixSet);
        tileJsonBuilder.minzoom(minMaxZoom.get(0))
                       .maxzoom(minMaxZoom.get(1));

        double centerLon = (Objects.nonNull(center) && center.length>=1) ? center[0] : bbox.get().getXmin()+(bbox.get().getXmax()-bbox.get().getXmin())*0.5;
        double centerLat = (Objects.nonNull(center) && center.length>=2) ? center[1] : bbox.get().getYmin()+(bbox.get().getYmax()-bbox.get().getYmin())*0.5;
        int defaultZoomLevel = zoomLevels.getDefault().orElse(minMaxZoom.get(0) + (minMaxZoom.get(1)-minMaxZoom.get(0))/2);
        tileJsonBuilder.center(ImmutableList.of(centerLon, centerLat, defaultZoomLevel));

        Map<String, FeatureTypeConfigurationOgcApi> featureTypesApi = apiData.getCollections();
        List<ImmutableVectorLayer> layers = featureTypesApi.values()
                .stream()
                .filter(featureTypeApi -> !collectionId.isPresent() || featureTypeApi.getId().equals(collectionId.get()))
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
                    Optional<FeaturesCoreConfiguration> featuresCoreConfiguration = featureTypeApi.getExtension(FeaturesCoreConfiguration.class);
                    Optional<GeoJsonConfiguration> geoJsonConfiguration = featureTypeApi.getExtension(GeoJsonConfiguration.class);
                    boolean flatten = geoJsonConfiguration.filter(cfg -> cfg.getNestedObjectStrategy() == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN && cfg.getMultiplicityStrategy() == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX)
                                                    .isPresent();
                    List<FeatureSchema> properties = flatten ? featureType.getAllNestedProperties() : featureType.getProperties();
                    // maps from the dotted path name to the path name with array brackets
                    Map<String,String> propertyNameMap = !flatten ? ImmutableMap.of() :
                            schemaGeneratorFeature.getPropertyNames(apiData, featureTypeApi.getId(),false, true).stream()
                                    .map(name -> new AbstractMap.SimpleImmutableEntry<String,String>(name.replace("[]",""), name))
                                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
                    ImmutableFields.Builder fieldsBuilder = new ImmutableFields.Builder();
                    AtomicReference<String> geometryType = new AtomicReference<>("unknown");
                    properties.stream()
                            .forEach(property -> {
                                boolean isArray = property.isArray();
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
                                                geometryType.set("point");
                                                break;
                                            case LINE_STRING:
                                            case MULTI_LINE_STRING:
                                                geometryType.set("line");
                                                break;
                                            case POLYGON:
                                            case MULTI_POLYGON:
                                                geometryType.set("polygon");
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

                    // TODO support layer-specific min/max zoom levels
                    return ImmutableVectorLayer.builder()
                            .id(featureTypeApi.getId())
                            .description(featureTypeApi.getDescription().orElse(""))
                            .minzoom(minMaxZoom.get(0))
                            .maxzoom(minMaxZoom.get(1))
                            .geometryType(geometryType.get())
                            .fields(fieldsBuilder.build())
                            .build();
                })
                .collect(Collectors.toList());
        tileJsonBuilder.vectorLayers(layers);

        return tileJsonBuilder.build();
    }

    private String getType(de.ii.xtraplatform.features.domain.SchemaBase.Type type) {
        switch (type) {
            case INTEGER:
                return "integer";
            case FLOAT:
                return "number";
            case BOOLEAN:
                return "boolean";
            case DATETIME:
                return "string, format=date-time";
            case STRING:
                return "string";
        }
        return "unknown";
    }

    private String getTilesUriTemplate(List<Link> links, TileMatrixSet tileMatrixSet) {
        return links.stream()
                .filter(link -> link.getRel().equalsIgnoreCase("item") && link.getType().equalsIgnoreCase("application/vnd.mapbox-vector-tile"))
                .findFirst()
                .map(link -> link.getHref())
                .orElseThrow(() -> new RuntimeException("No tile URI template with link relation type 'item' found for Mapbox Vector Tiles."))
                .replace("{tileMatrixSetId}", tileMatrixSet.getId())
                .replace("{tileMatrix}", "{z}")
                .replace("{tileRow}", "{y}")
                .replace("{tileCol}", "{x}");
    }

    private List<Integer> getMinMaxZoom(MinMax zoomLevels, TileMatrixSet tileMatrixSet) {
        if (Objects.nonNull(zoomLevels)) {
            return ImmutableList.<Integer>of(zoomLevels.getMin(), zoomLevels.getMax());
        } else {
            if (Objects.nonNull(tileMatrixSet)) {
                return ImmutableList.<Integer>of(tileMatrixSet.getMinLevel(), tileMatrixSet.getMaxLevel());
            } else {
                // fallback to some defaults; TODO: throw an exception?
                return ImmutableList.<Integer>of(0, 24);
            }
        }

    }

}
