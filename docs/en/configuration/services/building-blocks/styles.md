# Styles (STYLES)

Adds support for publishing and managing styles (*Mapbox Style* or *SLD*) and related resources (symbols and sprites).


## Scope

### Conformance classes

This module implements requirements of the conformance classes *Core*, *Manage Styles*, *Validation of styles*, *Resources*, *Manage resources*, *Mapbox Style*, *OGC SLD 1.0*, *OGC SLD 1.1*, *HTML* and *Style information* from the draft specification [OGC API - Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html). The implementation is subject to change in the course of the development and approval process of the draft.

### Resources

|Resource |Path |HTTP Method |Media Types
| --- | --- | --- | ---
|Styles |`/{apiId}/styles` |GET<br>POST |HTML, JSON<br>Mapbox Style, OGC SLD 1.0, OGC SLD 1.1
|Style |`/{apiId}/styles/{styleId}` |GET<br>PUT<br>DELETE |HTML, Mapbox Style, OGC SLD 1.0, OGC SLD 1.1<br>Mapbox Style, OGC SLD 1.0, OGC SLD 1.1<br>n/a
|Style metadata |`/{apiId}/styles/{styleId}/metadata` |GET<br>PUT, PATCH |HTML, JSON<br>JSON
|Resources |`/{apiId}/resources` |GET |HTML, JSON
|Resource |`/{apiId}/resources/{resourceId}` |GET<br>PUT<br>DELETE |\*<br>\*<br>n/a
|Collection |`/{apiId}/collections/{collectionId}` |PATCH |JSON

Allowed characters for `{styleId}` and `{resourceId}` are all characters with the exception of slash (`/`).


## Prerequisites

|Module |Required |Description
| --- | --- | ---
[Feature Collections](collections.md)| Yes | Provides the resource *Feature Collection*, which is extended by this module.


## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`styleEncodings` |array |`[ `Mapbox`, `HTML` ]` |List of enabled stylesheet encodings. Supported are Mapbox Style (`Mapbox`), OGC SLD 1.0 (`SLD10`), OGC SLD 1.1 (`SLD11`) and HTML (`HTML`). HTML is an output only encoding for web maps that requires a *Mapbox Style* stylesheet. For details see conformance classes *Mapbox Style*, *OGC SLD 1.0*, *OGC SLD 1.1* und *HTML*.
|`styleInfosOnCollection` |boolean |`false` |Option to include style information related to a collection in the *Collection* resource For details see conformance class *Style information*.
|`managerEnabled` |boolean |`false` |Option to manage styles using POST, PUT and DELETE. If `styleInfosOnCollection` is enabled, style information may be created and updated using PATCH. Siehe die Konformitätsklasse "Manage styles".
|`validationEnabled` |boolean |`false` |Option to validate styles when using POST and PUT by setting the query parameter `validate`. For details see conformance class *Validation of styles*.
|`resourcesEnabled` |boolean |`false` |Option to support generic file resources, e.g. for symbols or sprites. For details see conformance class *Resources*.
|`resourceManagerEnabled` |boolean |`false` |Option to manage file resources using PUT und DELETE. For details see conformance class *Manage resources*.
|`defaultStyle` |string |`null` |If set, the API landing page will contain a link to the styles HTML representation (web map). The chosen style should cover the whole dataset.
|`webmapWithPopup` |boolean |`true` |Option to support popups in web maps for *Mapbox Style* styles that show attributes for the top-most object.
|`webmapWithLayerControl` |boolean |`false` |Option to support layer controls in web maps for *Mapbox Style* styles. Allows to collectively enable and disable all layers for a certain feature collection.
|`layerControlAllLayers` |boolean |`false` |Option to support layer controls for additional layers like background maps. Requires `webmapWithLayerControl: true`. 

### Example

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

### Storage

The stylesheets, style metadata and style information all reside as files in the data directory:

* Stylesheets reside under the relative path `styles/{apiId}/{styleId}.{ext}`, where `{ext}` is either `mbs` (Mapbox), `sld10` (SLD 1.0) or `sld11` (SLD 1.1). The URIs (Sprites, Glyphs, Source.url, Source.tiles) used in  Mapbox styles links might contain `{serviceUrl}`.
* Style metadata reside under the relative path `styles/{apiId}/{styleId}.metadata`. Links might be templates (by setting `templated` to `true`) containing `{serviceUrl}`.
* Style information reside under the relative path `style-infos/{apiId}/{collectionId}.json`. Links might be templates (by setting `templated` to `true`) containing `{serviceUrl}` and `{collectionId}`.

#### Example Mapbox stylesheet

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

#### Example style metadata file

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

#### Example style information file

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
