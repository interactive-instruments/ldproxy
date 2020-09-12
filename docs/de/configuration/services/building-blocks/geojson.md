# Modul "Features GeoJSON" (GEO_JSON)

Das Modul "Features GeoJSON" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features und Feature in GeoJSON.

Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der Konformatitätsklasse "GeoJSON" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_geojson).

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`nestedObjectStrategy` |enum |`FLATTEN` |Steuert, ob im Schema des Feature-Providers ggf. definierte Objektstrukturen in der GeoJSON-Ausgabe abgeflacht werden (`FLATTEN`) oder nicht (`NESTED`). Bei der Angabe von `FLATTEN` muss der Wert von `multiplicityStrategy` gleich `SUFFIX` sein, bei `NESTED` muss er gleich `ARRAY` sein.
|`multiplicityStrategy` |enum |`SUFFIX` |Steuert, ob im Schema des Feature-Providers ggf. definierte Arraystrukturen in der GeoJSON-Ausgabe abgeflacht werden (`SUFFIX`) oder nicht (`ARRAY`). Bei der Angabe von `SUFFIX` muss der Wert von `nestedObjectStrategy` gleich `FLATTEN` sein, bei `ARRAY` muss er gleich `NESTED` sein.
|`separator` |string |"." |Steuert das Trennzeichen, das bei `FLATTEN`/`SUFFIX` für die Namen der Objekteigenschaften verwendet wird. Ein Trennzeichen wird immer dann eingesetzt, wenn eine Eigenschaft multipel (ein Array) oder strukturiert (ein Objekt) ist. Im Fall eines Array ergeben sich die Namen der abgeflachten Eigenschaften aus dem Namen der Eigenschaft im Schema und der Position im Array, getrennt durch das Trennzeichen. Bei einer objektwertigen Eigenschaft ergeben sich die Namen der abgeflachten Eigenschaften aus dem Namen der objektwertigen Eigenschaft im Schema und den Namen der Eigenschaften im Datentyp des Objekts, ebenfallse getrennt durch das Trennzeichen.
|`transformations` |object |`{}` |Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in der GeoJSON-Ausgabe [transformiert](#transformations) werden.

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
  nestedObjectStrategy: NESTED
  multiplicityStrategy: ARRAY
```
