# Setting up an API

This page describes step-by-step how to set up a Web API for a spatial dataset using ldproxy.

For this description, we will use the vineyards register (Weinbergsrolle) from the German state Rhineland-Palatinate as the sample dataset. It is available under an open data license ([Datenlizenz Deutschland - Namensnennung - Version 2.0](https://www.govdata.de/dl-de/by-2-0)). Please notice the [terms of use (in German only)](http://weinlagen.lwk-rlp.de/portal/nutzungsbedingungen.html).

The API based on the configuration described on this page is available at [https://demo.ldproxy.net/vineyards](https://demo.ldproxy.net/vineyards).

## Pre-conditions

This description assumes that the following preparation steps have been completed.

* ldproxy has been [deployed](../../docs/en/deployment.md). All file paths on this page are relative to the data directory of the deployment.
* A PostgreSQL deployment (version 9.6 or later) with PostGIS (version 2.4 or later) is availble.

## Step 1: Getting the data

The dataset can be downloaded from the website of the Landwirtschaftskammer: Go to the [download page](http://weinlagen.lwk-rlp.de/portal/funktionen-anleitung/download.html) or use the [direct shapefile download link](http://weinlagen.lwk-rlp.de/geoserver/lwk/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=lwk:Weinlagen&outputFormat=shape-zip).

If you do not want to deal with shapefiles, you can also use the SQL dump ([DDL](db/vineyards.ddl.sql) and [DML](db/vineyards.dml.sql)).

## Step 2: Load the data

The following assumes that the database is available at `localhost`, at the default port `5432`, with a user `postgres`. Adjust these parameter to your local setup.

First, create a database `vineyards` and add PostGIS as an extension. You will have to enter the password when requested by PostgreSQL.

```bash
dropdb -Upostgres -W -hlocalhost -p5432 vineyards
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

### The global configuration

Create a file `cfg.yml` with the following contents:

```yaml
---
logging:
  level: 'OFF'
  appenders:
    - type: console
  loggers:
    de.ii: INFO
```

See the configuration guide for more information about the options.

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
sourcePathDefaults:
  primaryKey: ogc_fid
  sortKey: ogc_fid
auto: true
autoPersist: true
```

Notes:

* `id` has to be the same as the filename without the file extension.
* `connectionInfo/host` has to be the address of the database as seen from the docker container. For example, that value has to be `host.docker.internal` instead of `localhost` on macOS, if the database is on the same machine.
* `connectionInfo/user` is the account name for the access.
* `connectionInfo/password` has to be the base64-encoded password of the user.
* `sourcePathDefaults` identifies the columns in feature tables that are used for selecting rows (`primaryKey`) and sorting (`sortKey`). If the data was loaded with GDAL, the value will be `ogc_fid`.
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

* `id` has to be the same as the filename without the file extension.
* All other values have to be as stated above. See the configuration guide for more information about the options.
* `auto` and `autoPersist` will use the feature provider of the same name as well as default API configuration and initialize the file with an updated configuration.

### Restart ldproxy

Restart ldproxy so that the new API is loaded. To restart, submit the following command:

```bash
docker restart ldproxy
```

You can monitor the start-up in the log file (`docker logs -f ldproxy`) and you should see something like:

```text
INFO  [2021-12-18 07:29:00,945]                          - --------------------------------------------------  
INFO  [2021-12-18 07:29:00,948]                          - Starting ldproxy 3.1.0  
INFO  [2021-12-18 07:29:16,899]                          - Started web server at http://localhost:7080  
INFO  [2021-12-18 07:29:25,367]                          - Store mode: READ_WRITE  
INFO  [2021-12-18 07:29:25,367]                          - Store location: /ldproxy/data/store  
INFO  [2021-12-18 07:29:35,624]                vineyards - Feature provider with id 'vineyards' is in auto mode, generating configuration ...  
INFO  [2021-12-18 07:29:37,879]                vineyards - Entity of type 'providers' with id 'vineyards' is in autoPersist mode, generated configuration was saved.  
INFO  [2021-12-18 07:29:38,140]                vineyards - Service with id 'vineyards' is in auto mode, generating configuration ...  
INFO  [2021-12-18 07:29:38,410]                vineyards - Entity of type 'services' with id 'vineyards' is in autoPersist mode, generated configuration was saved.  
INFO  [2021-12-18 07:29:39,089]                vineyards - Feature provider with id 'vineyards' started successfully. (min connections=8, max connections=8, stream capacity=8)  
INFO  [2021-12-18 07:29:39,400]                vineyards - Service with id 'vineyards' started successfully.  
```

Congratulations, you are now ready to use the API, just open the [landing page](http://localhost:7080/rest/services/vineyards) or the [vineyards features](http://localhost:7080/rest/services/vineyards/collections/vineyards/items).

Have a look at the updated configuration files. They should like the following, first the providers file, the the services file.

```yaml
---
id: vineyards
createdAt: 1639812575490
lastModified: 1639812575490
entityStorageVersion: 2
providerType: FEATURE
featureProviderType: SQL
nativeCrs:
  code: 25832
  forceAxisOrder: NONE
connectionInfo:
  database: vineyards
  host: localhost
  user: postgres
  password: <base64-encoded-password>
sourcePathDefaults:
  primaryKey: ogc_fid
  sortKey: ogc_fid
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
        role: PRIMARY_INSTANT
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
        role: PRIMARY_GEOMETRY
        geometryType: MULTI_POLYGON
```

```yaml
---
id: vineyards
createdAt: 1639812575613
lastModified: 1639812575613
entityStorageVersion: 2
label: vineyards
serviceType: OGC_API
collections:
  vineyards:
    id: vineyards
    label: vineyards
    enabled: true
    api:
    - buildingBlock: FEATURES_CORE
      queryables:
        spatial:
        - wkb_geometry
        temporal:
        - datum
```

## Step 4: Fine-tuning the API configuration

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
* The `geometry` is tagged as the primary geometry of the vineyard features. Since the features only contain one geometry property, this setting in not strictly necessary.
* The `date` is tagged as the primary temporal information of the vineyard features. Since the features only contain one temporal property, this setting in not strictly necessary.
* Use a decimal point in area values (`area_ha`) and map "k. A." to "unknown.

### Updating the API configuration

#### Metadata and descriptions

We have updated the title of the API and added a short description:

```yaml
label: Vineyards in Rhineland-Palatinate, Germany
description: 'Have you ever wondered where the wine that you are drinking comes from? If the wine comes from the wine-growing regions Mosel, Nahe, Rheinhessen, Pfalz, Ahr, or Mittelrhein you can find this information in this API.
<br><br>
The dataset shared by this API is the vineyard register (Weinbergsrolle) of Rhineland-Palatinate, available under an open-data license. It is managed by the Chamber of Agriculture of Rhineland-Palatinate (Landwirtschaftskammer RLP). 
<br>
<small>© Landwirtschaftskammer RLP (2020), dl-de/by-2-0, <a href=''http://weinlagen.lwk-rlp.de/'' target=''_blank''>weinlagen.lwk-rlp.de</a>; <a href=''http://weinlagen.lwk-rlp.de/portal/nutzungsbedingungen/gewaehrleistung-haftung.html'' target=''_blank''>Regelungen zu Gewährleistung und Haftung</a></small>'
```

We have added contact information for the API provider and the dataset creator as well as the applicable license and attribution information:

```yaml
metadata:
  contactName: Jane Doe
  contactEmail: doe@example.com
  creatorName: Landwirtschaftskammer Rheinland-Pfalz
  creatorUrl: https://www.lwk-rlp.de/
  publisherName: Acme Inc.
  publisherUrl: https://www.example.com/
  licenseName: Datenlizenz Deutschland - Namensnennung - Version 2.0
  licenseUrl: https://www.govdata.de/dl-de/by-2-0
  attribution: '&copy; Landwirtschaftskammer RLP (2020), dl-de/by-2-0, <a href="http://weinlagen.lwk-rlp.de/" target="_blank">weinlagen.lwk-rlp.de</a>, <a href="http://weinlagen.lwk-rlp.de/portal/nutzungsbedingungen/gewaehrleistung-haftung.html" target="_blank">Regelungen zu Gewährleistung und Haftung</a>'
```

If you set up a local copy, please use your own contact details.

When starting, the API will not be validated first by ldproxy:

```yaml
apiValidation: NONE
```

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

* `noIndexEnabled` signals to harvesters to not index the pages of the API.
* `schemaOrgEnabled` adds [schema.org](https://schema.org/) markup to HTML pages.
* `collectionDescriptionsInOverview` shows the description of each feature collection in the page at `/collections`.
* We add links to legal and privacy notices in the footer and link to hypothetical pages. If you set up a local copy, please link to your own pages.
* We set the URLs for a basemap as well as the proper data attribution. For the background map we use the TopPlus Open maps of the German mapping authorities instead of the default (OpenStreetMap).
* We configure a default style for use in map clients in HTML (used for tile sets, features and the default web map). See also "Enable styles" below.

```yaml
- buildingBlock: HTML
  enabled: true
  noIndexEnabled: true
  schemaOrgEnabled: true
  collectionDescriptionsInOverview: true
  legalName: Legal notice
  legalUrl: https://www.example.com/legal
  privacyName: Privacy notice
  privacyUrl: https://www.example.com/privacy
  basemapUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
  basemapAttribution: '&copy; <a href="https://www.bkg.bund.de" target="_new">Bundesamt f&uuml;r Kartographie und Geod&auml;sie</a> (2020), <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf" target="_new">Datenquellen</a>'
  defaultStyle: default
```

#### Support CQL filter expressions

The following building block configurations activate

* a JSON schema resource for the vineyard features;
* the publication of queryable properties in order to filter the dataset;
* filter expressions using CQL2 (text and json);
* the capability to return features sorted by selected properties;
* the capability to return only selected properties; and
* the capability to simplify geometries in responses using a Douglas-Peucker algorithm.

```yaml
- buildingBlock: SCHEMA
  enabled: true
- buildingBlock: QUERYABLES
  enabled: true
- buildingBlock: FILTER
  enabled: true
- buildingBlock: SORTING
  enabled: true
- buildingBlock: PROJECTIONS
  enabled: true
- buildingBlock: GEOMETRY_SIMPLIFICATION
  enabled: true    
```

#### Enable vector tiles

The following building block configuration activates support for vector tiles for the dataset:

* Vector tiles will be available for the [most commonly used tiling scheme](https://www.maptiler.com/google-maps-coordinates-tile-bounds-projection/) for the zoom levels 5 to 16.
* The server will pre-cache tiles in a MBTiles container for the zoom levels 5 to 11.

```yaml
- buildingBlock: TILES
  enabled: true
  cache: MBTILES
  tileProvider:
    type: FEATURES
    multiCollectionEnabled: true
    zoomLevels:
      WebMercatorQuad:
        min: 5
        max: 16
    seeding:
      WebMercatorQuad:
        min: 5
        max: 11
```

#### Enable styles

The following building block configuration activates support for map styles:

* Styles will be encoded as [Mapbox styles](https://www.mapbox.com/mapbox-gl-js/style-spec/). In addition, the style can also be shown using a web map (encoding `HTML`).
* The styles are provided as files with extension `.mbs` in folder `api-resources/styles/vineyards`.
* A link to a web map with the default style `default`, configured in the HTML module, will be added to the landing page.
* To enable the use of styles on the collection level, including for the tiles and features resources, we also activate that styles for the collection is derived from the API-level style using `deriveCollectionStyles`.

```yaml
- buildingBlock: STYLES
  enabled: true
  styleEncodings:
  - Mapbox
  - HTML
  deriveCollectionStyles: true
```

Copy the sample style [`default`](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/api-resources/styles/vineyards/default.mbs) to `api-resources/styles/vineyards/default.mbs`.

#### Update the vineyards collection

In addition to the configuration of the API modules, we want to finetune the representation of the feature data in the feature collection `vineyards`. The configuration below makes the following changes:

* Identify the spatial, temporal and regular properties that can be used in filters (in query parameters or in CQL expressions).
* Support sorting of responses by selected properties.
* Use the searchfield2 attribute as the label of the vineyard features in HTML.
* Hide the `village_info` field in HTML.
* Hide the searchfield1 attribute and the date in the HTML feature collections, only show them on the pages for a single feature.
* Aggregate vineyards on smaller scales in vector tiles.

Note that the title and description for the collection are inherited from the feature type defined in the provider configuration.

```yaml
collections:
  vineyards:
    id: vineyards
    label: vineyards
    enabled: true
    api:
    - buildingBlock: FEATURES_CORE
      enabled: true
      itemType: feature
      queryables:
        spatial:
        - geometry
        temporal:
        - date
        q:
        - name
        - region
        - subregion
        - cluster
        - village
        - searchfield1
        - searchfield2
        other:
        - registerId
        - area_ha
    - buildingBlock: SORTING
      enabled: true
      sortables:
      - name
      - region
      - subregion
      - cluster
      - village
      - registerId
      - area_ha
    - buildingBlock: FEATURES_HTML
      featureTitleTemplate: '{{searchfield2}}'
      transformations:
        village_info:
          remove: ALWAYS
        searchfield1:
          remove: IN_COLLECTION
        date:
          remove: IN_COLLECTION
          dateFormat: dd/MM/yyyy
    - buildingBlock: TILES
      rules:
        WebMercatorQuad:
        - min: 5
          max: 7
          merge: true
          groupBy:
          - region
        - min: 8
          max: 8
          merge: true
          groupBy:
          - region
          - subregion
        - min: 9
          max: 9
          merge: true
          groupBy:
          - region
          - subregion
          - cluster
```

## Step 5: Global configuration

If the server will be made available to others, configure the `externalUrl` in the `cfg.yml` file. For example:

```yaml
---
store:
  mode: READ_ONLY
server:
  externalUrl: https://www.example.com
logging:
  level: 'OFF'
  appenders:
    - type: console
      timeZone: Europe/Berlin
  loggers:
    de.ii: INFO
```
