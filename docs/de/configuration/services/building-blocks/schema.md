# Modul "Collections - Schema" (SCHEMA)

Das Modul "Collections Schema" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt eine Ressource als Sub-Ressource zu jeder Feature Collection, die das Schema der Features, in der API veröffentlicht. Das Schema wird aus den Schemainformationen im Feature-Provider abgeleitet. Aktuell wird JSON Schema (Draft 07) für die GeoJSON-Ausgabe unterstützt.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Schema |`/{apiId}/collections/{collectionId}/schema` |GET |JSON Schema

In der Konfiguration können keine Optionen gewählt werden.
