# Modul "Features Core"

Das Modul "Features Core" ist für jede über ldproxy bereitgestellte API mit einem Feature-Provider zu aktivieren. Es stellt die Ressourcen "Features" und "Feature" bereit.

"Features Core" implementiert alle Vorgaben der Konformatitätsklasse "Core" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) für die zwei genannten Ressourcen.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Features |`/{apiId}/collections/{collectionId}/items` |GET |[GeoJSON](geojson.md), [HTML](features-html.md), [GML](gml.md)
|Feature |`/{apiId}/collections/{collectionId}/items/{featureId}` |GET |[GeoJSON](geojson.md), [HTML](features-html.md), [GML](gml.md)

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`defaultCrs` |string |"CRS84" |Setzt das Standard-Koordinatenreferenzsystem, entweder 'CRS84' für einen Datensatz mit 2D-Geometrien oder 'CRS84h' für einen Datensatz mit 3D-Geometrien.
|`minimumPageSize` |int |1 |Setzt den Minimalwert für den Parameter `limit`.
|`defaultPageSize` |int |10 |Setzt den Detaultwert für den Parameter `limit`.
|`maximumPageSize` |int |10000 |Setzt den Maximalwert für den Parameter `limit`.
|`featureProvider` |string |die API-ID |Identifiziert den verwendeten Feature-Provider. Standardmäßig besitzt der Feature-Provider dieselbe ID wie die API.
|`featureType` |string |die Collection-ID |Identifiziert die verwendete Objektart im Feature-Provider. Standardmäßig besitzt die Objektart dieselbe ID wie die Collection. Diese Option ist nur im Kontext einer Feature Collection relevant.
|`showsFeatureSelfLink` |boolean |`false` |Steuert, ob in Features immer, auch in der Features-Ressourcen, ein `self`-Link enthalten ist.
|`queryables` |object |`{}` |Steuert, welche der Attribute in Queries für die Filterung von Daten verwendet werden können. Unterschieden werden räumliche (`spatial`), zeitliche (`temporal`) und "normale" (`other`) Attribute. Die Attribute unter `spatial` müssen im Provider-Schema vom Typ `GEOMETRY`, die Attribute unter `temporal` vom Typ `DATETIME` sein. Die suchbaren Attribute werden jeweils über ihren Namen in einem Array aufgelistet. Die Queryables können sowohl in Filter-Ausdrücken ([Modul "Filter - CQL"](filter.md)) als auch für die Filterparameter gemäß [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0) genutzt werden. Dabei wirkt der [Parameter `bbox`](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_bbox) auf das erstgenannte räumliche Attribut. Der [Parameter `datetime`](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_datetime) wirkt auf die ersten beiden zeitlichen Attribute, die als Beginn und Ende des Zeitintervalls interpretiert werden. Ist nur ein zeitliches Attribut angegeben, dann wirkt der Parameter `datetime` nur auf diesen Zeitpunkt. Die übrigen Attribute werden als [zusätzliche Parameter für die jeweilige Feature Collections](http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_parameters_for_filtering_on_feature_properties) definiert ("*" kann als Wildcard verwendet werden). Auf diese Weise ist eine Selektion von Objekten bereits ohne zusätziche Module möglich.
|`transformations` |object |`{}` |Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in allen Datenformaten [transformiert](README.md#transformations) werden.

Beispiel für die Angaben in der Konfigurationsdatei für die gesamte API:

```yaml
- buildingBlock: FEATURES_CORE
  defaultCrs: CRS84
  minimumPageSize: 1
  defaultPageSize: 10
  maximumPageSize: 10000
  showsFeatureSelfLink: true
```

Beispiel für die Angaben in der Konfigurationsdatei für eine Feature Collection:

```yaml
    - buildingBlock: FEATURES_CORE
      queryables:
        spatial:
        - geometry
        temporal:
        - beginLifespanVersion
        - endLifespanVersion
        other:
        - name
        - pointOfContact.address.postCode
        - pointOfContact.address.adminUnit
        - pointOfContact.telephoneVoice
      transformations:
        pointOfContact.telephoneVoice:
          null: 'bitte ausfüllen'
        inspireId:
          stringFormat: 'https://example.com/id/kinder/kita/{{value}}'
        pointOfContact.address.thoroughfare:
          stringFormat: "{{value | replace:'\\s*[0-9].*$':''}}"
        pointOfContact.address.locatorDesignator:
          null: '^\\D*$'
          stringFormat: "{{value | replace:'^[^0-9]*':''}}"
```
