# SQL Feature Provider

The specifics of the SQL feature provider.

<a name="connection-info"></a>

## Connection Info for SQL databases

The connection info object for PostgreSQL/PostGIS databases has the following properties:

|Property |Data Type |Default |Description
| --- | --- | --- | ---
|`connectorType` |enum | |Always `SLICK`.
|`dialect` |enum | |Always `PGIS`.
|`host` |string | |The database host. To use a non-default port, add it to the host separated by `:`, e.g. `db:30305`.
|`database` |string | |The name of the database.
|`schemas` |array |`[]` |The names of database schemas that should be used in addition to `public`.
|`user` |string | |The user name.
|`password` |string | |The base64 encoded password of the user.
|`initFailFast` |boolean |`true` |If disabled the provider will wait longer for the databse connection to be established. Should normally be disabled only on development systems.
|`maxConnections` |integer |dynamic |Maximum number of connections to the database. The default value is computed depending on the number of processor cores and the maximum number of joins per feature type in the [Types Configuration](README.md#feature-provider-types). The default value is recommended for optimal performance under load. The smallest possible value also depends on the maximum number of joins per feature type, smaller values are rejected. 
|`minConnections` |integer |`maxConnections` |Minimum number of connections to the database that are maintained.
|`maxThreads` |integer |dynamic |*Deprecated* See `maxConnections`
|`pathSyntax` |object |`{ 'defaultPrimaryKey': 'id', 'defaultSortKey': 'id', 'junctionTablePattern': '.+_2_.+' }` |`defaultPrimaryKey`: Default primary key for all tables. Default is `id`. <br>`defaultSortKey`: Default column that is used to sort rows from all tables. Default is `id`. For performance reasons it is recommended to use a whole-number column. <br>`junctionTablePattern`: Regular expression for the detection of junction tables, the default would match for example `featurea_2_featureb`.
|`computeNumberMatched` |boolean |`true` |Option to disable computation of the number of selected features for performance reasons that are returned in `numberMatched`. As a general rule this should be disabled for big datasets.

For `connectionInfo` [the whole object has to be set/repeated](../global-configuration.md#merge-exceptions) in all cases. A `connectionInfo` object in overrides will replace `connectionInfo` in the defaults or the regular provider. 

If `connectionInfo` is always set in overrides, a minimal configuration has to be set in the regular provider, e.g.:

```yaml
connectionInfo:
  host: ''
  database: ''
  user: ''
  password: ''
```

<a name="path-syntax"></a>

## Path Syntax

The fundamental elements of the path syntax are demonstrated in the example above. The path to a property is formed by concatenating the relative paths (`sourcePath`) with "/". A `sourcePath` has to be defined for the for object that represents the feature type and most child objects.

On the first level the path is formed by a "/" followed by the table name for the feature type. Every row in the table corresponds to a feature. Example: `/kita`

When defining a feature property on a deeper level using a column from the given table, the path equals the column name, e.g. `name`. The full path will then be `/kita/name`.

A join is defined using the pattern `[id=fk]tab`, where `id` is the primary key of the table from the parent object, `fk` is the foreign key of the joining table and `tab` is the name of the joining table. Example from above: `[oid=kita_fk]plaetze`. When a junction table should be used, two such joins are concatenated with "/", e.g. `[id=fka]a_2_b/[fkb=id]tab_b`.

Rows for a table can be filtered by adding `{filter=expression}` after the table name, where `expression` is a [CQL Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) expression. For details see the module [Filter / CQL](../services/filter.md), which provides the implementation but does not have to be enabled.

To select capacity information only when the value is not NULL and greater than zero in the example above, the filter would look like this: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`
