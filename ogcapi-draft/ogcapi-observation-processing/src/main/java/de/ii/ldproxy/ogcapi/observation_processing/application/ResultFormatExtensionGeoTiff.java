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
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.domain.FormatNotSupportedException;
import de.ii.ldproxy.ogcapi.observation_processing.api.DapaResultFormatExtension;
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
import scala.Tuple2;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Provides(specifications = {ResultFormatExtensionGeoTiff.class, DapaResultFormatExtension.class, FormatExtension.class, ApiExtension.class})
@Instantiate
public class ResultFormatExtensionGeoTiff implements DapaResultFormatExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultFormatExtensionGeoTiff.class);

    public static final ApiMediaType MEDIA_TYPE = new ImmutableApiMediaType.Builder()
            .type(new MediaType("image", "tiff"))
            .label("GeoTIFF")
            .parameter("tiff")
            .build();

    private final Schema schemaTiff;
    public final static String SCHEMA_REF_TIFF = "#/components/schemas/geotiff";

    public ResultFormatExtensionGeoTiff() {
        schemaTiff = new BinarySchema();
    }

    @Override
    public String getPathPattern() {
        return "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/grid(?:\\:aggregate-time)?/?$)";
    }

    @Override
    public ApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public Object initializeResult(FeatureProcessChain processes, Map<String, Object> processingParameters, List<Variable> variables, OutputStream outputStream, OgcApiDataV2 apiData) throws IOException {
        Result result = new Result(processes.getSubSubPath(), processingParameters, variables, outputStream);
        switch (result.processName.substring(DAPA_PATH_ELEMENT.length()+2)) {
            case "grid:retrieve":
                TemporalInterval interval = (TemporalInterval) processingParameters.get("interval");
                Comparable<Temporal> c1 = (Comparable<Temporal>) interval.getBegin();
                if (c1.compareTo(interval.getEnd())!=0)
                    throw new FormatNotSupportedException("GeoTIFF is only supported for 'grid', if 'datetime' is an instant, but an interval was provided.");
                break;
            case "grid:aggregate-time":
                break;
            case "default":
                throw new FormatNotSupportedException("GeoTIFF is only supported for 'grid:aggregate-time'.");
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
        Extent extent = new Extent(array.lon(0), array.lat(height - 1), array.lon(width - 1), array.lat(0));
        writeGeoTiff(((Result) result).outputStream, width, height, extent, vars, ((Result) result).variableDefinitions, array.array[0]);
        return true;
    }

    @Override
    public boolean addDataArray(Object result, DataArrayXy array) throws IOException {
        int width = array.getWidth();
        int height = array.getHeight();
        Vector<String> vars = array.getVars();
        Extent extent = new Extent(array.lon(0), array.lat(height - 1), array.lon(width - 1), array.lat(0));
        writeGeoTiff(((Result) result).outputStream, width, height, extent, vars, ((Result) result).variableDefinitions, array.array);
        return true;
    }

    public void writeGeoTiff(OutputStream outputStream, int width, int height, Extent extent, List<String> vars, List<Variable> varDefs, float[][][] array) throws IOException {
        int bands = vars.size();
        FloatArrayTile[] tiles = new FloatArrayTile[bands];
        for (int i = 0; i < bands; i++) {
            tiles[i] = FloatRawArrayTile.empty(width,height);
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    tiles[i].setDouble(x, y, array[y][x][i]);
        }
        CRS crs = CRS.fromEpsgCode(4326);
        scala.collection.immutable.Map<String, String> headMap = new scala.collection.immutable.HashMap<String, String>()
                .$plus(new Tuple2<>("TIFFTAG_SOFTWARE", "ldproxy"))
                .$plus(new Tuple2<>("AREA_OR_POINT", "Area"));
        List<scala.collection.immutable.Map<String, String>> bandMaps = new Vector<>();
        for (int i = 0; i < bands; i++) {
            int finalI = i;
            Optional<Variable> variable = varDefs.stream().filter(var -> var.getId().equals(vars.get(finalI))).findFirst();
            String uom = variable.isPresent() ? variable.get().getUom().orElse("unknown") : "unknown";
            String name = variable.isPresent() && variable.get().getTitle().isPresent() ? vars.get(i) + " ("+variable.get().getTitle().get()+")" : vars.get(i);
            bandMaps.add(new scala.collection.immutable.HashMap<String, String>().$plus(new Tuple2<>("UNITTYPE", uom))
                                                                                 .$plus(new Tuple2<>("COLORINTERP", "0"))
                                                                                 .$plus(new Tuple2<>("DESCRIPTION", name)));
        }
        Tags tags = new Tags(headMap, scala.collection.JavaConverters.asScalaBuffer(bandMaps).toList());
        GeoTiff tiff = bands > 1 ?
                new MultibandGeoTiff(new ArrayMultibandTile(tiles), extent, crs, tags, GeoTiffOptions.DEFAULT(), scala.collection.immutable.List.empty()) :
                new SinglebandGeoTiff(tiles[0], extent, crs, tags, GeoTiffOptions.DEFAULT(), scala.collection.immutable.List.empty());
        outputStream.write(tiff.toByteArray());
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
    public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
        String processId = path.substring(path.lastIndexOf("/")+1);
        if (!processId.startsWith("grid"))
            return null;

        return new ImmutableApiMediaTypeContent.Builder()
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
        final List<Variable> variableDefinitions;
        final List<ObservationProcessingStatisticalFunction> functions;
        final List<String> var_funct;
        final OutputStream outputStream;
        String csv;
        Result(String processName, Map<String, Object> processingParameters, List<Variable> variableDefinitions, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.processName = processName;
            this.variableDefinitions = variableDefinitions;
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
