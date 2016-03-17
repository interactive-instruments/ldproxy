# ldproxy

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Publish WFS services as Linked Data and as Web Sites using on-the-fly transformations

ldproxy was developed for [topic 4](https://github.com/geo4web-testbed/topic4-general) of the testbed [Spatial Data on the Web](http://www.geonovum.nl/onderwerp-artikel/testbed-locatie-data-het-web) organized by Geonovum.  
ldproxy provides web services, backed by WFS services, that are better suited for usage by non-geospatial experts, e.g. web developers, search engine crawlers and Linked Data experts.  
The implementation uses on-the-fly transformations, which means the generated HTML, JSON-LD and GeoJson representations are not persisted. They are created on the fly using live data from the WFS.

Have a look at the demo at http://www.ldproxy.net.

## Installation
ldproxy is available on [Docker Hub](https://hub.docker.com/r/iide/ldproxy/). If you are new to docker, read [this](https://docs.docker.com/linux/).  
To install ldproxy, just run the following command.

```bash
docker run -d -p 7080:7080 -v ldproxy_data:/ldproxy/data -w /ldproxy iide/ldproxy
```
Change the host port and volume bind directory to your needs. To update ldproxy, just remove the container and create a new one with the command above. Your data is saved in a volume, not in the container, so your service configurations will still be there after the update.

When your container is up and running, have a look at [Getting Started](https://github.com/interactive-instruments/ldproxy/doc/blob/master/00-getting-started.md)

## Development
The only requirement is an installation of JDK 7 or 8.  
To set up a local development environment, follow these steps:

```bash
git clone https://github.com/interactive-instruments/ldproxy.git
cd ldproxy
./gradlew build
./gradlew run
```

That's it, a local server is running at port 7080.

You can also create a distribution by running ```./gradlew distTar``` or ```./gradlew distZip```. The resulting archive can then be extracted on any machine with Java 7 or 8 and ldproxy can be started with one of the scripts under ```ldproxy/bin/```.
