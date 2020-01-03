/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Provides
@Instantiate
public class CollectionMultitilesGenerator implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionMultitilesGenerator.class);

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/multitiles";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiTilesEnabled)
                .isPresent();
    }

    /**
     * Construct a response for a multiple tiles request
     * @param tileMatrixSetId identifier of tile matrix set
     * @param bboxParam value of the bbox request parameter
     * @param scaleDenominatorParam value of the scaleDenominator request parameter
     * @param multiTileType value of the multiTileType request parameter
     * @param uriCustomizer uri customizer
     * @return multiple tiles
     */
    Response getMultitiles(String tileMatrixSetId, String bboxParam, String scaleDenominatorParam, String multiTileType,
                           URICustomizer uriCustomizer, String tileFormatParam, String collectionId, CrsTransformation crsTransformation,
                           UriInfo uriInfo, I18n i18n, Optional<Locale> language, OgcApiDataset service, VectorTilesCache cache,
                           OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson) {

        String tileFormat = MultitilesUtils.parseTileFormat(tileFormatParam);
        TileMatrixSet tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);
        List<Integer> tileMatrices = MultitilesUtils.parseScaleDenominator(scaleDenominatorParam, tileMatrixSet);
        double[] bbox = MultitilesUtils.parseBbox(bboxParam, tileMatrixSet);
        LOGGER.debug("GET TILE MULTITILES {} {}-{} {} {}", bbox, tileMatrices.get(0), tileMatrices.get(tileMatrices.size()-1), multiTileType, tileFormat);
        List<TileSetEntry> tileSetEntries = generateTilesetEntries(bbox, tileMatrices, uriCustomizer, MultitilesUtils.parseTileFormat(tileFormat), tileMatrixSet);

        if ("url".equals(multiTileType)) {
            return Response.ok(ImmutableMap.of("tileSet", tileSetEntries))
                    .type("application/geo+json")
                    .build();
        } else if (multiTileType == null || "tiles".equals(multiTileType) || "full".equals(multiTileType)) {
            File zip = generateZip(tileSetEntries, tileMatrixSetId, collectionId, "full".equals(multiTileType), crsTransformation,
                    uriInfo, i18n, language, uriCustomizer, service, cache, wfs3OutputFormatGeoJson, tileFormat);
            return Response.ok(zip)
                    .type("application/zip")
                    .build();
        }
        throw new NotFoundException("Unknown multiTileType");
    }

    /**
     * Generate a list of tiles that cover the bounding box for given tile matrices
     * @param bbox bounding box specified by two points and their longitude and latitude coordinates (WGS 84)
     * @param tileMatrices all tile matrices to be retrieved
     * @param uriCustomizer uri customizer
     * @return list of TileSet objects
     */
    private List<TileSetEntry> generateTilesetEntries(double[] bbox, List<Integer> tileMatrices, URICustomizer uriCustomizer,
                                                        String tileFormat, TileMatrixSet tileMatrixSet) {
        List<TileSetEntry> tileSets = new ArrayList<>();
        for (int tileMatrix : tileMatrices) {
            List<Integer> bottomTile = MultitilesUtils.pointToTile(bbox[0], bbox[1], tileMatrix, tileMatrixSet);
            List<Integer> topTile = MultitilesUtils.pointToTile(bbox[2], bbox[3], tileMatrix, tileMatrixSet);
            for (int row = topTile.get(0); row <= bottomTile.get(0); row++){
                for (int col = bottomTile.get(1); col <= topTile.get(1); col++) {
                    tileSets.add(ImmutableTileSetEntry.builder()
                            .tileURL(uriCustomizer.copy()
                                    .clearParameters()
                                    .ensureLastPathSegments(Integer.toString(tileMatrix), Integer.toString(row), Integer.toString(col))
                                    .ensureNoTrailingSlash()
                                    .addParameter("f", tileFormat)
                                    .toString())
                            .tileMatrix(tileMatrix)
                            .tileRow(row)
                            .tileCol(col)
                            .build());
                }
            }
        }
        return tileSets;
    }

    protected File generateZip(List<TileSetEntry> tileSetEntries, String tileMatrixSetId, String collectionId,
                               boolean isFull, CrsTransformation crsTransformation, UriInfo uriInfo, I18n i18n,
                               Optional<Locale> language, URICustomizer uriCustomizer, OgcApiDataset service,
                               VectorTilesCache cache, OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson,
                               String tileFormat) {
        File zip = null;

        try {
            zip = File.createTempFile(tileMatrixSetId, ".zip");
            FileOutputStream fout = new FileOutputStream(zip);
            ZipOutputStream zout = new ZipOutputStream(fout);

            if (isFull) {
                // add tileSet response document in the archive
                ObjectMapper mapper = new ObjectMapper();
                String jsonString = mapper.writeValueAsString(ImmutableMap.of("tileSet", tileSetEntries));
                File tmpFile = File.createTempFile(tileMatrixSetId, ".json");
                FileWriter writer = new FileWriter(tmpFile);
                writer.write(jsonString);
                writer.close();

                zout.putNextEntry(new ZipEntry(tileMatrixSetId + ".json"));

                try (FileInputStream fis = new FileInputStream(tmpFile.getAbsolutePath());
                     BufferedInputStream bis = new BufferedInputStream(fis, 1024)) {
                    int count;
                    byte[] data = new byte[1024];
                    while ((count = bis.read(data, 0, 1024)) != -1) {
                        zout.write(data, 0, count);
                    }
                }
                zout.closeEntry();
                tmpFile.deleteOnExit();

            }
            for (TileSetEntry entry : tileSetEntries) {
                File tileFile;
                VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, String.valueOf(entry.getTileMatrix()),
                        String.valueOf(entry.getTileRow()), String.valueOf(entry.getTileCol()), service, false, cache,
                        service.getFeatureProvider(), wfs3OutputFormatGeoJson);
                File tileFileJson = tile.getFile(cache, "json");

                if (!tileFileJson.exists()) {
                    OgcApiMediaType geoJsonMediaType = new ImmutableOgcApiMediaType.Builder()
                            .type(new MediaType("application", "geo+json"))
                            .label("GeoJSON")
                            .build();
                    TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, null, null, uriCustomizer,
                            geoJsonMediaType, true, tile, i18n, language);
                }

                // add the generated tile to the archive
                String path = new StringBuilder(tileMatrixSetId)
                        .append(File.separator)
                        .append(entry.getTileMatrix())
                        .append(File.separator)
                        .append(entry.getTileRow())
                        .append(File.separator)
                        .append(entry.getTileCol())
                        .append(".")
                        .append(tileFormat)
                        .toString();
                zout.putNextEntry(new ZipEntry(path));

                tileFile = tileFileJson;
                if ("mvt".equals(tileFormat)) {
                    File tileFileMvt = tile.getFile(cache, "pbf");
                    Wfs3EndpointTilesSingleCollection.generateTileCollection(collectionId, tileFileJson, tileFileMvt,
                            tile, null, crsTransformation);
                    tileFile = tileFileMvt;
                }

                try (FileInputStream fis = new FileInputStream(tileFile.getAbsolutePath());
                     BufferedInputStream bis = new BufferedInputStream(fis, 1024)) {
                    int count;
                    byte[] data = new byte[1024];
                    while ((count = bis.read(data, 0, 1024)) != -1) {
                        zout.write(data, 0, count);
                    }
                }
                zout.closeEntry();
                tileFile.delete();
            }
            zout.close();
            fout.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return zip;
    }

}
