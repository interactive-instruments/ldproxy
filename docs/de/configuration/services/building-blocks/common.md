# Modul "Common Core" (COMMON)

Das Modul "Common Core" ist für jede über ldproxy bereitgestellte API aktiv. Es stellt die Ressourcen "Landing Page", "Conformance Declaration" und "API Definition" bereit.

"Common Core" implementiert alle Vorgaben der Konformatitätsklasse "Core" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) für die drei genannten Ressourcen.

Hinweis: Die Konformatitätsklasse wurde in ldproxy auf drei Module aufgeteilt, da vorgesehen ist, die jeweiligen Anforderungen für die Nutzung in anderen OGC API Standards als zwei Teile von OGC API Common zu veröffentlichen. Die Module "Common Core" und "Feature Collections" bilden dies ab.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Landing Page |`/{apiId}/` |GET |JSON, HTML, XML
|Deklaration der Konformität |`/{apiId}/conformance` |GET |JSON, HTML, XML
|API-Definition |`/{apiId}/api` |GET |siehe [Modul "OpenAPI 3.0"](oas30.md)

Es werden die folgenden konfigurierbaren Optionen unterstützt:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`additionalLinks` |array |`[]` |Erlaubt es, zusätzliche Links in der Landing Page zu ergänzen. Der Wert ist ein Array von Link-Objekten. Anzugeben sind jeweils mindestens die URI (`href`), der anzuzeigende Text (`label`) und die Link-Relation (`rel`).

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: COMMON
  additionalLinks:
  - rel: describedby
    type: text/html
    title: Webseite mit weiteren Informationen
    href: 'https://example.com/pfad/zu/dokument'
    hreflang: de
```
