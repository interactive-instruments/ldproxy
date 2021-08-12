# Vector Tiles (TILES)

Adds support for vector tiles for the whole dataset and/or single collections.


## Scope

### Conformance classes

This module implements requirements of the conformance classes from the draft specification [OGC API - Tiles - Part 1: Core](https://github.com/opengeospatial/OGC-API-Tiles) and from the standard [OGC Two Dimensional Tile Matrix Set](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html). The implementation is subject to change in the course of the development and approval process of the draft.

### Resources

|Resource |Path |HTTP Method |Media Types
| --- | --- | --- | ---
|Tilesets |`/{apiId}/tiles`<br>`/{apiId}/collections/{collectionId}/tiles`|GET |HTML, JSON
|Tileset |`/{apiId}/tiles/{tileMatrixSetId}`<br>`/{apiId}/collections/{collectionId}/tiles/{tileMatrixSetId}` |GET |TileJSON
|Vector Tile |`/{apiId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}`<br>`/{apiId}/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}` |GET |MVT
|Tile Matrix Sets |`/{apiId}/tileMatrixSets` |GET |HTML, JSON
|Tile Matrix Set |`/{apiId}/tileMatrixSets/{tileMatrixSetId}` |GET |HTML, JSON

### Tile matrix sets

Supported tile matrix sets include [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62), [WorldCRS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#63) and [WorldMercatorWGS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#64).


## Prerequisites

|Module |Required |Description
| --- | --- | ---
[Feature Collections](collections.md)| Yes | Provides the resource *Feature Collection*, which is extended by this module.


## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`featureProvider` |string |API id |Id of the feature provider to use. Normally the feature provider and API ids are the same.
|`singleCollectionEnabled` |boolean |`true` |Enable vector tiles for each *Feature Collection*. Every tile contains a layer with the feature from the collection.
|`multiCollectionEnabled` |boolean |`true` |Enable vector tiles for the whole dataset. Every tile contains one layer per collection with the features of that collection.
|`ignoreInvalidGeometries` |boolean |`false` |Ignore features with invalid geometries. Before ignoring a feature, an attempt is made to transform the geometry to a valid geometry. The topology of geometries might be invalid in the data source or in some cases the quantization of coordinates to integers might render it invalid.
|`tileEncodings` |array |`[ `MVT` ]` |List of enabled tile encodings. Currently only *Mapbox Vector Tiles* (`MVT`) is supported.
|`zoomLevels` |object |`{ "WebMercatorQuad" : { "min": 0, "max": 23 } }` |Available zoom levels and default zoom level for enabled tile encodings.
|`zoomLevelsCache` |object |`{}` |Zoom levels for which tiles are cached.
|`center` |array |`null` |Longitude and latitude that a map with the tiles should be centered on by default.
|`filters` |object |`{}` |Filters to select a subset of feature for certain zoom levels using a CQL filter expression, see example below.
|`rules` |object |`{}` |Rules to postprocess the selected features for a certain zoom level. Supported operations are: selecting a subset of feature properties (`properties`), spatial merging of features that intersect (`merge`), with the option to restrict the operations to features with matching attributes (`groupBy`). See the example below. For `merge`, the resulting object will only obtain properties that are identical for all merged features.
|`seeding` |object |`{}` |Zoom levels per enabled tile encoding for which the tile cache should be seeded on startup.
|`cache` |string |`FILES` |`FILES` saves each tile as a file in the file system. `MBTILES` saves each tile in an MBTiles file (one MBTiles file per tile set).
|`limit` |integer |100000 |Maximum number of features contained in a single tile per query.
|`minimumSizeInPixel`| number |0.5 |Features with line geometries shorter that the given value are excluded from tiles. Features with surface geometries smaller than the square of the given value are excluded from the tiles. The value `0.5` corresponds to half a "pixel" in the used coordinate reference system.
|`maxRelativeAreaChangeInPolygonRepair` | number |0.1 |Maximum allowed relative change of surface sizes when attempting to fix an invalid surface geometry. The fixed geometry is only used when the condition is met. The value `0.1` means 10%.
|`maxAbsoluteAreaChangeInPolygonRepair` | number |1.0 |Maximum allowed absolute change of surface sizes when attempting to fix an invalid surface geometry. The fixed geometry is only used when the condition is met. The value `1.0` corresponds to one "pixel" in the used coordinate reference system.

### Example

```yaml
- buildingBlock: TILES
  enabled: true
  singleCollectionEnabled: true
  multiCollectionEnabled: true
  center:
  - 7.5
  - 51.5
  minimumSizeInPixel: 0.75
  zoomLevels:
    WebMercatorQuad:
      min: 12
      max: 20
      default: 16
  rules:
    WebMercatorQuad:
    - min: 12
      max: 13
      merge: true
      groupBy:
      - anzahl_geschosse
      - funktion
      properties:
      - anzahl_geschosse
      - funktion
      - name
    - min: 14
      max: 20
      properties:
      - anzahl_geschosse
      - funktion
      - name
      - anschrift
```

### Cache

The tile cache resides under the relative path `tiles/{apiId}/{collectionId}/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}.pbf` in the data directory, where `__all__` is used as value for `collectionId` for tiles based on the whole dataset.

If the data or configuration for an API changes, the cache directory for this API has to be deleted to refresh the tiles.
