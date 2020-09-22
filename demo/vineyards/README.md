# Setting up an API

This page describes step-by-step how to set up a Web API for a spatial dataset using ldproxy.

For this description, we will use the vineyards register (Weinbergsrolle) from the German state Rhineland-Palatinate as the sample dataset. It is available under an open data license ([Datenlizenz Deutschland - Namensnennung - Version 2.0](https://www.govdata.de/dl-de/by-2-0)). Please notice the [terms of use (in German only)](http://weinlagen.lwk-rlp.de/portal/nutzungsbedingungen.html).

The API based on the configuration described on this page is available at [https://demo.ldproxy.net/vineyards](https://demo.ldproxy.net/vineyards).

## Pre-conditions

This description assumes that the following preparation steps have been completed.

* ldproxy has been [deployed](deployment.md). All file paths on this page are relative to the data directory of the deployment.
* A PostgreSQL deployment (version 9.6 or later) with PostGIS (version 2.4 or later) are availble.

## Step 1: Getting the data

The dataset can be downloaded from the website of the Landwirtschaftskammer: Go to the [download page](http://weinlagen.lwk-rlp.de/portal/funktionen-anleitung/download.html) or use the [direct shapefile download link](http://weinlagen.lwk-rlp.de/geoserver/lwk/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=lwk:Weinlagen&outputFormat=shape-zip).

If you do not want to deal with shapefiles, you can also use the SQL dump ([DDL](vineyards.ddl.sql) and [DML](vineyards.dml.sql)).

## Step 2: Load the data

The following assumes that the database is available at `localhost`, at the default port `5432`, with a user `postgres`. Adjust these parameter to your local setup.

First, create a database `vineyards` and add PostGIS as an extension. You will have to enter the password when requested by PostgreSQL.

```bash
createdb -Upostgres -W -hlocalhost -p5432 vineyards
psql -Upostgres -W -hlocalhost -p5432 -dvineyards  -c "create extension postgis;"
```

There are multiple ways to load the shapefile into the database. One option is to use GDAL using the following command:

```bash
ogr2ogr -append -f "PostgreSQL" PG:"dbname=vineyards host=localhost port=5432 user=postgres" Weinlagen.shp -nln vineyards -nlt PROMOTE_TO_MULTI -oo ENCODING="ISO-8859-1"
```

If you want to load the SQL dump instead, use the following commands:

```bash
psql -Upostgres -hlocalhost -p5432 vineyards < vineyards.ddl.sql
psql -Upostgres -hlocalhost -p5432 vineyards < vineyards.dml.sql
```

## Step 3: Initialize the configuration

ldproxy configuration options are documented in detail in the [configuration guide](https://github.com/interactive-instruments/ldproxy/blob/master/docs/en/configuration/README.md).

To start an API, we need two files, one for the data provider at the backend and one for the configuration of the API. We only need to declare the minimum information in order to tell ldproxy where to find the data. ldproxy then analyses the data and creates a basic configuration.

### The data provider

Create a file at path `store/entities/providers/vineyards.yml` with the following contents:

```yaml
---
id: vineyards
entityStorageVersion: 2
providerType: FEATURE
featureProviderType: SQL
connectionInfo:
  connectorType: SLICK
  host: localhost
  database: vineyards
  user: postgres
  password: <base64-encoded-password>
  dialect: PGIS
  pathSyntax:
    defaultPrimaryKey: ogc_fid
    defaultSortKey: ogc_fid
auto: true
autoPersist: true
```

Notes:

* `id` has to be the same as the filename.
* `connectionInfo/host` has to be the address of the database as seen from the docker container. For example, that value has to be `host.docker.internal` instead of `localhost` on macOS, if the database is on the same machine.
* `connectionInfo/user` is the account name for the access.
* `connectionInfo/password` has to be the base64-encoded password of the user.
* `connectionInfo/pathSyntax` identifies the column in feature tables to use for sorting and selecting rows. If the data was loaded with GDAL, the value will be `ogc_fid`.
* All other values have to be as stated above. See the configuration guide for more information about the options.
* `auto` and `autoPersist` will derive the CRS used in the dataset plus the available feature schemas and update the file with the derived configuration.

### The API configuration

Create a file at path `store/entities/services/vineyards.yml` with the following contents:

```yaml
id: vineyards
entityStorageVersion: 2
serviceType: OGC_API
auto: true
autoPersist: true
```

Notes:

* `id` has to be the same as the filename.
* All other values have to be as stated above. See the configuration guide for more information about the options.
* `auto` and `autoPersist` will use the feature provider of the same name as well as default API configuration and initialize the file with an updated configuration.

### Restart ldproxy

Restart ldproxy so that the new API is loaded. To restart, submit the following command:

```bash
docker restart ldproxy
```

You can monitor the start-up in the log file (`log/xtraplatform.log`) and you should see something like:

```text
INFO  [2020-09-20 13:57:45,872] Starting ldproxy 2.0.x
INFO  [2020-09-20 13:58:00,439] Started web server at http://localhost:7080
INFO  [2020-09-20 13:58:01,866] Store mode: READ_WRITE
INFO  [2020-09-20 13:58:01,866] Store location: /ldproxy/data/store
INFO  [2020-09-20 13:58:12,121] Feature provider with id 'vineyards' is in auto mode, generating configuration ...
INFO  [2020-09-20 13:58:12,862] Entity of type 'providers' with id 'vineyards' is in autoPersist mode, generated configuration was saved.
INFO  [2020-09-20 13:58:12,862] Feature provider with id 'vineyards' is in auto mode, generating configuration ...
INFO  [2020-09-20 13:58:13,330] Service with id 'vineyards' is in auto mode, generating configuration ...
INFO  [2020-09-20 13:58:13,481] Entity of type 'services' with id 'vineyards' is in autoPersist mode, generated configuration was saved.
INFO  [2020-09-20 13:58:13,939] Feature provider with id 'vineyards' started successfully.
INFO  [2020-09-20 13:58:13,958] Service with id 'vineyards' started successfully.
```

Congratulations, you are now ready to use the API, just open the [landing page](http://localhost:7080/rest/services/vineyards) or the [vineyards features](http://localhost:7080/rest/services/vineyards/collections/vineyards/items).

Have a look at the updated configuration files. They should like the following, first the providers file, the the services file.

```yaml
---
id: vineyards
createdAt: 1600610292030
lastModified: 1600610292030
entityStorageVersion: 2
providerType: FEATURE
featureProviderType: SQL
connectionInfo:
  host: localhost
  database: vineyards
  user: postgres
  password: <base64-encoded-password>
  dialect: PGIS
  computeNumberMatched: true
  pathSyntax:
    defaultPrimaryKey: ogc_fid
    defaultSortKey: ogc_fid
    junctionTablePattern: .+_2_.+
    junctionTableFlag: '{junction}'
nativeCrs:
  code: 25832
  forceAxisOrder: NONE
types:
  vineyards:
    sourcePath: /vineyards
    type: OBJECT
    properties:
      ogc_fid:
        sourcePath: ogc_fid
        type: INTEGER
        role: ID
      wlg_nr:
        sourcePath: wlg_nr
        type: INTEGER
      datum:
        sourcePath: datum
        type: DATETIME
      suchfeld:
        sourcePath: suchfeld
        type: STRING
      suchfeld_1:
        sourcePath: suchfeld_1
        type: STRING
      anbaugebie:
        sourcePath: anbaugebie
        type: STRING
      bereich:
        sourcePath: bereich
        type: STRING
      grosslage:
        sourcePath: grosslage
        type: STRING
      wlg_name:
        sourcePath: wlg_name
        type: STRING
      gemeinde:
        sourcePath: gemeinde
        type: STRING
      gemarkunge:
        sourcePath: gemarkunge
        type: STRING
      rebflache_:
        sourcePath: rebflache_
        type: STRING
      gem_info:
        sourcePath: gem_info
        type: STRING
      gid:
        sourcePath: gid
        type: FLOAT
      wkb_geometry:
        sourcePath: wkb_geometry
        type: GEOMETRY
        geometryType: MULTI_POLYGON
```

```yaml
---
id: vineyards
createdAt: 1600610292106
lastModified: 1600610292106
entityStorageVersion: 2
label: vineyards
serviceType: OGC_API
collections:
  vineyards:
    id: vineyards
    label: vineyards
    enabled: true
    extent:
      spatialComputed: true
    api:
    - buildingBlock: FEATURES_CORE
      enabled: true
      queryables:
        spatial:
        - wkb_geometry
```

## Step 4: Finetuning the API configuration

While the auto-generated API can be used as it is, the API configuration should always be updated to improve usability and the capabilities that the API offers.

Updated configuration files are available here:

* [`store/defaults/ogc_api.yml`](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/defaults/ogc_api.yml)
* [`store/entities/providers/vineyards.yml`](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/providers/vineyards.yml)
* [`store/entities/services/vineyards.yml`](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/services/vineyards.yml)
* [`api-resources/styles/vineyards/default.mbs`](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/api-resources/styles/vineyards/default.mbs)

The changes are explained in the next sections.

### Updating the data provider

The following changes have been made to the auto-generated provider configuration:

* The vineyard register number is the persistent local identifier for each feature and will be used as the feature id. The property is, therefore, the first property in the list and has the value `ID` for `role` (instead of `ogc_fid`).
* The order of the properties has been changed to be more useful to users.
* Property names have been changed to tokens in English.
* Document the schema of the features by adding `label` fields. The labels are, for example, used in the HTML representation.
* Set `defaultLanguage` to `en` to document that the schema documentation is in English.
* `ogc_fid` and `gid` are internal fields that are of no interest to users.

### Updating the API configuration

#### Metadata and descriptions

We have updated the title of the API and added a short description:

```yaml
label: Vineyards in Rhineland-Palatinate, Germany
description: 'Have you ever wondered where the wine that you are drinking comes from? If the wine comes from the wine-growing regions Mosel, Nahe, Rheinhessen, Pfalz, Ahr, or Mittelrhein you can find this information in this API that implements multiple <a href="https://ogcapi.ogc.org/" target="_blank">OGC API standards</a>.
<br><br>
The dataset shared by this API is the vineyard register (Weinbergsrolle) of Rhineland-Palatinate, available under an open-data license. It is managed by the Chamber of Agriculture of Rhineland-Palatinate (Landwirtschaftskammer RLP).
<br>
<small>© Landwirtschaftskammer RLP (2020), dl-de/by-2-0, <a href="http://weinlagen.lwk-rlp.de/" target="_blank">weinlagen.lwk-rlp.de</a>; <a href="http://weinlagen.lwk-rlp.de/portal/nutzungsbedingungen/gewaehrleistung-haftung.html" target="_blank">Regelungen zu Gewährleistung und Haftung</a></small>'
```

We have added contact information for the API provider and the applicable license:

```yaml
metadata:
  contactName: Clemens Portele, interactive instruments GmbH
  contactEmail: portele@interactive-instruments.de
  licenseName: Datenlizenz Deutschland - Namensnennung - Version 2.0
  licenseUrl: https://www.govdata.de/dl-de/by-2-0
```

If you set up a local copy, please use your own contact details.

We have added links to a website with more information and the data sources:

```yaml
- buildingBlock: COLLECTIONS
  additionalLinks:
  - rel: related
    type: text/html 
    title: 'Weinlagen-Online website (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
    href: 'http://weinlagen.lwk-rlp.de/portal/weinlagen.html'
    hreflang: de
  - rel: related
    type: application/xml 
    title: 'OGC Web Map Service with the data (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
    href: 'http://weinlagen.lwk-rlp.de/cgi-bin/mapserv?map=/data/_map/weinlagen/einzellagen_rlp.map&service=WMS&request=GetCapabilities'
    hreflang: de
  - rel: related
    type: application/xml 
    title: 'OGC Web Feature Service with the data (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
    href: 'http://weinlagen.lwk-rlp.de/geoserver/lwk/ows?service=WFS&request=getcapabilities'
    hreflang: de
  - rel: enclosure
    type: application/x-shape
    title: 'Download the data as a shapefile (Provider: Landwirtschaftskammer Rheinland-Pfalz)'
    href: 'http://weinlagen.lwk-rlp.de/geoserver/lwk/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=lwk:Weinlagen&outputFormat=shape-zip'
    hreflang: de
```

#### Additional coordinate reference systems

The CRS building block configuration below adds support for the following coordinate reference systems:

* [ETRS89, UTM zone 32N](http://opengis.net/def/crs/EPSG/0/25832), the coordinate reference system in which the data is stored in the database
* [ETRS89, latitude/longitude](http://opengis.net/def/crs/EPSG/0/4258)
* [WGS84, latitude/longitude](http://opengis.net/def/crs/EPSG/0/25832)
* [Web Mercator](http://opengis.net/def/crs/EPSG/0/3857)

```yaml
- buildingBlock: CRS
  enabled: true
  additionalCrs:
  - code: 25832
    forceAxisOrder: NONE
  - code: 4258
    forceAxisOrder: NONE
  - code: 4326
    forceAxisOrder: NONE
  - code: 3857
    forceAxisOrder: NONE
```

#### Improve the presentation of the data in HTML

The following configuration parameters change how the API resources are presented in the web browser:

* `collectionDescriptionsInOverview` shows the description of each feature collection in the page at `/collections`.
* We add links to legal and privacy notices in the footer and link to the pages from interactive instruments. If you set up a local copy, please link to your own pages.
* ldproxy uses both Leaflet and OpenLayers for maps. We set the URLs for the background map as well as the proper data attribution. For the background map we use the TopPlus maps of the German mapping authorities instead of the default (OpenStreetMap).

```yaml
- buildingBlock: HTML
  collectionDescriptionsInOverview: true
  legalName: Legal notice
  legalUrl: https://www.interactive-instruments.de/en/about/impressum/
  privacyName: Privacy notice
  privacyUrl: https://www.interactive-instruments.de/en/about/datenschutzerklarung/
  leafletUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
  leafletAttribution: '&copy; <a href="https://www.bkg.bund.de" class="link0" target="_new">Bundesamt
    f&uuml;r Kartographie und Geod&auml;sie</a> (2020), <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf"
    class="link0" target="_new">Datenquellen</a>; &copy; Landwirtschaftskammer RLP (2020), dl-de/by-2-0, <a href="http://weinlagen.lwk-rlp.de/" class="link0" target="_blank">weinlagen.lwk-rlp.de</a>, <a href="http://weinlagen.lwk-rlp.de/portal/nutzungsbedingungen/gewaehrleistung-haftung.html" class="link0" target="_blank">Regelungen zu Gewährleistung und Haftung</a>'
  openLayersUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
  openLayersAttribution: '&copy; <a href="https://www.bkg.bund.de" class="link0" target="_new">Bundesamt
    f&uuml;r Kartographie und Geod&auml;sie</a> (2020), <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf"
    class="link0" target="_new">Datenquellen</a>; &copy; Landwirtschaftskammer RLP (2020), dl-de/by-2-0, <a href="http://weinlagen.lwk-rlp.de/" class="link0" target="_blank">weinlagen.lwk-rlp.de</a>, <a href="http://weinlagen.lwk-rlp.de/portal/nutzungsbedingungen/gewaehrleistung-haftung.html" class="link0" target="_blank">Regelungen zu Gewährleistung und Haftung</a>'
```

#### Support CQL filter expressions

The following building block configuration activates filter expressions using CQL (text and json):

```yaml
- buildingBlock: FILTER
  enabled: true
```

#### Enable vector tiles

The following building block configuration activates support for vector tiles for the dataset:

* Vector tiles will be available for the [most commonly used tiling scheme](https://www.maptiler.com/google-maps-coordinates-tile-bounds-projection/) for the zoom levels 6 to 16.
* The recommended default zoom level for maps is 8 with a center at 7.35°E, 49.8°N.
* The server will pre-cache tiles for the zoom levels 6 to 10.

```yaml
- buildingBlock: TILES
  enabled: true
  multiCollectionEnabled: true
  zoomLevels:
    WebMercatorQuad:
      min: 6
      max: 16
      default: 8
  seeding:
    WebMercatorQuad:
      min: 6
      max: 10
  center:
  - 7.35
  - 49.8
```

#### Enable styles

The following building block configuration activates support for map styles:

* Styles will be encoded as [Mapbox styles](https://www.mapbox.com/mapbox-gl-js/style-spec/). In addition, the style can also be shown using a web map (encoding `HTML`).
* The styles are provided as files with extension `.mbs` in folder `api-resources/styles/vineyards`.
* A link to a web map with the default style `default` will be added to the landing page.

```yaml
- buildingBlock: STYLES
  enabled: true
  styleEncodings:
  - Mapbox
  - HTML
  defaultStyle: default
```

Copy the sample style [`default`](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/api-resources/styles/vineyards/default.mbs) to `api-resources/styles/vineyards/default.mbs`.

#### Update the vineyards collection

In addition to the configuration of the API modules, we want to finetune the representation of the feature data in the since feature collection `vineyards`. The configuration below makes the following changes:

* Add a title and description for the collection.
* Identify the spatial, temporal and regular properties that can be used in filters (in query parameters or in CQL expressions).
* Always hide the `ogc_fid` and `gid` properties and hide the `village_info` field in HTML.
* Hide the search field attributes and the date in the HTML overviews, only show them on the pages for a single feature.
* Use a decimal point in area values and map "k. A." to "unknown.
* Use the vineyard name as the feature title in HTML.

```yaml
collections:
  vineyards:
    id: vineyards
    label: Vineyards
    description: 'The vineyard register constitutes the authorized list of names of single vineyards, vineyards clusters (Großlagen), sub-regions (Bereiche) and wine-growing regions (Anbaugebiete) for the protected designation of origin for wines in the German state Rhineland-Palatinate. It is managed by the Chamber of Agriculture of Rhineland-Palatinate (Landwirtschaftskammer RLP). 
    <br>
    The data for each vineyard includes the vineyard register number, the wine-growing region, the sub-region, the vineyard cluster, the name of the single vineyard, the village(s), the cadastral district(s) and the area with vines in hectares. The six-digit vineyard register number contains in the first digit the wine-growing region, in the second digit the sub-region, in the third and fourth digit the vineyard cluster and in the fifth and sixth digit the single vineyard.'
    enabled: true
    extent:
      spatialComputed: true
    api:
    - buildingBlock: FEATURES_CORE
      enabled: true
      queryables:
        spatial:
        - geometry
        temporal:
        - date
        other:
        - registerId
        - name
        - area_ha
        - region
        - subregion
        - cluster
        - village
        - searchfield1
        - searchfield2
      transformations:
        area_ha:
          null: 'k. A.'
          stringFormat: '{{value | replace:'','':''.''}}'
        ogc_fid:
          remove: ALWAYS
        gid:
          remove: ALWAYS
    - buildingBlock: FEATURES_HTML
      itemLabelFormat: '{{name}}'
      transformations:
        village_info:
          remove: ALWAYS
        searchfield1:
          remove: OVERVIEW
        searchfield2:
          remove: OVERVIEW
        date:
          remove: OVERVIEW
          dateFormat: dd/MM/yyyy
```

## Step 5: Global configuration

If the server will be available to other, configure the `externalUrl` in the `cfg.yml` file. For example:

```yaml
---
store:
  mode: READ_ONLY
server:
  externalUrl: https://demo.ldproxy.net
logging:
  level: 'OFF'
  appenders:
    - type: console
      timeZone: Europe/Berlin
  loggers:
    de.ii: INFO
```
