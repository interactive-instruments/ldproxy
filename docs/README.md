# ldproxy

#### Enhanced usability for existing WFS services

Did you ever wish you could access WFS services with a simple RESTful JSON API? Or that you could just click your way through the data in the browser to find out if it is interesting for you?

ldproxy is an adapter that sits in front of existing WFS services and provides a simple RESTful API and additional output formats like GeoJson, HTML and JSON-LD. These representations are created on the fly using live data from the WFS.

ldproxy was designed with the goal to enhance existing WFS services with the ideas from the [Spatial Data on the Web Best Practices](https://www.w3.org/TR/sdw-bp/) as well as the [Data on the Web Best Practices](https://www.w3.org/TR/dwbp/) developed by the W3C. In the meantime the OGC published the first draft of the [WFS 3.0 specification](https://cdn.rawgit.com/opengeospatial/WFS_FES/3.0.0-draft.1/docs/17-069.html), which also builds on these best practices and is mostly implemented by ldproxy.

An example service is published with [Open Data from North-Rhine Westphalia](https://www.ldproxy.nrw.de/rest/services/), the source code is on [GitHub](https://github.com/interactive-instruments/ldproxy). ldproxy is available under the [MPL 2.0](https://www.mozilla.org/en-US/MPL/2.0/).


