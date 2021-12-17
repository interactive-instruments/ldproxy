# Modul "Coordinate Reference Systems" (CRS)

Das Modul "CRS" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt die Unterstützung für weitere Koordinatenreferenzsysteme neben dem Standard-Koordinatenreferenzsystem [CRS84](http://www.opengis.net/def/crs/OGC/1.3/CRS84) (WGS 84).

Alle Koordinatentransformationen zwischen zwei Koordinatenreferenzsystemen erfolgen mit Geotools. Geotools entscheidet, welche Transformation verwendet wird, sofern mehrere verfügbar sind. Eine Konfigurationsmöglichkeit in ldproxy besteht nicht.

Das Modul implementiert alle Vorgaben der Konformitätsklasse "Coordinate Reference System by Reference" von [OGC API - Features - Part 2: Coordinate Reference System by Reference 1.0](http://www.opengis.net/doc/IS/ogcapi-features-2/1.0).

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`additionalCrs` |object |`{}` |Steuert, welche weitere Koordinatenreferenzsysteme in einer API oder für eine Feature Collection unterstützt werden sollen. Das native Koordinatenreferenzsystem der Daten und das Default-Koordinatenreferenzsystem der API sind automatisch aktiviert. Koordinatenreferenzsysteme werden über ihren EPSG-Code identifiziert (`code`). Zusätzlich ist in `forceAxisOrder` die Reihenfolge der Koordinatenachsen anzugeben (`NONE`: wie im Koordinatenreferenzsystem, `LON_LAT` oder `LAT_LON`: die Reihenfolge im Koordinatenreferenzsystem wird ignoriert und die angegebene Reihenfolge wird verwendet).

Das Default-Koordinatenreferenzsystem `CRS84` entspricht `code: 4326, forceAxisOrder: LON_LAT`, `CRS84h` entspricht `code: 4979, forceAxisOrder: LON_LAT`.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: CRS
  additionalCrs:
  - code: 25832
    forceAxisOrder: NONE
  - code: 4258
    forceAxisOrder: NONE
  - code: 4326
    forceAxisOrder: NONE
```

Durch Angabe des Query-Parameters `crs` bei den Ressourcen "Features" und "Feature" können die Koordinaten in einem der konfigurierten Koordinatenreferenzsystemen angefordert werden.
