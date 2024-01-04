# Introduction

ldproxy makes geospatial data [findable, accessible, interoperable, and reusable](https://www.go-fair.org/fair-principles/) via modern web APIs.

The software is characterized by the following features:

* **Easy to use**: The APIs support both JSON and HTML. Users of an API can use their favorite programming environment to access the data or simply use their browser.
* **Browseable**: All content is linked from the landing page of each API. A user can navigate through the API in any web browser and quickly get an impression of the data and the API capabilities. Search engines can index the data, too.
* **Linkable**: Each data item in the APIs has a stable URI and can be used in external links.
* **Based on standards**: ldproxy is a comprehensive implementation of the emerging [OGC API Standards](https://ogcapi.ogc.org/) and an increasing number of clients or libraries can use the APIs directly. This also applies to the supported formats returned by the APIs, e.g. GeoJSON, Mapbox Vector Tiles, Mapbox Styles or TileJSON. In addition, the APIs themselves are documented in a developer-friendly way via [OpenAPI 3.0](https://www.openapis.org/).
* **Certified**: ldproxy is a certified [OGC Reference Implementation](https://www.ogc.org/resource/products/details/?pid=1705) for [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0) and [OGC API - Features - Part 2: Coordinate Reference Systems by Reference 1.0](http://www.opengis.net/doc/IS/ogcapi-features-2/1.0).
* **Open Source**: The source code is available under the [Mozilla Public License 2.0](http://mozilla.org/MPL/2.0/) on [GitHub](https://github.com/interactive-instruments/ldproxy).
* **Multiple Data Sources**: Currently three types of data sources are supported: [PostgreSQL](https://www.postgresql.org/) databases with the [PostGIS extension](https://postgis.net/), [GeoPackage](https://www.geopackage.org) and [OGC Web Feature Services (WFS)](https://www.ogc.org/standards/wfs).
* **Extensible**: ldproxy is modular, written in Java (supported versions: 11 and 17) and designed to be extended to support your needs.

To get an idea how the APIs look like, have a look at the [Demo APIs](https://demo.ldproxy.net).

More information on the supported specifications and technologies is available in [English](https://docs.ldproxy.net/advanced/specifications.html) and [German](https://docs.ldproxy.net/de/advanced/specifications.html).
