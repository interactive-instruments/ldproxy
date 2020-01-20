/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
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
import java.util.*;

/**
 * extend API definition with tile resources
 *
 *
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
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    private boolean isMultiTilesEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiTilesEnabled)
                .isPresent();

    }

    private boolean isMultiCollectionEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .isPresent();

    }

    /**
     * extend the openAPI definition with necessary parameters and schemas. Add paths with parameters and responses to the OpenAPI definition.
     *
     * @param openAPI     the openAPI definition
     * @param datasetData the data from the Wfs3 Service
     * @return the extended OpenAPI definition
     */
    @Override
    public OpenAPI process(OpenAPI openAPI, OgcApiApiDataV2 datasetData) {

        boolean enableTilesInAPI = false;

        Optional<FeatureTypeConfigurationOgcApi> firstCollectionWithTiles = datasetData
                .getCollections()
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


        if (isEnabledForApi(datasetData)) {
            /**
             * Specify all new parameters:
             * tileMatrixSetId
             * tileMatrix
             * tileRow
             * tileCol
             * fVtTilesCollection/fVtOther/fVtTilesDataset
             * collections
             * properties
             * bbox
             * scaleDenominator
             * multiTileType
             * f-tile
             */

            Parameter tileMatrixSetId = new Parameter();
            tileMatrixSetId.setName("tileMatrixSetId");
            tileMatrixSetId.in("path");
            tileMatrixSetId.description("Local identifier of a specific tiling scheme. A list of all available tileMatrixSetIds can be found under the /tiles path or below. The default Tile Matrix Set is the Google Maps Tile Matrix Set.");
            tileMatrixSetId.setRequired(true);
            Schema tileMatrixSetIdSchema = new Schema();
            tileMatrixSetIdSchema.setType("string");

            List<String> tileMatrixSetEnum = new ArrayList<>(TileMatrixSetCache.getTileMatrixSetIds());

            tileMatrixSetId.setSchema(new StringSchema()._enum(tileMatrixSetEnum));
            tileMatrixSetId.example("WebMercatorQuad");

            Parameter tileMatrixParameter = new Parameter();
            tileMatrixParameter.setName("tileMatrix");
            tileMatrixParameter.in("path");
            tileMatrixParameter.description("Level of the tile. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps Tile Matrix Set. \\\n \\\n" +
                    "Example: Ireland is fully within the Tile with the following values: Level 5, Row 10 and Col 15");
            tileMatrixParameter.setRequired(true);
            tileMatrixParameter.setSchema(tileMatrixSetIdSchema);
            tileMatrixParameter.setExample("11");

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

            Parameter fVtTilesCollection = new Parameter();
            fVtTilesCollection.setName("f");
            fVtTilesCollection.in("query");
            fVtTilesCollection.description("\\\n" +
                    "        The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\\n" +
                    "        Pre-defined values are \"json\" and \"mvt\". The response to other values is determined by the server.");
            fVtTilesCollection.setRequired(false);
            fVtTilesCollection.setStyle(Parameter.StyleEnum.FORM);
            fVtTilesCollection.setExplode(false);
            List<String> fVtTilesCollectionEnum = new ArrayList<String>(); // TODO determine dynamically
            fVtTilesCollectionEnum.add("json");
            fVtTilesCollectionEnum.add("mvt");
            fVtTilesCollection.setSchema(new StringSchema()._enum(fVtTilesCollectionEnum));
            fVtTilesCollection.example("json");

            Parameter fVtOther = new Parameter();
            fVtOther.setName("f");
            fVtOther.in("query");
            fVtOther.description("\\\n" +
                    "        The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\\n" +
                    "        The only pre-defined value is \"json\". The response to other values is determined by the server.");
            fVtOther.setRequired(false);
            fVtOther.setStyle(Parameter.StyleEnum.FORM);
            fVtOther.setExplode(false);
            Schema fVtOtherSchema = new Schema();
            fVtOtherSchema.setType("string");
            List<String> fVtOtherEnum = new ArrayList<String>(); // TODO determine dynamically
            fVtOtherEnum.add("json");
            fVtOther.setSchema(new StringSchema()._enum(fVtOtherEnum));
            fVtOther.example("json");

            Parameter fVtTilesDataset = new Parameter();
            fVtTilesDataset.setName("f");
            fVtTilesDataset.in("query");
            fVtTilesDataset.description("\\\n" +
                    "        The format of the response. If no value is provided, the standard http rules apply, i.e., the accept header shall be used to determine the format.\\\n" +
                    "        The only pre-defined value is \"mvt\". The response to other values is determined by the server.");
            fVtTilesDataset.setRequired(false);
            fVtTilesDataset.setStyle(Parameter.StyleEnum.FORM);
            fVtTilesDataset.setExplode(false);
            List<String> fVtTilesDatasetEnum = new ArrayList<String>(); // TODO determine dynamically
            fVtTilesDatasetEnum.add("mvt");
            fVtTilesDataset.setSchema(new StringSchema()._enum(fVtTilesDatasetEnum));
            fVtTilesDataset.example("mvt");

            Parameter collections = new Parameter();
            collections.setName("collections");
            collections.in("query");
            collections.description("The collections that should be included in the tile. The parameter value is a list of collection identifiers. " +
                    "If not specified, all collections supporting the {tileMatrixSetId} parameter value will be considered");
            collections.setRequired(false);
            collections.setStyle(Parameter.StyleEnum.FORM);
            collections.setExplode(false);
            List<String> collectionsEnum = new ArrayList<String>();
            datasetData.getCollections()
                       .values()
                       .stream()
                       .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                       .filter(ft -> datasetData.isCollectionEnabled(ft.getId()))
                       .forEach(ft -> collectionsEnum.add(ft.getId()));
            Schema collectionsArrayItems = new StringSchema();
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
            Schema propertiesSchema = new ArraySchema().items(new StringSchema());
            properties.setSchema(propertiesSchema);

            /*Add the parameters to definition*/
            openAPI.getComponents()
                    .addParameters("fVtTilesCollection", fVtTilesCollection);
            openAPI.getComponents()
                    .addParameters("fVtOther", fVtOther);
            openAPI.getComponents()
                    .addParameters("fVtTilesDataset", fVtTilesDataset);
            openAPI.getComponents()
                    .addParameters("tileMatrixSetId", tileMatrixSetId);
            openAPI.getComponents()
                    .addParameters("tileMatrix", tileMatrixParameter);
            openAPI.getComponents()
                    .addParameters("tileRow", row);
            openAPI.getComponents()
                    .addParameters("tileCol", column);
            openAPI.getComponents()
                    .addParameters("collections", collections);
            openAPI.getComponents()
                    .addParameters("properties", properties);

            Parameter bbox = null;
            Parameter scaleDenominator = null;
            Parameter multiTileType = null;
            Parameter ftile = null;
            if (isMultiTilesEnabledForApi(datasetData)) {
                bbox = new Parameter();
                bbox.name("bbox");
                bbox.in("query");
                bbox.description("Only elements that have a geometry that intersects the bounding box are selected." +
                        "The bounding box is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis (elevation or depth). " +
                        "If not specified, the bounding box is set to the whole extent of the map");
                bbox.setRequired(false);
                bbox.setStyle(Parameter.StyleEnum.FORM);
                bbox.setExplode(false);
                bbox.setExample(new Double[]{333469.2232, 6565023.4598, 815328.2182, 7298818.9635});
                bbox.setSchema(new ArraySchema().items(new NumberSchema().format("double").minItems(4).maxItems(4)));

                scaleDenominator = new Parameter();
                scaleDenominator.name("scaleDenominator");
                scaleDenominator.in("query");
                scaleDenominator.description("A range of scale denominators (that can be used to generate a list of tileMatrix names). " +
                        "If not specified, all tile matrices (scales) are returned.");
                scaleDenominator.setRequired(false);
                scaleDenominator.setStyle(Parameter.StyleEnum.FORM);
                scaleDenominator.setExplode(false);
                scaleDenominator.example(new Double[]{2.5, 4.5});
                scaleDenominator.setSchema(new ArraySchema().items(new NumberSchema().format("double").minItems(2).maxItems(2)));

                multiTileType = new Parameter();
                multiTileType.name("multiTileType");
                multiTileType.in("query");
                multiTileType.description("The type of the response. If not specified, the parameter value defaults to `tiles`.");
                multiTileType.setRequired(false);
                multiTileType.setStyle(Parameter.StyleEnum.FORM);
                List<String> multiTileTypeEnum = new ArrayList<>(ImmutableList.of("url", "tiles", "full"));
                multiTileType.example("url");
                multiTileType.setSchema(new StringSchema()._enum(multiTileTypeEnum));

                ftile = new Parameter();
                ftile.name("f-tile");
                ftile.in("query");
                ftile.description("Specify the tile format in the multitiles request");
                ftile.setRequired(false);
                ftile.setStyle(Parameter.StyleEnum.FORM);
                List<String> ftileEnum = new ArrayList<>(ImmutableList.of("json", "mvt"));
                ftile.example("json");
                ftile.setSchema(new StringSchema()._enum(ftileEnum));

                openAPI.getComponents()
                        .addParameters("bbox", bbox);
                openAPI.getComponents()
                        .addParameters("scaleDenominator", scaleDenominator);
                openAPI.getComponents()
                        .addParameters("multiTileType", multiTileType);
                openAPI.getComponents()
                        .addParameters("f-tile", ftile);
            }

            Schema keywords = new ArraySchema().items(new StringSchema());

            Schema tileMatrixSetsArray = new Schema();
            tileMatrixSetsArray.setType("object");
            tileMatrixSetsArray.setRequired(ImmutableList.of("id","links"));
            tileMatrixSetsArray.addProperties("id", new StringSchema()
                                                                       .example("WebMercatorQuad"));
            tileMatrixSetsArray.addProperties("title", new StringSchema()
                    .example("Google Maps Compatible for the World"));
            tileMatrixSetsArray.addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));

            Schema tileMatrixSets = new Schema();
            tileMatrixSets.type("object");
            tileMatrixSets.setRequired(ImmutableList.of("tileMatrixSets"));
            tileMatrixSets.addProperties("tileMatrixSets", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileMatrixSetsArray")));

            Schema tileMatrixSetLimits = new Schema();
            tileMatrixSetLimits.type("object");
            tileMatrixSetLimits.setRequired(ImmutableList.of("tileMatrix", "minTileRow", "maxTileRow", "minTileCol", "maxTileCol"));
            tileMatrixSetLimits.addProperties("tileMatrix", new StringSchema());
            tileMatrixSetLimits.addProperties("minTileRow", new IntegerSchema().minimum(BigDecimal.valueOf(0)));
            tileMatrixSetLimits.addProperties("maxTileRow", new IntegerSchema().minimum(BigDecimal.valueOf(0)));
            tileMatrixSetLimits.addProperties("minTileCol", new IntegerSchema().minimum(BigDecimal.valueOf(0)));
            tileMatrixSetLimits.addProperties("maxTileCol", new IntegerSchema().minimum(BigDecimal.valueOf(0)));

            Schema tileMatrixSetLink = new Schema();
            tileMatrixSetLink.type("object");
            tileMatrixSetLink.setRequired(ImmutableList.of("tileMatrixSet"));
            tileMatrixSetLink.addProperties("tileMatrixSet", new StringSchema());
            tileMatrixSetLink.addProperties("tileMatrixSetURI", new StringSchema());
            tileMatrixSetLink.addProperties("tileMatrixSetLimits", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileMatrixSetLimits")));
            tileMatrixSetLink.addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/link")));

            Schema tileMatrixSetLinks = new Schema();
            tileMatrixSetLinks.type("object");
            tileMatrixSetLinks.setRequired(ImmutableList.of("tileMatrixSetLinks"));
            tileMatrixSetLinks.addProperties("tileMatrixSetLinks", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileMatrixSetLink")));

            Schema boundingBox = new Schema();
            boundingBox.setType("object");
            boundingBox.setRequired(ImmutableList.of("type", "lowerCorner", "upperCorner"));
            List<String> boundingBoxEnum = new ArrayList<String>();
            boundingBoxEnum.add("BoundingBoxType");
            boundingBox.addProperties("type", new StringSchema()._enum(boundingBoxEnum));
            boundingBox.addProperties("crs", new StringSchema());
            boundingBox.addProperties("lowerCorner", new ArraySchema().items(new NumberSchema().minItems(2).maxItems(2)));
            boundingBox.addProperties("upperCorner", new ArraySchema().items(new NumberSchema().minItems(2).maxItems(2)));

            Schema tileMatrix = new Schema();
            tileMatrix.setType("object");
            tileMatrix.setRequired(ImmutableList.of("type", "identifier", "scaleDenominator", "topLeftCorner", "tileWidth", "tileHeight", "matrixWidth", "matrixHeight"));
            List<String> matrixEnum = new ArrayList<String>();
            matrixEnum.add("TileMatrixType");
            tileMatrix.addProperties("type", new StringSchema()._enum(matrixEnum));
            tileMatrix.addProperties("identifier", new StringSchema());
            tileMatrix.addProperties("title", new StringSchema());
            tileMatrix.addProperties("abstract", new StringSchema());
            tileMatrix.addProperties("keywords", new Schema().$ref("#/components/schemas/keywords"));
            tileMatrix.addProperties("matrixHeight", new NumberSchema().minimum(BigDecimal.valueOf(1)).multipleOf(BigDecimal.valueOf(1)));
            tileMatrix.addProperties("matrixWidth", new NumberSchema().minimum(BigDecimal.valueOf(1)).multipleOf(BigDecimal.valueOf(1)));
            tileMatrix.addProperties("tileHeight", new NumberSchema().minimum(BigDecimal.valueOf(1)).multipleOf(BigDecimal.valueOf(1)));
            tileMatrix.addProperties("tileWidth", new NumberSchema().minimum(BigDecimal.valueOf(1)).multipleOf(BigDecimal.valueOf(1)));
            tileMatrix.addProperties("scaleDenominator", new NumberSchema());
            tileMatrix.addProperties("topLeftCorner", new ArraySchema().items(new NumberSchema().minItems(2).maxItems(2)));
            // TODO add variableMatrixWidth, currently not supported

            Schema tileMatrixSet = new Schema();
            tileMatrixSet.setType("object");
            tileMatrixSet.setRequired(ImmutableList.of("type", "identifier", "supportedCRS", "tileMatrix"));
            List<String> tileMatrixSetTypeEnum = new ArrayList<>();
            tileMatrixSetTypeEnum.add("TileMatrixSetType");
            tileMatrixSet.addProperties("type", new StringSchema()._enum(tileMatrixSetTypeEnum));
            tileMatrixSet.addProperties("identifier", new StringSchema());
            tileMatrixSet.addProperties("title", new StringSchema());
            tileMatrixSet.addProperties("abstract", new StringSchema());
            tileMatrixSet.addProperties("keywords", new Schema().$ref("#/components/schemas/keywords"));
            tileMatrixSet.addProperties("supportedCRS", new StringSchema());
            tileMatrixSet.addProperties("wellKnownScaleSet", new StringSchema());
            tileMatrixSet.addProperties("tileMatrix", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileMatrix")));
            tileMatrixSet.addProperties("boundingBox", new ArraySchema().items(new Schema().$ref("#/components/schemas/boundingBox")));

            Schema tileSetEntry = new Schema();
            tileSetEntry.setType("object");
            tileSetEntry.addProperties("tileURL", new StringSchema());
            tileSetEntry.addProperties("tileMatrix", new IntegerSchema());
            tileSetEntry.addProperties("tileRow", new IntegerSchema());
            tileSetEntry.addProperties("tileCol", new IntegerSchema());
            tileSetEntry.addProperties("width", new IntegerSchema());
            tileSetEntry.addProperties("height", new IntegerSchema());
            tileSetEntry.addProperties("top", new IntegerSchema());
            tileSetEntry.addProperties("left", new IntegerSchema());

            Schema tileSet = new Schema();
            tileSet.setType("object");
            tileSet.setRequired(ImmutableList.of("tileSet"));
            tileSet.addProperties("tileSet", new ArraySchema().items(new Schema().$ref("#/components/schemas/tileSetEntry")));


            Schema mvt = new Schema();
            mvt.type("string");
            mvt.format("binary");

            /*Add the schemas to definition*/
            openAPI.getComponents()
                    .addSchemas("keywords", keywords);
            openAPI.getComponents()
                   .addSchemas("tileMatrixSetsArray", tileMatrixSetsArray);
            openAPI.getComponents()
                   .addSchemas("tileMatrixSets", tileMatrixSets);
            openAPI.getComponents()
                   .addSchemas("boundingBox", boundingBox);
            openAPI.getComponents()
                   .addSchemas("tileMatrix", tileMatrix);
            openAPI.getComponents()
                   .addSchemas("tileMatrixSet", tileMatrixSet);
            openAPI.getComponents()
                    .addSchemas("tileMatrixSetLimits", tileMatrixSetLimits);
            openAPI.getComponents()
                    .addSchemas("tileMatrixSetLink", tileMatrixSetLink);
            openAPI.getComponents()
                    .addSchemas("tileMatrixSetLinks", tileMatrixSetLinks);
            openAPI.getComponents()
                    .addSchemas("mvt", mvt);
            openAPI.getComponents()
                    .addSchemas("tileSetEntry", tileSetEntry);
            openAPI.getComponents()
                    .addSchemas("tileSet", tileSet);

            /*create a new tag and add it to definition*/
            openAPI.getTags()
                   .add(new Tag().name("Tiles")
                                 .description("Access to data (features), partitioned into a hierarchy of tiles."));


            if (datasetData != null) {

                openAPI.getPaths()
                       .addPathItem("/tileMatrixSets", new PathItem().description("something"));  //create a new path
                PathItem pathItem = openAPI.getPaths()
                                           .get("/tileMatrixSets");
                ApiResponse success = new ApiResponse().description("A list of all available tile tileMatrix sets.")
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
                                    .summary("retrieve all available tile tileMatrix sets")
                                    .operationId("getTileMatrixSets")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/fVtOther"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                       .addPathItem("/tileMatrixSets", pathItem); //save to Path

                openAPI.getPaths()
                        .addPathItem("/tileMatrixSets/{tileMatrixSetId}", new PathItem().description("a tiling scheme"));
                pathItem = openAPI.getPaths()
                        .get("/tileMatrixSets/{tileMatrixSetId}");
                success = new ApiResponse().description("A tile tileMatrix set.")
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
                                    .summary("retrieve a tile tileMatrix set by id")
                                    .operationId("getTileMatrixSet")
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                    .addParametersItem(new Parameter().$ref("#/components/parameters/fVtOther"))
                                    //.requestBody(requestBody)
                                    .responses(new ApiResponses()
                                            .addApiResponse("200", success)
                                            .addApiResponse("default", exception))
                            );
                }
                openAPI.getPaths()
                        .addPathItem("/tileMatrixSets/{tileMatrixSetId}", pathItem);

                if (isMultiCollectionEnabledForApi(datasetData)) {
                    openAPI.getPaths()
                            .addPathItem("/tiles", new PathItem().description("something"));  //create a new path
                    pathItem = openAPI.getPaths()
                            .get("/tiles");
                    success = new ApiResponse().description("A list of tile tileMatrix sets.")
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
                                        .summary("retrieve all available tile tileMatrix sets")
                                        .operationId("getTileMatrixSetsAlt")
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/fVtOther"))
                                        //.requestBody(requestBody)
                                        .responses(new ApiResponses()
                                                .addApiResponse("200", success)
                                                .addApiResponse("default", exception))
                                );
                    }
                    openAPI.getPaths()
                            .addPathItem("/tiles", pathItem); //save to Path


                    if (isMultiTilesEnabledForApi(datasetData)) {
                        openAPI.getPaths()
                                .addPathItem("/tiles/{tileMatrixSetId}", new PathItem().description("something"));
                        pathItem = openAPI.getPaths()
                                .get("/tiles/{tileMatrixSetId}");
                        success = new ApiResponse().description("Multiple tiles from multiple collections.")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tileSet")))
                                );
                        exception = new ApiResponse().description("An error occurred.")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                );
                        if (Objects.nonNull(pathItem)) {
                            pathItem
                                    .get(new Operation()
                                            .addTagsItem("Tiles")
                                            .summary("retrieve more than one tile from more than on collection in a single request")
                                            .operationId("getCollectionMultitiles")
                                            .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                            .addParametersItem(new Parameter().$ref("#/components/parameters/bbox"))
                                            .addParametersItem(new Parameter().$ref("#/components/parameters/scaleDenominator"))
                                            .addParametersItem(new Parameter().$ref("#/components/parameters/multiTileType"))
                                            .addParametersItem(new Parameter().$ref("#/components/parameters/collections"))
                                            //.requestBody(requestBody)
                                            .responses(new ApiResponses()
                                                    .addApiResponse("200", success)
                                                    .addApiResponse("default", exception))
                                    );
                        }

                        openAPI.getPaths()
                                .addPathItem("/tiles/{tileMatrixSetId}", pathItem);
                    }

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
                                        .description("The tile in the requested tile tileMatrix set, on the requested zoom level in the tile tileMatrix set, with the requested grid coordinates (tileRow, tileCol) is returned. " +
                                                "Each collection of the dataset is returned as a separate layer. The collections and the feature properties to include in the tile representation can be limited using query" +
                                                " parameters.")
                                        .operationId("getTilesDataset")
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrix"))
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/tileRow"))
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/tileCol"))
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/collections"))
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/properties"))
                                        .addParametersItem(new Parameter().$ref("#/components/parameters/fVtTilesDataset"))
                                        //.requestBody(requestBody)
                                        .responses(new ApiResponses()
                                                .addApiResponse("200", success)
                                                .addApiResponse("default", exception))
                                );
                    }

                    openAPI.getPaths()
                            .addPathItem("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}", pathItem);
                }

                //do for every feature type
                datasetData.getCollections()
                           .values()
                           .stream()
                           .sorted(Comparator.comparing(FeatureTypeConfigurationOgcApi::getId))
                           .filter(ft -> datasetData.isCollectionEnabled(ft.getId()))
                           .forEach(ft -> {
                               boolean enableTilesCollectionInApi = isExtensionEnabled(datasetData, ft, TilesConfiguration.class);

                               if (enableTilesCollectionInApi) {
                                   openAPI.getPaths()
                                          .addPathItem("/collections/" + ft.getId() + "/tiles", new PathItem().description("something"));  //create a new path
                                   PathItem pathItem2 = openAPI.getPaths()
                                                               .get("/collections/" + ft.getId() + "/tiles");
                                   ApiResponse success2 = new ApiResponse().description("A list of tile tileMatrix sets from the collection" + ft.getLabel() + ".")
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
                                                       .summary("retrieve all available tile tileMatrix sets from the collection " + ft.getLabel())
                                                       .operationId("getTileMatrixSetsCollection" + ft.getId())
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/fVtOther"))
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
                                                       .description("The tile in the requested tile tileMatrix set, on the requested zoom level in the tile tileMatrix set, with the requested grid coordinates (tileRow, tileCol) is returned. " +
                                                               "The tile has a single layer with all selected features in the bounding box of the tile. The feature properties to " +
                                                               "include in the tile representation can be limited using a query parameter.")
                                                       .operationId("getTilesCollection" + ft.getId())
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrix"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileRow"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/tileCol"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/properties"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/fVtTilesCollection"))
                                                       .addParametersItem(new Parameter().$ref("#/components/parameters/datetime"))
                                                       //.requestBody(requestBody)
                                                       .responses(new ApiResponses()
                                                               .addApiResponse("200", success2)
                                                               .addApiResponse("default", exception2))
                                               );
                                       Map<String, String> filterableFields = ft.getExtension(OgcApiFeaturesCoreConfiguration.class).map(OgcApiFeaturesCoreConfiguration::getOtherFilterParameters).orElse(ImmutableMap.of());
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

                                   if (isMultiTilesEnabledForApi(datasetData)) {
                                       openAPI.getPaths()
                                               .addPathItem("/collections/" + ft.getId() + "/tiles/{tileMatrixSetId}", new PathItem().description("something"));  //create a new path
                                       pathItem2 = openAPI.getPaths()
                                               .get("/collections/" + ft.getId() + "/tiles/{tileMatrixSetId}");
                                       success2 = new ApiResponse().description("Multiple tiles from the collection " + ft.getLabel())
                                               .content(new Content()
                                                       .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/tileSet")))
                                               );
                                       exception2 = new ApiResponse().description("An error occured.")
                                               .content(new Content()
                                                       .addMediaType("application/json", new MediaType().schema(new Schema().$ref("#/components/schemas/exception")))
                                               );
                                       if (Objects.nonNull(pathItem2)) {
                                           pathItem2
                                                   .get(new Operation()
                                                           .addTagsItem("Tiles")
                                                           .summary("retrieve multiple tiles from the collection " + ft.getLabel())
                                                           .operationId("getMultitiles" + ft.getId())
                                                           .addParametersItem(new Parameter().$ref("#/components/parameters/tileMatrixSetId"))
                                                           .addParametersItem(new Parameter().$ref("#/components/parameters/bbox"))
                                                           .addParametersItem(new Parameter().$ref("#/components/parameters/scaleDenominator"))
                                                           .addParametersItem(new Parameter().$ref("#/components/parameters/multiTileType"))
                                                           .addParametersItem(new Parameter().$ref("#/components/parameters/f-tile"))
                                                           .responses(new ApiResponses()
                                                                   .addApiResponse("200", success2)
                                                                   .addApiResponse("default", exception2))
                                                   );
                                       }
                                       //                                   openAPI.getPaths()
                                       //                                           .addPathItem("/collections/" + ft.getId() + "tiles/{tileMatrixSetId}", pathItem2);
                                   }
                               }
                           });

            }
        }
        return openAPI;
    }
}
