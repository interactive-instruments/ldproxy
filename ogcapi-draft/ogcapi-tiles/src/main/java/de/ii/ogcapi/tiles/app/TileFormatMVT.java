/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.PARAMETER_BBOX;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.crs.domain.CrsSupport;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.domain.FeatureTransformationContextTiles;
import de.ii.ogcapi.tiles.domain.PredefinedFilter;
import de.ii.ogcapi.tiles.domain.Rule;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.SIntersects;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Unit;
import javax.ws.rs.core.MediaType;
import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileEncoder;
import org.apache.http.NameValuePair;
import org.kortforsyningen.proj.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class TileFormatMVT extends TileFormatWithQuerySupportExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileFormatMVT.class);

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "vnd.mapbox-vector-tile"))
          .label("MVT")
          .parameter("mvt")
          .build();

  private final CrsTransformerFactory crsTransformerFactory;
  private final FeaturesQuery queryParser;
  private final TileCache tileCache;
  private final CrsSupport crsSupport;
  private final CrsInfo crsInfo;

  @Inject
  public TileFormatMVT(
      CrsTransformerFactory crsTransformerFactory,
      FeaturesQuery queryParser,
      TileCache tileCache,
      CrsSupport crsSupport,
      CrsInfo crsInfo) {
    this.crsTransformerFactory = crsTransformerFactory;
    this.queryParser = queryParser;
    this.tileCache = tileCache;
    this.crsSupport = crsSupport;
    this.crsInfo = crsInfo;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public boolean canMultiLayer() {
    return true;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (path.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
        || path.equals(
            "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(SCHEMA_TILE)
          .schemaRef(SCHEMA_REF_TILE)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();

    return null;
  }

  @Override
  public String getExtension() {
    return "pbf";
  }

  @Override
  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContextTiles transformationContext) {
    return Optional.of(new FeatureEncoderMVT(transformationContext));
  }

  @Override
  public boolean getGzippedInMbtiles() {
    return true;
  }

  @Override
  public boolean getSupportsEmptyTile() {
    return true;
  }

  @Override
  public DataType getDataType() {
    return TileSet.DataType.vector;
  }

  @Override
  public FeatureQuery getQuery(
      Tile tile,
      List<OgcApiQueryParameter> allowedParameters,
      Map<String, String> queryParameters,
      TilesConfiguration tilesConfiguration,
      URICustomizer uriCustomizer) {

    String collectionId = tile.getCollectionId();
    String tileMatrixSetId = tile.getTileMatrixSet().getId();
    int level = tile.getTileLevel();

    final Map<String, List<PredefinedFilter>> predefFilters =
        tilesConfiguration.getFiltersDerived();
    final String predefFilter =
        (Objects.nonNull(predefFilters) && predefFilters.containsKey(tileMatrixSetId))
            ? predefFilters.get(tileMatrixSetId).stream()
                .filter(
                    filter ->
                        filter.getMax() >= level
                            && filter.getMin() <= level
                            && filter.getFilter().isPresent())
                .map(filter -> filter.getFilter().get())
                .findAny()
                .orElse(null)
            : null;

    String featureTypeId =
        tile.getApiData()
            .getCollections()
            .get(collectionId)
            .getExtension(FeaturesCoreConfiguration.class)
            .map(cfg -> cfg.getFeatureType().orElse(collectionId))
            .orElse(collectionId);
    ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureTypeId)
            .limit(
                Objects.requireNonNullElse(
                    tilesConfiguration.getLimitDerived(), TilesBuildingBlock.LIMIT_DEFAULT))
            .offset(0)
            .crs(tile.getTileMatrixSet().getCrs())
            .maxAllowableOffset(getMaxAllowableOffset(tile));

    final Map<String, List<Rule>> rules = tilesConfiguration.getRulesDerived();
    if (!queryParameters.containsKey("properties")
        && (Objects.nonNull(rules) && rules.containsKey(tileMatrixSetId))) {
      List<String> properties =
          rules.get(tileMatrixSetId).stream()
              .filter(rule -> rule.getMax() >= level && rule.getMin() <= level)
              .map(Rule::getProperties)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      if (!properties.isEmpty()) {
        queryParameters =
            ImmutableMap.<String, String>builder()
                .putAll(queryParameters)
                .put("properties", String.join(",", properties))
                .build();
      }
    }

    OgcApiDataV2 apiData = tile.getApiData();
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);

    final Map<String, String> filterableFields =
        queryParser.getFilterableFields(apiData, collectionData);
    final Map<String, String> queryableTypes =
        queryParser.getQueryableTypes(apiData, collectionData);

    Set<String> filterParameters = ImmutableSet.of();
    for (OgcApiQueryParameter parameter : allowedParameters) {
      filterParameters =
          parameter.getFilterParameters(filterParameters, apiData, collectionData.getId());
      queryParameters = parameter.transformParameters(collectionData, queryParameters, apiData);
    }

    final Set<String> finalFilterParameters = filterParameters;
    final Map<String, String> filters =
        queryParameters.entrySet().stream()
            .filter(
                entry ->
                    finalFilterParameters.contains(entry.getKey())
                        || filterableFields.containsKey(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    for (OgcApiQueryParameter parameter : allowedParameters) {
      parameter.transformQuery(collectionData, queryBuilder, queryParameters, apiData);
    }

    BoundingBox bbox = tile.getBoundingBox();
    // reduce bbox to the area in which there is data (to avoid coordinate transformation issues
    // with large scale and data that is stored in a regional, projected CRS)
    final EpsgCrs crs = bbox.getEpsgCrs();
    final Optional<BoundingBox> dataBbox = tile.getApi().getSpatialExtent(collectionId, crs);
    if (dataBbox.isPresent()) {
      bbox =
          ImmutableList.of(bbox, dataBbox.get()).stream()
              .map(BoundingBox::toArray)
              .reduce(
                  (doubles, doubles2) -> {
                    if (doubles2.length == 4) {
                      return new double[] {
                        Math.max(doubles[0], doubles2[0]),
                        Math.max(doubles[1], doubles2[1]),
                        Math.min(doubles[2], doubles2[2]),
                        Math.min(doubles[3], doubles2[3])
                      };
                    } else if (doubles2.length == 6) {
                      return new double[] {
                        Math.max(doubles[0], doubles2[0]),
                        Math.max(doubles[1], doubles2[1]),
                        Math.min(doubles[2], doubles2[3]),
                        Math.min(doubles[3], doubles2[4])
                      };
                    }
                    return new double[] {doubles[0], doubles[1], doubles[2], doubles[3]};
                  })
              .map(doubles -> BoundingBox.of(doubles[0], doubles[1], doubles[2], doubles[3], crs))
              .orElse(bbox);
    }

    Cql2Expression spatialPredicate =
        SIntersects.of(
            Property.of(filterableFields.get(PARAMETER_BBOX)),
            SpatialLiteral.of(Envelope.of(bbox)));
    if (predefFilter != null || !filters.isEmpty()) {
      Optional<Cql2Expression> otherFilter = Optional.empty();
      Optional<Cql2Expression> configFilter = Optional.empty();
      if (!filters.isEmpty()) {
        Optional<String> filterLang =
            uriCustomizer.getQueryParams().stream()
                .filter(param -> "filter-lang".equals(param.getName()))
                .map(NameValuePair::getValue)
                .findFirst();
        Cql.Format cqlFormat = Cql.Format.TEXT;
        if (filterLang.isPresent() && "cql2-json".equals(filterLang.get())) {
          cqlFormat = Cql.Format.JSON;
        }
        otherFilter =
            queryParser.getFilterFromQuery(
                filters, filterableFields, ImmutableSet.of("filter"), queryableTypes, cqlFormat);
      }
      if (predefFilter != null) {
        configFilter =
            queryParser.getFilterFromQuery(
                ImmutableMap.of("filter", predefFilter),
                filterableFields,
                ImmutableSet.of("filter"),
                queryableTypes,
                Cql.Format.TEXT);
      }
      Cql2Expression combinedFilter;
      if (otherFilter.isPresent() && configFilter.isPresent()) {
        combinedFilter = And.of(otherFilter.get(), configFilter.get(), spatialPredicate);
      } else if (otherFilter.isPresent()) {
        combinedFilter = And.of(otherFilter.get(), spatialPredicate);
      } else if (configFilter.isPresent()) {
        combinedFilter = And.of(configFilter.get(), spatialPredicate);
      } else {
        combinedFilter = spatialPredicate;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.trace("Filter: {}", combinedFilter);
      }

      queryBuilder.filter(combinedFilter);
    } else {
      queryBuilder.filter(spatialPredicate);
    }

    return queryBuilder.build();
  }

  @Override
  public MultiLayerTileContent combineSingleLayerTilesToMultiLayerTile(
      TileMatrixSet tileMatrixSet,
      Map<String, Tile> singleLayerTileMap,
      Map<String, ByteArrayOutputStream> singleLayerByteArrayMap)
      throws IOException {
    VectorTileEncoder encoder = new VectorTileEncoder(tileMatrixSet.getTileExtent());
    VectorTileDecoder decoder = new VectorTileDecoder();
    Set<String> processedCollections = new TreeSet<>();
    int count = 0;
    while (count++ <= 3) {
      for (String collectionId : singleLayerTileMap.keySet()) {
        if (!processedCollections.contains(collectionId)) {
          Tile singleLayerTile = singleLayerTileMap.get(collectionId);
          ByteArrayOutputStream tileBytes = singleLayerByteArrayMap.get(collectionId);
          if (Objects.nonNull(tileBytes) && tileBytes.size() > 0) {
            try {
              List<VectorTileDecoder.Feature> features =
                  decoder.decode(tileBytes.toByteArray()).asList();
              features.forEach(
                  feature ->
                      encoder.addFeature(
                          feature.getLayerName(),
                          feature.getAttributes(),
                          feature.getGeometry(),
                          feature.getId()));
              processedCollections.add(collectionId);
            } catch (IOException e) {
              // maybe the file is still generated, try to wait once before giving up
              String msg =
                  "Failure to access the single-layer tile {}/{}/{}/{} in dataset '{}', layer '{}', format '{}'. Trying again ...";
              LOGGER.warn(
                  msg,
                  tileMatrixSet.getId(),
                  singleLayerTile.getTileLevel(),
                  singleLayerTile.getTileRow(),
                  singleLayerTile.getTileCol(),
                  singleLayerTile.getApiData().getId(),
                  collectionId,
                  getExtension());
            } catch (IllegalArgumentException e) {
              // another problem generating the tile, remove the problematic tile file from the
              // cache
              try {
                tileCache.deleteTile(singleLayerTile);
              } catch (SQLException throwables) {
                // ignore
              }
              throw new RuntimeException(
                  String.format(
                      "Failure to process the single-layer tile %s/%d/%d/%d in dataset '%s', layer '%s', format '%s'.",
                      tileMatrixSet.getId(),
                      singleLayerTile.getTileLevel(),
                      singleLayerTile.getTileRow(),
                      singleLayerTile.getTileCol(),
                      singleLayerTile.getApiData().getId(),
                      collectionId,
                      getExtension()),
                  e);
            }
          } else {
            try {
              if (tileCache.tileIsEmpty(singleLayerTile).orElse(false)) {
                // an empty tile, so we are done for this collection
                processedCollections.add(collectionId);
              }
            } catch (Exception e) {
              LOGGER.warn(
                  "Failed to retrieve tile {}/{}/{}/{} for collection {} from the cache. Reason: {}",
                  singleLayerTile.getTileMatrixSet().getId(),
                  singleLayerTile.getTileLevel(),
                  singleLayerTile.getTileRow(),
                  singleLayerTile.getTileCol(),
                  collectionId,
                  e.getMessage());
            }
          }
        }
      }
      if (processedCollections.size() == singleLayerTileMap.size()) break;
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        // ignore and just continue
      }
    }

    MultiLayerTileContent result = new MultiLayerTileContent();
    result.byteArray = encoder.encode();
    result.isComplete = processedCollections.size() == singleLayerTileMap.size();

    return result;
  }

  @Override
  public double getMaxAllowableOffset(Tile tile) {
    double maxAllowableOffsetTileMatrixSet =
        tile.getTileMatrixSet()
            .getMaxAllowableOffset(tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
    Unit<?> tmsCrsUnit = crsInfo.getUnit(tile.getTileMatrixSet().getCrs());
    EpsgCrs nativeCrs = crsSupport.getStorageCrs(tile.getApiData(), Optional.empty());
    Unit<?> nativeCrsUnit = crsInfo.getUnit(nativeCrs);
    if (tmsCrsUnit.equals(nativeCrsUnit)) return maxAllowableOffsetTileMatrixSet;
    else if (tmsCrsUnit.equals(Units.DEGREE) && nativeCrsUnit.equals(Units.METRE))
      return maxAllowableOffsetTileMatrixSet * 111333.0;
    else if (tmsCrsUnit.equals(Units.METRE) && nativeCrsUnit.equals(Units.DEGREE))
      return maxAllowableOffsetTileMatrixSet / 111333.0;

    LOGGER.error(
        "TileFormatMVT.getMaxAllowableOffset: Cannot convert between axis units '{}' and '{}'.",
        tmsCrsUnit.getName(),
        nativeCrsUnit.getName());
    return 0;
  }

  /**
   * If the zoom Level is not valid generate empty JSON Tile or empty MVT.
   *
   * @param tile the tile
   * @return
   */
  public byte[] getEmptyTile(Tile tile) {
    return new VectorTileEncoder(tile.getTileMatrixSet().getTileExtent()).encode();
  }
}
