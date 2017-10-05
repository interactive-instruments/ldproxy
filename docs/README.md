# ldproxy

Publish WFS services as Linked Data and as Web Sites using on-the-fly transformations.

ldproxy provides web services, backed by WFS services, that are better suited for usage by non-geospatial experts, e.g. web developers, search engine crawlers and Linked Data experts.
The implementation uses on-the-fly transformations, which means the generated HTML, JSON-LD and GeoJson representations are not persisted. They are created on the fly using live data from the WFS.