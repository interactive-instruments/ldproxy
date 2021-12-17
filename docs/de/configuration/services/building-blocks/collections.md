# Modul "Feature Collections" (COLLECTIONS)

Das Modul "Feature Collections" ist für jede über ldproxy bereitgestellte API mit einem Feature-Provider zu aktivieren. Es stellt die Ressourcen "Collections" und "Collection" bereit. Derzeit sind Feature Collections die einzige unterstütze Art von Collections.

"Feature Collections" implementiert alle Vorgaben der Konformitätsklasse "Core" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) für die zwei genannten Ressourcen.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | ---
|Feature Collections |`/{apiId}/collections` |GET |JSON, HTML, XML
|Feature Collection |`/{apiId}/collections/{collectionId}` |GET |JSON, HTML, XML

Es werden die folgenden konfigurierbaren Optionen unterstützt:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`additionalLinks` |array |`[]` |Erlaubt es, zusätzliche Links in der Ressource Feature Collections zu ergänzen. Der Wert ist ein Array von Link-Objekten. Anzugeben sind jeweils mindestens die URI (`href`), der anzuzeigende Text (`label`) und die Link-Relation (`rel`).
|`collectionIdAsParameter` |boolean |`false` |Steuert, ob in der API-Definition jede Feature Collection und untergeordnete Ressourcen jeweils als einzelne Ressource aufgeführt wird (`false`), oder ob ein Pfad-Parameter `collectionId` verwendet wird und jede Ressource nur einmal in der Definition spezifiziert wird (`true`). Bei `true` wird die API-Definition einfacher und kürzer, aber das Schema ist nicht mehr Collection-spezifisch und Collection-spezifische Query-Parameter können nicht mehr in der API-Definition spezifiziert werden.
|`collectionDefinitionsAreIdentical` |boolean |`false` |Sofern im Fall von `collectionIdAsParameter: true` alle Collections ein strukturell identisches Schema besitzen und dieselben Queryables haben, kann mit dem Wert `true` gesteuert werden, dass in der API-Definition Schema und Queryables aus einer beliebigen Collection bestimmt werden.
|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.

Hinweis: Zusätzliche Links zu einer bestimmten Feature Collection einzelnen können bei der Konfiguration der Collection angegeben werden.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: COLLECTIONS
  additionalLinks:
  - rel: describedby
    type: text/html
    title: Webseite mit weiteren Informationen
    href: 'https://www.example.com/pfad/zu/webseite'
    hreflang: de
  - rel: enclosure
    type: application/geopackage+sqlite3
    title: Download des Datensatzes als GeoPackage
    href: 'https://www.example.com/pfad/zu/datei.gpkg'
    hreflang: de
    length: 23517882
```
