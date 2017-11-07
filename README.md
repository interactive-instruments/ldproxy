# ldproxy

[![License](https://img.shields.io/badge/license-MPL%202.0-blue.svg)](http://mozilla.org/MPL/2.0/)

**Enhanced usability for existing WFS services**

Did you ever wish you could access WFS services with a simple RESTful JSON API? Or that you could just browse the data to find out if it is interesting for you?

ldproxy is an adapter that sits in front of existing WFS services and provides a simple RESTful API and additional output formats like GeoJson, HTML and JSON-LD. These representations are created on the fly using live data from the WFS.

ldproxy was designed with the goal to enhance existing WFS services with the ideas from the [Spatial Data on the Web Best Practices](https://www.w3.org/TR/sdw-bp/) as well as the [Data on the Web Best Practices](https://www.w3.org/TR/dwbp/) developed by the W3C. In the meantime the OGC published the first draft of the [WFS 3.0 specification](https://cdn.rawgit.com/opengeospatial/WFS_FES/master/docs/17-069.html), which also builds on these best practices and is mostly implemented by ldproxy.

Have a look at the demo at http://www.ldproxy.net.

## Installation
ldproxy is available on [Docker Hub](https://hub.docker.com/r/iide/ldproxy/). If you are new to Docker, have a look at the  [Docker Documentation](https://docs.docker.com/).  
To install ldproxy, just run the following command.

```bash
docker run -d -p 7080:7080 -v ldproxy_data:/ldproxy/data iide/ldproxy
```
For more information, have a look at the [deployment guide](http://interactive-instruments.github.io/ldproxy/manual/00-deployment.html).

When your container is up and running, have a look at the [documentation](http://interactive-instruments.github.io/ldproxy/).

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
