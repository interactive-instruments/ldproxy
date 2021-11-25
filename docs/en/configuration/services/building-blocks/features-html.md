# Features HTML (FEATURES_HTML)

The module *Features HTML* may be enabled for every API with a feature provider. It provides the resources *Features* and *Feature* encoded as HTML.

*Features HTML* implements all requirements of conformance class *HTML* of [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html) for the two mentioned resources.

## Configuration for API

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`noIndexEnabled` |boolean |`true` |Set `noIndex` for all sites to prevent search engines from indexing.
|`schemaOrgEnabled` |boolean |`true` |Enable [schema.org](https://schema.org) annotations for all sites, which are used e.g. by search engines. The annotations are embedded as JSON-LD.
|`collectionDescriptionsInOverview`  |boolean |`true` |Show collection descriptions in *Feature Collections* resource for HTML.
|`layout` |enum |`CLASSIC` |Layout for *Features* and *Feature* resources. Either `CLASSIC` (mainly for simple objects with simple values) or `COMPLEX_OBJECTS` (supports more complex object structures and longer values).
|`mapClientType` |enum |`MAP_LIBRE` |The map client library to use to display features in the HTML representation. The default is MapLibre GL (`MAP_LIBRE`). WIP: Cesium (`CESIUM`) can be used for displaying 3D features on a globe.
|`style` |string |`DEFAULT` |An optional Mapbox style in the style repository to use for the map in the HTML representation of a feature or feature collection. If set to `DEFAULT`, the `defaultStyle` configured in the [HTML configuration](html.md) is used. If set to `NONE`, a simple wireframe style will be used with OpenStreetMap as a basemap. The value is ignored, if the map client is not MapLibre.
|`removeZoomLevelConstraints` |boolean |`false` |If `true`, any `minzoom` or `maxzoom` members are removed from the GeoJSON layers. The value is ignored, if the map client is not MapLibre or `style` is `NONE`.

### Example

```yaml
- buildingBlock: FEATURES_HTML
  schemaOrgEnabled: false
  layout: COMPLEX_OBJECTS
```

## Configuration for collection

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`itemLabelFormat` |string |`{{id}}` |Define how the feature label for HTML is formed. Default is the feature id. Property names in double curly braces will be replaced with the corresponding value.
|`transformations` |object |`{}` |Optional transformations for feature properties for HTML, see [transformations](README.md#transformations).

### Example

```yaml
    - buildingBlock: FEATURES_HTML
      itemLabelFormat: '{{name}}'
      transformations:
        geometry:
          remove: IN_COLLECTION
        occupancy.typeOfOccupant:
          remove: IN_COLLECTION
        occupancy.numberOfOccupants:
          remove: IN_COLLECTION
```
