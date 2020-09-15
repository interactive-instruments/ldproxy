/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaTypeContent;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.Intersects;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.streams.domain.Http;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileEncoder;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.http.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class TileFormatMVT implements TileFormatExtension {

    @Requires
    CrsTransformerFactory crsTransformerFactory;
    @Requires
    FeaturesQuery queryParser;
    @Requires
    TilesCache tilesCache;
    @Requires
    Http http;

    private static final Logger LOGGER = LoggerFactory.getLogger(TileFormatMVT.class);

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("application","vnd.mapbox-vector-tile"))
            .label("MVT")
            .parameter("mvt")
            .build();

    private final Schema schemaTile = new BinarySchema();
    public final static String SCHEMA_REF_TILE = "#/components/schemas/TileMVT";

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public boolean canMultiLayer() {
        return true;
    }

    @Override
    public boolean canTransformFeatures() {
        return true;
    }

    @Override
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        if (path.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
            return new ImmutableApiMediaTypeContent.Builder()
                    .schema(schemaTile)
                    .schemaRef(SCHEMA_REF_TILE)
                    .ogcApiMediaType(MEDIA_TYPE)
                    .build();

        throw new RuntimeException("Unexpected path: " + path);
    }

    @Override
    public String getExtension() {
        return "pbf";
    }

    @Override
    public Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContextTiles transformationContext, Optional<Locale> language) {

        return Optional.of(new FeatureTransformerTilesMVT(transformationContext, http.getDefaultClient()));
    }

    @Override
    public FeatureQuery getQuery(Tile tile,
                                 List<OgcApiQueryParameter> allowedParameters,
                                 Map<String, String> queryParameters,
                                 TilesConfiguration tilesConfiguration,
                                 URICustomizer uriCustomizer) {

        String collectionId = tile.getCollectionId();
        String tileMatrixSetId = tile.getTileMatrixSet().getId();
        int level = tile.getTileLevel();

        String featureTypeId = tile.getApi()
                                   .getData()
                                   .getCollections()
                                   .get(collectionId)
                                   .getExtension(FeaturesCoreConfiguration.class)
                                   .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                   .orElse(collectionId);
        ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                .type(featureTypeId)
                .limit(tilesConfiguration.getLimit())
                .offset(0)
                .crs(tile.getTileMatrixSet().getCrs())
                .maxAllowableOffset(getMaxAllowableOffsetNative(tile));

        List<String> properties = queryParameters.containsKey("properties") ?
                Splitter.on(",")
                        .trimResults()
                        .omitEmptyStrings()
                        .splitToList(queryParameters.get("properties")) :
                ImmutableList.of("*");
        queryBuilder.fields(properties);

        OgcApiDataV2 apiData = tile.getApi().getData();
        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);

        final Map<String, String> filterableFields = collectionData.getExtension(FeaturesCoreConfiguration.class)
                .map(FeaturesCoreConfiguration::getAllFilterParameters)
                .orElse(ImmutableMap.of());

        Set<String> filterParameters = ImmutableSet.of();
        for (OgcApiQueryParameter parameter : allowedParameters) {
            filterParameters = parameter.getFilterParameters(filterParameters, apiData, collectionData.getId());
            queryParameters = parameter.transformParameters(collectionData, queryParameters, apiData);
        }

        final Set<String> finalFilterParameters = filterParameters;
        final Map<String, String> filters = queryParameters.entrySet().stream()
                .filter(entry -> finalFilterParameters.contains(entry.getKey()) || filterableFields.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

        for (OgcApiQueryParameter parameter : allowedParameters) {
            parameter.transformQuery(collectionData, queryBuilder, queryParameters, apiData);
        }

        final  Map<String, List<PredefinedFilter>> predefFilters = tilesConfiguration.getFilters();
        final String predefFilter = (Objects.nonNull(predefFilters) && predefFilters.containsKey(tileMatrixSetId)) ?
                predefFilters.get(tileMatrixSetId).stream()
                    .filter(filter -> filter.getMax()>=level && filter.getMin()<=level)
                    .map(filter -> filter.getFilter())
                    .findAny()
                    .orElse(null) :
                null;

        CqlPredicate spatialPredicate = CqlPredicate.of(Intersects.of(filterableFields.get("bbox"), tile.getBoundingBox()));
        if ((predefFilter!=null || !filters.isEmpty()) && filterableFields != null) {
            Optional<CqlFilter> otherFilter = Optional.empty();
            Optional<CqlFilter> configFilter = Optional.empty();
            if (!filters.isEmpty()) {
                Optional<String> filterLang = uriCustomizer.getQueryParams().stream()
                        .filter(param -> "filter-lang".equals(param.getName()))
                        .map(NameValuePair::getValue)
                        .findFirst();
                Cql.Format cqlFormat = Cql.Format.TEXT;
                if (filterLang.isPresent() && "cql-json".equals(filterLang.get())) {
                    cqlFormat = Cql.Format.JSON;
                }
                otherFilter = queryParser.getFilterFromQuery(filters, filterableFields, ImmutableSet.of("filter"), cqlFormat);
            }
            if (predefFilter != null) {
                configFilter = queryParser.getFilterFromQuery(ImmutableMap.of("filter", predefFilter), filterableFields, ImmutableSet.of("filter"), Cql.Format.TEXT);
            }
            CqlFilter combinedFilter;
            if (otherFilter.isPresent() && configFilter.isPresent()) {
                combinedFilter = CqlFilter.of(And.of(otherFilter.get(), configFilter.get(), spatialPredicate));
            } else if (otherFilter.isPresent()) {
                combinedFilter = CqlFilter.of(And.of(otherFilter.get(), spatialPredicate));
            } else if (configFilter.isPresent()) {
                combinedFilter = CqlFilter.of(And.of(configFilter.get(), spatialPredicate));
            } else {
                combinedFilter = CqlFilter.of(spatialPredicate);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Filter: {}", combinedFilter);
            }

            queryBuilder.filter(combinedFilter);
        } else {
            queryBuilder.filter(CqlFilter.of(spatialPredicate));
        }

        return queryBuilder.build();
    }

    @Override
    public MultiLayerTileContent combineSingleLayerTilesToMultiLayerTile(TileMatrixSet tileMatrixSet, Map<String, Tile> singleLayerTileMap, Map<String, ByteArrayOutputStream> singleLayerByteArrayMap) throws IOException {
        VectorTileEncoder encoder = new VectorTileEncoder(tileMatrixSet.getTileExtent());
        VectorTileDecoder decoder = new VectorTileDecoder();
        Set<String> processedCollections = new TreeSet<>();
        int count = 0;
        while (count++ <= 3) {
            for (String collectionId : singleLayerTileMap.keySet()) {
                if (!processedCollections.contains(collectionId)) {
                    Tile singleLayerTile = singleLayerTileMap.get(collectionId);
                    ByteArrayOutputStream outputStream = singleLayerByteArrayMap.get(collectionId);
                    Path tileFile = tilesCache.getFile(singleLayerTile);
                    if (outputStream.size()>0) {
                        try {
                            List<VectorTileDecoder.Feature> features = decoder.decode(outputStream.toByteArray()).asList();
                            features.stream()
                                    .forEach(feature -> encoder.addFeature(feature.getLayerName(),feature.getAttributes(),feature.getGeometry(),feature.getId()));
                            processedCollections.add(collectionId);
                        } catch (IOException e) {
                            // maybe the file is still generated, try to wait once before giving up
                            String msg = "Failure to access the single-layer tile {}/{}/{}/{} in dataset '{}', layer '{}', format '{}'. Trying again ...";
                            LOGGER.info(msg, tileMatrixSet.getId(), singleLayerTile.getTileLevel(), singleLayerTile.getTileRow(), singleLayerTile.getTileCol(),
                                        singleLayerTile.getApi().getId(), collectionId, getExtension());
                        } catch (IllegalArgumentException e) {
                            // another problem generating the tile
                            throw new RuntimeException(String.format("Failure to process the single-layer tile %s/%d/%d/%d in dataset '%s', layer '%s', format '%s'.",
                                                                     tileMatrixSet.getId(), singleLayerTile.getTileLevel(), singleLayerTile.getTileRow(), singleLayerTile.getTileCol(),
                                                                     singleLayerTile.getApi().getId(), collectionId, getExtension()), e);
                        }
                    } else if (Files.exists(tileFile) && Files.size(tileFile)==0) {
                        // an empty tile, so we are done for this collection
                        processedCollections.add(collectionId);
                    }
                }
            }
            if (processedCollections.size()==singleLayerTileMap.size())
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // ignore and just continue
            }
        }

        MultiLayerTileContent result = new MultiLayerTileContent();
        result.byteArray = encoder.encode();
        result.isComplete = processedCollections.size()==singleLayerTileMap.size();

        return result;
    }

    @Override
    public double getMaxAllowableOffsetNative(Tile tile) {
        double maxAllowableOffsetTileMatrixSet = tile.getTileMatrixSet().getMaxAllowableOffset(tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
        double maxAllowableOffsetNative = maxAllowableOffsetTileMatrixSet; // TODO convert to native CRS units
        return maxAllowableOffsetNative;
    }

    @Override
    public double getMaxAllowableOffsetCrs84(Tile tile) {
        double maxAllowableOffsetCrs84 = 0;
        try {
            maxAllowableOffsetCrs84 = tile.getTileMatrixSet().getMaxAllowableOffset(tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), OgcCrs.CRS84, crsTransformerFactory);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error while computing maxAllowableOffsetCrs84: {}.", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", e);
            }
        }

        return maxAllowableOffsetCrs84;
    }

    /**
     * If the zoom Level is not valid generate empty JSON Tile or empty MVT.
     *
     * @param tile            the tile
     */
    public Object getEmptyTile(Tile tile) {
        return new VectorTileEncoder(tile.getTileMatrixSet().getTileExtent()).encode();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}
