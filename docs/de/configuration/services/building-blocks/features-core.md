# Modul "Features Core"

Das Modul "Features Core" ist für jede über ldproxy bereitgestellte API mit einem Feature-Provider zu aktivieren. Es stellt die Ressourcen "Features" und "Feature" bereit.

"Features Core" implementiert alle Vorgaben der Konformitätsklasse "Core" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_core) für die zwei genannten Ressourcen.

|Ressource |Pfad |HTTP-Methode |Unterstützte Ausgabeformate
| --- | --- | --- | ---
|Features |`/{apiId}/collections/{collectionId}/items` |GET |[GeoJSON](geojson.md), [HTML](features-html.md), [JSON-FG](json-fg.md), [GML](gml.md)
|Feature |`/{apiId}/collections/{collectionId}/items/{featureId}` |GET |[GeoJSON](geojson.md), [HTML](features-html.md), [JSON-FG](json-fg.md), [GML](gml.md)

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`defaultCrs` |string |"CRS84" |Setzt das Standard-Koordinatenreferenzsystem, entweder 'CRS84' für einen Datensatz mit 2D-Geometrien oder 'CRS84h' für einen Datensatz mit 3D-Geometrien.
|`coordinatePrecision` |object |`{}` |Steuert, ob Koordinaten in Abhängig des verwendeten Koordinatenreferenzsystems auf eine bestimmte Anzahl von Stellen begrenzt werden. Anzugeben ist die Maßeinheit und die zugehörige Anzahl der Nachkommastellen. Beispiel: `{ "metre" : 2, "degree" : 7 }`. Gültige Maßeinheiten sind "metre" (bzw. "meter") und "degree".
|`minimumPageSize` |int |1 |Setzt den Minimalwert für den Parameter `limit`.
|`defaultPageSize` |int |10 |Setzt den Defaultwert für den Parameter `limit`.
|`maximumPageSize` |int |10000 |Setzt den Maximalwert für den Parameter `limit`.
|`featureProvider` |string |die API-ID |Identifiziert den verwendeten Feature-Provider. Standardmäßig besitzt der Feature-Provider dieselbe ID wie die API.
|`featureType` |string |die Collection-ID |Identifiziert die verwendete Objektart im Feature-Provider. Standardmäßig besitzt die Objektart dieselbe ID wie die Collection. Diese Option ist nur im Kontext einer Feature Collection relevant.
|`showsFeatureSelfLink` |boolean |`false` |Steuert, ob in Features immer, auch in der Features-Ressourcen, ein `self`-Link enthalten ist.
|`queryables` |object |`{}` |Steuert, welche der Attribute in Queries für die Filterung von Daten verwendet werden können. Unterschieden werden räumliche (`spatial`), zeitliche (`temporal`) und "normale" (`q`, `other`) Attribute. Die Attribute unter `spatial` müssen im Provider-Schema vom Typ `GEOMETRY`, die Attribute unter `temporal` vom Typ `DATETIME` oder `DATE` sein. Die suchbaren Attribute werden jeweils über ihren Namen in einem Array aufgelistet. Die Queryables können in Filter-Ausdrücken ([Modul "Filter - CQL"](filter.md)) genutzt werden. Die primären räumlichen und zeitlichen Attribute (siehe Provider-Konfiguration) können über die [Parameter `bbox`](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_bbox) bzw. [Parameter `datetime`](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_parameter_datetime) für die Selektion verwendet werden. Die übrigen Attribute werden als [zusätzliche Parameter für die jeweilige Feature Collections](http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_parameters_for_filtering_on_feature_properties) definiert ("*" kann als Wildcard verwendet werden). Auf diese Weise ist eine Selektion von Objekten bereits ohne zusätzliche Module möglich. Die Attribute unter `q` werden außerdem bei der freien Textsuche im Query-Parameter mit demselben Namen berücksichtigt.
|`embeddedFeatureLinkRels` |array |`[]` |Steuert, welche Links bei jedem Feature in der Ressource "Features" angegeben werden sollen, sofern vorhanden. Die Werte sind die Link-Relation-Types, die berücksichtigt werden sollen. Standardmäßig werden Links wie `self` oder `alternate` bei den Features in einer FeatureCollection weggelassen, mit dieser Option können Sie bei Bedarf ergänzt werden.
|`transformations` |object |`{}` |Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in allen Datenformaten [transformiert](general-rules.md#transformations) werden.
|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.

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
  embeddedFeatureLinkRels:
  - self
  - about
  transformations:
    pointOfContact.telephoneVoice:
      nullify: 'bitte ausfüllen'
    inspireId:
      stringFormat: 'https://example.com/id/kinder/kita/{{value}}'
    pointOfContact.address.thoroughfare:
      stringFormat: "{{value | replace:'\\s*[0-9].*$':''}}"
    pointOfContact.address.locatorDesignator:
      null: '^\\D*$'
      stringFormat: "{{value | replace:'^[^0-9]*':''}}"
```
