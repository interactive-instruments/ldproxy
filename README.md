# Share geospatial data via modern Web APIs

[![License](https://img.shields.io/badge/license-MPL%202.0-blue.svg)](http://mozilla.org/MPL/2.0/)
[![Build Status](https://travis-ci.org/interactive-instruments/ldproxy.svg?branch=master)](https://travis-ci.org/interactive-instruments/ldproxy)
![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/interactive-instruments/ldproxy?sort=semver)

ldproxy allows you to quickly set up Web APIs that make geospatial data available to others or to your own applications via HTTP.

Work on ldproxy started in 2015 to explore how existing [spatial data infrastructures](https://en.wikipedia.org/wiki/Spatial_data_infrastructure) could be improved. The design of ldproxy has been inspired by the parallel development of the [W3C/OGC Spatial Data on the Web Best Practices](https://www.w3.org/TR/sdw-bp/) as well as the [W3C Data on the Web Best Practices](https://www.w3.org/TR/dwbp/). Since then, ldproxy has provided input to the [Open Geospatial Consortium (OGC)](https://www.ogc.org/) and the emerging [OGC API Standards](https://ogcapi.ogc.org/). It is one of the most complete implementations of these specifications.

Key characteristics:

* **Easy to use**: The APIs support both JSON and HTML. Users of an API can use their favorite programming environment to access the data or simply use their browser.
* **Browseable**: All content is linked from the landing page of each API. A user can navigate through the API in any web browser and quickly get an impression of the data and the API capabilities. Search engines can index the data, too.
* **Linkable**: Each data item in the APIs has a stable URI and can be used in external links.
* **Based on standards**: ldproxy is a comprehensive implementation of the emerging [OGC API Standards](https://ogcapi.org/) and an increasing number of clients or libraries can use the APIs directly. This also applies to the supported formats returned by the APIs, e.g. GeoJSON, Mapbox Vector Tiles, Mapbox Styles or TileJSON. In addition, the APIs themselves are documented in a developer-friendly way via [OpenAPI 3.0](https://www.openapis.org/).
* **Certified**: ldproxy was certified as the first [OGC Reference Implementation](https://www.ogc.org/resource/products/details/?pid=1598) for [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0).
* **Open Source**: The source code is available under the [Mozilla Public License 2.0](http://mozilla.org/MPL/2.0/) on [GitHub](https://github.com/interactive-instruments/ldproxy).
* **Multiple Data Sources**: Currently two types of data sources are supported: [PostgreSQL](https://www.postgresql.org/) databases with the [PostGIS extension](https://postgis.net/) and [OGC Web Feature Services (WFS)](https://www.ogc.org/standards/wfs).
* **Extensible**: ldproxy is modular, written in Java 11 and designed to be extended to support your needs.

To get an idea how the APIs look like, have a look at the [demos](docs/en/demos.md).

More information on the supported specifications and technologies is available in [English](docs/en/specifications.md) and [German](docs/de/specifications.md).

## Getting started

The recommended environment is a current Linux or macOS operating system with docker. ldproxy is available on [Docker Hub](https://hub.docker.com/r/iide/ldproxy/). If you are new to Docker, have a look at the [Docker Documentation](https://docs.docker.com/).  

To install and start the lastest stable ldproxy version, just run the following command:

```bash
docker run -d -p 7080:7080 -v ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

For more information, have a look at the deployment guide ([English](docs/en/deployment.md), [German](docs/de/deployment.md)).

When your container is up and running, have a look at the configuration guide ([English](docs/en/configuration/README.md), [German](docs/de/configuration/README.md)).

To run ldproxy without docker, a 64-bit Java environment with Java 11 is recommended.

## Development

The only requirement is an installation of JDK 11. To set up a local development environment, follow these steps:

```bash
git clone https://github.com/interactive-instruments/ldproxy.git
cd ldproxy
git submodule update --init
./gradlew -PdownloadNode=true assemble
./gradlew run
```

That's it, a local server is running at port 7080.

You can also create a distribution by running `./gradlew distTar` or `./gradlew distZip`. The resulting archive can then be extracted on any machine with Java 11 and ldproxy can be started with one of the scripts under `ldproxy/bin/`.

Additional information will be documented in a developer and design documentation (work-in-progress).

<!--
## Community extensions

For additional extensions to ldproxy that are not part of the releases, see [(TODO)](https://github.com/interactive-instruments/ldproxy-community).

## Migrating from ldproxy v1.3 to v2.0

To migrate an existing deployment of version 1.3 to version 2.0 have a look at the at the [migration guide (TODO)](TODO).
-->
