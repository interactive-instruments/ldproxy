# Foundation (FOUNDATION)

Adds an API catalog with all published APIs. Provides base functionality for all other modules and therefore cannot be disabled.

## Scope

### Resources

|Resource |Path |HTTP Method |Media Types | Description
| --- | --- | ---
|API Catalog |`/` |GET |JSON, HTML | Provides a list of all published APIs.

#### *API Catalog* Schema

```yaml
type: object
required:
  - apis
properties:
  title:
    type: string
  description:
    type: string
  apis:
    type: array
    items:
      type: object
      required:
        - title
        - landingPageUri
      properties:
        title:
          type: string
        description:
          type: string
        landingPageUri:
          type: string
          format: uri
```

### Query parameters

|Name |Resources |Description
| --- | --- | ---
|lang |Any |The desired response language, overrides the `Accept-Lang` header if set. *Only available if enabled in the configuration.*

## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`includeLinkHeader` |boolean |`true` |Return links contained in API responses also as [HTTP header](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_link_headers).
|`useLangParameter` |boolean |`false` |Support query parameter `lang` to set the desired response language.
|`apiCatalogLabel` |string |"API Overview" |Title for resource *API Catalog*.
|`apiCatalogDescription` |string |"The following OGC APIs are available." |Description for resource *API Catalog*. May contain HTML elements.

### Example

```yaml
- buildingBlock: FOUNDATION
  includeLinkHeader: true
  useLangParameter: false
  apiCatalogLabel: 'APIs für INSPIRE-relevante Datensätze'
  apiCatalogDescription: 'Alle Datensätze ...'
```
