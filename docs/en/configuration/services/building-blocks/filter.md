# Filter - CQL (FILTER)

Adds support for CQL filter expressions in queries to select [Features](features-core.md) or [Vector Tiles](tiles.md).

## Scope

### Conformance classes

This module implements requirements of the conformance classes *Filter*, *Features Filter*, *Simple CQL*, *CQL Text* and *CQL JSON* from the draft specification [OGC API - Features - Part 3: Common Query Language](http://docs.opengeospatial.org/DRAFTS/19-079.html). The implementation is subject to change in the course of the development and approval process of the draft.

### Query parameters

|Name |Resources |Description
| --- | --- | ---
filter |*Features*, *Vector Tile*| The filter expression.
filter-lang |*Features*, *Vector Tile*| The filter language, either `cql-text` or `cql-json`.

## Prerequisites

|Module |Required |Description
| --- | --- | ---
[Collections Queryables](queryables.md)| Yes | Publishes the queryables, i.e. the feature properties usable in filter expressions.
[Features Core](features-core.md) |Yes | Provides the resource *Features*, which is extended by this module.
[Vector Tiles](tiles.md) |No | Provides the resource *Vector Tile*, which is extended by this module.

## Configuration

This module has no configuration options.
