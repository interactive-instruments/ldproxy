# Modul "Collections Queryables" (QUERYABLES)

Das Modul "Collections Queryables" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt eine Ressource als Sub-Ressource zu jeder Feature Collection, die die Objekteigenschaften, die zur Selektion in Queries verwendet werden können, in der API veröffentlicht.

Das Modul basiert auf den Vorgaben der Konformitätsklasse "Queryables" aus dem [Entwurf von OGC API - Styles](http://docs.opengeospatial.org/DRAFTS/20-009.html#rc_queryables). Die Ressource wird sich zukünftig im Zuge der Harmonisierung mit den Vorgaben zu "Queryables" aus dem [Entwurf von OGC API - Features - Part 3: Common Query Language](http://docs.opengeospatial.org/DRAFTS/19-079.html#filter-queryables) noch ändern.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Queryables |`/{apiId}/collections/{collectionId}/queryables` |GET |HTML, JSON

In der Konfiguration können keine Optionen gewählt werden.

|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.
