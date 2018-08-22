package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static de.ii.ldproxy.target.geojson.Wfs3OutputFormatGeoJson.createJsonGenerator;
import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static de.ii.ldproxy.wfs3.vt.Wfs3EndpointTilingSchemes.TILING_SCHEMES_DIR_NAME;
import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTiles implements Wfs3EndpointExtension {

    private static final String TILES_DIR_NAME = "tiles";

    // in dev env this would be build/data/tiles
    private final File tilesDirectory;

    // in dev env this would be build/data/tilingSchemes
    private final File tilingSchemesDirectory;

    Wfs3EndpointTiles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        this.tilesDirectory = new File(new File(bundleContext.getProperty(DATA_DIR_KEY)), TILES_DIR_NAME);
        this.tilingSchemesDirectory = new File(new File(bundleContext.getProperty(DATA_DIR_KEY)), TILING_SCHEMES_DIR_NAME);

        if (!tilesDirectory.exists()) {
            tilesDirectory.mkdirs();
        }
    }

    @Override
    public String getPath() {
        return "collections";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/(?:\\w+)\\/tiles\\/?.*$";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("GET");
    }

    @Override
    public boolean matches(String firstPathSegment, String method, String subPath) {
        return Wfs3EndpointExtension.super.matches(firstPathSegment, method, subPath);
    }

    @Path("/{collectionId}/tiles/{tilingSchemeId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingScheme(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId) {

        //TODO read from tilingSchemesDirectory or 301

        return Response.ok(ImmutableMap.of())
                       .build();
    }

    @Path("/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces(Wfs3MediaTypes.GEO_JSON)
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service) throws CrsTransformationException, FileNotFoundException {

        File tileFile = getTileFile(level, row, col);

        if (!tileFile.exists()) {
            generateTileJson(tileFile, collectionId, level, row, col, service);
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFile), outputStream);
        };

        return Response.ok(streamingOutput)
                       .build();
    }

    private File getTileFile(String level, String row, String col) {
        return new File(tilesDirectory, String.format("%s_%s_%s.json", level, row, col));
    }

    private String getFilterForTile(String geometryField, String level, String row, String col, Wfs3Service service) throws CrsTransformationException {

        //TODO
        // calculate bbox in crs84
        BoundingBox bbox = new BoundingBox(-180, -90, 180, 90, DEFAULT_CRS);

        // transform to native crs
        BoundingBox bbox2 = service.transformBoundingBox(bbox);

        return String.format(Locale.US, "BBOX(%s, %.3f, %.3f, %.3f, %.3f, '%s')", geometryField, bbox2.getXmin(), bbox2.getYmin(), bbox2.getXmax(), bbox2.getYmax(), bbox2.getEpsgCrs()
                                                                                                                                                                          .getAsSimple());
    }

    private void generateTileJson(File tileFile, String collectionId, String level, String row, String col, Service service) throws FileNotFoundException, CrsTransformationException {
        Wfs3Service wfs3Service = (Wfs3Service) service;

        OutputStream outputStream = new FileOutputStream(tileFile);

        String geometryField = wfs3Service.getData()
                                          .getFilterableFieldsForFeatureType(collectionId)
                                          .get("bbox");

        String filter = getFilterForTile(geometryField, level, row, col, (Wfs3Service) service);

        //TODO
        // calculate maxAllowableOffset
        double maxAllowableOffset = 0;

        ImmutableFeatureQuery query = ImmutableFeatureQuery.builder()
                                                           .type(collectionId)
                                                           .filter(filter)
                                                           .maxAllowableOffset(maxAllowableOffset)
                                                           .build();

        FeatureStream<FeatureTransformer> featureTransformStream = wfs3Service.getFeatureProvider().getFeatureTransformStream(query);

        FeatureTransformerGeoJson featureTransformer = new FeatureTransformerGeoJson(createJsonGenerator(outputStream), true, wfs3Service.getCrsTransformer(null), ImmutableList.of(), 0, "", 0);

        try {
            featureTransformStream.apply(featureTransformer)
                                  .toCompletableFuture()
                                  .join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            }
            throw new IllegalStateException("Feature stream error", e.getCause());
        }
    }
}
