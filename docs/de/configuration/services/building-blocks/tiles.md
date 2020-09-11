# Modul "Vector Tiles" (TILES)

Das Modul "Vector Tiles" kann für jede über ldproxy bereitgestellte API mit einem SQL-Feature-Provider aktiviert werden. Es aktiviert die Ressourcen "Tilesets", "Tileset", "Vector Tile", "Tile Matrix Sets" und "Tile Matrix Set".

Das Modul basiert auf dem Entwurf von [OGC API - Tiles - Part 1: Core](https://github.com/opengeospatial/OGC-API-Tiles) und dem Standard [OGC Two Dimensional Tile Matrix Set](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html). Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Tilesets |`/{apiId}/tiles`<br>`/{apiId}/collections/{collectionId}/tiles`|GET |HTML, JSON
|Tileset |`/{apiId}/tiles/{tileMatrixSetId}`<br>`/{apiId}/collections/{collectionId}/tiles/{tileMatrixSetId}` |GET |TileJSON
|Vector Tile |`/{apiId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}`<br>`/{apiId}/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}` |GET |MVT
|Tile Matrix Sets |`/{apiId}/tileMatrixSets` |GET |HTML, JSON
|Tile Matrix Set |`/{apiId}/tileMatrixSets/{tileMatrixSetId}` |GET |HTML, JSON

Als Kachelschemas stehen u.a. zur Verfügung: [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62), [WorldCRS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#63), [WorldMercatorWGS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#64).

Der Tile-Cache liegt im ldproxy-Datenverzeichnis unter dem relativen Pfad `tiles/{apiId}/{collectionId}/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}.pbf`, wobei "__all__" als Wert für `collectionId` bei den Kacheln für den gesamten Datensatz verwendet wird.

Wenn die Daten zu einer API oder Kachelkonfiguration geändert wurden, dann sollte das Cache-Verzeichnis für die API gelöscht werden, damit der Cache mit den aktualisierten Daten oder Regeln neu aufgebaut wird.

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`featureProvider` |string |die API-ID |Identifiziert den verwendeten Feature-Provider. Standardmäßig besitzt der Feature-Provider dieselbe ID wie die API.
|`singleCollectionEnabled` |boolean |`true` |Steuert, ob Vector Tiles für jede Feature Collection aktiviert werden sollen. Jede Kachel hat einen Layer mit den Features aus der Collection.
|`multiCollectionEnabled` |boolean |`true` |Steuert, ob Vector Tiles für den Datensatz aktiviert werden sollen. Jede Kachel hat einen Layer pro Collection mit den Features aus der Collection.
|`tileEncodings` |array |`[ "MVT" ]` |Steuert, welche Formate für die Kacheln unterstützt werden sollen. Zur Verfügung stehen derzeit nur Mapbox Vector Tiles ("MVT").
|`zoomLevels` |object |`{ "WebMercatorQuad" : { "min": 0, "max": 23 } }` |Steuert die Zoomstufen, die für jedes aktive Kachelschema verfügbar sind sowie welche Zoomstufe als Default bei verwendet werden soll.
|`zoomLevelsCache` |object |`{}` |Steuert die Zoomstufen, in denen erzeugte Kacheln gecacht werden.
|`center` |array |`null` |Legt Länge und Breite fest, auf die standardmäßig eine Karte mit den Kacheln zentriert werden sollte.
|`filters` |object |`{}` |Über Filter kann gesteuert werden, welche Features auf welchen Zoomstufen selektiert werden sollen. Dazu dient ein CQL-Filterausdruck, der in `filter` angegeben wird. Siehe das Beispiel unten.
|`seeding` |object |`{}` |Steuert die Zoomstufen, die für jedes aktive Kachelschema beim Start vorberechnet werden.
|`limit` |integer |100000 |Steuert die maximale Anzahl der Features, die pro Query für eine Kachel berücksichtigt werden.
|`maxPointPerTileDefault` |integer |10000 |Steuert die maximale Anzahl von Features mit (Multi-)Point-Geometrie, die für eine Kachel berücksichtigt werden.
|`maxLineStringPerTileDefault` |integer |10000 |Steuert die maximale Anzahl von Features mit (Multi-)LineString-Geometrie, die für eine Kachel berücksichtigt werden.
|`maxPolygonPerTileDefault`  |integer |10000 |Steuert die maximale Anzahl von Features mit (Multi-)Polygon-Geometrie, die für eine Kachel berücksichtigt werden.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: TILES
  enabled: true
  multiCollectionEnabled: true
  center:
  - 10.0
  - 51.5
  zoomLevels:
    WebMercatorQuad:
      min: 4
      max: 18
      default: 8
  zoomLevelsCache:
    WebMercatorQuad:
      min: 4
      max: 15
  filters:
    WebMercatorQuad:
    - min: 5
      max: 7
      filter: 'strasse.strassenklasse IN (''A'')'
    - min: 8
      max: 9
      filter: 'strasse.strassenklasse IN (''A'',''B'')'
    - min: 10
      max: 10
      filter: 'strasse.strassenklasse IN (''A'',''B'',''L'',''K'')'
  seeding:
    WebMercatorQuad:
      min: 4
      max: 10
```
