# SQL Feature Provider

The specifics of the SQL feature provider.

<a name="connection-info"></a>

## Connection Info for SQL databases

The connection info object for SQL databases has the following properties:

|Property |Data Type |Default |Description
| --- | --- | --- | ---
|`dialect` |enum |`PGIS` |`PGIS` for PostgreSQL/PostGIS, `GPKG` for GeoPackage or SQLite/SpatiaLite.
|`database` |string | |The name of the database. For `GPKG` the file path, either absolute or relative to the [data folder](../../data-folder.md).
|`host` |string | |The database host. To use a non-default port, add it to the host separated by `:`, e.g. `db:30305`. Not relevant for `GPKG`. 
|`user` |string | |The user name. Not relevant for `GPKG`.
|`password` |string | |The base64 encoded password of the user. Not relevant for `GPKG`.
|`schemas` |array |`[]` |The names of database schemas that should be used in addition to `public`. Not relevant for `GPKG`.
|`pool` |object |see below |Connection pool settings, for details see [Pool](#connection-pool) below. 
|`sourcePathDefaults` |object |see below |Defaults for the path expressions in `sourcePath`, for details see [Source Path Defaults](#source-path-defaults) below. 
|`queryGeneration` |object |see below |Options for query generation, for details see [Query Generation](#query-generation) below. 
|`driverOptions` |object |`{}` |Custom options for the JDBC driver. For `PGIS`, you might pass `gssEncMode`, `ssl`, `sslmode`, `sslcert`, `sslkey`, `sslrootcert` and `sslpassword`. For details see the [driver documentation](https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters).
|`initFailFast` |boolean |`true` |*Deprecated* See `pool.initFailFast`.
|`maxConnections` |integer |dynamic |*Deprecated* See `pool.maxConnections`.
|`minConnections` |integer |`maxConnections` |*Deprecated* See `pool.minConnections`.
|`maxThreads` |integer |dynamic |*Deprecated* See `pool.maxConnections`.
|`pathSyntax` |object |`{ 'defaultPrimaryKey': 'id', 'defaultSortKey': 'id' }` | *Deprecated* See [Source Path Defaults](#source-path-defaults) below. 
|`computeNumberMatched` |boolean |`true` |*Deprecated* See [Query Generation](#query-generation) below. 

For `connectionInfo` [the whole object has to be set/repeated](../global-configuration.md#merge-exceptions) in all cases. A `connectionInfo` object in overrides will replace `connectionInfo` in the defaults or the regular provider. 

If `connectionInfo` is always set in overrides, a minimal configuration has to be set in the regular provider, e.g.:

```yaml
connectionInfo:
  database: ''
```

<a name="connection-pool"></a>

### Pool

Settings for the connection pool.

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`maxConnections` |integer |dynamic |Maximum number of connections to the database. The default value is computed depending on the number of processor cores and the maximum number of joins per feature type in the [Types Configuration](README.md#feature-provider-types). The default value is recommended for optimal performance under load. The smallest possible value also depends on the maximum number of joins per feature type, smaller values are rejected. 
|`minConnections` |integer |`maxConnections` |Minimum number of connections to the database that are maintained.
|`idleTimeout` |string |`10m` |The maximum amount of time that a connection is allowed to sit idle in the pool. Only applies to connections beyond the `minConnections` limit. A value of 0 means that idle connections are never removed from the pool.
|`initFailFast` |boolean |`true` |If disabled the provider will wait longer for the first database connection to be established. Has no effect if `minConnections` is `0`. Should normally be disabled only on development systems.

<a name="source-path-defaults"></a>

### Source Path Defaults

Defaults for the path expressions in `sourcePath`.

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`sortKey` |string |`id` |The default column that is used to sort rows if no differing sort key is set in the [sourcePath](#path-syntax).
|`primaryKey` |string |`id` |The default column that is used for join analysis if no differing primary key is set in the [sourcePath](#path-syntax).

<a name="query-generation"></a>

### Query Generation

Options for query generation.

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`computeNumberMatched` |boolean |`true` |Option to disable computation of the number of selected features for performance reasons that are returned in `numberMatched`. As a general rule this should be disabled for big datasets.


<a name="path-syntax"></a>

## Source Path Syntax

The fundamental elements of the path syntax are demonstrated in the example above. The path to a property is formed by concatenating the relative paths (`sourcePath`) with "/". A `sourcePath` has to be defined for the for object that represents the feature type and most child objects.

On the first level the path is formed by a "/" followed by the table name for the feature type. Every row in the table corresponds to a feature. Example: `/kita`

When defining a feature property on a deeper level using a column from the given table, the path equals the column name, e.g. `name`. The full path will then be `/kita/name`.

A join is defined using the pattern `[id=fk]tab`, where `id` is the primary key of the table from the parent object, `fk` is the foreign key of the joining table and `tab` is the name of the joining table. Example from above: `[oid=kita_fk]plaetze`. When a junction table should be used, two such joins are concatenated with "/", e.g. `[id=fka]a_2_b/[fkb=id]tab_b`.

Rows for a table can be filtered by adding `{filter=expression}` after the table name, where `expression` is a [CQL Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) expression. For details see the module [Filter / CQL](../services/filter.md), which provides the implementation but does not have to be enabled.

To select capacity information only when the value is not NULL and greater than zero in the example above, the filter would look like this: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`

A non-default sort key can be set by adding `{sortKey=columnName}` after the table name.

A non-default primary key can be set by adding `{primaryKey=columnName}` after the table name.
