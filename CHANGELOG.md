# Changelog

## v3.2.1 (16/03/2022)
*No changelog for this release.*

---

## v3.2.0 (15/03/2022)

#### Implemented enhancements

-  support OGC API Routes [#501](https://github.com/interactive-instruments/ldproxy/issues/501)
-  use PROJ for CRS transformations instead of Geotools [#439](https://github.com/interactive-instruments/ldproxy/issues/439)
-  update the CQL implementation [#432](https://github.com/interactive-instruments/ldproxy/issues/432)
-  add Sortables endpoint [#388](https://github.com/interactive-instruments/ldproxy/issues/388)

#### Improvements

-  improve bbox in tile queries [#594](https://github.com/interactive-instruments/ldproxy/issues/594)
-  replace OSGi/iPOJO with JPMS/Dagger [#577](https://github.com/interactive-instruments/ldproxy/issues/577)
-  improve exception handling/routing [#569](https://github.com/interactive-instruments/ldproxy/issues/569)
-  different coordinate precision for each axis [#402](https://github.com/interactive-instruments/ldproxy/issues/402)
-  support NTv2 coordinate conversions [#398](https://github.com/interactive-instruments/ldproxy/issues/398)
-  add optional strict config parsing [#261](https://github.com/interactive-instruments/ldproxy/issues/261)

#### Fixed bugs

-  basemap duplicated in the maplibre clients in html format [#588](https://github.com/interactive-instruments/ldproxy/issues/588)
-  API without feature provider starts successfully [#585](https://github.com/interactive-instruments/ldproxy/issues/585)
-  500 response for tile set resources of a collection [#575](https://github.com/interactive-instruments/ldproxy/issues/575)
-  500 response to a filter "string > 0" [#538](https://github.com/interactive-instruments/ldproxy/issues/538)

#### Dependency updates

-  update sqlite driver to 3.36.0.3 [#618](https://github.com/interactive-instruments/ldproxy/issues/618)
-  update postgresql driver to 42.3.3 [#617](https://github.com/interactive-instruments/ldproxy/issues/617)
-  update dropwizard to 2.0.38 [#616](https://github.com/interactive-instruments/ldproxy/issues/616)

---

## v3.1.0 (10/12/2021)

#### Implemented enhancements

-  query parameter "intersects" on "items" [#541](https://github.com/interactive-instruments/ldproxy/issues/541)
-  support POST on "items" [#540](https://github.com/interactive-instruments/ldproxy/issues/540)
-  add option to disable ST_ForcePolygonCCW() wrapper [#506](https://github.com/interactive-instruments/ldproxy/issues/506)
-  add tile matrix sets for ETRS89/UTM32 [#505](https://github.com/interactive-instruments/ldproxy/issues/505)
-  support self joins in the SQL feature provider [#503](https://github.com/interactive-instruments/ldproxy/issues/503)
-  add support for JSON-FG [#499](https://github.com/interactive-instruments/ldproxy/issues/499)
-  support for Maps (based on Vector Tiles and Styles) [#498](https://github.com/interactive-instruments/ldproxy/issues/498)
-  DB connection pool per server and user [#497](https://github.com/interactive-instruments/ldproxy/issues/497)
-  consolidate use of mapping libraries, use default style for all maps [#496](https://github.com/interactive-instruments/ldproxy/issues/496)
-  support Cesium JS as map client in HTML feature view [#494](https://github.com/interactive-instruments/ldproxy/issues/494)
-  support "now" as a temporal value in datetime/filter  [#481](https://github.com/interactive-instruments/ldproxy/issues/481)
-  support caching headers / conditional GET requests [#475](https://github.com/interactive-instruments/ldproxy/issues/475)
-  add option to disable the tile cache [#474](https://github.com/interactive-instruments/ldproxy/issues/474)
-  Tiles: add support for other tile formats [#471](https://github.com/interactive-instruments/ldproxy/issues/471)
-  add admin task to purge files from a tile cache [#469](https://github.com/interactive-instruments/ldproxy/issues/469)
-  support mbtiles as an additional option for the tile cache [#467](https://github.com/interactive-instruments/ldproxy/issues/467)
-  support GeoPackage and SQLite/SpatiaLite [#444](https://github.com/interactive-instruments/ldproxy/issues/444)
-  align Styles implementation with latest draft [#440](https://github.com/interactive-instruments/ldproxy/issues/440)
-  support for CQL array predicates [#431](https://github.com/interactive-instruments/ldproxy/issues/431)
-  generalize tiles to support other providers [#193](https://github.com/interactive-instruments/ldproxy/issues/193)

#### Improvements

-  improve OpenAPI documentation of Part 3 filter parameters [#535](https://github.com/interactive-instruments/ldproxy/issues/535)
-  add content-disposition headers [#513](https://github.com/interactive-instruments/ldproxy/issues/513)
-  avoid external resources in HTML [#495](https://github.com/interactive-instruments/ldproxy/issues/495)
-  support non-unique secondary sort keys [#488](https://github.com/interactive-instruments/ldproxy/issues/488)
-  support date/datetime values as sort keys and in CQL comparison predicates [#480](https://github.com/interactive-instruments/ldproxy/issues/480)
-  timestamps must conform to RFC 3339 [#472](https://github.com/interactive-instruments/ldproxy/issues/472)
-  broken HTTP header = silent failure [#463](https://github.com/interactive-instruments/ldproxy/issues/463)
-  align Tiles with the latest draft [#459](https://github.com/interactive-instruments/ldproxy/issues/459)
-  WFS improvements and bugfixes [#457](https://github.com/interactive-instruments/ldproxy/issues/457)
-  HTML: reactivate image URL handling [#455](https://github.com/interactive-instruments/ldproxy/issues/455)
-  support envelopes that span the anti-meridian [#447](https://github.com/interactive-instruments/ldproxy/issues/447)
-  HTML bbox filter editor improvements [#403](https://github.com/interactive-instruments/ldproxy/issues/403)
-  Schema: distinguish Date and DateTime properties [#335](https://github.com/interactive-instruments/ldproxy/issues/335)
-  queryables and datetime/bbox [#323](https://github.com/interactive-instruments/ldproxy/issues/323)
-  optimize cache for empty tiles [#302](https://github.com/interactive-instruments/ldproxy/issues/302)
-  add options for tile cache seeding [#278](https://github.com/interactive-instruments/ldproxy/issues/278)

#### Fixed bugs

-  set correct default CRS for 'crs', 'bbox-crs' and 'filter-crs' [#542](https://github.com/interactive-instruments/ldproxy/issues/542)
-  various minor issues identified during testing [#539](https://github.com/interactive-instruments/ldproxy/issues/539)
-  precision is not considered, if no other coordinate transformation is needed [#489](https://github.com/interactive-instruments/ldproxy/issues/489)
-  HTML exceptions includes fail  [#484](https://github.com/interactive-instruments/ldproxy/issues/484)
-  consider a filter on a provider type in the bbox computation [#465](https://github.com/interactive-instruments/ldproxy/issues/465)
-  feature collections cannot be disabled [#452](https://github.com/interactive-instruments/ldproxy/issues/452)

#### Dependency updates

-  upgrade dropwizard from 1.3 to 2.0 [#427](https://github.com/interactive-instruments/ldproxy/issues/427)

---

## v3.1.0-beta.1 (28/10/2021)

---

## v3.0.0 (07/05/2021)

#### Implemented enhancements

-  support full text search via q parameter [#420](https://github.com/interactive-instruments/ldproxy/issues/420)
-  reintroduce manager web app for easy configuration [#419](https://github.com/interactive-instruments/ldproxy/issues/419)
-  enhance HTML and schema.org representation [#405](https://github.com/interactive-instruments/ldproxy/issues/405)
-  update Filtering/CQL to Part 3, 1.0.0-draft.2 [#395](https://github.com/interactive-instruments/ldproxy/issues/395)
-  support sortby parameter [#386](https://github.com/interactive-instruments/ldproxy/issues/386)
-  add support for OGC API Records [#369](https://github.com/interactive-instruments/ldproxy/issues/369)
-  add support for JSON Schema draft-07 [#365](https://github.com/interactive-instruments/ldproxy/issues/365)
-  support auto-reload of entity configurations [#267](https://github.com/interactive-instruments/ldproxy/issues/267)
- support GeoPackage and SQLite/SpatiaLite [#444](https://github.com/interactive-instruments/ldproxy/issues/444)

#### Improvements

-  temporal filter improvements [#409](https://github.com/interactive-instruments/ldproxy/issues/409)
-  support option to reduce the number of decimal places [#399](https://github.com/interactive-instruments/ldproxy/issues/399)
-  accept limit values greater than the maximum value [#396](https://github.com/interactive-instruments/ldproxy/issues/396)
-  suppress selected properties in OgcApiCollection, if there are no spatial/temporal queryables [#372](https://github.com/interactive-instruments/ldproxy/issues/372)
-  make property references more tolerant/robust [#356](https://github.com/interactive-instruments/ldproxy/issues/356)
-  improve sql logging [#270](https://github.com/interactive-instruments/ldproxy/issues/270)
-  add admin task to change log level [#268](https://github.com/interactive-instruments/ldproxy/issues/268)
-  add admin task to reload entity configuration [#266](https://github.com/interactive-instruments/ldproxy/issues/266)
-  advanced database connection configuration [#264](https://github.com/interactive-instruments/ldproxy/issues/264)
-  add service startup validation [#263](https://github.com/interactive-instruments/ldproxy/issues/263)
-  add context information in log messages from the database (PostgreSQL) [#211](https://github.com/interactive-instruments/ldproxy/issues/211)

#### Fixed bugs

-  tile seeding fails if some type has no geometry [#429](https://github.com/interactive-instruments/ldproxy/issues/429)
-  connectionInfo configuration is not merged [#418](https://github.com/interactive-instruments/ldproxy/issues/418)
-  HTML layout COMPLEX_OBJECTS is incompatible with WFS providers [#414](https://github.com/interactive-instruments/ldproxy/issues/414)
-  OpenAPI 3.0: info.license.name is required [#393](https://github.com/interactive-instruments/ldproxy/issues/393)
-  not all links shown in Link header on the landing page [#391](https://github.com/interactive-instruments/ldproxy/issues/391)
-  error in Mapbox Style serialization [#390](https://github.com/interactive-instruments/ldproxy/issues/390)
-  integer enums in schemas result in NPE [#383](https://github.com/interactive-instruments/ldproxy/issues/383)
-  limit/offset ignored in some queries [#381](https://github.com/interactive-instruments/ldproxy/issues/381)
-  default CRS in spatial extents is EPSG 4326, not CRS84 [#371](https://github.com/interactive-instruments/ldproxy/issues/371)
-  exception when OAS30 is disabled [#366](https://github.com/interactive-instruments/ldproxy/issues/366)
-  GML output raises an exception [#244](https://github.com/interactive-instruments/ldproxy/issues/244)

#### Dependency updates

-  bump dropwizard from 1.3.24 to 1.3.29 [#426](https://github.com/interactive-instruments/ldproxy/issues/426)
-  update vector tiles dep 1.3.10 -> 1.3.13 [#361](https://github.com/interactive-instruments/ldproxy/issues/361)

---

## v2.5.1 (08/02/2021)

#### Fixed bugs

-  codelists return empty strings [#359](https://github.com/interactive-instruments/ldproxy/issues/359)

---

## v2.5.0 (04/02/2021)

#### Implemented enhancements

-  HTML: support string template filters in itemLabelFormat [#351](https://github.com/interactive-instruments/ldproxy/issues/351)
-  add optional mapping validation on startup [#260](https://github.com/interactive-instruments/ldproxy/issues/260)

#### Improvements

-  handle queryables that are not in the provider schema [#350](https://github.com/interactive-instruments/ldproxy/issues/350)
-  allow more characters in {featureId} [#349](https://github.com/interactive-instruments/ldproxy/issues/349)

#### Fixed bugs

-  GeoJSON: errors in complex object structures with arrays [#347](https://github.com/interactive-instruments/ldproxy/issues/347)
-  GeoJSON MultiPoint has extra array level [#344](https://github.com/interactive-instruments/ldproxy/issues/344)
-  double encode/decode in featureId [#343](https://github.com/interactive-instruments/ldproxy/issues/343)

---

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
