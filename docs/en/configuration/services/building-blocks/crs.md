# Coordinate Reference Systems (CRS)

The module *Coordinate Reference Systems* may be enabled for every API with a feature provider. It provides support for additional coordinate reference systems apart from the default [CRS84](http://www.opengis.net/def/crs/OGC/1.3/CRS84) (WGS 84).

All transformations between two coordinate reference systems are handled by *Geotools*. If multiple transformations are available, *Geotools* decides which one to use. Transformations are currently not configurable.

*Coordinate Reference Systems* implements all requirements of conformance class *Coordinate Reference System by Reference* of [OGC API - Features - Part 2: Coordinate Reference System by Reference 1.0.0-draft.1](http://docs.opengeospatial.org/DRAFTS/18-058.html).

## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`additionalCrs` |object |`{}` |Add additonal coordinate reference systems to an API or a collection.

### Example

```yaml
- buildingBlock: CRS
  additionalCrs:
  - code: 25832
    forceAxisOrder: NONE
  - code: 4258
    forceAxisOrder: NONE
  - code: 4326
    forceAxisOrder: NONE
```
