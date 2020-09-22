# ldproxy Dokumentation

## Übersicht

ldproxy macht Geodaten auf einfache Weise über moderne Web APIs zugänglich.

Die Software zeichnet sich durch folgende Eigenschaften aus:

* Einfach nutzbar: Die APIs untersützen für nahezu alle Inhalte mindestens JSON und HTML. Bei den HTML-Ausgaben wird Wert auf intuitive Verständlichkeit gelegt.
* Durchsuchbar: Da alle Inhalte über Links verkünft sind, kann sich ein Nutzer in jedem Webbrowser durch eine API navigieren und so schnell einen Eindruck der angebotenen Daten und Funktionen bekommen. Suchmaschinen können die Inhalte indizieren.
* Verlinkbar: Da jede Ressource in den APIs einen feste URI besitzt, kann auch auf einzelne Daten dauerhaft verlinkt werden.
* Standardkonform: ldproxy ist eine umfangreiche Implementierung der neuen [OGC API Standards](https://ogcapi.org/), sodass zunehmend Clients die APIs direkt nutzen können. Das gilt auch für die unterstützten Formate, z.B. GeoJSON, Mapbox Vector Tiles, Mapbox Styles oder TileJSON. Die APIs selbst werden außerdem über [OpenAPI-Definitionen](https://www.openapis.org/) entwicklerfreundlich dokumentiert.
* Zertifiziert: ldproxy ist als OGC-Referenzimplementierung für [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0) zertifiziert.
* Open Source: Der Quellcode ist unter der [Mozilla Public License 2.0](http://mozilla.org/MPL/2.0/) auf [GitHub](https://github.com/interactive-instruments/ldproxy) verfügbar.
* Optimiert: Es werden Threads verwendet, um Hardwareressourcen zu schonen - es gibt nur einen Betriebssystemprozess für alle Dienste pro Server. Feature-Daten werden als Stream übertragen, um schneller mit der Datenübertragung beginnen zu können.
* Variabel: Die Unterstützung mehrerer APIs in einem einzigen Deployment wird unterstützt, zum Beispiel für mehrere Datensätze.
* Erweiterbar: ldproxy ist modular aufgebaut, in Java geschrieben (aktuell unterstützte Version: Java 11) und kann einfach erweitert werden.

Um eine Vorstellung davon zu bekommen, wie die APIs aussehen, werfen Sie einen Blick auf [Beispiele für APIs](demos.md).

Weitere Informationen über die unterstützten Spezifikationen und Technologien finden Sie [hier](specifications.md).

## Installation und Betrieb

* [Installation und Aktualisierung von ldproxy](deployment.md)
* [Das Daten-Verzeichnis](data-folder.md)

## Konfiguration

* [Konfiguration einer API - Übersicht](configuration/README.md)
  * [An einem Beispiel: die Weinlagen API](../../demo/vineyards/README.md)
  * [Globale Konfiguration](configuration/global-configuration.md)
  * [ldproxy-Manager: Konfiguration im Webbrowser](configuration/manager/README.md)
  * Konfigurationsobjekte:
    * [Daten-Provider](configuration/providers/README.md)
      * [SQL-Feature-Provider](configuration/providers/sql.md)
      * [WFS-Feature-Provider](configuration/providers/wfs.md)
    * [APIs](configuration/services/README.md)
      * [API-Module in der Übersicht](configuration/services/building-blocks/README.md)
      * [Grundsätzliche Regeln für alle API-Module](configuration/services/building-blocks/general-rules.md)
      * [API-Modul: ldproxy Foundation](configuration/services/building-blocks/foundation.md) - Basisklassen für ldproxy
      * [API-Modul: Common Core](configuration/services/building-blocks/common.md) - Ressourcen "Landing Page", "Conformance Declaration" und "API Definition"
      * [API-Modul: HTML](configuration/services/building-blocks/html.md) - HTML-Ausgabe bei allgemeinen API-Ressourcen ohne spezifische Formate
      * [API-Modul: JSON](configuration/services/building-blocks/json.md) - JSON-Ausgabe bei allgemeinen API-Ressourcen ohne spezifische Formate
      * [API-Modul: XML](configuration/services/building-blocks/xml.md) - XML-Ausgabe bei allgemeinen API-Ressourcen ohne spezifische Formate (sofern implementiert)
      * [API-Modul: OpenAPI 3.0](configuration/services/building-blocks/oas30.md) - Unterstützung für die API-Definition in OpenAPI 3.0
      * [API-Modul: Feature Collections](configuration/services/building-blocks/collections.md) - Ressourcen "Feature Collections" und "Feature Collection"
      * [API-Modul: Features Core](configuration/services/building-blocks/features-core.md) - Ressourcen "Features" und "Feature"
      * [API-Modul: Features HTML](configuration/services/building-blocks/features-html.md) - HTML-Ausgabe für die Ressourcen "Features" und "Feature"
      * [API-Modul: Features GeoJSON](configuration/services/building-blocks/geojson.md) - GeoJSON-Ausgabe für die Ressourcen "Features" und "Feature"
      * [API-Modul: Features GML](configuration/services/building-blocks/gml.md) - GML-Ausgabe für die Ressourcen "Features" und "Feature" (nur bei WFS-Providern)
      * [API-Modul: Coordinate Reference Systems](configuration/services/building-blocks/crs.md) - Unterstützung für Koordinatenreferenzsysteme neben dem Standardsystem CRS84
      * [API-Modul: Collections Queryables](configuration/services/building-blocks/queryables.md) - Ressource "Queryables" für Feature Collections
      * [API-Modul: Collections Schema](configuration/services/building-blocks/schema.md) - Ressource "Schema" für Feature Collections
      * [API-Modul: Features GeoJSON-LD](configuration/services/building-blocks/geojson-ld.md) |GEO_JSON_LD |draft |Nein |Aktiviert JSON-LD-Erweiterungen in der GeoJSON-Ausgabe
      * [API-Modul: Filter / CQL](configuration/services/building-blocks/filter.md) - CQL-Filter für die Ressourcen "Features" und "Vector Tiles"
      * [API-Modul: Geometry Simplification](configuration/services/building-blocks/geometry-simplification.md) - Vereinfachung von Geometrien nach Douglas-Peucker bei den Ressourcen "Features" und "Feature"
      * [API-Modul: Projections](configuration/services/building-blocks/projections.md) - Begrenzung der zurückgelieferten Feature-Eigenschaften bei den Ressourcen "Features", "Feature" und "Vector Tiles"
      * [API-Modul: Styles](configuration/services/building-blocks/styles.md) - Bereitstellung und Verwaltung von Styles (Mapbox Style oder SLD) und zugehöriger Ressourcen (Synmbole, Sprites) in der API
      * [API-Modul: Vector Tiles](configuration/services/building-blocks/tiles.md) - Bereitstellung von Vector Tiles im Format Mapbox Vector Tiles für den gesamten Datensatz und/oder einzelne Collections
      * [API-Modul: Simple Transactions](configuration/services/building-blocks/transactional.md) - Verändern von Features unter Verwendung der Standardlogik der HTTP-Methoden POST/PUT/DELETE/PATCH
    * [Codelisten](configuration/codelists/README.md)
