# Modul "Collections - Schema" (SCHEMA)

Das Modul "Collections Schema" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt Ressourcen als Sub-Ressource zu jeder Feature Collection, die das Schema der GeoJSON Features veröffentlicht. Das Schema wird aus den Schemainformationen im Feature-Provider abgeleitet. Aktuell wird JSON Schema 2019-09 für die GeoJSON-Ausgabe unterstützt.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Feature Schema |`/{apiId}/collections/{collectionId}/schemas/feature` |GET |JSON Schema for each GeoJSON feature in the collection
|FeatureCollection Schema |`/{apiId}/collections/{collectionId}/schemas/collection` |GET |JSON Schema for GeoJSON feature collections of the features in the collection

In der Konfiguration können keine Optionen gewählt werden.

Es werden die folgenden konfigurierbaren Optionen unterstützt:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.
