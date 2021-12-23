# Supported specifications and technologies

## Web API Specifications

The following published standards and drafts of future standards for geospatial Web APIs or HTTP APIs are implemented in ldproxy:

* [OGC API - Features - Part 1: Core, Version 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0)
* [OGC API - Features - Part 2: Coordinate Reference Systems by Reference 1.0](http://www.opengis.net/doc/IS/ogcapi-features-2/1.0)
* [OGC API - Features - Part 3: Filtering, draft](https://docs.ogc.org/DRAFTS/19-079r1.html)
* [OGC API - Features - Part 4: Create, Replace, Update and Delete, draft](https://docs.ogc.org/DRAFTS/20-002.html)
* [OGC API - Tiles - Part 1: Core, draft](https://docs.ogc.org/DRAFTS/20-057.html)
* [OGC API - Styles, draft](https://docs.ogc.org/DRAFTS/20-009.html)
* [Common Query Language (CQL2), draft](https://docs.ogc.org/DRAFTS/21-0065.html)

The functionality implemented in lproxy differs from the current draft specifications since the drafts are changing.

Additionally, several ldproxy extensions are supported for which no official drafts are available at this time.

ldproxy is certified as [OGC reference implementation for "OGC API - Features - Part 1: Core" and "OGC API - Features - Part 2: Coordinate Reference Systems by Reference"](http://www.ogc.org/resource/products/details/?pid=1705).

<img src='https://cite.opengeospatial.org/teamengine/site/certification-logo.gif' alt='Certified OGC Compliant Logo' height='74' style='padding:0;margin:0;border:0;'/>

## Formats

ldproxy supports the following formats that are supported by existing tools and libraries:

* General resources
  * JSON
  * HTML
  * XML (only for resources specified in OGC API Features)
* API definitions
  * [OpenAPI 3.0](http://spec.openapis.org/oas/v3.0.3) as JSON, YAML and HTML
* Features
  * [GeoJSON](http://tools.ietf.org/rfc/rfc7946.txt)
  * [JSON-FG](https://github.com/opengeospatial/ogc-feat-geo-json)
  * HTML
  * [GML Simple Features Profile](http://portal.opengeospatial.org/files/?artifact_id=42729) (only for WFS feature providers)
* Tiles
  * [Mapbox Vector Tiles 2.1](https://github.com/mapbox/vector-tile-spec/tree/master/2.1)
  * PNG (only for MBTiles-Tile-Provider or Map Tiles)
  * JPEG (only for MBTiles-Tile-Provider or Map Tiles)
  * WebP (only for MBTiles-Tile-Provider or Map Tiles)
  * TIFF (only for MBTiles-Tile-Provider)
* Tile set descriptions
  * [TileJSON](https://github.com/mapbox/tilejson-spec)
* Style encodings
  * [Mapbox Styles](https://www.mapbox.com/mapbox-gl-js/style-spec/)

HTML can be annotated with [schema.org](https://schema.org/) markup ([schema:Place](https://schema.org/Place), [schema:Dataset](https://schema.org/Dataset) and [schema:DataCatalog](https://schema.org/DataCatalog)).

## Data Providers

All feature data in ldproxy is provided by feature providers, where each API has one provider. Two types of providers are supported:

* PostgreSQL databases (version 9.6 or later) with PostGIS (version 2.4 or later)
  * All tables of a provider must be in one database and one schema. Each table, except intermediate tables, must have a unique identifier column (primary key or with unique index). It is recommended to use integers for the identifiers for performance reasons.
* GeoPackage
* OGC Web Feature Services (WFS)
  * The WFS should support the query parameters `COUNT` and `STARTINDEX` in the GetFeature operation.
  
### Tiles

All tiles are provided by a tile provider.

Three types of tile providers are supported:

* Features (provided by the same API)
* MBTiles
* TileServer GL (only for Map Tiles)
