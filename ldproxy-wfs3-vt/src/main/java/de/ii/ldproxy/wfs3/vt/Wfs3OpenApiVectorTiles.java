/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.ldproxy.wfs3.oas30.Wfs3OpenApiExtension;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.*;

import static de.ii.ldproxy.wfs3.vt.TilesConfiguration.EXTENSION_KEY;

/**
 * extend API definition with tile resources
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3OpenApiVectorTiles implements Wfs3OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 20;
    }

    /**
     * extend the openAPI definition with necessary parameters and schemas. Add paths with parameters and responses to the OpenAPI definition.
     *
     * @param openAPI the openAPI definition
     * @param serviceData the data from the Wfs3 Service
     * @return the extended OpenAPI definition
     */
    @Override
    public OpenAPI process(OpenAPI openAPI, Wfs3ServiceData serviceData) {

        boolean enableTilesInAPI = false;

        Optional<FeatureTypeConfigurationWfs3> firstCollectionWithTiles = serviceData
                .getFeatureTypes()
                .values()
                .stream()
                .filter(ft -> { try {
                    if (ft.getExtensions().containsKey(EXTENSION_KEY)) {
                        TilesConfiguration tilesConfiguration = (TilesConfiguration) ft.getExtensions().get(EXTENSION_KEY);

                        if(tilesConfiguration.getTiles().getEnabled())
                            return true;
                    }
                    return false;
                } catch (NullPointerException ignored){return false;} })
                .findFirst();

        if(firstCollectionWithTiles.isPresent())
            enableTilesInAPI = true;

        if(!firstCollectionWithTiles.isPresent())
            enableTilesInAPI = false;


        if(enableTilesInAPI) {
            /*specify all new parameters. They are:
             * tilingSchemeId
             * zoomLevel
             * row
             * column
             * f2/f3/f4
             * collections
             * properties*/

            Parameter tilingSchemeId = new Parameter();
            tilingSchemeId.setName("tilingSchemeId");
            tilingSchemeId.in("path");
            tilingSchemeId.description("Local identifier of a specific tiling scheme. A list of all available tilingSchemeIds can be found under the /tiles path or below. The default Tiling Scheme is the Google Maps Tiling Scheme.");
            tilingSchemeId.setRequired(true);
            Schema tilingSchemeIdSchema = new Schema();
            tilingSchemeIdSchema.setType("string");

            List<String> tilingSchemeEnum = new ArrayList<String>();
            tilingSchemeEnum.add("default");

            tilingSchemeId.setSchema(new StringSchema()._enum(tilingSchemeEnum));
            tilingSchemeId.example("default");


            Parameter zoomLevel = new Parameter();
            zoomLevel.setName("level");
            zoomLevel.in("path");
            zoomLevel.description("Level of the tile. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps Tiling Scheme. \\\n \\\n" +
                    "Example: Ireland is fully within the Tile with the following values: Level 5, Row 10 and Col 15");
            zoomLevel.setRequired(true);
            zoomLevel.setSchema(tilingSchemeIdSchema);
            zoomLevel.setExample("11");

            Parameter row = new Parameter();
            row.setName("row");
            row.in("path");
            row.description("Row index of the tile on the selected zoom level. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps Tiling Scheme.\\\n \\\n" +
                    "Example: Ireland is fully within the Tile with the following values: Level 5, Row 10 and Col 15");
            row.setRequired(true);
            row.setSchema(tilingSchemeIdSchema);
            row.setExample("827");

            Parameter column = new Parameter();
            column.setName("col");
            column.in("path");
            column.description("Column index of the tile on the selected zoom level. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps Tiling Scheme.\\\n \\\n" +
                    "Example: Ireland is fully within the Tile with the following values: Level 5, Row 10 and Col 15");
            column.setRequired(true);
            column.setSchema(tilingSchemeIdSchema);
            column.setExample("1231");


            Parameter f2 = new Parameter();
            f2.setName("f");
            f2.in("query");
            f2.description("\\\n" +
                    "        The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\\n" +
                    "        Pre-defined values are \"json\" and \"mvt\". The response to other values is determined by the server.");
            f2.setRequired(false);
            f2.setStyle(Parameter.StyleEnum.FORM);
            f2.setExplode(false);
            List<String> f2Enum = new ArrayList<String>();
            f2Enum.add("json");
            f2Enum.add("mvt");
            f2.setSchema(new StringSchema()._enum(f2Enum));
            f2.example("json");

            Parameter f3 = new Parameter();
            f3.setName("f");
            f3.in("query");
            f3.description("\\\n" +
                    "        The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\\n" +
                    "        The only pre-defined value is \"json\". The response to other values is determined by the server.");
            f3.setRequired(false);
            f3.setStyle(Parameter.StyleEnum.FORM);
            f3.setExplode(false);
            Schema f3Schema = new Schema();
            f3Schema.setType("string");
            List<String> f3Enum = new ArrayList<String>();
            f3Enum.add("json");
            f3.setSchema(new StringSchema()._enum(f3Enum));
            f3.example("json");

            Parameter f4 = new Parameter();
            f4.setName("f");
            f4.in("query");
            f4.description("\\\n" +
                    "        The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\\n" +
                    "        The only pre-defined value is \"mvt\". The response to other values is determined by the server.");
            f4.setRequired(false);
            f4.setStyle(Parameter.StyleEnum.FORM);
            f4.setExplode(false);
            List<String> f4Enum = new ArrayList<String>();
            f4Enum.add("mvt");
            f4.setSchema(new StringSchema()._enum(f4Enum));
            f4.example("mvt");

            Parameter collections = new Parameter();
            collections.setName("collections");
            collections.in("query");
            collections.description("The collections that should be included in the tile. The parameter value is a list of collection identifiers.");
            collections.setRequired(false);
            collections.setStyle(Parameter.StyleEnum.FORM);
            collections.setExplode(false);
            List<String> collectionsEnum = new ArrayList<String>();
            serviceData.getFeatureTypes()
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                    .filter(ft -> serviceData.isFeatureTypeEnabled(ft.getId()))
                    .forEach(ft -> collectionsEnum.add(ft.getId()));
            Schema collectionsArrayItems = new Schema().type("string");
            collectionsArrayItems.setEnum(collectionsEnum);
            Schema collectionsSchema = new ArraySchema().items(collectionsArrayItems);
            collections.setSchema(collectionsSchema);

            Parameter properties = new Parameter();
            properties.name("properties");
            properties.in("query");
            properties.description("The properties that should be included for each feature. The parameter value is a list of property names.");
            properties.setRequired(false);
            properties.setStyle(Parameter.StyleEnum.FORM);
            properties.setExplode(false);
            Schema propertiesSchema = new ArraySchema().items(new Schema().type("string"));
            properties.setSchema(propertiesSchema);

            /*Add the parameters to definition*/
            openAPI.getComponents().addParameters("f2", f2);
            openAPI.getComponents().addParameters("f3", f3);
            openAPI.getComponents().addParameters("f4", f4);
            openAPI.getComponents().addParameters("tilingSchemeId", tilingSchemeId);
            openAPI.getComponents().addParameters("level", zoomLevel);
            openAPI.getComponents().addParameters("row", row);
            openAPI.getComponents().addParameters("col", column);
            openAPI.getComponents().addParameters("collections", collections);
            openAPI.getComponents().addParameters("properties", properties);

            List<String> modelRequirements = new LinkedList<String>();
            modelRequirements.add("type");
            List<String> tilingSchemeRequirements = new LinkedList<String>();
            tilingSchemeRequirements.add("type");
            tilingSchemeRequirements.add("identifier");

            Schema tilingSchemesArray = new Schema();
            tilingSchemesArray.setType("object");
            tilingSchemesArray.setRequired(modelRequirements);
            tilingSchemesArray.addProperties("identifier", new Schema().type("string").example("default"));
            tilingSchemesArray.addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));

            Schema tilingSchemes = new Schema();
            tilingSchemes.type("object");
            tilingSchemes.setRequired(modelRequirements);
            tilingSchemes.addProperties("tilingSchemes", new ArraySchema().items(new Schema().$ref("#/components/schemas/tilingSchemesArray")));

            Schema boundingBox = new Schema();
            boundingBox.setType("object");
            boundingBox.setRequired(modelRequirements);
            List<String> boundingBoxEnum = new ArrayList<String>();
            boundingBoxEnum.add("BoundingBox");
            boundingBox.addProperties("type", new StringSchema()._enum(boundingBoxEnum));
            boundingBox.addProperties("crs", new Schema().type("string").example("http://www.opengis.net/def/crs/EPSG/0/3857"));
            List<Double> lowerCorner = new ArrayList<>();
            lowerCorner.add(-20037508.3427892);
            lowerCorner.add(-20037508.342789);
            boundingBox.addProperties("lowerCorner", new Schema().type("array").example(lowerCorner));
            List<Double> upperCorner = new ArrayList<>();
            upperCorner.add(20037508.3427892);
            upperCorner.add(20037508.3427892);
            boundingBox.addProperties("upperCorner", new Schema().type("array").example(upperCorner));


            Schema matrix = new Schema();
            matrix.setType("object");
            matrix.setRequired(modelRequirements);
            List<String> matrixEnum = new ArrayList<String>();
            matrixEnum.add("TileMatrix");
            matrix.addProperties("type", new StringSchema()._enum(matrixEnum));
            matrix.addProperties("identifier", new Schema().type("string").example('0'));
            matrix.addProperties("MatrixHeight", new Schema().minimum(BigDecimal.valueOf(0)).type("integer").example(1));
            matrix.addProperties("MatrixWidth", new Schema().minimum(BigDecimal.valueOf(0)).type("integer").example(1));
            matrix.addProperties("TileHeight", new Schema().minimum(BigDecimal.valueOf(0)).type("integer").example(256));
            matrix.addProperties("TileWidth", new Schema().minimum(BigDecimal.valueOf(0)).type("integer").example(256));
            matrix.addProperties("scaleDenominator", new Schema().type("number").example(559082264.028717));
            List<Double> topLeftCorner = new ArrayList<>();
            topLeftCorner.add(-20037508.3427892);
            topLeftCorner.add(20037508.3427892);
            matrix.addProperties("topLeftCorner", new Schema().type("array").example(topLeftCorner));

            Schema tilingScheme = new Schema();
            tilingScheme.setType("object");
            tilingScheme.setRequired(tilingSchemeRequirements);
            List<String> tileMatrixSetEnum = new ArrayList<String>();
            tileMatrixSetEnum.add("TileMatrixSet");
            List<String> tilingSchemeSupportedCrsEnum = new ArrayList<String>();
            tilingSchemeSupportedCrsEnum.add("http://www.opengis.net/def/crs/EPSG/0/3857");
            List<String> tilingSchemeWellKnownEnum = new ArrayList<String>();
            tilingSchemeWellKnownEnum.add("http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible");
            tilingScheme.addProperties("type", new StringSchema()._enum(tileMatrixSetEnum));
            tilingScheme.addProperties("identifier", new Schema().type("string").example("default"));
            tilingScheme.addProperties("title", new Schema().type("string").example("Google Maps Compatible for the World"));
            tilingScheme.addProperties("supportedCrs", new StringSchema()._enum(tilingSchemeSupportedCrsEnum).example("http://www.opengis.net/def/crs/EPSG/0/3857"));
            tilingScheme.addProperties("wellKnownScaleSet", new StringSchema()._enum(tilingSchemeWellKnownEnum).example("http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible"));
            tilingScheme.addProperties("TileMatrix", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileMatrix")));
            tilingScheme.addProperties("boundingBox", new ArraySchema().items(new Schema().$ref("#/components/schemas/boundingBox")));

            Schema mvt = new Schema();
            mvt.type("string");
            mvt.format("binary");

            /*Add the schemas to definition*/
            openAPI.getComponents().addSchemas("tilingSchemesArray", tilingSchemesArray);
            openAPI.getComponents().addSchemas("tilingSchemes", tilingSchemes);
            openAPI.getComponents().addSchemas("boundingBox", boundingBox);
            openAPI.getComponents().addSchemas("tileMatrix", matrix);
            openAPI.getComponents().addSchemas("tilingScheme", tilingScheme);
            openAPI.getComponents().addSchemas("mvt", mvt);

            /*create a new tag and add it to definition*/
            openAPI.getTags().add(new Tag().name("Tiles").description("Access to data (features), partitioned into a hierarchy of tiles."));


            if (serviceData != null && serviceData.getFeatureProvider().supportsTransactions()) {

                openAPI.getPaths().addPathItem("/tilingSchemes", new PathItem().description("something"));  //create a new path
                PathItem pathItem = openAPI.getPaths().get("/tilingSchemes");
                ApiResponse success = new ApiResponse().description("A list of tiling schemes.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tilingSchemes")))
                        );
                ApiResponse exception = new ApiResponse().description("An error occured.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                        );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve all available tiling schemes")
                                    .operationId("getTilingSchemes")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                        .addPathItem("/tilingSchemes", pathItem); //save to Path

                openAPI.getPaths().addPathItem("/tilingSchemes/{tilingSchemeId}", new PathItem().description("something"));
                pathItem = openAPI.getPaths().get("/tilingSchemes/{tilingSchemeId}");
                success = new ApiResponse().description("A tiling scheme.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tilingScheme")))
                        );
                exception = new ApiResponse().description("An error occured.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                        );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve a tiling scheme by id")
                                    .operationId("getTilingScheme")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tilingSchemeId"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                        .addPathItem("/tilingSchemes/{tilingSchemeId}", pathItem);


                openAPI.getPaths().addPathItem("/tiles", new PathItem().description("something"));  //create a new path
                pathItem = openAPI.getPaths().get("/tiles");
                success = new ApiResponse().description("A list of tiling schemes.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tilingSchemes")))
                        );
                exception = new ApiResponse().description("An error occured.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                        );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve all available tiling schemes")
                                    .operationId("getTilingSchemesAlt")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                        .addPathItem("/tiles", pathItem); //save to Path


                openAPI.getPaths().addPathItem("/tiles/{tilingSchemeId}", new PathItem().description("something"));
                pathItem = openAPI.getPaths().get("/tiles/{tilingSchemeId}");
                success = new ApiResponse().description("A tiling scheme used to partition the dataset into tiles.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tilingScheme")))
                        );
                exception = new ApiResponse().description("An error occured.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                        );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve a tiling scheme used to partition the dataset into tiles")
                                    .operationId("getTilingSchemePartion")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tilingSchemeId"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }

                openAPI.getPaths()
                        .addPathItem("/tiles/{tilingSchemeId}", pathItem);


                openAPI.getPaths().addPathItem("/tiles/{tilingSchemeId}/{level}/{row}/{col}", new PathItem().description("something"));
                pathItem = openAPI.getPaths().get("/tiles/{tilingSchemeId}/{level}/{row}/{col}");
                success = new ApiResponse().description("A tile of the dataset.")
                        .content(new Content()
                                .addMediaType("application/vnd.mapbox-vector-tile", new MediaType().schema(new Schema().$ref("#/components/schemas/mvt")))
                        );
                exception = new ApiResponse().description("An error occured.")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                        );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve a tile of the dataset")
                                    .description("The tile in the requested tiling scheme, on the requested zoom level in the tiling scheme, with the requested grid coordinates (row, column) is returned. " +
                                            "Each collection of the dataset is returned as a separate layer. The collections and the feature properties to include in the tile representation can be limited using query" +
                                            " parameters.")
                                    .operationId("getTilesDataset")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tilingSchemeId"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/level"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/row"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/col"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/collections"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/properties"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f4"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }

                openAPI.getPaths()
                        .addPathItem("/tiles/{tilingSchemeId}/{level}/{row}/{col}", pathItem);

                //do for every feature type
                serviceData.getFeatureTypes()
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(FeatureTypeConfigurationWfs3::getId))
                        .filter(ft -> serviceData.isFeatureTypeEnabled(ft.getId()))
                        .forEach(ft -> {
                            boolean enableTilesCollectionInApi = true;

                            try{
                                if (ft.getExtensions().containsKey(EXTENSION_KEY)) {
                                    final TilesConfiguration tilesConfiguration = (TilesConfiguration) ft.getExtensions().get(EXTENSION_KEY);

                                    boolean tilesCollectionEnabled = tilesConfiguration.getTiles().getEnabled();
                                    if (!tilesCollectionEnabled)
                                        enableTilesCollectionInApi = false;
                                }
                            }catch(NullPointerException e){
                                enableTilesCollectionInApi = false;
                            }

                            if(enableTilesCollectionInApi) {
                                openAPI.getPaths().addPathItem("/collections/" + ft.getId() + "/tiles", new PathItem().description("something"));  //create a new path
                                PathItem pathItem2 = openAPI.getPaths().get("/collections/" + ft.getId() + "/tiles");
                                ApiResponse success2 = new ApiResponse().description("A list of tiling schemes from the collection" + ft.getLabel() + ".")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tilingSchemes")))
                                        );
                                ApiResponse exception2 = new ApiResponse().description("An error occured.")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                        );
                                if (Objects.nonNull(pathItem2)) {
                                    pathItem2
                                            .get(new Operation()
                                                    .addTagsItem("Tiles")
                                                    .summary("retrieve all available tiling schemes from the collection " + ft.getLabel())
                                                    .operationId("getTilingSchemesCollection" + ft.getId())
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                                    //.requestBody(requestBody)
                                                    .responses(new ApiResponses()
                                                            .addApiResponse("200", success2)
                                                            .addApiResponse("default", exception2))
                                            );
                                }
                                openAPI.getPaths().addPathItem("/collections/" + ft.getId() + "/tiles", pathItem2); //save to Path


                                openAPI.getPaths().addPathItem("/collections/" + ft.getId() + "/tiles/{tilingSchemeId}", new PathItem().description("something"));
                                pathItem2 = openAPI.getPaths().get("/collections/" + ft.getId() + "/tiles/{tilingSchemeId}");
                                success2 = new ApiResponse().description("A tiling scheme used to partition the collection " + ft.getLabel() + " into tiles.")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tilingScheme")))
                                        );
                                exception2 = new ApiResponse().description("An error occured.")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                        );
                                if (Objects.nonNull(pathItem2)) {
                                    pathItem2
                                            .get(new Operation()
                                                    .addTagsItem("Tiles")
                                                    .summary("retrieve a tiling scheme used to partition the collection" + ft.getLabel() + " into tiles")
                                                    .operationId("getTilingSchemeCollection" + ft.getId())
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tilingSchemeId"))
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                                    //.requestBody(requestBody)
                                                    .responses(new ApiResponses()
                                                            .addApiResponse("200", success2)
                                                            .addApiResponse("default", exception2))
                                            );
                                }

                                openAPI.getPaths()
                                        .addPathItem("/collections/" + ft.getId() + "/tiles/{tilingSchemeId}", pathItem2);


                                openAPI.getPaths().addPathItem("/collections/" + ft.getId() + "/tiles/{tilingSchemeId}/{level}/{row}/{col}", new PathItem().description("something"));
                                pathItem2 = openAPI.getPaths().get("/collections/" + ft.getId() + "/tiles/{tilingSchemeId}/{level}/{row}/{col}");
                                success2 = new ApiResponse().description("A tile of the collection " + ft.getLabel() + ".")
                                        .content(new Content()
                                                .addMediaType("application/geo+json", new MediaType().schema(new Schema().$ref("#/components/schemas/featureCollectionGeoJSON")))
                                                .addMediaType("application/vnd.mapbox-vector-tile", new MediaType().schema(new Schema().$ref("#/components/schemas/mvt")))
                                        );
                                exception2 = new ApiResponse().description("An error occured.")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                        );
                                if (Objects.nonNull(pathItem2)) {
                                    pathItem2
                                            .get(new Operation()
                                                    .addTagsItem("Tiles")
                                                    .summary("retrieve a tile of the collection " + ft.getLabel())
                                                    .description("The tile in the requested tiling scheme, on the requested zoom level in the tiling scheme, with the requested grid coordinates (row, column) is returned. " +
                                                            "The tile has a single layer with all selected features in the bounding box of the tile. The feature properties to " +
                                                            "include in the tile representation can be limited using a query parameter.")
                                                    .operationId("getTilesCollection" + ft.getId())
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tilingSchemeId"))
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/level"))
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/row"))
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/col"))
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/properties"))
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f2"))
                                                    .addParametersItem(new Parameter().$ref("#/components/parameters/time"))
                                                    //.requestBody(requestBody)
                                                    .responses(new ApiResponses()
                                                            .addApiResponse("200", success2)
                                                            .addApiResponse("default", exception2))
                                            );
                                    Map<String, String> filterableFields = serviceData.getFilterableFieldsForFeatureType(ft.getId(), true);
                                    PathItem finalPathItem = pathItem2;
                                    filterableFields.keySet()
                                            .forEach(field -> {
                                                finalPathItem.getGet()
                                                        .addParametersItem(
                                                                new Parameter()
                                                                        .name(field)
                                                                        .in("query")
                                                                        .description("Filter the collection by " + field)
                                                                        .required(false)
                                                                        .schema(new StringSchema())
                                                                        .style(Parameter.StyleEnum.FORM)
                                                                        .explode(false)
                                                        );
                                            });
                                }
                                openAPI.getPaths()
                                        .addPathItem("/collections/" + ft.getId() + "/tiles/{tilingSchemeId}/{level}/{row}/{col}", pathItem2);
                            }
                        });

            }
        }
        return openAPI;
    }
}
