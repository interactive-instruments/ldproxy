# Changelog

## v3.6.3 (11/03/2024)
#### Fixed bugs

-  Tiles: various seeding issues [#1177](https://github.com/interactive-instruments/ldproxy/issues/1177)
-  CRUD: inserting (Multi)Polygons may fail [#1176](https://github.com/interactive-instruments/ldproxy/issues/1176)
-  using additional store source as tile cache may fail [#1175](https://github.com/interactive-instruments/ldproxy/issues/1175)
-  CRUD: inserting geometries with schema type `ANY` fails [#1167](https://github.com/interactive-instruments/ldproxy/issues/1167)
-  legend icon for `fill-pattern` does not work [#1165](https://github.com/interactive-instruments/ldproxy/issues/1165)

---

## v3.6.2 (09/02/2024)
#### Fixed bugs

-  CRUD: PUT/PATCH hangs after multiple requests [#1164](https://github.com/interactive-instruments/ldproxy/issues/1164)
-  avoid unnecessary generation of endpoint definitions [#1161](https://github.com/interactive-instruments/ldproxy/issues/1161)
-  rename transformation does not support objects [#1160](https://github.com/interactive-instruments/ldproxy/issues/1160)
-  bounding box returning 502 error for large polygon datasets [#1157](https://github.com/interactive-instruments/ldproxy/issues/1157)
-  tiles: various seeding issues [#1155](https://github.com/interactive-instruments/ldproxy/issues/1155)
-  concat/coalesce: additional cases do not work as expected [#1154](https://github.com/interactive-instruments/ldproxy/issues/1154)
-  datetime queries fails [#1099](https://github.com/interactive-instruments/ldproxy/issues/1099)

---

## v3.6.1 (12/01/2024)

#### Improvements

-  PostGIS: support geometry columns with curves [#1119](https://github.com/interactive-instruments/ldproxy/issues/1119)

#### Fixed bugs

-  CRUD operations ignore feature content [#1148](https://github.com/interactive-instruments/ldproxy/issues/1148)
-  OpenId Connect callback might not work behind reverse proxy [#1147](https://github.com/interactive-instruments/ldproxy/issues/1147)
-  auto mode does not generate any types [#1146](https://github.com/interactive-instruments/ldproxy/issues/1146)
-  Projections: feature references are ignored [#1144](https://github.com/interactive-instruments/ldproxy/issues/1144)
-  feature reference included on null value [#1139](https://github.com/interactive-instruments/ldproxy/issues/1139)
-  XACML: obligations are ignored [#1136](https://github.com/interactive-instruments/ldproxy/issues/1136)
-  Tiles: Issues in the edge case that tiles are only enabled for selected collections [#1134](https://github.com/interactive-instruments/ldproxy/issues/1134)
-  HTML: Incorrect paging links, if query parameters use non-ASCII chars  [#1120](https://github.com/interactive-instruments/ldproxy/issues/1120)
-  significant increase in memory consumption after reloading configs via tasks/reload-entities [#1088](https://github.com/interactive-instruments/ldproxy/issues/1088)

---

## v3.6.0 (08/12/2023)

> [!IMPORTANT]
> This is definitely the last feature release for `v3.x`. The upcoming major release `v4.0` will bring some breaking changes:
> - All configurations options marked as deprecated will be removed (#1113).
> - The current data directory layout is deprecated and will no longer be supported. It is superseded by the new [store](https://docs.ldproxy.net/application/20-configuration/10-store-new.html) (#1114).
> - The manager is deprecated and will be removed (#1116). 
> - Custom style metadata is deprecated and will be removed (#1115). 
> 
> To prepare your configurations for the upcoming changes, check out the [upgrade documentation](https://docs.ldproxy.net/application/40-upgrades.html#configuration).

#### Implemented enhancements

-  support re-sorting of json column properties [#1089](https://github.com/interactive-instruments/ldproxy/issues/1089)
-  support queryables/sortables that are not returnables [#1084](https://github.com/interactive-instruments/ldproxy/issues/1084)
-  support title in feature reference links [#977](https://github.com/interactive-instruments/ldproxy/issues/977)
-  schema enhancements [#974](https://github.com/interactive-instruments/ldproxy/issues/974)
-  support s3 store sources [#878](https://github.com/interactive-instruments/ldproxy/issues/878)

#### Improvements

-  upgrade Schema to OGC API Features Part 5 [#1092](https://github.com/interactive-instruments/ldproxy/issues/1092)
-  CRUD cannot be supported with GPKG providers [#1079](https://github.com/interactive-instruments/ldproxy/issues/1079)
-  improve storage strategies [#1077](https://github.com/interactive-instruments/ldproxy/issues/1077)
-  upgrade JSON-FG to version 0.2 [#1065](https://github.com/interactive-instruments/ldproxy/issues/1065)
-  collections without a tileset in the tile provider (part 2) [#1005](https://github.com/interactive-instruments/ldproxy/issues/1005)

#### Fixed bugs

-  typo in API maturity statement in OpenAPI document [#1097](https://github.com/interactive-instruments/ldproxy/issues/1097)
-  coalesce and arrays [#1076](https://github.com/interactive-instruments/ldproxy/issues/1076)
-  DEBUG log messages despite logging.level set to INFO [#1075](https://github.com/interactive-instruments/ldproxy/issues/1075)
-  incorrect handling of object array properties [#1071](https://github.com/interactive-instruments/ldproxy/issues/1071)
-  GeoJSON: requesting profiles rel-as-key/uri throws an exception [#1069](https://github.com/interactive-instruments/ldproxy/issues/1069)
-  java.lang.NoClassDefFoundError: org/jsoup/Jsoup [#1068](https://github.com/interactive-instruments/ldproxy/issues/1068)

#### Dependency updates

-  upgrade sqlite-jdbc from 3.40.0.0 to 3.41.2.2 [#1111](https://github.com/interactive-instruments/ldproxy/issues/1111)
-  upgrade PROJ from 9.1.0 to 9.3.0 [#1086](https://github.com/interactive-instruments/ldproxy/issues/1086)
-  upgrade dropwizard from 3.0.1 to 3.0.4 [#1074](https://github.com/interactive-instruments/ldproxy/issues/1074)

---
## v3.5.0 (05/10/2023)

> [!IMPORTANT]
> This is likely the last feature release for `v3.x`. The upcoming major release `v4.0` will bring some breaking changes:
> - All configurations options marked as deprecated will be removed.
> - The current data directory layout is deprecated and will no longer be supported. It is superseded by the new [store concept](https://docs.ldproxy.net/application/41-store-new.html).
> - The manager is deprecated and will be removed. There will be a replacement with a focus on the automatic creation of configurations that is currently part of the manager, but other features might be discontinued.
> 
> To prepare your configurations for the upcoming changes, check out the [migration documentation](https://docs.ldproxy.net/migration/).

#### Implemented enhancements

-  support queryables/sortables in JSON columns [#1050](https://github.com/interactive-instruments/ldproxy/issues/1050)
-  introduce GraphQL feature provider [#1037](https://github.com/interactive-instruments/ldproxy/issues/1037)
-  add support for terrain im MapLibre styles [#1023](https://github.com/interactive-instruments/ldproxy/issues/1023)
-  improve layer control in webmap [#1015](https://github.com/interactive-instruments/ldproxy/issues/1015)
-  Schemas: use encoding-agnostic schema instead of describing the GeoJSON representation [#973](https://github.com/interactive-instruments/ldproxy/issues/973)
-  controlling the encoding of feature relationships [#972](https://github.com/interactive-instruments/ldproxy/issues/972)
-  Feature Schema: support representations that better match the application schema [#971](https://github.com/interactive-instruments/ldproxy/issues/971)
-  Feature Schema: data type for references to features [#970](https://github.com/interactive-instruments/ldproxy/issues/970)
-  add audience claim support [#925](https://github.com/interactive-instruments/ldproxy/issues/925)
-  add attribute based access control [#722](https://github.com/interactive-instruments/ldproxy/issues/722)
-  add auth flow integration [#721](https://github.com/interactive-instruments/ldproxy/issues/721)
-  add fine-grained role based access control [#720](https://github.com/interactive-instruments/ldproxy/issues/720)
-  enhanced access control [#717](https://github.com/interactive-instruments/ldproxy/issues/717)

#### Improvements

-  update OpenAPI definition to state maturity of API components [#1056](https://github.com/interactive-instruments/ldproxy/issues/1056)
-  JSON-FG: use the objectType in the provider schema as the default feature type [#1029](https://github.com/interactive-instruments/ldproxy/issues/1029)
-  improve html customization [#1003](https://github.com/interactive-instruments/ldproxy/issues/1003)

#### Fixed bugs

-  support queryables in nested arrays [#1049](https://github.com/interactive-instruments/ldproxy/issues/1049)
-  CQL2-JSON: error when parsing geometries [#1041](https://github.com/interactive-instruments/ldproxy/issues/1041)

#### Dependency updates

-  upgrade MapLibre from 2.1 to 3.2 [#980](https://github.com/interactive-instruments/ldproxy/issues/980)
-  upgrade dropwizard from 2.1.6 to 3.0.1 [#789](https://github.com/interactive-instruments/ldproxy/issues/789)

---
## v3.4.2 (08/08/2023)

#### Fixed bugs

-  fix feature mutation operations [#1027](https://github.com/interactive-instruments/ldproxy/issues/1027)
-  Collections without a tileset in the tile provider (part 1) [#1011](https://github.com/interactive-instruments/ldproxy/issues/1011)
-  JSON-FG not updated for the Search module [#1009](https://github.com/interactive-instruments/ldproxy/issues/1009)
-  Incorrect conversion of CQL2 to SQL  [#1007](https://github.com/interactive-instruments/ldproxy/issues/1007)

---
## v3.4.1 (21/06/2023)

#### Improvements

-  improve stability during startup [#985](https://github.com/interactive-instruments/ldproxy/issues/985)

#### Fixed bugs

-  3D Tiles seeding fails with 'too many open files' [#997](https://github.com/interactive-instruments/ldproxy/issues/997)
-  accessing a non-existent feature does not return a 404 [#996](https://github.com/interactive-instruments/ldproxy/issues/996)
-  fix CRUD issues [#988](https://github.com/interactive-instruments/ldproxy/issues/988)
-  memory leak when seeding a MBTiles cache [#968](https://github.com/interactive-instruments/ldproxy/issues/968)

#### Dependency updates

-  upgrade sqlite driver from 3.40.0.0 to 3.41.2.2 [#1002](https://github.com/interactive-instruments/ldproxy/issues/1002)

---
## v3.4.0 (05/05/2023)

#### Implemented enhancements

-  add option to suppress empty collections in the HTML page for the Feature Collections overview [#945](https://github.com/interactive-instruments/ldproxy/issues/945)
-  3D Tiles: add option to include building outlines [#939](https://github.com/interactive-instruments/ldproxy/issues/939)
-  option to suppress the global CRS list [#937](https://github.com/interactive-instruments/ldproxy/issues/937)
-  support allOf in provider schema [#931](https://github.com/interactive-instruments/ldproxy/issues/931)
-  styling of 3D Tiles should be configurable [#921](https://github.com/interactive-instruments/ldproxy/issues/921)
-  support role 'secondary geometry' and querying polyhedron geometries [#915](https://github.com/interactive-instruments/ldproxy/issues/915)
-  JSON Schema as feature schema [#908](https://github.com/interactive-instruments/ldproxy/issues/908)
-  support JSON values in SQL databases [#907](https://github.com/interactive-instruments/ldproxy/issues/907)
-  Search: support equivalent to 'filter-crs' parameter [#905](https://github.com/interactive-instruments/ldproxy/issues/905)
-  property tooltips with descriptions [#897](https://github.com/interactive-instruments/ldproxy/issues/897)
-  make path separator in queryables/sortables configurable [#871](https://github.com/interactive-instruments/ldproxy/issues/871)
-  add Search building block [#755](https://github.com/interactive-instruments/ldproxy/issues/755)
-  support 3D Tiles [#692](https://github.com/interactive-instruments/ldproxy/issues/692)
-  support GPKG in the Manager [#504](https://github.com/interactive-instruments/ldproxy/issues/504)

#### Improvements

-  upgrade CesiumJS to 1.105 [#941](https://github.com/interactive-instruments/ldproxy/issues/941)
-  publish software bill of materials [#909](https://github.com/interactive-instruments/ldproxy/issues/909)
-  improve HTML filter editor [#899](https://github.com/interactive-instruments/ldproxy/issues/899)
-  q: comparison should be case-insensitive  [#872](https://github.com/interactive-instruments/ldproxy/issues/872)
-  unstable order of links in collection schema [#639](https://github.com/interactive-instruments/ldproxy/issues/639)
-  make Cesium implementation more robust [#545](https://github.com/interactive-instruments/ldproxy/issues/545)

#### Fixed bugs

-  GPKG: Incorrect CRS detection in auto mode [#959](https://github.com/interactive-instruments/ldproxy/issues/959)
-  Sorting: Invalid features when sorting using a nullable attribute [#952](https://github.com/interactive-instruments/ldproxy/issues/952)
-  glTF: A building without a solid geometry throws an exception [#951](https://github.com/interactive-instruments/ldproxy/issues/951)
-  fetching style metadata can lead to server errors [#659](https://github.com/interactive-instruments/ldproxy/issues/659)

#### Dependency updates

-  upgrade dropwizard from 2.0.34 to 2.1.6 [#911](https://github.com/interactive-instruments/ldproxy/issues/911)
-  upgrade swagger from 2.1.13 to 2.2.8 [#790](https://github.com/interactive-instruments/ldproxy/issues/790)

---
## v3.3.6 (14/04/2023)

#### Fixed bugs

-  properties are not always sorted according to provider schema [#949](https://github.com/interactive-instruments/ldproxy/issues/949)

---

## v3.3.5 (13/03/2023)

#### Fixed bugs

-  file log misses first two lines [#934](https://github.com/interactive-instruments/ldproxy/issues/934)
-  incorrect detection of time elements in dateFormat transformations [#920](https://github.com/interactive-instruments/ldproxy/issues/920)

---

## v3.3.4 (14/02/2023)

#### Fixed bugs

-  tile seeding fails with 'too many open files' [#902](https://github.com/interactive-instruments/ldproxy/issues/902)

---

## v3.3.3 (09/02/2023)

#### Improvements

-  support XACML JSON 1.0 [#893](https://github.com/interactive-instruments/ldproxy/issues/893)
-  switch docker base image to temurin [#891](https://github.com/interactive-instruments/ldproxy/issues/891)

#### Fixed bugs

-  auto mode for WFS providers does not work for complex schemas [#895](https://github.com/interactive-instruments/ldproxy/issues/895)
-  sortby does not work with GPKG provider [#894](https://github.com/interactive-instruments/ldproxy/issues/894)
-  error message content in HTML is not escaped [#888](https://github.com/interactive-instruments/ldproxy/issues/888)
-  XML content handling errors [#886](https://github.com/interactive-instruments/ldproxy/issues/886)
-  Collections and Collection resources are always provided as XML [#885](https://github.com/interactive-instruments/ldproxy/issues/885)
-  missing collection extents [#874](https://github.com/interactive-instruments/ldproxy/issues/874)

---
## v3.3.2 (13/01/2023)

#### Fixed bugs

-  additionalLocations should not be written to [#868](https://github.com/interactive-instruments/ldproxy/issues/868)
-  GML: use of custom GML schemas breaks schemaLocation [#864](https://github.com/interactive-instruments/ldproxy/issues/864)
-  GeoPackage: t_intersects() fails for intervals [#863](https://github.com/interactive-instruments/ldproxy/issues/863)
-  only seed layers with features [#862](https://github.com/interactive-instruments/ldproxy/issues/862)
-  unsorted fields dropdown in filter editor [#848](https://github.com/interactive-instruments/ldproxy/issues/848)
---

## v3.3.1 (19/12/2022)

#### Fixed bugs

- tile provider cache levels are ignored on deserialization [#850](https://github.com/interactive-instruments/ldproxy/issues/850)
---

## v3.3.0 (16/12/2022)

#### Implemented enhancements

-  add support for PATCH [#839](https://github.com/interactive-instruments/ldproxy/issues/839)
-  add support for CSV output [#838](https://github.com/interactive-instruments/ldproxy/issues/838)
-  add unit to the JSON Schema of a feature property [#781](https://github.com/interactive-instruments/ldproxy/issues/781)
-  introduce entity groups [#773](https://github.com/interactive-instruments/ldproxy/issues/773)
-  support sql schema prefixes [#772](https://github.com/interactive-instruments/ldproxy/issues/772)
-  support immutable tile caches [#735](https://github.com/interactive-instruments/ldproxy/issues/735)
-  add auto-setup for db trigger [#734](https://github.com/interactive-instruments/ldproxy/issues/734)
-  add dataset change listener [#733](https://github.com/interactive-instruments/ldproxy/issues/733)
-  parallel db connection pools [#732](https://github.com/interactive-instruments/ldproxy/issues/732)
-  add access control for GET operations [#718](https://github.com/interactive-instruments/ldproxy/issues/718)
-  GML support for SQL feature providers [#681](https://github.com/interactive-instruments/ldproxy/issues/681)
-  add support for CityJSON [#667](https://github.com/interactive-instruments/ldproxy/issues/667)
-  support CQL2 JSON [#658](https://github.com/interactive-instruments/ldproxy/issues/658)
-  support for feature changes [#500](https://github.com/interactive-instruments/ldproxy/issues/500)
-  support for conditional requests and optimistic locking [#404](https://github.com/interactive-instruments/ldproxy/issues/404)
-  add number of features on dataset pages [#30](https://github.com/interactive-instruments/ldproxy/issues/30)

#### Improvements

-  align tiles building block with spec v1.0.0 [#785](https://github.com/interactive-instruments/ldproxy/issues/785)
-  GML: add support for local bare name links [#752](https://github.com/interactive-instruments/ldproxy/issues/752)
-  support JSON in query parameters [#751](https://github.com/interactive-instruments/ldproxy/issues/751)
-  add var-base attribute to link templates [#750](https://github.com/interactive-instruments/ldproxy/issues/750)
-  improve api request logging [#737](https://github.com/interactive-instruments/ldproxy/issues/737)
-  align JSON-FG with draft v0.1 [#710](https://github.com/interactive-instruments/ldproxy/issues/710)
-  align CRUD with latest draft [#703](https://github.com/interactive-instruments/ldproxy/issues/703)
-  support docker platform linux/arm64 [#693](https://github.com/interactive-instruments/ldproxy/issues/693)
-  support for 3D bounding boxes [#655](https://github.com/interactive-instruments/ldproxy/issues/655)
-  http header Content-Disposition for queryables changed to upper case [#638](https://github.com/interactive-instruments/ldproxy/issues/638)
-  improve exception handling/routing [#569](https://github.com/interactive-instruments/ldproxy/issues/569)

#### Fixed bugs

-  issues with MBTiles tile providers [#779](https://github.com/interactive-instruments/ldproxy/issues/779)
-  configuring TILES in overrides [#775](https://github.com/interactive-instruments/ldproxy/issues/775)
-  support both WKT MULTIPOINT encodings [#747](https://github.com/interactive-instruments/ldproxy/issues/747)
-  paging with filter doesn't work when the filter contains the letters ÆØÅ [#728](https://github.com/interactive-instruments/ldproxy/issues/728)
-  transformations on geometry properties are ignored [#719](https://github.com/interactive-instruments/ldproxy/issues/719)
-  WFS: interior rings in polygons lead to bad request with Java RuntimeException [#716](https://github.com/interactive-instruments/ldproxy/issues/716)
-  filters with degenerate envelopes return no features [#709](https://github.com/interactive-instruments/ldproxy/issues/709)
-  GeoJSON: 'properties' is required, but missing when empty [#701](https://github.com/interactive-instruments/ldproxy/issues/701)
-  WFS: fetching a single feature results in an exception [#698](https://github.com/interactive-instruments/ldproxy/issues/698)
-  exceptions are not routed via the ldproxy exception handler [#697](https://github.com/interactive-instruments/ldproxy/issues/697)
-  manager password change not working [#687](https://github.com/interactive-instruments/ldproxy/issues/687)
-  tooltip labels for start/stop button are backwards [#686](https://github.com/interactive-instruments/ldproxy/issues/686)
-  timeZone for logging has no effect [#685](https://github.com/interactive-instruments/ldproxy/issues/685)
-  value transformations do not properly handle null values [#666](https://github.com/interactive-instruments/ldproxy/issues/666)
-  bbox requests with coordinates outside the native CRS domain return 400 [#661](https://github.com/interactive-instruments/ldproxy/issues/661)
-  OpenAPI document invalid [#660](https://github.com/interactive-instruments/ldproxy/issues/660)
-  line breaks in stringFormat markdown do not work [#544](https://github.com/interactive-instruments/ldproxy/issues/544)

#### Dependency updates

-  upgrade jts, flatgeobuf, java-vector-tile and json-schema-validator [#799](https://github.com/interactive-instruments/ldproxy/issues/799)
-  upgrade sqlite driver from 3.36.0.3 to 3.40.0.0 [#793](https://github.com/interactive-instruments/ldproxy/issues/793)
-  upgrade postgresql driver from 42.3.3 to 42.5.1 [#792](https://github.com/interactive-instruments/ldproxy/issues/792)
-  upgrade dropwizard from 2.0.28 to 2.0.34 [#788](https://github.com/interactive-instruments/ldproxy/issues/788)
-  upgrade PROJ from 8.2.0 to 9.1.0 [#654](https://github.com/interactive-instruments/ldproxy/issues/654)

---

## v3.2.4 (21/06/2022)
#### Fixed bugs

-  paging error in HTML (f=html) when using a filter with a numeric character string [#634](https://github.com/interactive-instruments/ldproxy/issues/634)
-  Tiles: NPE thrown if no spatial queryable is configured [#674](https://github.com/interactive-instruments/ldproxy/issues/674)
---

## v3.2.3 (04/05/2022)

#### Fixed bugs

-  unable to disable the manager [#652](https://github.com/interactive-instruments/ldproxy/issues/652)

---

## v3.2.2 (25/04/2022)

#### Fixed bugs

-  ldproxy-cfg uses wrong slf4j import [#648](https://github.com/interactive-instruments/ldproxy/issues/648)

---

## v3.2.1 (16/03/2022)

#### Fixed bugs

-  unexpected postgresql error "Connection reset" [#622](https://github.com/interactive-instruments/ldproxy/issues/622)
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
