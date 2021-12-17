# Modul "Collections Queryables" (QUERYABLES)

Das Modul "Collections Queryables" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt eine Ressource als Sub-Ressource zu jeder Feature Collection, die die Objekteigenschaften, die zur Selektion in Queries verwendet werden können, in der API veröffentlicht.

Das Modul basiert auf den Vorgaben der Konformitätsklasse "Filter" aus dem [Entwurf von OGC API - Features - Part 3: Filtering](https://docs.ogc.org/DRAFTS/19-079r1.html#filter-queryables).

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Queryables |`/{apiId}/collections/{collectionId}/queryables` |GET |HTML, JSON

Es werden die folgenden konfigurierbaren Optionen unterstützt:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.
