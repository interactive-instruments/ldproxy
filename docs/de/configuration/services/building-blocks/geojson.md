# Modul "Features GeoJSON" (GEO_JSON)

Das Modul "Features GeoJSON" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features und Feature in GeoJSON.

Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der Konformitätsklasse "GeoJSON" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_geojson).

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`nestedObjectStrategy` |enum |`FLATTEN` |*Deprecated* Wird abgelöst durch die [`flatten`-Transformation](../../providers/transformations.md).
|`multiplicityStrategy` |enum |`SUFFIX` |*Deprecated* Wird abgelöst durch die [`flatten`-Transformation](../../providers/transformations.md).
|`separator` |string |"." |*Deprecated* Wird abgelöst durch die [`flatten`-Transformation](../../providers/transformations.md).
|`transformations` |object |`{}` |Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in der GeoJSON-Ausgabe [transformiert](general-rules.md#transformations) werden.

Ein Beispiel zur Abflachung. Das nicht abgeflachte Feature

```json
{
  "type" : "Feature",
  "id" : "1",
  "geometry" : {
    "type" : "Point",
    "coordinates" : [ 7.0, 50.0 ]
  },
  "properties" : {
    "name" : "Beispiel",
    "inspireId" : "https://example.org/id/soziales/kindergarten/1",
    "serviceType" : {
      "title" : "Kinderbetreuung",
      "href" : "http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService"
    },
    "pointOfContact" : {
      "address" : {
        "thoroughfare" : "Beispielstr.",
        "locatorDesignator" : "123",
        "postCode" : "99999",
        "adminUnit" : "Irgendwo"
      },
      "telephoneVoice" : "0211 16021740"
    },
    "occupancy" : [ {
      "typeOfOccupant" : "vorschule",
      "numberOfOccupants" : 20
    }, {
      "typeOfOccupant" : "schulkinder",
      "numberOfOccupants" : 25
    } ]
  }
}
```

sieht abgeflacht mit dem Standardtrennzeichen wie folgt aus:

```json
{
  "type" : "Feature",
  "id" : "1",
  "geometry" : {
    "type" : "Point",
    "coordinates" : [ 7.0, 50.0 ]
  },
  "properties" : {
    "name" : "Beispiel",
    "inspireId" : "https://example.org/id/soziales/kindergarten/1",
    "serviceType.title" : "Kinderbetreuung",
    "serviceType.href" : "http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService",
    "pointOfContact.address.thoroughfare" : "Otto-Pankok-Str.",
    "pointOfContact.address.locatorDesignator" : "29",
    "pointOfContact.address.postCode" : "40231",
    "pointOfContact.address.adminUnit" : "Düsseldorf",
    "pointOfContact.telephoneVoice" : "0211 16021740",
    "occupancy.1.typeOfOccupant" : "vorschule",
    "occupancy.1.numberOfOccupants" : 20,
    "occupancy.2.typeOfOccupant" : "schulkinder",
    "occupancy.2.numberOfOccupants" : 25
  }
}
```

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: GEO_JSON
  transformations:  
    '*':
      flatten: '.'
```
