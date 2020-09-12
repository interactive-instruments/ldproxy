# Modul "Features GeoJSON-LD" (GEO_JSON_LD)

Das Modul "Features GeoJSON-LD" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden, sofern die GeoJSON-Ausgabe aktiviert ist.

Es ergänzt die GeoJSON-Ausgabe um die folgenden Angaben:

* Einen JSON-LD-Context, auf den aus den GeoJSON-Ausgaben der Ressourcen Features und Feature verwiesen wird. Der Context kann extern liegen oder über die API bereitgestellt werden. Dafür muss im ldproxy-Datenverzeichnis der Context unter dem relativen Pfad `json-ld-contexts/{apiId}/{collectionId}.jsonld` liegen. Der Context muss mindestens die folgenden Einträge enthalten:
  * `"@version": 1.1`
  * `"geojson": "https://purl.org/geojson/vocab#"`
  * `"FeatureCollection": "geojson:FeatureCollection"`
  * `"features": { "@id": "geojson:features", "@container": "@set" }`
  * `"Feature": "geojson:Feature"`
  * `"type": "geojson:type"`
  * `"properties": "@nest"`
* Zusätzlich zur Eigenschaft "type", die in GeoJSON fest mit "Feature" belegt ist, wird "@type" als weitere Eigenschaft mit den in der Konfiguration angegeben Werten ergänzt.
* Zusätzlich zur Eigenschaft "id", wird "@id" als weitere Eigenschaft auf Basis des Wertes aus "id" und dem in der Konfiguration angegeben URI-Template ergänzt. Dabei wird `{{serviceUrl}}` durch die Landing-Page-URI der API, `{{collectionId}}` durch die Collection-ID und `{{featureId}}` durch den Wert von "id" ersetzt.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|JSON-LD Context |`/{apiId}/collections/{collectionId}/context` |GET |JSON-LD Context

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`context` |string |`null` |Die URI des JSON-LD-Context-Dokuments. Dabei wird `{{serviceUrl}}` durch die Landing-Page-URI der API und `{{collectionId}}` durch die Collection-ID ersetzt. Sofern der Context nicht extern liegt, sollte der Wert "{{serviceUrl}}/collections/{{collectionId}}/context" sein.
|`types` |array |`[ "geojson:Feature" ]` |Der Wert von "@type" bei den Features der Collection.
|`idTemplate` |string |`null` |Der Wert von "@id" bei den Features der Collection. Dabei wird `{{serviceUrl}}` durch die Landing-Page-URI der API, `{{collectionId}}` durch die Collection-ID und `{{featureId}}` durch den Wert von "id" ersetzt.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: GEO_JSON_LD
  enabled: true
  context: '{{serviceUrl}}/collections/{{collectionId}}/context'
  types:
  - geojson:Feature
  - sosa:Observation
  idTemplate: '{{serviceUrl}}/collections/{{collectionId}}/items/{{featureId}}'
```
