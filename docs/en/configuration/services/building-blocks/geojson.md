# Features GeoJSON (GEO_JSON)

The module *Features GeoJSON* may be enabled for every API with a feature provider. It provides the resources *Features* and *Feature* encoded as GeoJSON.

*Features GeoJSON* implements all requirements of conformance class *GeoJSON* from [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_geojson) for the two mentioned resources.

## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`nestedObjectStrategy` |enum |`NEST` |*Deprecated* Use the [`flatten` transformation](../../providers/transformations.md) instead.
|`multiplicityStrategy` |enum |`ARRAY` |*Deprecated* Use the [`flatten` transformation](../../providers/transformations.md) instead.
|`separator` |string |"." |*Deprecated* Use the [`flatten` transformation](../../providers/transformations.md) instead.
|`transformations` |object |`{}` |Optional transformations for feature properties for GeoJSON, see [transformations](README.md#transformations).

### Examples

#### Nested Feature

```yaml
- buildingBlock: GEO_JSON
```

```json
{
  "type" : "Feature",
  "id" : "1",
  "geometry" : {
    "type" : "Point",
    "coordinates" : [ 7.0, 50.0 ]
  },
  "properties" : {
    "name" : "Beispiel",
    "inspireId" : "https://example.org/id/soziales/kindergarten/1",
    "serviceType" : {
      "title" : "Kinderbetreuung",
      "href" : "http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService"
    },
    "pointOfContact" : {
      "address" : {
        "thoroughfare" : "Beispielstr.",
        "locatorDesignator" : "123",
        "postCode" : "99999",
        "adminUnit" : "Irgendwo"
      },
      "telephoneVoice" : "0211 16021740"
    },
    "occupancy" : [ {
      "typeOfOccupant" : "vorschule",
      "numberOfOccupants" : 20
    }, {
      "typeOfOccupant" : "schulkinder",
      "numberOfOccupants" : 25
    } ]
  }
}
```

#### Flattened Feature

```yaml
- buildingBlock: GEO_JSON
  transformations:  
    '*':
      flatten: '.'
```

```json
{
  "type" : "Feature",
  "id" : "1",
  "geometry" : {
    "type" : "Point",
    "coordinates" : [ 7.0, 50.0 ]
  },
  "properties" : {
    "name" : "Beispiel",
    "inspireId" : "https://example.org/id/soziales/kindergarten/1",
    "serviceType.title" : "Kinderbetreuung",
    "serviceType.href" : "http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService",
    "pointOfContact.address.thoroughfare" : "Otto-Pankok-Str.",
    "pointOfContact.address.locatorDesignator" : "29",
    "pointOfContact.address.postCode" : "40231",
    "pointOfContact.address.adminUnit" : "Düsseldorf",
    "pointOfContact.telephoneVoice" : "0211 16021740",
    "occupancy.1.typeOfOccupant" : "vorschule",
    "occupancy.1.numberOfOccupants" : 20,
    "occupancy.2.typeOfOccupant" : "schulkinder",
    "occupancy.2.numberOfOccupants" : 25
  }
}
```
