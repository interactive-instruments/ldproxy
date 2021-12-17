# Modul "Features HTML" (FEATURES_HTML)

Das Modul "Features HTML" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features und Feature in HTML.

Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der Konformitätsklasse "HTML" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html).

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`layout` |enum |`CLASSIC` |*Deprecated* Wird abgelöst von `mapPosition` und der [`flatten`-Transformation](../providers/transformations.md).
|`mapPosition` |enum |`AUTO` |Mögliche Werte sind `TOP`, `RIGHT` und `AUTO`. `AUTO` ist der Default, es wählt automatisch `TOP` wenn verschachtelte Objekte gefunden werden und sonst `RIGHT`.
|`style` |string |`DEFAULT` |Ein Style im Style-Repository, der standardmäßig in Karten mit den Features verwendet werden soll. Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet. Bei `NONE` wird ein einfacher Style mit OpenStreetMap als Basiskarte verwendet. Der Style sollte alle Daten abdecken und muss im Format Mapbox Style verfügbar sein. Es wird zuerst nach einem Style mit dem Namen für die Feature Collection gesucht; falls keiner gefunden wird, wird nach einem Style mit dem Namen auf der API-Ebene gesucht. Wird kein Style gefunden, wird `NONE` verwendet.
|`removeZoomLevelConstraints`|boolean |`false` |Bei `true` werden aus dem in `style` angegebenen Style die `minzoom`- und `maxzoom`-Angaben bei den Layer-Objekten entfernt, damit die Features in allen Zoomstufen angezeigt werden. Diese Option sollte nicht gewählt werden, wenn der Style unterschiedliche Präsentationen je nach Zoomstufe vorsieht, da ansonsten alle Layer auf allen Zoomstufen gleichzeitig angezeigt werden.
|`featureTitleTemplate` oder `itemLabelFormat` |string |`{{id}}` |Steuert, wie der Titel eines Features in der HTML-Ausgabe gebildet wird. Standardmäßig ist der Titel der Identifikator. In der Angabe können über die Angabe des Attributnamens in doppelt-geschweiften Klammern Ersetzungspunkte für die Attribute des Features verwendet werden. Es können nur Attribute verwendet werden, die nur einmal pro Feature vorkommen können. Neben einer direkten Ersetzung mit dem Attributwert können auch [Filter](general-rules.md#String-Template-Filter) angewendet werden. Ist ein Attribut `null`, dann wird der Ersetzungspunkt durch einen leeren String ersetzt.
|`mapClientType` |enum |`MAP_LIBRE` |Auswahl des in den Ressourcen "Features" und "Feature" zu verwendenden Map-Clients. Der Standard ist MapLibre GL JS. Alternativ wird als auch `CESIUM` unterstützt (CesiumJS). Die Unterstützung von CesiumJS zielt vor allem auf die Darstellung von 3D-Daten ab und besitzt in der aktuellen Version experimentellen Charakter, es werden keine Styles unterstützt.
|`geometryProperties` |array |`[]` |Diese Option wirkt nur für CesiumJS als Map-Client. Als Standard wird die im Provider als PRIMARY_GEOMETRY identifizierte Geometrie für die Darstellung in der Karte verwendet. Diese Option ermöglicht es, mehrere Geometrieeigenschaften anzugeben in einer Liste anzugeben. Die erste Geometrieeigenschaft, die für ein Feature gesetzt ist, wird dabei verwendet.
|`maximumPageSize` |int |`null` |Mit dieser Option kann für die HTML-Ausgabe ein eigener Maximalwert für den Parameter `limit` gesetzt werden. Sofern kein Wert angegeben ist, so gilt der Wert aus dem Modul "Features Core". Bei der Verwendung von CesiumJS als Map-Client wird ein Wert von 100 empfohlen.
|`transformations` |object |`{}` |Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in der HTML-Ausgabe [transformiert](general-rules.md#transformations) werden.

Beispiel für die Angaben in der Konfigurationsdatei für die gesamte API:

```yaml
- buildingBlock: FEATURES_HTML
  schemaOrgEnabled: false
  mapPosition: TOP
```

Beispiel für die Angaben in der Konfigurationsdatei für eine Feature Collection:

```yaml
    - buildingBlock: FEATURES_HTML
      itemLabelFormat: '{{name}}{{thematicId | prepend:'' ('' | append:'')''}}'
      transformations:
        geometry:
          remove: IN_COLLECTION
        occupancy.typeOfOccupant:
          remove: IN_COLLECTION
        occupancy.numberOfOccupants:
          remove: IN_COLLECTION
```

Beispiel für die Verwendung von CesiumJS für Gebäudedaten, die teilweise aus Bauteilen zusammengesetzt sind. Als Fallback wird die Bodenplatte verwendet:

```yaml
- buildingBlock: FEATURES_HTML
  mapClientType: CESIUM
  geometryProperties: 
  - consistsOfBuildingPart.lod1Solid
  - lod1Solid
  - lod1GroundSurface
```
