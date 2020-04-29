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
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.target.geojson.GeoJsonConfiguration;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureType;

import javax.ws.rs.ServerErrorException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is responsible for generating tilejson metadata.
 */
public class VectorTilesMetadataGenerator {

    /**
     * generates the tilejson metadata
     *
     * @param providers
     * @param serviceData
     * @param collectionId empty for multi-collection tiles
     * @param tileMatrixSet
     * @param zoomLevels
     * @param links
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a List with links
     */
    public Map<String,Object> generateTilejson(OgcApiFeatureCoreProviders providers, OgcApiApiDataV2 serviceData,
                                               Optional<String> collectionId,
                                               FeatureTransformerGeoJson.NESTED_OBJECTS nestingStrategy,
                                               FeatureTransformerGeoJson.MULTIPLICITY multiplicityStrategy,
                                               TileMatrixSet tileMatrixSet, MinMax zoomLevels, double[] center,
                                               List<OgcApiLink> links, I18n i18n, Optional<Locale> language) {

        String tilesUriTemplate = getTilesUriTemplate(links, tileMatrixSet);

        // TODO support i18n
        ImmutableMap.Builder<String,Object> tilejson = ImmutableMap.<String,Object>builder()
                .put("tilejson", "3.0.0")
                .put("name", serviceData.getLabel())
                .put("description", serviceData.getDescription().orElse(""))
                .put("tiles", ImmutableList.of(tilesUriTemplate));

        // TODO: add support for attribution and version (manage revisions to the data)

        BoundingBox bbox = collectionId.isPresent() ? serviceData.getSpatialExtent(collectionId.get()) : serviceData.getSpatialExtent();
        if (Objects.nonNull(bbox))
            tilejson.put("bounds", ImmutableList.of(bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax()) );

        List<Integer> minMaxZoom = getMinMaxZoom(zoomLevels, tileMatrixSet);
        tilejson.put("minzoom", minMaxZoom.get(0))
                .put("maxzoom", minMaxZoom.get(1));

        double centerLon = (Objects.nonNull(center) && center.length>=1) ? center[0] : bbox.getXmin()+(bbox.getXmax()-bbox.getXmin())*0.5;
        double centerLat = (Objects.nonNull(center) && center.length>=2) ? center[1] : bbox.getYmin()+(bbox.getYmax()-bbox.getYmin())*0.5;
        int defaultZoomLevel = zoomLevels.getDefault().orElse(minMaxZoom.get(0) + (minMaxZoom.get(1)-minMaxZoom.get(0))/2);
        tilejson.put("center", ImmutableList.of(centerLon, centerLat, defaultZoomLevel));

        ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> featureTypesApi = serviceData.getCollections();
        List<ImmutableMap<String, Object>> layers = featureTypesApi.values().stream()
                .map(featureTypeApi -> {
                    if (collectionId.isPresent()) {
                        if (!featureTypeApi.getId().equals(collectionId.get()))
                            return ImmutableMap.<String,Object>of();
                    }
                    FeatureProvider2 featureProvider = providers.getFeatureProvider(serviceData, featureTypeApi);
                    FeatureType featureType = featureProvider.getData()
                            .getTypes()
                            .get(featureTypeApi.getId());
                    Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = featureTypeApi.getExtension(OgcApiFeaturesCoreConfiguration.class);

                    SchemaObject schema = serviceData.getSchema(featureType,
                            nestingStrategy == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN &&
                            multiplicityStrategy == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX);
                    ImmutableMap.Builder<String, Object> fieldsBuilder = ImmutableMap.<String, Object>builder();
                    schema.properties.stream()
                            .forEach(prop -> {
                                boolean isArray = prop.maxItems > 1;
                                if (prop.literalType.isPresent()) {
                                    fieldsBuilder.put(prop.id, prop.literalType.get());
                                } else if (prop.wellknownType.isPresent()) {
                                    switch (prop.wellknownType.get()) {
                                        case "Link":
                                            fieldsBuilder.put(prop.id, "link");
                                            break;
                                        case "Point":
                                        case "MultiPoint":
                                        case "LineString":
                                        case "MultiLineString":
                                        case "Polygon":
                                        case "MultiPolygon":
                                        case "Geometry":
                                        default:
                                            break;
                                    }
                                } else if (prop.objectType.isPresent()) {
                                    fieldsBuilder.put(prop.id, "object");
                                }
                            });

                    // TODO temporary hack for VTP2 pilot, find general solution
                    boolean point = featureTypeApi.getId().endsWith("Pnt") || featureTypeApi.getId().endsWith("_p");
                    boolean line = featureTypeApi.getId().endsWith("Crv") || featureTypeApi.getId().endsWith("_l");
                    boolean polygon = featureTypeApi.getId().endsWith("Srf") || featureTypeApi.getId().endsWith("_a");
                    String geometryType = point ? "point" : line ? "line" : polygon ? "polygon" : "unknown";

                    // TODO support layer-specific min/max zoom levels
                    return ImmutableMap.<String, Object>builder()
                            .put("id", featureTypeApi.getId())
                            .put("description", featureTypeApi.getDescription().orElse(""))
                            .put("minzoom", minMaxZoom.get(0))
                            .put("maxzoom", minMaxZoom.get(1))
                            .put("geometry_type", geometryType)
                            .put("fields", fieldsBuilder.build())
                            .build();
                })
                .filter(map -> !map.isEmpty())
                .collect(Collectors.toList());
        tilejson.put("vector_layers", layers);

        return tilejson.build();
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
