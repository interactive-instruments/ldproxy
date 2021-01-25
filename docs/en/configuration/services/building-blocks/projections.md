# Projections (PROJECTIONS)

The module *Projections* may be enabled for every API with a feature provider. It adds the following query parameters:

* `properties` (for resources *Features*, *Feature* and *Vector Tile*): if set only the given properties are included in the output. Only applies to GeoJSON  `properties` and Mapbox Vector Tiles `tags`.
* `skipGeometry` (for resources *Features* and *Feature*): if set to `true`, geometries will be skipped in the output.<br>_since version 2.2_

## Configuration

This module has no configuration options.
