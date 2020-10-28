# Changelog

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
