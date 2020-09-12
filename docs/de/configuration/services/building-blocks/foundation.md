# Modul "ldproxy Foundation" (FOUNDATION)

Das Modul "ldproxy Foundation" ist für jede über ldproxy bereitgestellte API aktiv. Es stellt im Wesentliche interne Funktionalitäten für die übrigen API-Module bereit.

Zusätzlich wird auch die ldproxy-spezifische Ressource "API Catalog" als Liste der aktiven APIs in dem Deployment bereitgestellt.

Das Schema der API-Catalog-Ressource ist:

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

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|API Catalog |`/` |GET |JSON, HTML

Es werden die folgenden konfigurierbaren Optionen unterstützt:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`includeLinkHeader` |boolean |`true` |Steuert, ob die in Antworten der API enthaltenen Links auch als [HTTP-Header](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_link_headers) zurückgegeben werden.
|`useLangParameter` |boolean |`false` |Steuert, ob die Sprache der Antwort bei allen GET-Operationen nur über den `Accept-Lang`-Header oder auch über einen Parameter `lang` ausgewählt werden kann.
|`apiCatalogLabel` |string |"API-Übersicht" |Titel für die API-Catalog-Ressource in diesem Deployment.
|`apiCatalogDescription` |string |"Die folgenden OGC APIs sind verfügbar." |Beschreibung für die API-Catalog-Ressource in diesem Deployment. HTML-Markup wird bei der HTML-Ausgabe aufbereitet.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: FOUNDATION
  useLangParameter: false
  includeLinkHeader: true
  apiCatalogLabel: 'APIs für INSPIRE-relevante Datensätze'
  apiCatalogDescription: 'Alle Datensätze ...'
```
