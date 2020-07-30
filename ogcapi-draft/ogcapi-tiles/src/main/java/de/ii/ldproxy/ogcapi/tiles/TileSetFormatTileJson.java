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
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.infra.json.SchemaGenerator;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.target.geojson.GeoJsonConfiguration;
import de.ii.ldproxy.target.geojson.SchemaGeneratorFeature;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
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
    OgcApiFeatureCoreProviders providers;

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(MediaType.APPLICATION_JSON_TYPE)
            .label("JSON")
            .parameter("json")
            .build();

    private final Schema schemaTileJson;
    public final static String SCHEMA_REF_TILE_JSON = "#/components/schemas/TileJson";

    public TileSetFormatTileJson() {
        schemaTileJson = schemaGenerator.getSchema(TileJson.class);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .isPresent();
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        if (path.endsWith("/tiles/{tileMatrixSetId}"))
            return new ImmutableOgcApiMediaTypeContent.Builder()
                    .schema(schemaTileJson)
                    .schemaRef(SCHEMA_REF_TILE_JSON)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new ServerErrorException("Unexpected path "+path,500);
    }

    @Override
    public Object getTileSetEntity(OgcApiApiDataV2 apiData, OgcApiRequestContext requestContext,
                                   Optional<String> collectionId,
                                   TileMatrixSet tileMatrixSet, MinMax zoomLevels, double[] center,
                                   List<OgcApiLink> links) {

        String tilesUriTemplate = getTilesUriTemplate(links, tileMatrixSet);

        ImmutableTileJson.Builder tileJsonBuilder = ImmutableTileJson.builder();

        tileJsonBuilder.tilejson("3.0.0")
                .name(apiData.getLabel())
                .description(apiData.getDescription())
                .tiles(ImmutableList.of(tilesUriTemplate));

        // TODO: add support for attribution and version (manage revisions to the data)

        BoundingBox bbox = collectionId.isPresent() ? apiData.getSpatialExtent(collectionId.get()) : apiData.getSpatialExtent();
        if (Objects.nonNull(bbox))
            tileJsonBuilder.bounds(ImmutableList.of(bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax()));

        List<Integer> minMaxZoom = getMinMaxZoom(zoomLevels, tileMatrixSet);
        tileJsonBuilder.minzoom(minMaxZoom.get(0))
                       .maxzoom(minMaxZoom.get(1));

        double centerLon = (Objects.nonNull(center) && center.length>=1) ? center[0] : bbox.getXmin()+(bbox.getXmax()-bbox.getXmin())*0.5;
        double centerLat = (Objects.nonNull(center) && center.length>=2) ? center[1] : bbox.getYmin()+(bbox.getYmax()-bbox.getYmin())*0.5;
        int defaultZoomLevel = zoomLevels.getDefault().orElse(minMaxZoom.get(0) + (minMaxZoom.get(1)-minMaxZoom.get(0))/2);
        tileJsonBuilder.center(ImmutableList.of(centerLon, centerLat, defaultZoomLevel));

        ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> featureTypesApi = apiData.getCollections();
        List<ImmutableVectorLayer> layers = featureTypesApi.values()
                .stream()
                .filter(featureTypeApi -> !collectionId.isPresent() || featureTypeApi.getId().equals(collectionId.get()))
                .map(featureTypeApi -> {
                    FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, featureTypeApi);
                    FeatureSchema featureType = featureProvider.getData()
                            .getTypes()
                            .get(featureTypeApi.getId());
                    Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = featureTypeApi.getExtension(OgcApiFeaturesCoreConfiguration.class);
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

    private String getTilesUriTemplate(List<OgcApiLink> links, TileMatrixSet tileMatrixSet) {
        return links.stream()
                .filter(link -> link.getRel().equalsIgnoreCase("item") && link.getType().equalsIgnoreCase("application/vnd.mapbox-vector-tile"))
                .findFirst()
                .map(link -> link.getHref())
                .orElseThrow(() -> new ServerErrorException(500))
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
