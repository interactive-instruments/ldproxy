# Features GeoJSON-LD (GEO_JSON_LD)

Adds support for JSON-LD by extending the [GeoJSON](geojson.md) encoding.


## Scope

### GeoJSON encoding

GeoJSON FeatureCollection and Feature objects are enhanced with additional properties.

|Property |GeoJSON type |Description
| --- | --- | ---
|@context |FeatureCollection, Feature | The JSON-LD context, only added to the root object. *Only added if `context` is set in the configuration.*
|@type |FeatureCollection, Feature | The JSON-LD type. If the root object is a FeatureCollection, adds `@type` with value `geojson:FeatureCollection` to the root object and `@type` with the value set in the configuration to every nested Feature . If the root object is a Feature, adds `@type` with the value set in the configuration.  A value of `{{type}}` will be replaced by the value of the property with `role: TYPE`.
|@id |Feature |URI based id in addition to the default GeoJSON id. *Only added if `idTemplate` is set in the configuration.*

### Resources

|Resource |Path |HTTP Method |Media Types
| --- | --- | --- | ---
|JSON-LD Context |`/{apiId}/collections/{collectionId}/context` |GET |JSON-LD Context


## Prerequisites

|Module |Required |Description
| --- | --- | ---
[Features GeoJSON](geojson.md)| Yes | Provides the GeoJSON encoding, which is extended by this module.


## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`contextFileName` |string |`null` |File name of the JSON-LD context document in the folder `json-ld-contexts/{apiId}`.
|`context` |string |`null` |URI of the JSON-LD context document. The value should either be an external URI or `{{serviceUrl}}/collections/{{collectionId}}/context` for contexts provided by the API (see below for details). The template may contain `{{serviceUrl}}` (substituted with the API landing page URI) and `{{collectionId}}` (substituted with the collection id).
|`types` |array |`[ "geojson:Feature" ]` |Value of `@type` that is added to every feature.
|`idTemplate` |string |`null` |Value of `@id` that is added to every feature. The template may contain `{{serviceUrl}}` (substituted with the API landing page URI), `{{collectionId}}` (substituted with the collection id) and `{{featureId}}` (substituted with the feature id).

### Example

```yaml
- buildingBlock: GEO_JSON_LD
  enabled: true
  context: '{{serviceUrl}}/collections/{{collectionId}}/context'
  types:
  - geojson:Feature
  - sosa:Observation
  idTemplate: '{{serviceUrl}}/collections/{{collectionId}}/items/{{featureId}}'
```

### JSON-LD context

If the context should be provided by the API, a file with relative path `json-ld-contexts/{apiId}/{collectionId}.jsonld` has to reside in the data directory.

The context document must contain the following entries, regardless of wether the context is provided by the API or via an external URI:

* `"@version": 1.1`
* `"geojson": "https://purl.org/geojson/vocab#"`
* `"FeatureCollection": "geojson:FeatureCollection"`
* `"features": { "@id": "geojson:features", "@container": "@set" }`
* `"Feature": "geojson:Feature"`
* `"type": "geojson:type"`
* `"properties": "@nest"`