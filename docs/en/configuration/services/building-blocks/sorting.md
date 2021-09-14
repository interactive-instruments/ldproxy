# Sorting (SORTING)

The module *Sorting* may be enabled for every API with a feature provider that supports sorting. It adds the following query parameter:

* `sortby` (resource *Features*): If the parameter is specified, the features are returned sorted according to the attributes specified in a comma-separated list. The attribute name can be preceded by `+` (ascending, the default behavior) or `-` (descending). Example: `sortby=type,-name`.

The following options can be selected in the configuration:

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`sortables` |object |`{}` |Controls which of the attributes in queries can be used for sorting data. Only direct attributes of the data types `STRING`, `DATETIME`, `INTEGER` and `FLOAT` are allowed (no attributes from arrays or embedded objects). A current limitation is that all attributes must have unique values, see [Issue 488](https://github.com/interactive-instruments/ldproxy/issues/488).
