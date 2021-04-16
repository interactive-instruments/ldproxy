# API

Represents a single deployment of an OGC API.

## Configuration

Options without default are mandatory.

Details regarding the API modules can be found [here](building-blocks/README.md), see `api` in the table below.

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`id` |string | |API identifier. Allowed characters are (A-Z, a-z), numbers (0-9), underscore and hyphen.
|`apiVersion` |integer |`null` |Optional version used in the API URI. If not set, the landing page URI is `/{id}`. If set, the landing page URI is `/{id}/v{apiVersion}`, e.g. `/{id}/v1` for `1`.
|`createdAt` |integer | |Unix time stamp, only for internal usage.
|`lastModified` |integer | |Unix time stamp, only for internal usage.
|`entityStorageVersion` |integer | |Version of the configuration schema. This documentation describes version 2 and all files using this schema need to have the value 2. Configurations with version 1 are automatically migrated to version 2.
|`label` |string |der Wert von `id` |Human readable label.
|`description` |string |`null` |Human readable description.
|`serviceType` |enum | |Always `OGC_API`.
|`enabled` |boolean |`true` |Publish the API on startup?
|`shouldStart` |boolean |`true` |*Deprecated* See `enabled`
|`metadata` |object |`{}` |General metadata for the API (version, contact details, license information). Supported keys (with affected resources in braces): `version` (*API Definition*), `contactName` (*API Definition*, *HTML Landing Page*), `contactUrl` (*API Definition*, *HTML Landing Page*), `contactEmail` (*API Definition*, *HTML Landing Page*), `contactPhone` (*HTML Landing Page*), `licenseName` (*API Definition*, *HTML Landing Page*, *Feature Collections*, *Feature Collection*), `licenseUrl` (*API Definition*, *HTML Landing Page*, *Feature Collections*, *Feature Collection*) und `keywords` (*HTML Landing Page*). All values are strings, with the exception of keywords, which are an array of strings.
|`tags` |array |`null` |Tags for this API. Every tag is a string without white space. Tags are shown in the *API Catalog* and can be used to filter the catalog response with the query parameter `tags`, e.g. `tags=INSPIRE`.<br>_since version 2.1_
|`externalDocs` |object |`{}` |External document with additional information about this API, required keys are `url` and `description`.
|`defaultExtent` |object |`{'spatialComputed': true, 'temporalComputed': true}` |Default value for spatial (`spatial`) and/or temporal (`temporal`) extent for each collection, if not set in the collection configuration. Required keys for spatial extents (all values in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Required keys for temporal extents (all values in milliseconds since 1 January 1970): `start`, `end`. If the spatial extent should be derived from the data source on startup, set `spatialComputed` to `true`. If the temporal extent should be derived from the data source on startup, set `temporalComputed` to `true`. For big datasets this will slow down the startup. Note: This is not the extent for the whole dataset, that will always be derived from the collection extents.
|`api` |array |`[]` |Array with [API module](building-blocks/README.md) configurations.
|`collections` |object |`{}` |Collection configurations, the key is the collection id, for the value see [Collection](#collection) below.
|`auto` |boolean |false |Option to derive `collections` automatically from the feature provider on startup (auto mode). If enabled `collections` must not be set.
|`autoPersist` |boolean |false |Option to persist definitions generated with `auto: true` to the configuration file. Will remove `auto` und `autoPersist` from the configuration file. If the configuration file does not reside in `store/entities/services` (see `additionalLocations`), a new file will be created in `store/entities/services`. The `store` must not be `READ_ONLY` for this to take effect.

<a name="collection"></a>

### Collection

Every collection corresponds to a feature type defined in the feature provider (only *Feature Collections* are currently supported).

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`id` |string | |Collection identifier. Allowed characters are (A-Z, a-z), numbers (0-9), underscore and hyphen. Normally the feature type and collection ids are the same. 
|`label` |string |value of `id` |Human readable label.
|`description` |string |`null` |Human readable description.
|`persistentUriTemplate` |string |`null` |The *Feature* resource defines a unique URI for every feature, but this URI is only stable as long as the API URI stays the same. For use cases where external persistent feature URIs, which redirect to the current API URI, are used, this option allows to use such URIs as canonical URI of every feature. To enable this option, provide an URI template where `{{value}}` is replaced with the feature id.
|`extent` |object |`{}` |Spatial (`spatial`) and/or temporal (`temporal`) extent for this collection. Required keys for spatial extents (all values in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Required keys for temporal extents (all values in milliseconds since 1 January 1970): `start`, `end`. If the spatial extent should be derived from the data source on startup, set `spatialComputed` to `true`. If the temporal extent should be derived from the data source on startup, set `temporalComputed` to `true`. For big datasets this will slow down the startup. If `spatialComputed` or `temporalComputed` was enabled in `defaultExtent`, it can be disabled for a single collection by setting `false` here. `spatialComputed` and `temporalComputed` are only taken into account if  `spatial` or `temporal` are not set respectively.
|`additionalLinks` |array |`[]` |Array of additional link objects, required keys are `href` (the URI), `label` and `rel` (the relation).
|`api` |array |`[]` |Array with [API module](building-blocks/README.md) configurations for this collection.

### API modules

Modules might be configured for the API or for single collections. The final configuration is formed by merging the following sources in this order:

* The module defaults, see [API modules](building-blocks/README.md).
* Optional deployment defaults in the `defaults` directory.
* API level configuration.
* Collection level configuration.
* Optional deployment overrides in the `overrides` directory.

### Example

```yaml
id: kita
createdAt: 1598603585258
lastModified: 1598603585258
entityStorageVersion: 2
label: Kindertageseinrichtungen
description: Hier steht eine Beschreibung der API und seiner Inhalte, die einem Nutzer erläutert, was ihm die API bietet.
enabled: true
secured: false
serviceType: OGC_API
apiVersion: 1
externalDocs:
  url: "https://example.com/pfad/zum/dokument"
  description: Weitere Informationen zu den Kita-Daten
defaultExtent:
  spatial:
    xmin: 5.8663153
    ymin: 47.2701114
    xmax: 15.0419319
    ymax: 55.099161
metadata:
  keywords:
  - Kinderbetreuung
  - Kindertageseinrichtungen
  - Kindertagesstätten
  - Kindergarten
  - Spielgruppen
  - Kinder
  - Kita
  - INSPIRE
api:
- buildingBlock: COLLECTIONS
  additionalLinks:
  - rel: describedby
    type: application/xml
    title: INSPIRE-Metadaten zum Datensatz
    href: 'https://example.org/pfad/zu/metadaten'
    hreflang: de
  - rel: enclosure
    type: text/csv
    title: Download der Daten als CSV
    href: 'https://example.org/pfad/zu/datei.csv'
    hreflang: de
collections:
  governmentalservice:
    id: governmentalservice
    label: Staatlicher Dienst
    description: 'Staatliche Verwaltungs- und Sozialdienste wie öffentliche Verwaltung, Katastrophenschutz, Schulen und Krankenhäuser, die von öffentlichen oder privaten Einrichtungen erbracht werden, soweit sie in den Anwendungsbereich der Richtlinie 2007/2/EG fallen. Dieser Datensatz enthält Informationen zu Diensten der Kinderbetreuung.'
    persistentUriTemplate: '{{value | prepend:''https://example.com/id/soziales/kindergarten/''}}'
    extent:
      spatialComputed: true
    api:
    - buildingBlock: FEATURES_CORE
      featureType: governmentalservice
      queryables:
        spatial:
        - geometry
        other:
        - name
        - pointOfContact.address.postCode
        - pointOfContact.address.adminUnit
        - pointOfContact.telephoneVoice
        - occupancy.typeOfOccupant
        - occupancy.numberOfOccupants
      transformations:
        pointOfContact.telephoneVoice:
          null: 'bitte ausf(ue|ü)llen'
        occupancy[].anzahl:
          null: '0'
        inspireId:
          stringFormat: 'https://example.com/id/soziales/kindergarten/{{value}}'
    - buildingBlock: FEATURES_HTML
      itemLabelFormat: '{{name}}'
      transformations:
        geometry:
          remove: OVERVIEW
        occupancy[].typeOfOccupant:
          remove: OVERVIEW
        occupancy[].numberOfOccupants:
          remove: OVERVIEW
```


## Storage 

API configurations reside under the relative path `store/entities/services/{apiId}.yml` in the data directory.