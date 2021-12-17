# Modul "Map Tiles" (MAP_TILES)

Das Modul "Map Tiles" aktiviert die Ressourcen "Map Tilesets", "Map Tileset" und "Map Tile".

Das Modul basiert auf den Entwürfen von [OGC API - Maps](https://github.com/opengeospatial/OGC-API-Maps). Die Implementierung wird sich im Zuge der weiteren Standardisierung der Entwürfe noch ändern.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Tilesets |`/{apiId}/map/tiles`<br>`/{apiId}/collections/{collectionId}/map/tiles`|GET |HTML, JSON
|Tileset |`/{apiId}/map/tiles/{tileMatrixSetId}`<br>`/{apiId}/collections/{collectionId}/map/tiles/{tileMatrixSetId}` |GET |JSON, TileJSON
|Tile |`/{apiId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}`<br>`/{apiId}/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}` |GET |Kachelformate

Die unterstützten Kachelformate sind:

- PNG
- WebP
- JPEG

Es steht nur das Kachelschema [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62) zur Verfügung.

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`mapProvider` |object |`null` |Spezifiziert die Datenquelle für die Kacheln, unterstützt werden derzeit nur [TileServer-Tile-Provider](tiles.md#tile-provider-tileserver).
