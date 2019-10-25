/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Component
@Provides
@Instantiate
public class CollectionsMultitilesGenerator implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultitilesGenerator.class);

    CollectionsMultitilesGenerator() {
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/cols-multitiles";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * Construct a response for a multitiles request for multiple collections
     * @param tileMatrixSetId identifier of tile matrix set
     * @param bboxParam value of the bbox request parameter
     * @param scaleDenominatorParam value of the scaleDenominator request parameter
     * @param multiTileType value of the multiTileType request parameter
     * @param uriCustomizer  uri customizer
     * @param collections requested collections
     * @return nultiple tiles from multiple collections
     */
    Response getCollectionsMultitiles(String tileMatrixSetId, String bboxParam, String scaleDenominatorParam,
                                      String multiTileType, URICustomizer uriCustomizer, Set<String> collections) throws UnsupportedEncodingException {

        MultitilesGenerator.checkTileMatrixSet(tileMatrixSetId);
        List<Integer> tileMatrices = MultitilesGenerator.parseScaleDenominator(scaleDenominatorParam);
        double[] bbox = MultitilesGenerator.parseBbox(bboxParam);
        String collectoinsCsv = String.join(",", collections);
        LOGGER.debug("GET TILES COLLECTIONS MULTITILES {} {}-{} {} {}", bbox, tileMatrices.get(0), tileMatrices.get(tileMatrices.size() - 1), multiTileType, collectoinsCsv);
        List<TileSetEntry> tileSetEntries = generateCollectionsTileSetEntries(bbox, tileMatrices, uriCustomizer, collectoinsCsv);

        if ("url".equals(multiTileType)) {
            return Response.ok(ImmutableMap.of("tileSet", tileSetEntries))
                    .type("application/geo+json")
                    .build();
        } else {
            //generateZip(tileSetEntries)
            return Response.ok(null)
                    .type("application/zip")
                    .build();

        }
    }

    /**
     * Generate a list of tiles that cover the bounding box for given tile matrices
     * @param bbox bounding box specified by two points and their longitude and latitude coordinates (WGS 84)
     * @param tileMatrices all tile matrices to be retrieved
     * @param uriCustomizer uri customizer
     * @return list of TileSet objects
     */
    private List<TileSetEntry> generateCollectionsTileSetEntries(double[] bbox, List<Integer> tileMatrices,
                                                                 URICustomizer uriCustomizer, String collections) throws UnsupportedEncodingException {
        List<TileSetEntry> tileSets = new ArrayList<>();
        for (int tileMatrix : tileMatrices) {
            List<Integer> bottomTile = MultitilesGenerator.pointToTile(bbox[0], bbox[1], tileMatrix);
            List<Integer> topTile = MultitilesGenerator.pointToTile(bbox[2], bbox[3], tileMatrix);
            for (int row = bottomTile.get(0); row <= topTile.get(0); row++){
                for (int col = topTile.get(1); col <= bottomTile.get(1); col++) {
                    tileSets.add(ImmutableTileSetEntry.builder()
                            .tileURL(URLDecoder.decode(uriCustomizer.copy()
                                    .clearParameters()
                                    .ensureLastPathSegments(Integer.toString(tileMatrix), Integer.toString(row), Integer.toString(col))
                                    .ensureNoTrailingSlash()
                                    .addParameter("collections", collections)
                                    .toString(), "UTF-8"))
                            .tileMatrix(tileMatrix)
                            .tileRow(row)
                            .tileCol(col)
                            .build());
                }
            }
        }
        return tileSets;
    }
}
