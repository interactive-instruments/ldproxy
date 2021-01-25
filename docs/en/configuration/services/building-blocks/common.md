# Common Core (COMMON)

The module *Common Core* is always enabled. It provides the resources *Landing Page*, *Conformance Declaration* and *API Definition*.

*Common Core* implements all requirements of conformance class *Core* of [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) for the three mentioned resources.

> **Note**: The conformance class *Core* was split up into multiple modules in anticipation of the upcoming standard *OGC API Common*.

|Resource |Path |HTTP Method |Media Types
| --- | --- | --- | ---
|Landing Page |`/{apiId}/` |GET |JSON, HTML, XML
|Conformance Declaration |`/{apiId}/conformance` |GET |JSON, HTML, XML
|API Definition |`/{apiId}/api` |GET |see module [OpenAPI 3.0](oas30.md)

## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`additionalLinks` |array |`[]` |Add additional links to the *Landing Page* resource. The value is an array of link objects. Required properties of a link are a URI (`href`), a label (`label`) and a relation (`rel`).

### Example

```yaml
- buildingBlock: COMMON
  additionalLinks:
  - rel: describedby
    type: text/html
    title: Webseite mit weiteren Informationen
    href: 'https://example.com/pfad/zu/dokument'
    hreflang: de
```
