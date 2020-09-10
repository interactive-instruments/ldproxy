# Modul "Styles" (STYLES)

Das Modul "Styles" kann für jede über ldproxy bereitgestellte API aktiviert werden. Es ergänzt verschiedene Ressourcen für die Bereitstellung und Verwaltung von Styles (Mapbox Style oder SLD) und zugehöriger Ressourcen (Synmbole, Sprites) in der API.

Das Modul basiert auf den Vorgaben der Konformatitätsklassen "Core", "Manage styles", "Validation of styles", "Resources", "Manage resources", "Mapbox Style", "OGC SLD 1.0", "OGC SLD 1.1", "HTML" und "Style information" aus dem [Entwurf von OGC API - Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html#rc_queryables). Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ein- und Ausgabeformate
| --- | --- | --- | ---
|Styles |`/{apiId}/styles` |GET<br>POST |HTML, JSON<br>Mapbox Style, OGC SLD 1.0, OGC SLD 1.1
|Style |`/{apiId}/styles/{styleId}` |GET<br>PUT<br>DELETE |HTML, Mapbox Style, OGC SLD 1.0, OGC SLD 1.1<br>Mapbox Style, OGC SLD 1.0, OGC SLD 1.1<br>n/a
|Style metadata |`/{apiId}/styles/{styleId}/metadata` |GET<br>PUT, PATCH |HTML, JSON<br>JSON
|Resources |`/{apiId}/resources` |GET |HTML, JSON
|Resource |`/{apiId}/resources/{resourceId}` |GET<br>PUT<br>DELETE |\*<br>\*<br>n/a
|Collection |`/{apiId}/collections/{collectionId}` |PATCH |JSON

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`styleEncodings` |array |`[ "Mapbox", "HTML" ]` |Steuert, welche Formate für Stylesheets unterstützt werden sollen. Zur Verfügung stehen Mapbox Style ("Mapbox"), OGC SLD 1.0 ("SLD10"), OGC SLD 1.1 ("SLD11") und HTML ("HTML"). HTML ist ein reines Ausgabeformat im Sinne einer Webmap und wird nur für Styles unterstützt, für die ein Stylesheet im Format Mapbox Style verfügbar ist. Siehe die Konformitätsklassen "Mapbox Style", "OGC SLD 1.0", "OGC SLD 1.1" und "HTML".
|`styleInfosOnCollection` |boolean |`false` |Steuert, ob in den Collection-Ressourcen Informationen zu den Styles zu diesen Features aufgenommen werden sollen. Siehe die Konformitätsklasse "Style information".
|`managerEnabled` |boolean |`false` |Steuert, ob die Styles über POST, PUT und DELETE verwaltet werden können. Ist `styleInfosOnCollection` aktiv, dann können auch die Style-Informationen über PATCH erzeugt bzw. aktualisiert werden. Siehe die Konformitätsklasse "Manage styles".
|`validationEnabled` |boolean |`false` |Steuert, ob bei POST und PUT von Styles die Validierung der Styles über den Query-Parameter `validate` unterstützt werden soll. Siehe die Konformitätsklasse "Validation of styles".
|`resourcesEnabled` |boolean |`false` |Steuert, ob die API auch beliebige File-Ressourcen, z.B. für Symbole oder Sprites, unterstützen soll. Siehe die Konformitätsklasse "Resources".
|`resourceManagerEnabled` |boolean |`false` |Steuert, ob die Ressourcen über PUT und DELETE über die API erzeugt und gelöscht werden können sollen. Siehe die Konformitätsklasse "Manage resources".
|`defaultStyle` |string |`null` |Ist ein Style angegeben, dann wird auf die HTML-Repräsentierung (Webkarte) von der Landing-Page verlinkt. Der Style sollte alle Daten im Datensatz abdecken.

Die Stylesheets, die Style-Metadaten und die Style-Informationen liegen als Dateien im ldproxy-Datenverzeichnis:

* Die Stylesheets müssen unter dem relativen Pfad `styles/{apiId}/{styleId}.{ext}` liegen, wobei `{ext}` entweder "mbs" (für Mapbox), "sld10" (für SLD 1.0) oder "sld11" (für SLD 1.1) sein. Die URIs (Sprites, Glyphs, Source.url, Source.tiles) bei den Mapbox-Styles Links können dabei als Parameter `{serviceUrl}` enthalten.
* Die Style-Metdaten müssen unter dem relativen Pfad `styles/{apiId}/{styleId}.metadata` liegen. Links können dabei Templates sein (d.h. `templated` ist `true`) und als Parameter `{serviceUrl}` enthalten.
* Die Style-Informationen müssen unter dem relativen Pfad `style-infos/{apiId}/{collectionId}.json` liegen. Die Links können dabei Templates sein (d.h. `templated` ist `true`) und als Parameter `{serviceUrl}` und `{collectionId}` enthalten.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: STYLES
  enabled: true
  styleEncodings:
  - Mapbox
  - HTML
  styleInfosOnCollection: true
  managerEnabled: false
  validationEnabled: false
  resourcesEnabled: true
  resourceManagerEnabled: false
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

Beispiel für eine Style-Information-Datei mit einem Style:

```json
{
  "styles" : [ {
    "id" : "kitas",
    "title" : "Standard-Style für Kindertagesstätten",
    "description" : "(Hier steht eine Beschreibung des Styles...)",
    "links" : [ {
      "rel" : "stylesheet",
      "type" : "application/vnd.mapbox.style+json",
      "title" : "Stylesheet im Format 'Mapbox Style'",
      "href" : "{serviceUrl}/styles/kitas?f=mbs",
      "templated" : true  
    }, {
      "rel" : "map",
      "title" : "Webkarte mit dem Style",
      "href" : "{serviceUrl}/styles/kitas?f=html",
      "templated" : true  
    }, {
      "rel" : "describedby",
      "title" : "Metadaten zum Style",
      "href" : "{serviceUrl}/styles/kitas/metadata",
      "templated" : true  
    } ]
  } ],
  "defaultStyle" : "kitas"
}
```
