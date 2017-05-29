# ldproxy

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Publish WFS services as Linked Data and as Web Sites using on-the-fly transformations

ldproxy was developed for [topic 4](https://github.com/geo4web-testbed/topic4-general) of the testbed [Spatial Data on the Web](http://www.geonovum.nl/onderwerp-artikel/testbed-locatie-data-het-web) organized by Geonovum.  
ldproxy provides web services, backed by WFS services, that are better suited for usage by non-geospatial experts, e.g. web developers, search engine crawlers and Linked Data experts.  
The implementation uses on-the-fly transformations, which means the generated HTML, JSON-LD and GeoJson representations are not persisted. They are created on the fly using live data from the WFS.

Have a look at the demo at http://www.ldproxy.net.

## Installation
ldproxy is available on [Docker Hub](https://hub.docker.com/r/iide/ldproxy/). If you are new to Docker, have a look at the  [Docker Documentation](https://docs.docker.com/).  
To install ldproxy, just run the following command.

```bash
docker run -d -p 7080:7080 -v ldproxy_data:/ldproxy/data iide/ldproxy
```
For more information, have a look at the [deployment guide](docs/manual/00-deployment.md).

When your container is up and running, have a look at [managing services](docs/manual/01-managing-services.md).

## Development
The only requirement is an installation of JDK 8.  
To set up a local development environment, follow these steps:

```bash
git clone https://github.com/interactive-instruments/ldproxy.git
cd ldproxy
./gradlew build
./gradlew run
```

That's it, a local server is running at port 7080.

You can also create a distribution by running ```./gradlew distTar``` or ```./gradlew distZip```. The resulting archive can then be extracted on any machine with Java 8 and ldproxy can be started with one of the scripts under ```ldproxy/bin/```.
