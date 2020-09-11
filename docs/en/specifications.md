# Supported specifications and technologies

## Web API Specifications

The following published standards and drafts of future standards for geospatial Web APIs or HTTP APIs are implemented in ldproxy:

* [OGC API - Features - Part 1: Core, Version 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0)
* [OGC API - Features - Part 2: Coordinate Reference Systems by Reference, draft](http://docs.opengeospatial.org/DRAFTS/18-058.html)
* [OGC API - Features - Part 3: Common Query Language, draft](http://docs.opengeospatial.org/DRAFTS/19-079.html)
* [OGC API - Features - Part 4: Simple Transactions, draft](http://docs.opengeospatial.org/DRAFTS/20-002.html)
* [OGC API - Tiles - Part 1: Core, draft](https://htmlpreview.github.io/?https://github.com/opengeospatial/OGC-API-Tiles/blob/master/core/standard/OAPI_Tiles.html)
* [OGC API - Styles, draft](http://docs.opengeospatial.org/DRAFTS/20-009.html)

The functionality implemented in lproxy differs in some cases from the current draft specifications since the drafts are changing.

Additionally, several ldproxy extensions are supported for which no official drafts are available at this time.

ldproxy is certified as [OGC reference implementation for "OGC API - Features - Part 1: Core"](http://www.ogc.org/resource/products/details/?pid=1598).

<img src='https://portal.ogc.org/public_ogc/compliance/Certified_OGC_Compliant_Logo_Web.gif' alt='Certified OGC Compliant Logo' height='74' style='padding:0;margin:0;border:0;'/>

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
  * HTML
  * [GML Simple Features Profile](http://portal.opengeospatial.org/files/?artifact_id=42729) (only for WFS feature providers)
* Vector tiles
  * [Mapbox Vector Tiles 2.1](https://github.com/mapbox/vector-tile-spec/tree/master/2.1)
* Tile set descriptions
  * [TileJSON](https://github.com/mapbox/tilejson-spec)
* Style encodings
  * [Mapbox Styles](https://www.mapbox.com/mapbox-gl-js/style-spec/)

HTML can be annotated with [schema.org](https://schema.org/) markup ([schema:Place](https://schema.org/Place), [schema:Dataset](https://schema.org/Dataset) and [schema:DataCatalog](https://schema.org/DataCatalog)).

## Data Providers

All feature data in ldproxy is provided by feature providers, where each API has one provider. Two types of providers are supported:

* PostgreSQL databases (version 9.6 or later) with PostGIS (version 2.4 or later)
  * All tables of a provider must be in one database and one schema. Each table, except intermediate tables, must have a unique identifier column (primary key or with unique index). It is recommended to use integers for the identifiers for performance reasons.
* OGC Web Feature Services (WFS)
  * The WFS should support the query parameters `COUNT` and `STARTINDEX` in the GetFeature operation.
