# Features Core

The module *Features Core* has to be enabled for every API with a feature provider. It provides the resources *Features* and *Feature*.

*Features Core* implements all requirements of conformance class *Core* of [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) for the two mentioned resources.

|Resource |Path |HTTP Method |Media Types
| --- | --- | --- | ---
|Features |`/{apiId}/collections/{collectionId}/items` |GET |[GeoJSON](geojson.md), [HTML](features-html.md), [GML](gml.md)
|Feature |`/{apiId}/collections/{collectionId}/items/{featureId}` |GET |[GeoJSON](geojson.md), [HTML](features-html.md), [GML](gml.md)

## Configuration for API

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`defaultCrs` |string |`CRS84` |Default coordinate reference system, either `CRS84` for datasets with 2D geometries or `CRS84h` for datasets with 3D geometries.
|`minimumPageSize` |int |1 |Minimum value for parameter `limit`.
|`defaultPageSize` |int |10 |Default value for parameter `limit`.
|`maximumPageSize` |int |10000 |Maximum value for parameter `limit`.
|`featureProvider` |string | API id |Id of the feature provider to use. Normally the feature provider and API ids are the same.
|`showsFeatureSelfLink` |boolean |`false` |Always add `self` link to features, even in the *Features* resource.

### Example

```yaml
- buildingBlock: FEATURES_CORE
  defaultCrs: CRS84
  minimumPageSize: 1
  defaultPageSize: 10
  maximumPageSize: 10000
  showsFeatureSelfLink: true
```

## Configuration for collection

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`featureType` |string | collection id |Id of the feature type to use as defined in the given feature provider. Normally the feature type and collection ids are the same. 
|`queryables` |object |`{}` |Feature properties that can be used in queries to select the returned features, split into `spatial`, `temporal` and `other`. Properties in `spatial` have to be of type `GEOMETRY` in the provider, properties in `temporal` of type `DATETIME`. Properties are listed in an array by name. Queryables can be used in filter expressions ([Filter - CQL](filter.md)) or as filter parameters according to [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0). The parameter [bbox](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_bbox) acts on the first spatial property. The parameter [datetime](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_datetime) acts on the first two temporal properties, which are interpreted as start and end of an interval. If only one temporal property is given, it is interpreted as instant. Other properties are added as [additional parameters](http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_parameters_for_filtering_on_feature_properties) for the collection ("*" can be used as wildcard). Using the described parameters allows selection of features without additional modules.
|`transformations` |object |`{}` |Optional transformations for feature properties for all media types, see [transformations](general-rules.md#transformations).

### Example

```yaml
    - buildingBlock: FEATURES_CORE
      queryables:
        spatial:
        - geometry
        temporal:
        - beginLifespanVersion
        - endLifespanVersion
        other:
        - name
        - pointOfContact.address.postCode
        - pointOfContact.address.adminUnit
        - pointOfContact.telephoneVoice
      transformations:
        pointOfContact.telephoneVoice:
          null: 'bitte ausfüllen'
        inspireId:
          stringFormat: 'https://example.com/id/kinder/kita/{{value}}'
        pointOfContact.address.thoroughfare:
          stringFormat: "{{value | replace:'\\s*[0-9].*$':''}}"
        pointOfContact.address.locatorDesignator:
          null: '^\\D*$'
          stringFormat: "{{value | replace:'^[^0-9]*':''}}"
```
