# Collections Queryables (QUERYABLES)

The module *Collections Queryables* may be enabled for every API with a feature provider. It provides the sub-resource *Queryables* for the resource *Feature Collection* that publishes the feature properties that may be used in queries. 

*Collections Queryables* implements all requirements of conformance class *Queryables* from the draft of [OGC API - Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html#rc_queryables). The resource will change in the future due to the harmonization with the requirements for *Queryables* from the draft of [OGC API - Features - Part 3: Common Query Language](http://docs.opengeospatial.org/DRAFTS/19-079.html#filter-queryables).

|Resource |Path |HTTP Method |Media Types
| --- | --- | --- | ---
|Queryables |`/{apiId}/collections/{collectionId}/queryables` |GET |HTML, JSON

## Configuration

This module has no configuration options.
