# API modules

The OGC API functionality is split up into modules based on the OGC API standards. The modules are classified according to the state of the implemented specification:

- For approved standards or drafts in the final voting stage, related modules are classified as `stable`. 
- For drafts in earlier stages, related modules are classified as `draft` (due to the dynamic nature of draft specifications, the implementation might not represent the current state at any time). 
- Furthermore there are external community modules classified as `experimental` which are not within the scope of this documentation.

There are some [general rules](general-rules.md) that apply to all modules.

<a name="api-module-overview"></a>

## Overview

|API module |Identifier |Classification |Enabled by default? |Description
| --- | --- | --- | --- | ---
|[Foundation](foundation.md) |FOUNDATION |stable |Yes |Base functionality
|[Common Core](common.md) |COMMON |stable |Yes |Resources *Landing Page*, *Conformance Declaration* and *API Definition*
|[HTML](html.md) |HTML |stable |Yes |Enables HTML encoding for resources without more specific encodings
|[JSON](json.md) |JSON |stable |Yes |Enables JSON encoding for resources without more specific encodings
|[XML](xml.md) |XML |stable |No |Enables XML encoding for resources without more specific encodings (if implemented)
|[OpenAPI 3.0](oas30.md) |OAS30 |stable |Yes |Enables API definitions according to OpenAPI 3.0
|[Feature Collections](collections.md) |COLLECTIONS |stable |Yes |Resources *Feature Collections* and *Feature Collection*
|[Features Core](features-core.md) |FEATURES_CORE |stable |Yes |Resources *Features* and *Feature*
|[Features HTML](features-html.md) |FEATURES_HTML |stable |Yes |Enables HTML encoding for resources *Features* and *Feature*
|[Features GeoJSON](geojson.md) |GEO_JSON |stable |Yes |Enables GeoJSON encoding for resources *Features* and *Feature*
|[Features GML](gml.md) |GML |stable |No |Enables GML encoding for resources *Features* and *Feature* (only for WFS provider)
|[Coordinate Reference Systems](crs.md) |CRS |stable |Yes |Enables support for coordinate reference systems other than CRS84
|[Collections Queryables](queryables.md) |QUERYABLES |draft |No |Subresource *Queryables* for resource *Feature Collection*
|[Collections Schema](schema.md) |SCHEMA |draft |No |Subresource *Schema* for resource *Feature Collection*
|[Features GeoJSON-LD](geojson-ld.md) |GEO_JSON_LD |draft |No |Enables JSON-LD extensions for GeoJSON encoding
|[Filter / CQL](filter.md) |FILTER |draft |No |Enables CQL filters for resources *Features* and *Vector Tiles*
|[Geometry Simplification](geometry-simplification.md) |GEOMETRY_SIMPLIFICATION |draft |No |Enables simplification of geometries according to Douglas-Peucker for resources *Features* and *Feature*
|[Projections](projections.md) |PROJECTIONS |draft |No |Enables limitation of returned feature properties for resources *Features*, *Feature* and *Vector Tiles*
|[Sorting](sorting.md) |SORTING |draft |No |Enables the option for sorting the returned features at the *Features* resource
|[Styles](styles.md) |STYLES |draft |No |Enables support for styles (*Mapbox Style* oder *SLD*) and related resources (symbols, sprites)
|[Vector Tiles](tiles.md) |TILES |draft |No |Enables support for vector tiles (*Mapbox Vector Tiles*) for the whole dataset and/or single collections
|[Simple Transactions](transactional.md) |TRANSACTIONAL |draft |No |Enables feature mutations using HTTP methods POST/PUT/DELETE/PATCH
