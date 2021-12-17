# Modul "Tiles" (TILES)

Das Modul "Tiles" kann für jede über ldproxy bereitgestellte API mit einem SQL-Feature-Provider oder mit einem MBTiles-Tile-Provider aktiviert werden. Es aktiviert die Ressourcen "Tilesets", "Tileset", "Tile", "Tile Matrix Sets" und "Tile Matrix Set".

Das Modul basiert auf dem Entwurf von [OGC API - Tiles - Part 1: Core](https://github.com/opengeospatial/OGC-API-Tiles) und dem Entwurf von [OGC Two Dimensional Tile Matrix Set and Tile Set Metadata](https://docs.ogc.org/DRAFTS/17-083r3.html). Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Tilesets |`/{apiId}/tiles`<br>`/{apiId}/collections/{collectionId}/tiles`|GET |HTML, JSON
|Tileset |`/{apiId}/tiles/{tileMatrixSetId}`<br>`/{apiId}/collections/{collectionId}/tiles/{tileMatrixSetId}` |GET |JSON, TileJSON
|Tile |`/{apiId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}`<br>`/{apiId}/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}` |GET |Kachelformate
|Tile Matrix Sets |`/{apiId}/tileMatrixSets` |GET |HTML, JSON
|Tile Matrix Set |`/{apiId}/tileMatrixSets/{tileMatrixSetId}` |GET |HTML, JSON

Die unterstützten Kachelformate sind:

- MVT (Mapbox Vector Tile)
- PNG
- WebP
- JPEG
- TIFF

Als vorkonfigurierte Kachelschemas stehen zur Verfügung:

- [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62)
- [WorldCRS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#63)
- [WorldMercatorWGS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#64)
- AdV_25832 (Kachelschema der AdV für Deutschland)
- EU_25832 (Kachelschema des BKG, basierend auf AdV_25832, erweitert auf Europa)
- gdi_de_25832 (von der GDI-DE empfohlenes Kachelschema)

Weitere Kachelschemas können als JSON-Datei gemäß dem aktuellen Entwurf für den OGC-Standard [Two Dimensional Tile Matrix Set and Tile Set Metadata 2.0](https://docs.ogc.org/DRAFTS/17-083r3.html) im Datenverzeichnis unter `api-resources/tile-matrix-sets/{tileMatrixSetId}.json` konfiguriert werden.

Der Tile-Cache liegt im ldproxy-Datenverzeichnis unter dem relativen Pfad `cache/tiles/{apiId}`. Wenn die Daten zu einer API oder Kachelkonfiguration geändert wurden, dann sollte das Cache-Verzeichnis für die API gelöscht werden, damit der Cache mit den aktualisierten Daten oder Regeln neu aufgebaut wird.

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`tileProvider` |object |`{ "type": "FEATURES", ... }` |Spezifiziert die Datenquelle für die Kacheln, siehe [Tile-Provider-Objekte](#tile-provider).
|`tileSetEncodings` |array |`[ "JSON", "TileJSON" ]` |Steuert, welche Formate für die Tileset-Ressourcen unterstützt werden sollen. Zur Verfügung stehen [OGC TileSetMetadata](https://docs.ogc.org/DRAFTS/17-083r3.html#tsmd-json-encoding) ("JSON") und [TileJSON](https://github.com/mapbox/tilejson-spec) ("TileJSON").
|`cache` |string |`FILES` |`FILES` speichert jede Kachel als Datei im Dateisystem. `MBTILES` speichert die Kacheln in einer MBTiles-Datei (eine MBTiles-Datei pro Tileset). Es wird die Verwendung von `MBTILES` empfohlen. Es ist geplant, den Default mit der Version 4.0 auf `MBTILES` zu ändern.
|`style` |string |`DEFAULT` |Ein Style im Style-Repository, der standardmäßig in Karten mit den Tiles verwendet werden soll. Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet. Bei `NONE` wird ein einfacher Style mit OpenStreetMap als Basiskarte verwendet. Der Style sollte alle Daten abdecken und muss im Format Mapbox Style verfügbar sein. Es wird zuerst nach einem Style mit dem Namen für die Feature Collection gesucht; falls keiner gefunden wird, wird nach einem Style mit dem Namen auf der API-Ebene gesucht. Wird kein Style gefunden, wird `NONE` verwendet.
|`removeZoomLevelConstraints`|boolean |`false` |Bei `true` werden aus dem in `style` angegebenen Style die `minzoom`- und `maxzoom`-Angaben bei den Layer-Objekten entfernt, damit die Features in allen Zoomstufen angezeigt werden. Diese Option sollte nicht gewählt werden, wenn der Style unterschiedliche Präsentationen je nach Zoomstufe vorsieht, da ansonsten alle Layer auf allen Zoomstufen gleichzeitig angezeigt werden.
|`mapClientType` |enum |`MAP_LIBRE` |Auswahl des zu verwendenden Map-Clients in der HTML-Ausgabe. Der Standard ist MapLibre GL JS, unterstützt wird nur das Kachelschema "WebMercatorQuad". Alternativ wird als auch `OPEN_LAYERS` unterstützt (OpenLayers). Die Unterstützung von Open Layers ist nur sinnvoll, wenn in der HTML Ausgabe auch andere der vordefinierten Kachelschemas unterstützt werden sollen. Bei `OPEN_LAYERS` werden keine Styles unterstützt.
|`tileEncodings` |array |`[ "MVT" ]` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`zoomLevels` |object |`{ "WebMercatorQuad" : { "min": 0, "max": 23 } }` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`zoomLevelsCache` |object |`{}` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`center` |array |`[ 0, 0 ]` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`filters` |object |`{}` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`rules` |object |`{}` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`seeding` |object |`{}` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`singleCollectionEnabled` |boolean |`true` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`multiCollectionEnabled` |boolean |`true` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`ignoreInvalidGeometries` |boolean |`false` |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`limit` |integer |100000 |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`minimumSizeInPixel`| number |0.5 |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`maxRelativeAreaChangeInPolygonRepair` | number |0.1 |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`maxAbsoluteAreaChangeInPolygonRepair` | number |1.0 |*Deprecated* Siehe [Tile-Provider Features](#tile-provider-features).
|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.

<a name="tile-provider"></a>

## Die Tile-Provider-Objekte

Es werden aktuell drei Arten von Tile-Providern unterstützt:

- `FEATURES`: Die Kacheln werden aus einem Feature-Provider abgeleitet.
- `MBTILES`: Die Kacheln eines Tileset im Kachelschema "WebMercatorQuad" liegen in einem MBTiles-Archiv vor.
- `TILESERVER`: Die Kacheln werden von einer TileServer-GL-Instanz abgerufen.

<a name="tile-provider-features"></a>

### Tile-Provider FEATURES

Bei diesem Tile-Provider werden die Kacheln im Format Mapbox Vector Tiles aus den von der API bereitgestellten Features im Gebiet der Kachel abgeleitet.

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`type` |string |`FEATURES` |Fester Wert, identifiziert die Tile-Provider-Art.
|`tileEncodings` |array |`[ "MVT" ]` |Steuert, welche Formate für die Kacheln unterstützt werden sollen. Zur Verfügung steht derzeit nur Mapbox Vector Tiles ("MVT").
|`zoomLevels` |object |`{ "WebMercatorQuad" : { "min": 0, "max": 23 } }` |Steuert die Zoomstufen, die für jedes aktive Kachelschema verfügbar sind sowie welche Zoomstufe als Default bei verwendet werden soll.
|`zoomLevelsCache` |object |`{}` |Steuert die Zoomstufen, in denen erzeugte Kacheln gecacht werden.
|`seeding` |object |`{}` |Steuert die Zoomstufen, die für jedes aktive Kachelschema beim Start vorberechnet werden.
|`seedingOptions` |object | |Steuert wie und wann Kacheln vorberechnet werden, siehe [Optionen für das Seeding](#seeding-options).
|`filters` |object |`{}` |Über Filter kann gesteuert werden, welche Features auf welchen Zoomstufen selektiert werden sollen. Dazu dient ein CQL-Filterausdruck, der in `filter` angegeben wird. Siehe das Beispiel unten.
|`rules` |object |`{}` |Über Regeln können die selektierten Features in Abhängigkeit der Zoomstufe nachbearbeitet werden. Unterstützt wird eine Reduzierung der Attribute (`properties`), das geometrische Verschmelzen von Features, die sich geometrisch schneiden (`merge`), ggf. eingeschränkt auf Features mit bestimmten identischen Attributen (`groupBy`). Siehe das Beispiel unten. Beim Verschmelzen werden alle Attribute in das neue Objekt übernommen, die in den verschmolzenen Features identisch sind.
|`center` |array |`[ 0, 0 ]` |Legt Länge und Breite fest, auf die standardmäßig eine Karte mit den Kacheln zentriert werden sollte.
|`limit` |integer |100000 |Steuert die maximale Anzahl der Features, die pro Query für eine Kachel berücksichtigt werden.
|`singleCollectionEnabled` |boolean |`true` |Steuert, ob Vector Tiles für jede Feature Collection aktiviert werden sollen. Jede Kachel hat einen Layer mit den Features aus der Collection.
|`multiCollectionEnabled` |boolean |`true` |Steuert, ob Vector Tiles für den Datensatz aktiviert werden sollen. Jede Kachel hat einen Layer pro Collection mit den Features aus der Collection.
|`ignoreInvalidGeometries` |boolean |`false` |Steuert, ob Objekte mit ungültigen Objektgeometrien ignoriert werden. Bevor Objekte ignoriert werden, wird zuerst versucht, die Geometrie in eine gültige Geometrie zu transformieren. Nur wenn dies nicht gelingt, wird die Geometrie ignoriert. Die Topologie von Geometrien können entweder schon im Provider ungültig sein oder die Geometrie kann in seltenen Fällen als Folge der Quantisierung der Koordinaten zu Integern für die Speicherung in der Kachel ungültig werden.
|`minimumSizeInPixel`| number |0.5 |Objekte mit Liniengeometrien, die kürzer als der Wert sind, werden nicht in die Kachel aufgenommen. Objekte mit Flächengeometrien, die kleiner als das Quadrat des Werts sind, werden nicht in die Kachel aufgenommen. Der Wert 0.5 entspricht einem halben "Pixel" im Kachelkoordinatensystem.
|`maxRelativeAreaChangeInPolygonRepair` | number |0.1 |Steuert die maximal erlaubte relative Änderung der Flächengröße beim Versuch eine topologisch ungültige Polygongeometrie im Koordinatensystem der Kachel zu reparieren. Ist die Bedingung erfüllt, wird die reparierte Polygongeometrie verwendet. Der Wert 0.1 entspricht 10%.
|`maxAbsoluteAreaChangeInPolygonRepair` | number |1.0 |Steuert die maximal erlaubte absolute Änderung der Flächengröße beim Versuch eine topologisch ungültige Polygongeometrie im Koordinatensystem der Kachel zu reparieren. Ist die Bedingung erfüllt, wird die reparierte Polygongeometrie verwendet. Der Wert 1.0 entspricht einem "Pixel" im Kachelkoordinatensystem.
|`transformations` |object |`{}` |Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe [transformiert](general-rules.md#transformations) werden.

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

<a name="seeding-options"></a>

#### Optionen für das Seeding

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`runOnStartup` |boolean |`true` |Steuert, ob das Seeding beim Start einer API ausgeführt wird.
|`runPeriodic` |string |`null` |Ein Crontab-Pattern für die regelmäßige Ausführung des Seedings. Das Seeding wird stets nur einmal pro API zur gleichen Zeit ausgeführt, d.h. falls eine weitere Ausführung geplant ist, während die vorherige noch läuft, wird diese übersprungen.
|`purge` |boolean |`false` |Steuert, ob der Cache vor dem Seeding bereinigt wird.
|`maxThreads` |integer |`1` |Die maximale Anzahl an Threads, die für das Seeding verwendet werden darf. Die tatsächlich verwendete Zahl der Threads hängt davon ab, wie viele Threads für [Hintergrundprozesse](../../global-configuration.md#background-tasks) zur Verfügung stehen, wenn das Seeding startet. Wenn mehr als ein Thread erlaubt sein soll, ist zunächst zu prüfen, ob genügend Threads für [Hintergrundprozesse](../../global-configuration.md#background-tasks) konfiguriert sind. Es ist zu berücksichtigen, dass alle APIs um die vorhandenen Threads für [Hintergrundprozesse](../../global-configuration.md#background-tasks) konkurrieren.

<a name="tile-provider-mbtiles"></a>

### Tile-Provider MBTILES

Bei diesem Tile-Provider werden die Kacheln über eine [MBTiles-Datei](https://github.com/mapbox/mbtiles-spec) bereitgestellt. Das Kachelformat und alle anderen Eigenschaften der Tileset-Ressource ergeben sich aus dem Inhalt der MBTiles-Datei. Unterstützt wird nur das Kachelschema "WebMercatorQuad".

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`type` |string |`MBTILES` |Fester Wert, identifiziert die Tile-Provider-Art.
|`filename` |string |`null` |Dateiname der MBTiles-Datei im Verzeichnis `api-resources/tiles/{apiId}`.

Beispielkonfiguration:

```yaml
- buildingBlock: TILES
  enabled: true
  tileProvider:
    type: MBTILES
    filename: satellite-lowres-v1.2-z0-z5.mbtiles
```

<a name="tile-provider-tileserver"></a>

### Tile-Provider TILESERVER

Bei diesem Tile-Provider werden die Kacheln über eine [TileServer-GL-Instanz](https://github.com/maptiler/tileserver-gl) bezogen. Unterstützt wird nur das Kachelschema "WebMercatorQuad".

In der aktuellen Version wird dieser Provider nur im Modul [Map Tiles](map-tiles.md) unterstützt. Unterstützt werden nur die Bitmap-Kachelformate. Seeding oder Caching werden nicht unterstützt.

Dieser Tile-Provider ist experimentell und seine Konfigurationsoptionen können sich in zukünftigen Versionen ändern.

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`type` |string |`TILESERVER` |Fester Wert, identifiziert die Tile-Provider-Art.
|`urlTemplate` |string |`null` |URL-Template für den Zugriff auf Kacheln. Zu verwenden sind die Parameter `{tileMatrix}`, `{tileRow}`, `{tileCol}` und `{fileExtension}`.
|`urlTemplateSingleCollection` |string |`null` |URL-Template für den Zugriff auf Kacheln für eine Collection.
|`tileEncodings` |array |`[]` |Liste der zu unterstützenden Kachelformate, erlaubt sind `PNG`, `WebP` und `JPEG`.

Beispielkonfiguration:

```yaml
- buildingBlock: MAP_TILES
  enabled: true
  mapProvider:
    type: TILESERVER
    urlTemplate: 'https://www.example.com/tileserver/styles/topographic/{tileMatrix}/{tileCol}/{tileRow}@2x.{fileExtension}'
    tileEncodings:
      - WebP
      - PNG
```

Ein Beispiel für eine TileServer-GL-Konfiguration mit dem Style "topographic", der z.B. als Datenquelle die Vector Tiles der API verwenden kann:

```json
{
  "options": {},
  "styles": {
    "topographic": {
      "style": "topographic.json",
      "tilejson": {
        "type": "overlay",
        "bounds": [35.7550727, 32.3573507, 37.2052764, 33.2671397]
      }
    }
  },
  "data": {}
}
```
