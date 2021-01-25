# Feature Collections (COLLECTIONS)

The module *Feature Collections* has to be enabled for every API with a feature provider. It provides the resources *Feature Collections* and *Feature Collection*. Currently feature collections are the only supported type of collection.

*Feature Collections* implements all requirements of conformance class *Core* of [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) for the two mentioned resources.

|Resource |Path |HTTP Method |Media Types
| --- | --- | ---
|Feature Collections |`/{apiId}/collections` |GET |JSON, HTML, XML
|Feature Collection |`/{apiId}/collections/{collectionId}` |GET |JSON, HTML, XML

## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`additionalLinks` |array |`[]` |Add additional links to the *Collections* resource. The value is an array of link objects. Required properties of a link are a URI (`href`), a label (`label`) and a relation (`rel`).

> **Note**: additional links for a specific *Collection* can be defined in the collection configuration.

### Example

```yaml
- buildingBlock: COLLECTIONS
  additionalLinks:
  - rel: describedby
    type: text/html
    title: Webseite mit weiteren Informationen
    href: 'https://example.com/pfad/zu/webseite'
    hreflang: de
  - rel: enclosure
    type: application/geopackage+sqlite3
    title: Download des Datensatzes als GeoPackage
    href: 'https://example.com/pfad/zu/datei.gpkg'
    hreflang: de
```
