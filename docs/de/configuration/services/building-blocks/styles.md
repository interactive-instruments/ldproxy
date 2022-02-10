# Modul "Styles" (STYLES)

Das Modul "Styles" kann für jede über ldproxy bereitgestellte API aktiviert werden. Es ergänzt verschiedene Ressourcen für die Bereitstellung und Verwaltung von Styles.  (Mapbox Style, SLD).

Das Modul basiert auf den Vorgaben der Konformitätsklassen *Core*, *Manage styles*, *Validation of styles*, *Mapbox Style*, *OGC SLD 1.0* und *OGC SLD 1.1* aus dem [Entwurf von OGC API - Styles](https://docs.ogc.org/DRAFTS/20-009.html). Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ein- und Ausgabeformate
| --- | --- | --- | ---
|Styles |`/{baseResource}/styles` |GET<br>POST |HTML, JSON<br>alle Style-Formate
|Style |`/{baseResource}/styles/{styleId}` |GET<br>PUT<br>DELETE |HTML, alle Style-Formate<br>alle Style-Formate<br>n/a
|Style metadata |`/{baseResource}/styles/{styleId}/metadata` |GET<br>PUT, PATCH |HTML, JSON<br>JSON

Unterstützte Style-Formate sind:

- Mapbox Style
- OGC SLD 1.0
- OGC SLD 1.1
- QGIS QML
- ArcGIS Desktop (lyr)
- ArcGIS Pro (lyrx)

Style-Collections werden unter den folgenden `{baseResource}` zur Verfügung gestellt:

- `{apiId}`
- `{apiId}/collection/{collectionId}`

Erlaubte Zeichen für `{styleId}` sind alle Zeichen bis auf den Querstrich ("/").

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`styleEncodings` |array |`[ "Mapbox", "HTML" ]` |Steuert, welche Formate für Stylesheets unterstützt werden sollen. Zur Verfügung stehen Mapbox Style ("Mapbox"), OGC SLD 1.0 ("SLD10"), OGC SLD 1.1 ("SLD11"), QGIS QML ("QML"), ArcGIS Layer ("lyr" und "lyrx") und HTML ("HTML"). HTML ist ein reines Ausgabeformat im Sinne einer Webmap und wird nur für Styles unterstützt, für die ein Stylesheet im Format Mapbox Style verfügbar ist. Siehe die Konformitätsklassen "Mapbox Style", "OGC SLD 1.0", "OGC SLD 1.1" und "HTML".
|`managerEnabled` |boolean |`false` |Steuert, ob die Styles über POST, PUT und DELETE verwaltet werden können. Siehe die Konformitätsklasse "Manage styles".
|`validationEnabled` |boolean |`false` |Steuert, ob bei POST und PUT von Styles die Validierung der Styles über den Query-Parameter `validate` unterstützt werden soll. Siehe die Konformitätsklasse "Validation of styles".
|`defaultStyle` |string |`null` |*Deprecated* Siehe `defaultStyle` in [Modul HTML](html.md).
|`resourcesEnabled` |boolean |`false` |*Deprecated* Siehe `enabled` in [Modul Resources](resources.md).
|`resourceManagerEnabled` |boolean |`false` |*Deprecated* Siehe `managerEnabled` in [Modul Resources](resources.md).
|`webmapWithPopup` |boolean |`true` |Steuert, ob bei Webkarten zu Styles im Format Mapbox Style ein Popup mit den Attributen zum obersten Objekt angezeigt werden soll.
|`webmapWithLayerControl` |boolean |`false` |Steuert, ob bei Webkarten zu Styles im Format Mapbox Style die Layer ein- und ausgeschaltet werden können. Ein- und ausgeschaltet werden können jeweils gebündelt alle Layer zu einer Feature Collection.
|`layerControlAllLayers` |boolean |`false` |Nur wirksam bei `webmapWithLayerControl: true`. Steuert, ob auch Kartenlayer, die nicht aus den Vector Tiles dieser API, z.B. eine Hintergrundkarte, ein- und ausgeschaltet werden können.
|`deriveCollectionStyles` |boolean |`false` |Nur wirksam bei Styles im Format Mapbox Style. Steuert, ob die Styles auf der Ebene der Collections aus den Styles aus der übergeordneten Style-Collection abgeleitet werden sollen. Voraussetzung ist, dass der Name der `source` im Stylesheet der `{apiId}` entspricht und der Name der `source-layer` der `{collectionId}`. Sofern ein Style für die Darstellung von Features im Modul FEATURES_HTML verwendet werden soll, sollte die Option aktiviert sein.
|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.

Die Stylesheets, die Style-Metadaten und die Style-Informationen liegen als Dateien im ldproxy-Datenverzeichnis:

- Die Stylesheets müssen unter dem relativen Pfad `api-resources/styles/{apiId}/{styleId}.{ext}` liegen. Die URIs (Sprites, Glyphs, Source.url, Source.tiles) bei den Mapbox-Styles Links können dabei als Parameter `{serviceUrl}` enthalten. Die Dateikennung `{ext}` muss den folgenden Wert in Abhängigkeit des Style-Formats haben:
  - Mapbox Style: "mbs"
  - OGC SLD 1.0: "sld10"
  - OGC SLD 1.1: "sld11"
  - QGIS QML: "qml"
  - ArcGIS Desktop: "lyr"
  - ArcGIS Pro: "lyrx
- Die Style-Metadaten müssen unter dem relativen Pfad `api-resources/styles/{apiId}/{styleId}.metadata` liegen. Links können dabei Templates sein (d.h. `templated` ist `true`) und als Parameter `{serviceUrl}` enthalten.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: STYLES
  enabled: true
  styleEncodings:
  - Mapbox
  - HTML
  deriveCollectionStyles: true
  managerEnabled: false
  validationEnabled: false
```

Beispiel für ein Mapbox-Stylesheet:

```json
{
  "bearing" : 0.0,
  "version" : 8,
  "pitch" : 0.0,
  "name" : "kitas",
  "center": [
    10.0,
    51.5
  ],
  "zoom": 12,
  "sources" : {
    "kindergarten" : {
      "type" : "vector",
      "tiles" : [ "{serviceUrl}/collections/governmentalservice/tiles/WebMercatorQuad/{z}/{y}/{x}?f=mvt" ],
      "maxzoom" : 16
    },
    "basemap" : {
      "type" : "raster",
      "tiles" : [ "https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png" ],
      "attribution" : "&copy; <a href=\"http://www.bkg.bund.de\" class=\"link0\" target=\"_new\">Bundesamt f&uuml;r Kartographie und Geod&auml;sie</a> 2017, <a href=\"http://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf\" class=\"link0\" target=\"_new\">Datenquellen</a>"
    }
  },
  "sprite" : "{serviceUrl}/resources/sprites-kitas",
  "glyphs": "https://go-spatial.github.io/carto-assets/fonts/{fontstack}/{range}.pbf",
  "layers" : [ {
      "id": "background",
      "type": "raster",
    "source" : "basemap"
  }, {
    "type" : "symbol",
    "source-layer" : "governmentalservice",
    "layout" : {
      "icon-image" : "kita",
      "icon-size" : 0.5
    },
    "paint" : {
      "icon-halo-width" : 2,
      "icon-opacity" : 1
    },
    "id" : "kita",
    "source" : "kitas"
  } ]
}
```

Beispiel für eine Style-Metadaten-Datei:

```json
{
  "id": "kitas",
  "title": "Kindertageseinrichtungen",
  "description": "(Hier steht eine Beschreibung des Styles...)",
  "keywords": [ ],
  "scope": "style",
  "version": "0.0.1",
  "stylesheets": [
    {
      "title": "Mapbox Style",
      "version": "8",
      "specification": "https://docs.mapbox.com/mapbox-gl-js/style-spec/",
      "native": true,
      "tilingScheme": "GoogleMapsCompatible",
      "link": {
        "href": "{serviceUrl}/styles/kitas?f=mbs",
        "rel": "stylesheet",
        "type": "application/vnd.mapbox.style+json",
        "templated": true
      }
    }
  ],
  "layers": [
    {
      "id": "governmentalservice",
      "type": "point",
      "sampleData": {
        "href": "{serviceUrl}/collections/governmentalservice/items?f=json",
        "rel": "data",
        "type": "application/geo+json",
        "templated": true
      }
    }
  ],
  "links": [
    {
      "href": "{serviceUrl}/resources/kitas-thumbnail.png",
      "rel": "preview",
      "type": "image/png",
      "title": "Thumbnail des Styles für Kindertagesstätten",
      "templated": true
    }
  ]
}
```
