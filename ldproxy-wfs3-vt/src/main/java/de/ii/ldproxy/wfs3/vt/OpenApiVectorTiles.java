/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.oas30.OpenApiExtension;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * extend API definition with tile resources
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class OpenApiVectorTiles implements OpenApiExtension {
    @Override
    public int getSortPriority() {
        return 20;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * extend the openAPI definition with necessary parameters and schemas. Add paths with parameters and responses to the OpenAPI definition.
     *
     * @param openAPI     the openAPI definition
     * @param datasetData the data from the Wfs3 Service
     * @return the extended OpenAPI definition
     */
    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiDatasetData datasetData) {

        boolean enableTilesInAPI = false;

        Optional<FeatureTypeConfigurationOgcApi> firstCollectionWithTiles = datasetData
                .getFeatureTypes()
                .values()
                .stream()
                .filter(ft -> {
                    try {
                        if (isExtensionEnabled(datasetData, ft, TilesConfiguration.class)) {
                            TilesConfiguration tilesConfiguration = getExtensionConfiguration(datasetData, ft, TilesConfiguration.class).get();

                            if (tilesConfiguration.getEnabled())
                                return true;
                        }
                        return false;
                    } catch (NullPointerException ignored) {
                        return false;
                    }
                })
                .findFirst();

        if (firstCollectionWithTiles.isPresent())
            enableTilesInAPI = true;

        if (!firstCollectionWithTiles.isPresent())
            enableTilesInAPI = false;


        if (enableTilesInAPI) {
            /*specify all new parameters. They are:
             * tileMatrixSetId
             * tileMatrix
             * tileRow
             * tileCol
             * f2/f3/f4
             * collections
             * properties*/

            Parameter tileMatrixSetId = new Parameter();
            tileMatrixSetId.setName("tileMatrixSetId");
            tileMatrixSetId.in("path");
            tileMatrixSetId.description("Local identifier of a specific tiling scheme. A list of all available tileMatrixSetIds can be found under the /tiles path or below. The default Tile Matrix Set is the Google Maps Tile Matrix Set.");
            tileMatrixSetId.setRequired(true);
            Schema tileMatrixSetIdSchema = new Schema();
            tileMatrixSetIdSchema.setType("string");

            List<String> tilingSchemeEnum = new ArrayList<>();
            tilingSchemeEnum.add("WebMercatorQuad");

            tileMatrixSetId.setSchema(new StringSchema()._enum(tilingSchemeEnum));
            tileMatrixSetId.example("WebMercatorQuad");


            Parameter tileMatrix = new Parameter();
            tileMatrix.setName("tileMatrix");
            tileMatrix.in("path");
            tileMatrix.description("Level of the tile. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps Tile Matrix Set. \\\n \\\n" +
                    "Example: Ireland is fully within the Tile with the following values: Level 5, Row 10 and Col 15");
            tileMatrix.setRequired(true);
            tileMatrix.setSchema(tileMatrixSetIdSchema);
            tileMatrix.setExample("11");

            Parameter row = new Parameter();
            row.setName("tileRow");
            row.in("path");
            row.description("Row index of the tile on the selected zoom level. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps Tile Matrix Set.\\\n \\\n" +
                    "Example: Ireland is fully within the Tile with the following values: Level 5, Row 10 and Col 15");
            row.setRequired(true);
            row.setSchema(tileMatrixSetIdSchema);
            row.setExample("827");

            Parameter column = new Parameter();
            column.setName("tileCol");
            column.in("path");
            column.description("Column index of the tile on the selected zoom level. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps Tile Matrix Set.\\\n \\\n" +
                    "Example: Ireland is fully within the Tile with the following values: Level 5, Row 10 and Col 15");
            column.setRequired(true);
            column.setSchema(tileMatrixSetIdSchema);
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
            datasetData.getFeatureTypes()
                       .values()
                       .stream()
                       .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                       .filter(ft -> datasetData.isFeatureTypeEnabled(ft.getId()))
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
            openAPI.getComponents()
                   .addParameters("f2", f2);
            openAPI.getComponents()
                   .addParameters("f3", f3);
            openAPI.getComponents()
                   .addParameters("f4", f4);
            openAPI.getComponents()
                   .addParameters("tileMatrixSetId", tileMatrixSetId);
            openAPI.getComponents()
                   .addParameters("tileMatrix", tileMatrix);
            openAPI.getComponents()
                   .addParameters("tileRow", row);
            openAPI.getComponents()
                   .addParameters("tileCol", column);
            openAPI.getComponents()
                   .addParameters("collections", collections);
            openAPI.getComponents()
                   .addParameters("properties", properties);

            List<String> modelRequirements = new LinkedList<String>();
            modelRequirements.add("type");
            List<String> tileMatrixSetRequirements = new LinkedList<String>();
            tileMatrixSetRequirements.add("type");
            tileMatrixSetRequirements.add("identifier");

            Schema tileMatrixSetsArray = new Schema();
            tileMatrixSetsArray.setType("object");
            tileMatrixSetsArray.setRequired(modelRequirements);
            tileMatrixSetsArray.addProperties("id", new Schema().type("string")
                                                                       .example("WebMercatorQuad"));
            tileMatrixSetsArray.addProperties("title", new Schema().type("string")
                    .example("Google Maps Compatible for the World"));
            tileMatrixSetsArray.addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));

            Schema tileMatrixSets = new Schema();
            tileMatrixSets.type("object");
            tileMatrixSets.setRequired(modelRequirements);
            tileMatrixSets.addProperties("tileMatrixSetLinks", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileMatrixSetsArray")));

            Schema tilesSchema = new Schema();
            tilesSchema.type("object");
            tilesSchema.addProperties("tileMatrixSet", new Schema().type("string")
                    .example("WebMercatorQuad"));
            tilesSchema.addProperties("tileMatrixSetURI", new Schema().type("string")
                    .example("http://www.opengis.net/def/tilematrixset/OGC/1.0/WebMercatorQuad"));
            tilesSchema.addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));

            Schema tileMatrixSetLinks = new Schema();
            tileMatrixSetLinks.type("object");
            tileMatrixSetLinks.addProperties("tileMatrixSetLinks", new ArraySchema().items(new Schema().$ref("#/components/schemas/tilesSchema")));

            Schema boundingBox = new Schema();
            boundingBox.setType("object");
            boundingBox.setRequired(modelRequirements);
            List<String> boundingBoxEnum = new ArrayList<String>();
            boundingBoxEnum.add("BoundingBox");
            boundingBox.addProperties("type", new StringSchema()._enum(boundingBoxEnum));
            boundingBox.addProperties("crs", new Schema().type("string")
                                                         .example("http://www.opengis.net/def/crs/EPSG/0/3857"));
            List<Double> lowerCorner = new ArrayList<>();
            lowerCorner.add(-20037508.3427892);
            lowerCorner.add(-20037508.342789);
            boundingBox.addProperties("lowerCorner", new ArraySchema().items(new NumberSchema().minItems(2).maxItems(2))
                                                                 .example(lowerCorner));
            List<Double> upperCorner = new ArrayList<>();
            upperCorner.add(20037508.3427892);
            upperCorner.add(20037508.3427892);
            boundingBox.addProperties("upperCorner", new ArraySchema().items(new NumberSchema().minItems(2).maxItems(2))
                                                                 .example(upperCorner));


            Schema matrix = new Schema();
            matrix.setType("object");
            matrix.setRequired(modelRequirements);
            List<String> matrixEnum = new ArrayList<String>();
            matrixEnum.add("TileMatrix");
            matrix.addProperties("type", new StringSchema()._enum(matrixEnum));
            matrix.addProperties("identifier", new Schema().type("string")
                                                           .example('0'));
            matrix.addProperties("MatrixHeight", new Schema().minimum(BigDecimal.valueOf(0))
                                                             .type("integer")
                                                             .example(1));
            matrix.addProperties("MatrixWidth", new Schema().minimum(BigDecimal.valueOf(0))
                                                            .type("integer")
                                                            .example(1));
            matrix.addProperties("TileHeight", new Schema().minimum(BigDecimal.valueOf(0))
                                                           .type("integer")
                                                           .example(256));
            matrix.addProperties("TileWidth", new Schema().minimum(BigDecimal.valueOf(0))
                                                          .type("integer")
                                                          .example(256));
            matrix.addProperties("scaleDenominator", new Schema().type("number")
                                                                 .example(559082264.028717));
            List<Double> topLeftCorner = new ArrayList<>();
            topLeftCorner.add(-20037508.3427892);
            topLeftCorner.add(20037508.3427892);
            matrix.addProperties("topLeftCorner", new ArraySchema().items(new NumberSchema().minItems(2).maxItems(2))
                                                              .example(topLeftCorner));

            Schema tileMatrixSet = new Schema();
            tileMatrixSet.setType("object");
            tileMatrixSet.setRequired(tileMatrixSetRequirements);
            List<String> tileMatrixSetEnum = new ArrayList<>();
            tileMatrixSetEnum.add("TileMatrixSet");
            List<String> tileMatrixSetSupportedCrsEnum = new ArrayList<>();
            tileMatrixSetSupportedCrsEnum.add("http://www.opengis.net/def/crs/EPSG/0/3857");
            List<String> tileMatrixSetWellKnownEnum = new ArrayList<>();
            tileMatrixSetWellKnownEnum.add("http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible");
            tileMatrixSet.addProperties("type", new StringSchema()._enum(tileMatrixSetEnum));
            tileMatrixSet.addProperties("identifier", new Schema().type("string")
                                                                 .example("WebMercatorQuad"));
            tileMatrixSet.addProperties("title", new Schema().type("string")
                                                            .example("Google Maps Compatible for the World"));
            tileMatrixSet.addProperties("supportedCrs", new StringSchema()._enum(tileMatrixSetSupportedCrsEnum)
                                                                         .example("http://www.opengis.net/def/crs/EPSG/0/3857"));
            tileMatrixSet.addProperties("wellKnownScaleSet", new StringSchema()._enum(tileMatrixSetWellKnownEnum)
                                                                              .example("http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible"));
            tileMatrixSet.addProperties("TileMatrix", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileMatrix")));
            tileMatrixSet.addProperties("boundingBox", new ArraySchema().items(new Schema().$ref("#/components/schemas/boundingBox")));

            Schema mvt = new Schema();
            mvt.type("string");
            mvt.format("binary");

            /*Add the schemas to definition*/
            openAPI.getComponents()
                   .addSchemas("tileMatrixSetsArray", tileMatrixSetsArray);
            openAPI.getComponents()
                   .addSchemas("tileMatrixSets", tileMatrixSets);
            openAPI.getComponents()
                   .addSchemas("boundingBox", boundingBox);
            openAPI.getComponents()
                   .addSchemas("tileMatrix", matrix);
            openAPI.getComponents()
                   .addSchemas("tileMatrixSet", tileMatrixSet);
            openAPI.getComponents()
                   .addSchemas("mvt", mvt);
            openAPI.getComponents()
                    .addSchemas("tilesSchema", tilesSchema);
            openAPI.getComponents()
                    .addSchemas("tileMatrixSetLinks", tileMatrixSetLinks);

            /*create a new tag and add it to definition*/
            openAPI.getTags()
                   .add(new Tag().name("Tiles")
                                 .description("Access to data (features), partitioned into a hierarchy of tiles."));


            if (datasetData != null) {

                openAPI.getPaths()
                       .addPathItem("/tileMatrixSets", new PathItem().description("something"));  //create a new path
                PathItem pathItem = openAPI.getPaths()
                                           .get("/tileMatrixSets");
                ApiResponse success = new ApiResponse().description("A list of all available tile matrix sets.")
                                                       .content(new Content()
                                                               .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tileMatrixSets")))
                                                       );
                ApiResponse exception = new ApiResponse().description("An error occurred.")
                                                         .content(new Content()
                                                                 .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                                         );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve all available tile matrix sets")
                                    .operationId("getTileMatrixSets")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                       .addPathItem("/tileMatrixSets", pathItem); //save to Path

                openAPI.getPaths()
                       .addPathItem("/tileMatrixSets/{tileMatrixSetId}", new PathItem().description("something"));
                pathItem = openAPI.getPaths()
                                  .get("/tileMatrixSets/{tileMatrixSetId}");
                success = new ApiResponse().description("A tile matrix set.")
                                           .content(new Content()
                                                   .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tileMatrixSet")))
                                           );
                exception = new ApiResponse().description("An error occurred.")
                                             .content(new Content()
                                                     .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                             );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve a tile matrix set by id")
                                    .operationId("getTileMatrixSet")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                       .addPathItem("/tileMatrixSets/{tileMatrixSetId}", pathItem);


                openAPI.getPaths()
                       .addPathItem("/tiles", new PathItem().description("something"));  //create a new path
                pathItem = openAPI.getPaths()
                                  .get("/tiles");
                success = new ApiResponse().description("A list of tile matrix sets.")
                                           .content(new Content()
                                                   .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tileMatrixSetLinks")))
                                           );
                exception = new ApiResponse().description("An error occurred.")
                                             .content(new Content()
                                                     .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                             );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve all available tile matrix sets")
                                    .operationId("getTileMatrixSetsAlt")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                       .addPathItem("/tiles", pathItem); //save to Path


                openAPI.getPaths()
                       .addPathItem("/tiles/{tileMatrixSetId}", new PathItem().description("something"));
                pathItem = openAPI.getPaths()
                                  .get("/tiles/{tileMatrixSetId}");
                success = new ApiResponse().description("A tile matrix set used to partition the dataset into tiles.")
                                           .content(new Content()
                                                   .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tileMatrixSet")))
                                           );
                exception = new ApiResponse().description("An error occurred.")
                                             .content(new Content()
                                                     .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                             );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve a tile matrix set used to partition the dataset into tiles")
                                    .operationId("getTileMatrixSetPartition")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }

                openAPI.getPaths()
                       .addPathItem("/tiles/{tileMatrixSetId}", pathItem);


                openAPI.getPaths()
                       .addPathItem("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}", new PathItem().description("something"));
                pathItem = openAPI.getPaths()
                                  .get("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
                success = new ApiResponse().description("A tile of the dataset.")
                                           .content(new Content()
                                                   .addMediaType("application/vnd.mapbox-vector-tile", new MediaType().schema(new Schema().$ref("#/components/schemas/mvt")))
                                           );
                exception = new ApiResponse().description("An error occurred.")
                                             .content(new Content()
                                                     .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                             );
                if (Objects.nonNull(pathItem)) {
                    pathItem
                            .get(new Operation()
                                    .addTagsItem("Tiles")
                                    .summary("retrieve a tile of the dataset")
                                    .description("The tile in the requested tile matrix set, on the requested zoom level in the tile matrix set, with the requested grid coordinates (tileRow, tileCol) is returned. " +
                                            "Each collection of the dataset is returned as a separate layer. The collections and the feature properties to include in the tile representation can be limited using query" +
                                            " parameters.")
                                    .operationId("getTilesDataset")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrix"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tileRow"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tileCol"))
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
                       .addPathItem("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}", pathItem);

                //do for every feature type
                datasetData.getFeatureTypes()
                           .values()
                           .stream()
                           .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                           .filter(ft -> datasetData.isFeatureTypeEnabled(ft.getId()))
                           .forEach(ft -> {
                               boolean enableTilesCollectionInApi = isExtensionEnabled(datasetData, ft, TilesConfiguration.class);

                               if (enableTilesCollectionInApi) {
                                   openAPI.getPaths()
                                          .addPathItem("/collections/" + ft.getId() + "/tiles", new PathItem().description("something"));  //create a new path
                                   PathItem pathItem2 = openAPI.getPaths()
                                                               .get("/collections/" + ft.getId() + "/tiles");
                                   ApiResponse success2 = new ApiResponse().description("A list of tile matrix sets from the collection" + ft.getLabel() + ".")
                                                                           .content(new Content()
                                                                                   .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tileMatrixSetLinks")))
                                                                           );
                                   ApiResponse exception2 = new ApiResponse().description("An error occurred.")
                                                                             .content(new Content()
                                                                                     .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                                                             );
                                   if (Objects.nonNull(pathItem2)) {
                                       pathItem2
                                               .get(new Operation()
                                                       .addTagsItem("Tiles")
                                                       .summary("retrieve all available tile matrix sets from the collection " + ft.getLabel())
                                                       .operationId("getTileMatrixSetsCollection" + ft.getId())
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/f3"))
                                                       //.requestBody(requestBody)
                                                       .responses(new ApiResponses()
                                                               .addApiResponse("200", success2)
                                                               .addApiResponse("default", exception2))
                                               );
                                   }
                                   openAPI.getPaths()
                                          .addPathItem("/collections/" + ft.getId() + "/tiles", pathItem2); //save to Path


                                   openAPI.getPaths()
                                          .addPathItem("/collections/" + ft.getId() + "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}", new PathItem().description("something"));
                                   pathItem2 = openAPI.getPaths()
                                                      .get("/collections/" + ft.getId() + "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
                                   success2 = new ApiResponse().description("A tile of the collection " + ft.getLabel() + ".")
                                                               .content(new Content()
                                                                       .addMediaType("application/geo+json", new MediaType().schema(new Schema().$ref("#/components/schemas/featureCollectionGeoJSON")))
                                                                       .addMediaType("application/vnd.mapbox-vector-tile", new MediaType().schema(new Schema().$ref("#/components/schemas/mvt")))
                                                               );
                                   exception2 = new ApiResponse().description("An error occurred.")
                                                                 .content(new Content()
                                                                         .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                                                 );
                                   if (Objects.nonNull(pathItem2)) {
                                       pathItem2
                                               .get(new Operation()
                                                       .addTagsItem("Tiles")
                                                       .summary("retrieve a tile of the collection " + ft.getLabel())
                                                       .description("The tile in the requested tile matrix set, on the requested zoom level in the tile matrix set, with the requested grid coordinates (tileRow, tileCol) is returned. " +
                                                               "The tile has a single layer with all selected features in the bounding box of the tile. The feature properties to " +
                                                               "include in the tile representation can be limited using a query parameter.")
                                                       .operationId("getTilesCollection" + ft.getId())
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrix"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileRow"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileCol"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/properties"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/f2"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/time"))
                                                       //.requestBody(requestBody)
                                                       .responses(new ApiResponses()
                                                               .addApiResponse("200", success2)
                                                               .addApiResponse("default", exception2))
                                               );
                                       Map<String, String> filterableFields = datasetData.getFilterableFieldsForFeatureType(ft.getId(), true);
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
                                          .addPathItem("/collections/" + ft.getId() + "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}", pathItem2);
                               }
                           });

            }
        }
        return openAPI;
    }
}
