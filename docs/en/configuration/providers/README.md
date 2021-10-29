# Feature Provider

A feature provider is defined in a configuration file by an object with the following properties. Properties without default are mandatory.

|Property |Data Type |Default |Description
| --- | --- | --- | ---
|`id` |string | |Unique identifier of the provider. Allowed are characters (A-Z, a-z), numbers (0-9), underscore and hyphen.
|`createdAt` |integer | |Unix time stamp, only for internal usage.
|`lastModified` |integer | |Unix time stamp, only for internal usage.
|`entityStorageVersion` |integer | |Version of the configuration schema. This documentation describes version 2 and all files using this schema need to have the value 2. Configurations with version 1 are automatically migrated to version 2.
|`providerType` |enum | |Always `FEATURE`.
|`featureProviderType` |enum | |`SQL` for SQL DBMS as data source, `WFS` for *OGC Web Feature Service* as data source.
|`connectionInfo` |object | |Configuration of the data source. Depends on `featureProviderType`, see[SQL](sql.md#connection-info) and [WFS](wfs.md#connection-info).
|`nativeCrs` |object | |Coordinate reference system of geometries in the dataset. The EPSG code of the coordinate reference system is given as integer in `code`. `forceAxisOrder` may be set to use a non-default axis order:  `LON_LAT` uses longitude/east as first value and latitude/north as second value, `LAT_LON` uses the reverse. `NONE` uses the default axis order and is the default value. Example: The default coordinate reference system `CRS84` would look like this: `code: 4326` and `forceAxisOrder: LON_LAT`.
|`nativeTimeZone` |string | `UTC` |A timezone ID, such as `Europe/Berlin`. Is applied to temporal values without timezone in the dataset.
|`types` |object |`{}` |Definition of object types, see [below](#feature-provider-types).
|`typeValidation` |enum |`NONE` |Optional type definition validation with regard to the data source (only for SQL). `NONE` means no validation. With `LAX` the validation will fail and the provider will not start, when issues are detected that would definitely lead to runtime errors. Issues that might lead to runtime errors depending on the data will be logged as warning. With `STRICT` the validation will fail for any detected issue. That means the provider will only start if runtime errors with regard to the data source can be ruled out.
|`auto` |boolean |`false` |Option to derive `types` definitions automatically from the data source. When enabled `types` must not be set.
|`autoPersist` |boolean |`false` |Option to persist definitions generated with `auto: true` to the configuration file. Will remove `auto` und `autoPersist` from the configuration file. If the configuration file does not reside in `store/entities/providers` (see `additionalLocations`), a new file will be created in `store/entities/providers`. The `store` must not be `READ_ONLY` for this to take effect.
|`autoTypes` |boolean |`[]` |List of source types to include in derived `types` definitions when `auto: true`. Currently only works for [SQL](sql.md).

<a name="feature-provider-types"></a>

## Types

The types object has an entry for every feature type with the feature type identifier as key and a schema object with `type: OBJECT` as value. 

|Property |Data Type |Default |Description
| --- | --- | --- | ---
|`type` |enum |`STRING` / `OBJECT` |Data type of the schema object. Default is `OBJECT` when `properties` is set, otherwise it is `STRING`. Possible values:<ul><li>`FLOAT`, `INTEGER`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE` for simple values.</li><li>`GEOMETRY` for geometries.</li><li>`OBJECT` for objects.</li><li>`OBJECT_ARRAY` a list of objects.</li><li>`VALUE_ARRAY` a list of simple values.</li></ul>
|`sourcePath` |string | |The relative path for this schema object. The syntax depends on the provider types, see [SQL](sql.md#path-syntax) or [WFS](wfs.md#path-syntax).
|`constantValue` |string / number / boolean |`null` |Might be used instead of `sourcePath` to define a property with a constant value.
|`label` |string | |Label for the schema object, used for example in HTML representations.
|`description` |string | |Description for the schema object, used for example in HTML representations or JSON Schema.
|`properties` |object | |Only for `OBJECT` and `OBJECT_ARRAY`. Object with the property names as keys and schema objects as values.
|`role` |enum |`null` |`ID` has to be set for the property that should be used as the unique feature id. As a rule that should be the first property ion the  `properties` object. Property names cannot contain spaces (" ") or slashes ("/"). Set `TYPE` for a property that specifies the type name of the object.
|`objectType` |string | |Optional name for an object type, used for example in JSON Schema. For properties that should be mapped as links according to *RFC 8288*, use `Link`.
|`geometryType` |enum | |The specific geometry type for properties with `type: GEOMETRY`. Possible values are simple feature geometry types: `POINT`, `MULTI_POINT`, `LINE_STRING`, `MULTI_LINE_STRING`, `POLYGON`, `MULTI_POLYGON`, `GEOMETRY_COLLECTION` and `ANY`
|`forcePolygonCCW` |boolean |`true` |Option to disable enforcement of counter-clockwise orientation for exterior rings and a clockwise orientation for interior rings (only for SQL).
|`transformations` |object |`{}` |Optional transformations for the property, see [transformations](transformations.md).

## Connection Info

For data source specifics, see [SQL](sql.md#connection-info) and [WFS](wfs.md#connection-info).

## Example Configuration (SQL)

```yaml
id: kita
createdAt: 1598603585258
lastModified: 1598603585258
entityStorageVersion: 2
providerType: FEATURE
featureProviderType: SQL
connectionInfo:
  connectorType: SLICK
  host: db
  database: kita
  user: postgres
  password: cGxlYXNlY2hhbmdlbWUK
  dialect: PGIS
  computeNumberMatched: true
  pathSyntax:
    defaultPrimaryKey: oid
    defaultSortKey: oid
  schemas:
  - public
nativeCrs:
  code: 25832
  forceAxisOrder: NONE
types:
  governmentalservice:
    sourcePath: /kita
    type: OBJECT
    label: Staatlicher Dienst
    description: 'Staatliche Verwaltungs- und Sozialdienste wie öffentliche Verwaltung, Katastrophenschutz, Schulen und Krankenhäuser, die von öffentlichen oder privaten Einrichtungen erbracht werden, soweit sie in den Anwendungsbereich der Richtlinie 2007/2/EG fallen. Dieser Datensatz enthält Informationen zu Diensten der Kinderbetreuung (KiTas).'
    properties:
      thematicId:
        sourcePath: kitaid
        type: STRING
        role: ID
        label: Fachidentifikator
        description: Beschreibender eindeutiger Identifikator für Geo-Objekte in einem bestimmten Datenthema.
      oid:
        sourcePath: oid
        type: INTEGER
        label: Interner Identifikator
      inspireId:
        sourcePath: kitaid
        type: STRING
        label: INSPIRE-Objektidentifikator
        description: Externer Objektidentifikator des Geo-Objekts.
      name:
        sourcePath: name
        type: STRING
        label: Name
        description: Der Name des staatlichen Verwaltungsdienstes.
      serviceType:
        type: OBJECT
        objectType: Link
        label: Art des Dienstes
        description: Art des staatlichen Verwaltungsdienstes.
        properties:
          title:
            constantValue: Kinderbetreuung
            type: STRING
            label: Beschreibung
          href:
            constantValue: 'http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService'
            type: STRING
            label: Verweis
      pointOfContact:
        type: OBJECT
        objectType: Contact
        label: Kontaktdaten
        description: Enthält notwendige Informationen über den Zugang zu einem Dienst und/oder erste Informationen über einen Dienst an sich.
        properties:
          address:
            type: OBJECT
            objectType: AddressRepresentation
            label: Adresse
            properties:
              thoroughfare:
                sourcePath: strasse
                type: STRING
                label: Straße
                description: Die Bezeichnung(en) eines Durchgangs oder eines Verkehrswegs von einem Standort zu einem anderen – etwa einer Straße oder einer Wasserstraße.
              locatorDesignator:
                sourcePath: hausnummer
                type: STRING
                label: Hausnummer
                description: Eine Anzahl oder Abfolge von Zeichen, die es dem Nutzer oder einer Anwendung erlaubt, den Locator innerhalb des jeweiligen Geltungsbereichs zu interpretieren, zu analysieren und zu formatieren. Ein Locator kann mehrere Locator-Bezeichner enthalten.
              postCode:
                sourcePath: plz
                type: STRING
                label: Postleitzahl
                description: Ein zu postalischen Zwecken geschaffener und zur Untergliederung von Adressen und Zustellungspunkten verwendeter Code.
              adminUnit:
                sourcePath: ort
                type: STRING
                label: Ort
                description: Die Bezeichnung(en) einer Verwaltungseinheit, in der ein Mitgliedstaat Hoheitsbefugnisse für die lokale, regionale und nationale Verwaltung hat und/oder ausübt.
          telephoneVoice:
            sourcePath: telefon
            type: STRING
            label: Telefon
            description: Telefonnummer der Organisation oder Person.
      occupancy:
        type: OBJECT_ARRAY
        objectType: Occupancy
        sourcePath: [oid=kita_fk]plaetze
        label: Belegungskapazität
        description: Belegungskapazität pro Altersklasse
        properties:
          typeOfOccupant:
            sourcePath: art
            type: STRING
            label: Altersklasse
          numberOfOccupants:
            sourcePath: anzahl
            type: INTEGER
            label: Kapazität
            description: Belegungskapazität für die Altersklasse
      geometry:
        sourcePath: geometry
        type: GEOMETRY
        geometryType: POINT
        label: Geometrie
```
