# Geometry Simplification (GEOMETRY_SIMPLIFICATION)

The module *Geometry Simplification* may be enabled for every API with a feature provider. It adds the following query parameters:

* `maxAllowableOffset` (for resources *Features* and *Feature*): if set all geometries are simplified using the [Douglas Peucker algorithm](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm). The value defines the maximum distance between original and simplified geometry ([Hausdorff distance](https://en.wikipedia.org/wiki/Hausdorff_distance)). The value has to use the unit of the given coordinate reference system (`CRS84` or the value of parameter `crs`).

## Configuration

This module has no configuration options.
