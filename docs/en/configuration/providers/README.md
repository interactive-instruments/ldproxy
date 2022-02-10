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

See the [feature provider](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/providers/vineyards.yml) of the API [Vineyards in Rhineland-Palatinate, Germany](https://demo.ldproxy.net/vineyards).
