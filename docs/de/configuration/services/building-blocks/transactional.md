# Modul "Create, Replace, Update, Delete" (TRANSACTIONAL)

Das Modul basiert auf den Vorgaben der Konformitätsklassen "Create/Replace/Delete" und "Features" aus dem [Entwurf von OGC API - Features - Part 4: Create, Replace, Update and Delete](https://docs.ogc.org/DRAFTS/20-002.html). Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ein- und Ausgabeformate
| --- | --- | --- | ---
|Features |`{apiId}/collection/{collectionId}/items` |POST |GeoJSON
|Feature |`{apiId}/collection/{collectionId}/items/{featureId}` |PUT<br>DELETE |GeoJSON<br>n/a

In der Konfiguration können keine Optionen gewählt werden.
