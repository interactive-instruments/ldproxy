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
|`ignoreInvalidGeometries` |boolean |`false` |Steuert, ob Objekte mit ungültigen Objektgeometrien ignoriert werden. Bevor Objekte ignoriert werden, wird zuerst versucht, die Geometrie in eine gültige Geometrie zu transformieren. Nur wenn dies nicht gelingt, wird die Geometrie ignoriert. Die Topologie von Geometrien können entweder schon im Provider ungültig sein oder die Geometrie kann in seltenen Fällen als Folge der Quantisierung der Koordinaten zu Integern für die Speicherung in der Kachel ungültig werden.
|`tileEncodings` |array |`[ "MVT" ]` |Steuert, welche Formate für die Kacheln unterstützt werden sollen. Zur Verfügung stehen derzeit nur Mapbox Vector Tiles ("MVT").
|`zoomLevels` |object |`{ "WebMercatorQuad" : { "min": 0, "max": 23 } }` |Steuert die Zoomstufen, die für jedes aktive Kachelschema verfügbar sind sowie welche Zoomstufe als Default bei verwendet werden soll.
|`zoomLevelsCache` |object |`{}` |Steuert die Zoomstufen, in denen erzeugte Kacheln gecacht werden.
|`center` |array |`null` |Legt Länge und Breite fest, auf die standardmäßig eine Karte mit den Kacheln zentriert werden sollte.
|`filters` |object |`{}` |Über Filter kann gesteuert werden, welche Features auf welchen Zoomstufen selektiert werden sollen. Dazu dient ein CQL-Filterausdruck, der in `filter` angegeben wird. Siehe das Beispiel unten.
|`rules` |object |`{}` |Über Regeln können die selektierten Features in Abhängigkeit der Zoomstufe nachbearbeitet werden. Unterstützt wird eine Reduzierung der Attribute (`properties`), das geometrische Verschmelzen von Features, die sich geometrisch schneiden (`merge`), ggf. eingeschränkt auf Features mit bestimmten identischen Attributen (`groupBy`). Siehe das Beispiel unten. Beim Verschmelzen werden alle Attribute in das neue Objekt übernommen, die in den verschmolzenen Features identisch sind.
|`seeding` |object |`{}` |Steuert die Zoomstufen, die für jedes aktive Kachelschema beim Start vorberechnet werden.
|`limit` |integer |100000 |Steuert die maximale Anzahl der Features, die pro Query für eine Kachel berücksichtigt werden.
|`minimumSizeInPixel`| number |0.5 |Objekte mit Liniengeometrien, die kürzer als der Wert sind, werden nicht in die Kachel aufgenommen. Objekte mit Flächengeometrien, die kleiner als das Quadrat des Werts sind, werden nicht in die Kachel aufgenommen. Der Wert 0.5 entspricht einem halben "Pixel" im Kachelkoordinatensystem.
|`maxRelativeAreaChangeInPolygonRepair` | number |0.1 |Steuert die maximal erlaubte relative Änderung der Flächengröße beim Versuch eine topologisch ungültige Polygongeometrie im Koordinatensystem der Kachel zu reparieren. Ist die Bedingung erfüllt, wird die reparierte Polygongeometrie verwendet. Der Wert 0.1 entspricht 10%.
|`maxAbsoluteAreaChangeInPolygonRepair` | number |1.0 |Steuert die maximal erlaubte absolute Änderung der Flächengröße beim Versuch eine topologisch ungültige Polygongeometrie im Koordinatensystem der Kachel zu reparieren. Ist die Bedingung erfüllt, wird die reparierte Polygongeometrie verwendet. Der Wert 1.0 entspricht einem "Pixel" im Kachelkoordinatensystem.

Beispiel für die Angaben in der Konfigurationsdatei für Gebäude:

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
