/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.TemporalExtent;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.DatasetView;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.ImmutableSource;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.MapClient.Popup;
import de.ii.ogcapi.html.domain.MapClient.Source.TYPE;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.ogcapi.html.domain.NavigationDTO;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class FeatureCollectionView extends DatasetView {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCollectionView.class);

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
    public POSITION mapPosition;
    public final MapClient mapClient;
    public final FilterEditor filterEditor;
    public final CesiumData cesiumData;

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
        this.cesiumData = new CesiumData(features, geometryProperties);

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
            this.mapClient = new ImmutableMapClient.Builder()
                .type(mapClientType)
                .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                                   .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl()))
                                   .map(url -> url.replace("{z}", "{TileMatrix}")
                                       .replace("{y}", "{TileRow}")
                                       .replace("{x}", "{TileCol}")))
                .attribution(getAttribution())
                .build();
        } else {
            LOGGER.error("Configuration error: {} is not a supported map client for the HTML representation of features.", mapClientType);
            this.mapClient = null;
        }

        if (Objects.nonNull(queryables)) {
            this.filterEditor = new ImmutableFilterEditor.Builder()
                .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                    .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
                .attribution(htmlConfig.getBasemapAttribution())
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
