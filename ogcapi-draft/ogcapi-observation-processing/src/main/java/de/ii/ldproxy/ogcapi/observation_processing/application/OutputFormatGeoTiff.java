/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingOutputFormat;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXy;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXyt;
import de.ii.ldproxy.ogcapi.observation_processing.data.Geometry;
import geotrellis.proj4.CRS;
import geotrellis.raster.ArrayMultibandTile;
import geotrellis.raster.FloatArrayTile;
import geotrellis.raster.FloatRawArrayTile;
import geotrellis.raster.io.geotiff.*;
import geotrellis.vector.Extent;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides(specifications = {OutputFormatGeoTiff.class, ObservationProcessingOutputFormat.class, FormatExtension.class, OgcApiExtension.class})
@Instantiate
public class OutputFormatGeoTiff implements ObservationProcessingOutputFormat {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutputFormatGeoTiff.class);

    public static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("image", "tiff"))
            .label("GeoTIFF")
            .parameter("tiff")
            .build();

    private final Schema schemaTiff;
    public final static String SCHEMA_REF_TIFF = "#/components/schemas/geotiff";

    public OutputFormatGeoTiff() {
        schemaTiff = new BinarySchema();
    }

    @Override
    public String getPathPattern() {
        return "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/resample-to-grid(?:\\:aggregate-time)?/?$)";
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object initializeResult(FeatureProcessChain processes, Map<String, Object> processingParameters, OutputStream outputStream) {
        Result result = new Result(processes.getSubSubPath(), processingParameters, outputStream);
        switch (result.processName.substring(DAPA_PATH_ELEMENT.length()+2)) {
            case "resample-to-grid":
                TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");
                Comparable<Temporal> c1 = (Comparable<Temporal>) interval.getBegin();
                if (c1.compareTo(interval.getEnd())!=0)
                    throw new IllegalStateException("GeoTIFF is only supported for 'resample-to-grid', if 'datetime' is an instant, but an interval was provided.");
                break;
            case "resample-to-grid:aggregate-time":
                break;
            case "default":
                throw new IllegalStateException("GeoTIFF is only supported for 'resample-to-grid:aggregate-time'.");
        }
        return result;
    }

    @Override
    public boolean addDataArray(Object result, DataArrayXyt array) throws IOException {
        if (array.getSteps()>1)
            return false;

        int width = array.getWidth();
        int height = array.getHeight();
        Vector<String> vars = array.getVars();
        int bands = vars.size();
        FloatArrayTile[] tiles = new FloatArrayTile[bands];
        for (int i = 0; i < bands; i++) {
            tiles[i] = FloatRawArrayTile.empty(width,height);
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    tiles[i].setDouble(x, y, array.array[0][y][x][i]);
        }
        CRS crs = CRS.fromEpsgCode(4326);
        Extent extent = new Extent(array.lon(0), array.lat(height - 1), array.lon(width - 1), array.lat(0));
        GeoTiff tiff = bands > 1 ?
                new MultibandGeoTiff(new ArrayMultibandTile(tiles), extent, crs, Tags.empty(), GeoTiffOptions.DEFAULT(), scala.collection.immutable.List.empty()) :
                new SinglebandGeoTiff(tiles[0], extent, crs, Tags.empty(), GeoTiffOptions.DEFAULT(), scala.collection.immutable.List.empty());
        ((Result) result).outputStream.write(tiff.toByteArray());
        return true;
    }

    @Override
    public boolean addDataArray(Object result, DataArrayXy array) throws IOException {
        int width = array.getWidth();
        int height = array.getHeight();
        Vector<String> vars = array.getVars();
        int bands = vars.size();
        FloatArrayTile[] tiles = new FloatArrayTile[bands];
        for (int i = 0; i < bands; i++) {
            tiles[i] = FloatRawArrayTile.empty(width,height);
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    tiles[i].setDouble(x, y, array.array[height-1-y][x][i]);
        }
        CRS crs = CRS.fromEpsgCode(4326);
        Extent extent = new Extent(array.lon(0), array.lat(height - 1), array.lon(width - 1), array.lat(0));
        GeoTiff tiff = bands > 1 ?
                new MultibandGeoTiff(new ArrayMultibandTile(tiles), extent, crs, Tags.empty(), GeoTiffOptions.DEFAULT(), scala.collection.immutable.List.empty()) :
                new SinglebandGeoTiff(tiles[0], extent, crs, Tags.empty(), GeoTiffOptions.DEFAULT(), scala.collection.immutable.List.empty());
        ((Result) result).outputStream.write(tiff.toByteArray());
        return true;
    }

    @Override
    public void addFeature(Object entity, Optional<String> locationCode, Optional<String> locationName, Geometry geometry, Temporal timeBegin, Temporal timeEnd, Map<String, Number> values) throws IOException {
        throw new InternalError("This method should never be called for the GeoTIFF output format.");
    }

    @Override
    public void finalizeResult(Object result) throws IOException {
        ((Result) result).outputStream.flush();
    }

    @Override
    public OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path) {
        String processId = path.substring(path.lastIndexOf("/")+1);
        if (!processId.startsWith("resample-to-grid"))
            return null;

        return new ImmutableOgcApiMediaTypeContent.Builder()
                .schema(schemaTiff)
                .schemaRef(SCHEMA_REF_TIFF)
                .ogcApiMediaType(MEDIA_TYPE)
                .build();
    }

    @Override
    public boolean contentPerApi() {
        return false;
    }

    @Override
    public boolean contentPerResource() {
        return false;
    }

    class Result {
        final String processName;
        final List<String> variables;
        final List<ObservationProcessingStatisticalFunction> functions;
        final List<String> var_funct;
        final OutputStream outputStream;
        String csv;
        Result(String processName, Map<String, Object> processingParameters, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.processName = processName;
            variables = (List<String>) processingParameters.getOrDefault("variables", ImmutableList.of());
            functions = (List<ObservationProcessingStatisticalFunction>) processingParameters.getOrDefault("functions", ImmutableList.of());
            var_funct = variables.stream()
                    .map(var -> functions.stream().map(funct -> var+"_"+funct.getName()).collect(Collectors.toList()))
                    .flatMap(Collection::stream)
                    .sorted()
                    .collect(Collectors.toList());
            csv = "";
        }
    }
}
