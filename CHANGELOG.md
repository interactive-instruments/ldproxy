# Changelog

## v2.4.0 (10/12/2020)

#### Implemented enhancements

-  support temporal extent computation [#174](https://github.com/interactive-instruments/ldproxy/issues/174)

#### Improvements

-  Tiles: explain use of tiles in HTML [#339](https://github.com/interactive-instruments/ldproxy/issues/339)
-  JSON Schema: use 2019-09 instead of draft-07 [#336](https://github.com/interactive-instruments/ldproxy/issues/336)
-  queryables resource as a JSON Schema [#333](https://github.com/interactive-instruments/ldproxy/issues/333)

#### Fixed bugs

-  GeoJSON: id in properties object [#337](https://github.com/interactive-instruments/ldproxy/issues/337)
-  errors in feature schema (returnables) [#334](https://github.com/interactive-instruments/ldproxy/issues/334)

---

## v2.3.0 (24/11/2020)

#### Implemented enhancements

-  make web map popups configurable [#318](https://github.com/interactive-instruments/ldproxy/issues/318)
-  add support for a layer control in web maps [#317](https://github.com/interactive-instruments/ldproxy/issues/317)
-  make thresholds in tile generation configurable [#316](https://github.com/interactive-instruments/ldproxy/issues/316)
-  add support for including only a subsets of the feature properties in tiles [#315](https://github.com/interactive-instruments/ldproxy/issues/315)
-  add support for merging polygons in tiles that intersect [#314](https://github.com/interactive-instruments/ldproxy/issues/314)

#### Improvements

-  various improvements to vector tiles and styles [#310](https://github.com/interactive-instruments/ldproxy/issues/310)
-  return CORS headers also for "Sec-Fetch-Mode" headers with value "cors" [#309](https://github.com/interactive-instruments/ldproxy/issues/309)
-  improve deserialization error messages [#305](https://github.com/interactive-instruments/ldproxy/issues/305)
-  feature id filters do not work with some WFS servers [#296](https://github.com/interactive-instruments/ldproxy/issues/296)
-  datetime parameter with intervals does not work with string columns [#282](https://github.com/interactive-instruments/ldproxy/issues/282)

#### Fixed bugs

-  allow non-word characters in style identifiers [#320](https://github.com/interactive-instruments/ldproxy/issues/320)
-  incorrect treatment of MultiPolygon geometries in tiles [#319](https://github.com/interactive-instruments/ldproxy/issues/319)
-  datetime returns an error, if no temporal queryable has been configured [#307](https://github.com/interactive-instruments/ldproxy/issues/307)
-  entities with mismatching filename and id are not rejected [#304](https://github.com/interactive-instruments/ldproxy/issues/304)
-  date formatting fails when timestamp has fractional seconds [#303](https://github.com/interactive-instruments/ldproxy/issues/303)
-  406 (Not Acceptable) error message incorrect [#301](https://github.com/interactive-instruments/ldproxy/issues/301)
-  quotes in title and description on HTML landing page  [#299](https://github.com/interactive-instruments/ldproxy/issues/299)
-  feature id filters are rejected [#298](https://github.com/interactive-instruments/ldproxy/issues/298)

#### Dependency updates

-  bump postgresql driver from 42.2.16 to 42.2.18 [#306](https://github.com/interactive-instruments/ldproxy/issues/306)

---

## v2.2.0 (27/10/2020)

#### Implemented enhancements

-  make tile geometry processing more robust [#281](https://github.com/interactive-instruments/ldproxy/issues/281)
-  add option to drop geometries in feature responses [#223](https://github.com/interactive-instruments/ldproxy/issues/223)

#### Improvements

-  support dots in feature ids [#291](https://github.com/interactive-instruments/ldproxy/issues/291)
-  improve term/stop signal handling [#271](https://github.com/interactive-instruments/ldproxy/issues/271)
-  improve logging context [#269](https://github.com/interactive-instruments/ldproxy/issues/269)
-  optimize service background task handling [#262](https://github.com/interactive-instruments/ldproxy/issues/262)

#### Fixed bugs

-  Tiles: properties that are numbers are written as strings in MVT files [#284](https://github.com/interactive-instruments/ldproxy/issues/284)
-  Tiles: feature id is not set  [#283](https://github.com/interactive-instruments/ldproxy/issues/283)
-  Seeding too many collections/tiles [#280](https://github.com/interactive-instruments/ldproxy/issues/280)
-  layer zoom levels not always taken into account in multi-layer tiles [#275](https://github.com/interactive-instruments/ldproxy/issues/275)
-  FeatureTransformerHtml exception, if dimension is null [#273](https://github.com/interactive-instruments/ldproxy/issues/273)
-  deadlock in sql queries with small maxThreads [#265](https://github.com/interactive-instruments/ldproxy/issues/265)

#### Dependency updates

- bump swagger-ui from 3.17.6 to 3.36.0

---

## v2.1.0 (27/10/2020)

#### Implemented enhancements

-  support style without explicit metadata [#241](https://github.com/interactive-instruments/ldproxy/issues/241)
-  Support grouping of APIs [#233](https://github.com/interactive-instruments/ldproxy/issues/233)

#### Fixed bugs

-  Log errors, if vector tile seeding is not enabled [#249](https://github.com/interactive-instruments/ldproxy/issues/249)
-  CLASSIC layout does not support labels from the feature provider schema [#248](https://github.com/interactive-instruments/ldproxy/issues/248)
-  unknown featureId should return a 404 [#246](https://github.com/interactive-instruments/ldproxy/issues/246)
-  instance in exception is incorrect, if externalUrl is set [#243](https://github.com/interactive-instruments/ldproxy/issues/243)
-  every startup task is started twice [#237](https://github.com/interactive-instruments/ldproxy/issues/237)
-  featureType not always taken into account [#236](https://github.com/interactive-instruments/ldproxy/issues/236)
-  Improve error logging [#234](https://github.com/interactive-instruments/ldproxy/issues/234)
-  fix link title [#230](https://github.com/interactive-instruments/ldproxy/issues/230)
-  remove transformations do not work for vector tiles [#229](https://github.com/interactive-instruments/ldproxy/issues/229)
-  Error in Feature resource requests with WFS backends [#228](https://github.com/interactive-instruments/ldproxy/issues/228)

---

## v2.0.0 (11/09/2020)

ldproxy 2.0.0 is a major update from the previous version 1.3.
