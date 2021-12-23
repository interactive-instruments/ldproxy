# Modul "Features JSON-FG" (JSON_FG)

Das Modul "Features JSON-FG" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features und Feature in JSON-FG.

Das Modul basiert auf den [Entwürfen für JSON-FG](https://github.com/opengeospatial/ogc-feat-geo-json). Die Implementierung wird sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`when` |boolean |`true` |Aktiviert die Ausgabe von "when" bei Features mit einer primären zeitlichen Eigenschaft
|`where` |object |`{ "enabled": true, "alwaysIncludeGeoJsonGeometry": false }` |Aktiviert die Ausgabe von "where" bei Features mit Geometrien in einem anderen Koordinatenreferenzsystem als `CRS84` oder `CRS84h`
|`coordRefSys` |boolean |`true` |Aktiviert die Ausgabe von "coordRefSys" bei Features
|`describedby` |boolean |`true` |Aktiviert die Ausgabe von Links auf JSON-Schema-Dokumente zu der JSON-Instant, z.B. zur Validierung
|`featureType` |array |`[]` |Aktiviert die Ausgabe von "featureType" mit den angegebenen Werten bei Features. Ist eine Objektart angegeben, dann wird ein String ausgegeben, ansonsten ein Array von Strings.
|`links` |array |`[]` |Ergänzt den "links"-Array von Features um die angegebenen Links. Alle Werte des Arrays müssen ein gültiges Link-Objekt mit `href` und `rel` sein.
|`includeInGeoJson` |array |`[]` |Die Option ermöglicht, dass ausgewählte JSON-FG-Erweiterungen auch im GeoJSON-Encoding berücksichtigt werden. Erlaubte Werte sind: `describedby`, `featureType`, `when`, `where`, `coordRefSys`, `links``

Beispiel für die Angaben in der Konfigurationsdatei für die gesamte API (aus der API für [Topographische Daten in Daraa, Syrien](https://demo.ldproxy.net/daraa)):

```yaml
- buildingBlock: JSON_FG
  enabled: true
  featureType: 
  - nas:{{type}}
```

Ergänzende Angaben pro Feature Collection mit einem Attribut `F_CODE` (für das in der Provider-Konfiguration `role: TYPE` gesetzt wurde), um die Objektart zu setzen:

```yaml
- buildingBlock: JSON_FG
  featureType: 
  - nas:{{type}}
```

Hierdurch wird bei einem Wert von "GB075" im Attrubut `F_CODE` die Objektart wie folgt ausgegeben:

```json
{
  "type": "Feature",
  "id": 1,
  "featureType": "nas:GB075",
  ...
}
```
