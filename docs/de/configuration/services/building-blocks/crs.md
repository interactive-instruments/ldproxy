# Modul "Coordinate Reference Systems" (CRS)

Das Modul "CRS" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt die Unterstützung für weitere Koordinatenreferenzsysteme neben dem Standard-Koordinatenreferenzsystem [CRS84](http://www.opengis.net/def/crs/OGC/1.3/CRS84) (WGS 84).

Alle Koordinatentransformationen zwischen zwei Koordinatenreferenzsystemen erfolgen mit Geotools. Geotools entscheidet, welche Transformation verwendet wird, sofern mehrere verfügbar sind. Eine Konfigurationsmöglichkeit in ldproxy besteht nicht.

Das Modul implementiert alle Vorgaben der Konformatitätsklasse "Coordinate Reference System by Reference" von [OGC API - Features - Part 2: Coordinate Reference System by Reference 1.0.0-draft.1](http://docs.opengeospatial.org/DRAFTS/18-058.html).

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`additionalCrs` |object |`{}` |Steuert, welche weitere Koordinatenreferenzsysteme in einer API oder für eine Feature Collection unterstützt werden sollen.

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
