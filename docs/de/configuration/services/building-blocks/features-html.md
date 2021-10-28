# Modul "Features HTML" (FEATURES_HTML)

Das Modul "Features HTML" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features und Feature in HTML.

Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der Konformitätsklasse "HTML" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html).

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`noIndexEnabled` |boolean |`true` |Steuert, ob in allen Seiten "noIndex" gesetzt wird und Suchmaschinen angezeigt wird, dass sie die Seiten nicht indizieren sollen.
|`schemaOrgEnabled` |boolean |`true` |Steuert, ob in die HTML-Ausgabe schema.org-Annotationen, z.B. für Suchmaschinen, eingebettet sein sollen. Die Annotationen werden im Format JSON-LD eingebettet.
|`collectionDescriptionsInOverview`  |boolean |`true` |Steuert, ob in der HTML-Ausgabe der Feature-Collections-Ressource für jede Collection die Beschreibung ausgegeben werden soll.
|`layout` |enum |`CLASSIC` |Steuert, welches HTML-Template für die Ausgabe der Features- und Feature-Ressourcen verwendet werden soll. Verfügbar sind `CLASSIC` (vor allem für einfache Objekte mit einfachen Werten) und `COMPLEX_OBJECTS` (unterstützt auch komplexere Objektstrukturen und längere Werte).
|`itemLabelFormat` |string |`{{id}}` |Steuert, wie der Titel eines Features in der HTML-Ausgabe gebildet wird. Standardmäßig ist der Titel der Identifikator. In der Angabe können über die Angabe des Attributnamens in doppelt-geschweiften Klammern Ersetzungspunkte für die Attribute des Features verwendet werden. Es können nur Attribute verwendet werden, die nur einmal pro Feature vorkommen können. Neben einer direkten Ersetzung mit dem Attributwert können auch [Filter](general-rules.md#String-Template-Filter) angewendet werden. Ist ein Attribut `null`, dann wird der Ersetzungspunkt durch einen leeren String ersetzt.
|`transformations` |object |`{}` |Steuert, ob und wie die Werte von Objekteigenschaften für die Ausgabe in der HTML-Ausgabe [transformiert](general-rules.md#transformations) werden.
|`style` |string |`DEFAULT` |Ein Style im Style-Repository, der standardmäßig in Karten mit den Features verwendet werden soll. Bei `DEFAULT` wird der `defaultStyle` aus [Modul HTML](html.md) verwendet. Bei `NONE` wird ein einfacher Style mit OpenStreetMap als Basiskarte verwendet. Der Style sollte alle Daten abdecken und muss im Format Mapbox Style verfügbar sein. Es wird zuerst nach einem Style mit dem Namen für die Feature Collection gesucht; falls keiner gefunden wird, wird nach einem Style mit dem Namen auf der API-Ebene gesucht. Wird kein Style gefunden, wird `NONE` verwendet.

Beispiel für die Angaben in der Konfigurationsdatei für die gesamte API:

```yaml
- buildingBlock: FEATURES_HTML
  schemaOrgEnabled: false
  layout: COMPLEX_OBJECTS
```

Beispiel für die Angaben in der Konfigurationsdatei für eine Feature Collection:

```yaml
    - buildingBlock: FEATURES_HTML
      itemLabelFormat: '{{name}}{{thematicId | prepend:'' ('' | append:'')''}}'
      transformations:
        geometry:
          remove: OVERVIEW
        occupancy[].typeOfOccupant:
          remove: OVERVIEW
        occupancy[].numberOfOccupants:
          remove: OVERVIEW
```
