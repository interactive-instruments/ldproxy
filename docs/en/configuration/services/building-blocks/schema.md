# Collections Schema (SCHEMA)

The module *Collections Schema* may be enabled for every API with a feature provider. It provides a sub-resource *Schema* for the resource *Feature Collection* that publishes the JSON Schema (Draft 07) of the features. The schema is automatically derived from the type definitions in the feature provider.

|Resource |Path |HTTP Method |Media Types
| --- | --- | --- | ---
|Schema |`/{apiId}/collections/{collectionId}/schema` |GET |JSON Schema

## Configuration

This module has no configuration options.
