# Modul "Collections Queryables" (QUERYABLES)

Das Modul "Features Custom Extensions" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. 

Es ergänzt die Unterstützung der HTTP-Methode POST auf der Features-Ressource. Der Unterschied zum Aufruf mit GET ist, dass die Query-Parameter als Content im Aufruf übergeben werden. Dies kann aus zwei Gründen gewünscht sein:

* URLs sind in HTTP-Implementierungen in der Länge beschränkt. Umfangreiche Filterausdrücke in GET-Aufrufen sind oft zu lang. Die Verwendung von POST umgeht diese Einschränkung.
* Bei der Verwendung von POST werden die Query-Parameter bei der Verwendung von HTTPS verschlüsselt übertragen und werden nicht in Request-Logs protokolliert. Dies kann aus Sicherheits- oder Datenschutzgründen erwünscht sein.

|Ressource |Pfad |HTTP-Methode |Unterstützte Eingabeformate |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Features |`/{apiId}/collections/{collectionId}/items` |POST |`application/x-www-form-urlencoded` |wie bei GET

Das Modul ergänzt weiterhin die Unterstützung für den folgenden Query-Parameter:

* `intersects` (Ressource "Features"): Ist der Parameter angegeben, werden die Features zusätzlich nach der als Wert angegeben Geometrie selektiert und es werden nur Features zurückgeliefert, deren primäre Geometrie sich mit der angegebenen Geometrie schneidet. Als Geometrie kann entweder eine WKT-Geometrie angegeben werden oder eine URL für ein GeoJSON-Objekt mit einer Geometrie. Im Fall einer FeatureCollection wird die erste Geometrie verwendet.

Es werden die folgenden konfigurierbaren Optionen unterstützt:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`postOnItems` |boolean |`false` |Aktiviert die Unterstützung für die HTTP-Methode POST auf der Ressource "Features"
|`intersectsParameter` |boolean |`false` |Aktiviert die Unterstützung für den Query-Parameter `intersects` auf der Ressource "Features"
