/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ldproxy.ogcapi.features.html.domain.Geometry;
import de.ii.ldproxy.ogcapi.html.domain.DatasetView;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableSource;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.ldproxy.ogcapi.html.domain.MapClient.Popup;
import de.ii.ldproxy.ogcapi.html.domain.MapClient.Source.TYPE;
import de.ii.ldproxy.ogcapi.html.domain.MapClient.Type;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author zahnen
 */
public class FeatureCollectionView extends DatasetView {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCollectionView.class);

    private class FeatureGeometry {
        String name;
        Geometry<?> geometry;
        FeatureGeometry(String name, Geometry<?> geometry) {
            this.name = name;
            this.geometry = geometry;
        }
    }

    private URI uri;
    public List<NavigationDTO> pagination;
    public List<NavigationDTO> metaPagination;
    public List<FeatureHtml> features;
    public boolean hideMap = true; // set to "hide"; change to "false" when we see a geometry
    public Set<Map.Entry<String, String>> filterFields;
    public Map<String, String> bbox;
    public TemporalExtent temporalExtent;
    public URICustomizer uriBuilder;
    public URICustomizer uriBuilderWithFOnly;
    public boolean bare;
    public boolean isCollection;
    public String persistentUri;
    public boolean spatialSearch;
    public boolean schemaOrgFeatures;
    public FeaturesHtmlConfiguration.POSITION mapPosition;
    public final MapClient mapClient;
    public final FilterEditor filterEditor;
    public List<String> geometryProperties;
    private List<FeatureGeometry> geometries = null;
    private boolean clampToGround;

    public FeatureCollectionView(OgcApiDataV2 apiData,
        FeatureTypeConfigurationOgcApi collectionData, String template,
        URI uri, String name, String title, String description, String attribution,
        String urlPrefix, HtmlConfiguration htmlConfig, String persistentUri, boolean noIndex,
        I18n i18n, Locale language, POSITION mapPosition,
        Type mapClientType, String styleUrl, boolean removeZoomLevelConstraints,
        Map<String, String> queryables, List<String> geometryProperties) {
        super(template, uri, name, title, description, attribution, urlPrefix, htmlConfig, noIndex);
        this.features = new ArrayList<>();
        this.isCollection = !"featureDetails".equals(template);
        this.uri = uri; // TODO need to overload getPath() as it currently forces trailing slashes while OGC API uses no trailing slashes
        this.persistentUri = persistentUri;
        this.schemaOrgFeatures = Objects.nonNull(htmlConfig) && Objects.equals(htmlConfig.getSchemaOrgEnabled(), true);
        this.mapPosition = mapPosition;
        this.uriBuilder = new URICustomizer(uri);
        this.geometryProperties = geometryProperties;
        this.clampToGround = true; // TODO make configurable

        this.bbox = apiData.getSpatialExtent(collectionData.getId())
            .map(boundingBox -> ImmutableMap.of(
            "minLng", Double.toString(boundingBox.getXmin()),
            "minLat", Double.toString(boundingBox.getYmin()),
            "maxLng", Double.toString(boundingBox.getXmax()),
            "maxLat", Double.toString(boundingBox.getYmax())))
            .orElse(null);

        if (mapClientType.equals(MapClient.Type.MAP_LIBRE)) {
            this.mapClient = new ImmutableMapClient.Builder()
                    .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                                           .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
                    .attribution(getAttribution())
                    .bounds(Optional.ofNullable(bbox))
                    .data(new ImmutableSource.Builder()
                                  .type(TYPE.geojson)
                                  .url(uriBuilder.removeParameters("f").ensureParameter("f", "json").toString())
                                  .build())
                    .popup(Popup.HOVER_ID)
                    .styleUrl(Optional.ofNullable(styleUrl))
                    .removeZoomLevelConstraints(removeZoomLevelConstraints)
                    .build();
        } else if (mapClientType.equals(MapClient.Type.CESIUM)) {
            //TODO: Cesium
            this.mapClient = new ImmutableMapClient.Builder()
                .type(mapClientType)
                .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                                   .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl()))
                                   .map(url -> url.replace("{z}", "{TileMatrix}")
                                       .replace("{y}", "{TileRow}")
                                       .replace("{x}", "{TileCol}")))
                .attribution(getAttribution())
                // TODO everything below is ignored / irrelevant
                .bounds(Optional.ofNullable(bbox))
                .data(new ImmutableSource.Builder()
                          .type(TYPE.geojson)
                          .url(uriBuilder.removeParameters("f").ensureParameter("f", "json").toString())
                          .build())
                .popup(Popup.HOVER_ID)
                .styleUrl(Optional.ofNullable(styleUrl))
                .removeZoomLevelConstraints(removeZoomLevelConstraints)
                .build();
        } else {
            LOGGER.error("Configuration error: {} is not a supported map client for the HTML representation of features.", mapClientType);
            this.mapClient = null;
        }

        if (Objects.nonNull(queryables)) {
            this.filterEditor = new ImmutableFilterEditor.Builder()
                .fields(queryables.entrySet())
                .build();
        } else {
            this.filterEditor = null;
        }
    }

    @Override
    public String getPath() {
        String path = uri.getPath();
        return path;
    }

    public boolean isMapTop() {
        return mapPosition == POSITION.TOP || (mapPosition == POSITION.AUTO && (features.isEmpty() || features.stream().anyMatch(FeatureHtml::hasObjects)));
    }

    public boolean isMapRight() {
        return mapPosition == POSITION.RIGHT || (mapPosition == POSITION.AUTO && !features.isEmpty() && features.stream().noneMatch(FeatureHtml::hasObjects));
    }

    public String getFeatureExtent() {
        if (Objects.isNull(geometries))
            geometries = getGeometries(features, geometryProperties);
        List<Geometry.Coordinate> coordinates = geometries.stream()
            .map(f -> f.geometry.getCoordinatesFlat())
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableList());
        double minLon = getMin(coordinates, 0).orElse(-180.0);
        double minLat = getMin(coordinates, 1).orElse(-90.0);
        double maxLon = getMax(coordinates, 0).orElse(180.0);
        double maxLat = getMax(coordinates, 1).orElse(90.0);
        return "Cesium.Rectangle.fromDegrees("+minLon+","+minLat+","+maxLon+","+maxLat+");";
    }

    public List<String> getMapEntities() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        if (Objects.isNull(geometries))
            geometries = getGeometries(features, geometryProperties);
        geometries.stream()
            .forEach(f -> {
                final Double minHeight = clampToGround && f.geometry.is3d()
                    ? getMin(f.geometry.getCoordinatesFlat(), 2).orElse(0.0)
                    : null;
                switch (f.geometry.getType()) {
                    case MultiPoint:
                        ((Geometry.MultiPoint) f.geometry).getCoordinates()
                            .stream()
                            .forEach(point -> addPoint(builder, f.name, point, minHeight));
                        break;
                    case Point:
                        addPoint(builder, f.name, (Geometry.Point) f.geometry, minHeight);
                        break;
                    case MultiLineString:
                        ((Geometry.MultiLineString) f.geometry).getCoordinates()
                            .stream()
                            .forEach(line -> addLineString(builder, f.name, line, minHeight));
                        break;
                    case LineString:
                        addLineString(builder, f.name, (Geometry.LineString) f.geometry, minHeight);
                        break;
                    case MultiPolygon:
                        ((Geometry.MultiPolygon) f.geometry).getCoordinates()
                            .stream()
                            .forEach(polygon -> addPolygon(builder, f.name, polygon, minHeight));
                        break;
                    case Polygon:
                        addPolygon(builder, f.name, (Geometry.Polygon) f.geometry, minHeight);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported geometry type: " + f.geometry.getType());
                }
            });
        return builder.build();
    }

    private void addPolygon(ImmutableList.Builder<String> builder, String name, Geometry.Polygon polygon, Double minHeight) {
        boolean is3d = polygon.is3d();
        if (polygon.getCoordinates().size()==1) {
            builder.add("viewer.entities.add({" +
                            "name:\"" + name + "\"," +
                            (is3d
                                ? "polygon:{hierarchy:Cesium.Cartesian3.fromDegreesArrayHeights([" + getCoordinatesString(polygon.getCoordinates().get(0), minHeight) + "]),perPositionHeight:true,"
                                : "polygon:{hierarchy:Cesium.Cartesian3.fromDegreesArrayHeights([" + getCoordinatesString(polygon.getCoordinates().get(0)) + "]),perPositionHeight:true,") +
                            "material:Cesium.Color.BLUE.withAlpha(0.5)," +
                            "outline:true,outlineColor:Cesium.Color.BLUE" +
                            "}});");
        } else {
            builder.add("viewer.entities.add({" +
                            "name:\"" + name + "\"," +
                            (is3d
                                ? "polygon:{hierarchy:{positions:Cesium.Cartesian3.fromDegreesArrayHeights([" + getCoordinatesString(polygon.getCoordinates().get(0), minHeight) + "])," +
                                "holes:[" +
                                IntStream.range(1, polygon.getCoordinates().size())
                                    .mapToObj(n -> "{positions:Cesium.Cartesian3.fromDegreesArrayHeights(["+getCoordinatesString(polygon.getCoordinates().get(n), minHeight)+"])}")
                                    .collect(Collectors.joining(",")) +
                                "]},perPositionHeight:true,"
                                : "polygon:{hierarchy:{positions:Cesium.Cartesian3.fromDegreesArrayHeights([" + getCoordinatesString(polygon.getCoordinates().get(0)) + "])," +
                                "holes:[" +
                                IntStream.range(1, polygon.getCoordinates().size())
                                    .mapToObj(n -> "{positions:Cesium.Cartesian3.fromDegreesArrayHeights([" + getCoordinatesString(polygon.getCoordinates().get(n)) + "])}")
                                    .collect(Collectors.joining(",")) +
                                "]},perPositionHeight:true,") +
                            "material:Cesium.Color.BLUE.withAlpha(0.5)," +
                            "outline:true,outlineColor:Cesium.Color.BLUE" +
                            "}});");
        }
    }

    private void addLineString(ImmutableList.Builder<String> builder, String name, Geometry.LineString line, Double minHeight) {
        builder.add("viewer.entities.add({" +
                        "name:\"" + name + "\"," +
                        (line.is3d()
                            ? "polyline:{positions:Cesium.Cartesian3.fromDegreesArrayHeights([" + getCoordinatesString(line.getCoordinates(), minHeight) + "]),perPositionHeight:true,"
                            : "polyline:{positions:Cesium.Cartesian3.fromDegreesArrayHeights([" + getCoordinatesString(line.getCoordinates()) + "]),perPositionHeight:true,") +
                        // "outline:true,outlineColor:Cesium.Color.BLUE," +
                        "width:1," +
                        "material:Cesium.Color.BLUE" +
                        "}});");

    }

    private void addPoint(ImmutableList.Builder<String> builder, String name, Geometry.Point point, Double minHeight) {
        builder.add("viewer.entities.add({" +
                        "name:\"" + name + "\"," +
                        "position:Cesium.Cartesian3.fromDegrees(" + getCoordinatesString(point.getCoordinates(), minHeight) + ")," +
                        "point:{pixelSize:5,color:Cesium.Color.BLUE}" +
                        "});");
    }

    private List<FeatureGeometry> getGeometries(List<FeatureHtml> features, List<String> geometryProperties) {
        return features.stream()
            .map(feature -> {
                List<PropertyHtml> geomProperties = ImmutableList.of();
                for (String geometryProperty : geometryProperties) {
                    geomProperties = feature.findPropertiesByPath(geometryProperty);
                    if (!geomProperties.isEmpty())
                        break;
                }
                if (geomProperties.isEmpty()) {
                    Optional<PropertyHtml> defaultGeom = feature.getGeometry();
                    if (defaultGeom.isPresent()) {
                        geomProperties = ImmutableList.of(defaultGeom.get());
                    }
                }
                return geomProperties.stream()
                    .map(PropertyHtml::parseGeometry)
                    .map(geom -> new FeatureGeometry(feature.getName(), geom))
                    .collect(Collectors.toUnmodifiableList());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableList());
    }

    private Optional<Double> getMin(List<Geometry.Coordinate> coordinates, int axis) {
        return coordinates.stream().map(coord -> coord.get(axis)).min(Comparator.naturalOrder());
    }

    private Optional<Double> getMax(List<Geometry.Coordinate> coordinates, int axis) {
        return coordinates.stream().map(coord -> coord.get(axis)).max(Comparator.naturalOrder());
    }

    private boolean is3d(List<Geometry.Coordinate> coordinates) {
        return !coordinates.isEmpty() && coordinates.get(0).size()==3;
    }

    private String getCoordinatesString(List<Geometry.Coordinate> coordinates) {
        if (is3d(coordinates))
            return coordinates.stream()
                .flatMap(List::stream)
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return coordinates.stream()
            .map(coord -> ImmutableList.of(coord.get(0), coord.get(1), 0.0))
            .flatMap(List::stream)
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    private String getCoordinatesString(List<Geometry.Coordinate> coordinates, Double deltaHeight) {
        if (Objects.isNull(deltaHeight))
            return getCoordinatesString(coordinates);
        return coordinates.stream()
            .map(coord -> Geometry.Coordinate.of(coord.get(0), coord.get(1), coord.get(2) - deltaHeight))
            .flatMap(List::stream)
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    @Override
    public String getAttribution() {
        String basemapAttribution = super.getAttribution();
        if (Objects.nonNull(attribution)) {
            if (Objects.nonNull(basemapAttribution))
                return String.join(" | ", attribution, basemapAttribution);
            else
                return attribution;
        }
        return basemapAttribution;
    }

    public Optional<String> getCanonicalUrl() throws URISyntaxException {
        if (!isCollection && persistentUri!=null)
            return Optional.of(persistentUri);

        URICustomizer canonicalUri = uriBuilder.copy()
                                               .ensureNoTrailingSlash()
                                               .clearParameters();

        boolean hasOtherParams = !canonicalUri.isQueryEmpty();
        boolean hasPrevLink = Objects.nonNull(metaPagination) && metaPagination.stream()
                                                                               .anyMatch(navigationDTO -> "prev".equals(navigationDTO.label));

        return !hasOtherParams && (!isCollection || !hasPrevLink)
                ? Optional.of(canonicalUri.toString())
                : Optional.empty();
    }

    public Optional<String> getPersistentUri() throws URISyntaxException {
        if (!isCollection && persistentUri!=null)
            return Optional.of(persistentUri);

        return Optional.empty();
    }

    public String getQueryWithoutPage() {
        List<NameValuePair> query = URLEncodedUtils.parse(getQuery().substring(1), Charset.forName("utf-8"))
                                                   .stream()
                                                   .filter(kvp -> !kvp.getName().equals("offset") &&
                                                                  !kvp.getName().equals("limit"))
                                                   .collect(Collectors.toList());

        return '?' + URLEncodedUtils.format(query, '&', Charset.forName("utf-8")) + '&';
    }

    public Function<String, String> getCurrentUrlWithSegment() {
        return segment -> uriBuilderWithFOnly.copy()
                                    .ensureLastPathSegment(segment)
                                    .ensureNoTrailingSlash()
                                    .toString();
    }

    public Function<String, String> getCurrentUrlWithSegmentClearParams() {
        return segment -> uriBuilder.copy()
                                    .ensureLastPathSegment(segment)
                                    .ensureNoTrailingSlash()
                                    .clearParameters()
                                    .toString();
    }
}
