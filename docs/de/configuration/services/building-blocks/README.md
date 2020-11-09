# API-Module

Die API-Funktionalität ist in Module, die sich an den OGC API Standards orientieren, aufgeteilt. Jedes Modul ist ein [OSGi](https://de.wikipedia.org/wiki/OSGi)-Bundle. Module können damit grundsätzlich zur Laufzeit hinzugefügt und wieder entfernt werden.

Die ldproxy-Module werden nach der Stabilität der zugrundeliegenden Spezifikation unterschieden. Implementiert ein Modul einen verabschiedeten Standard oder einen Entwurf, der sich in der Schlußabstimmung befindet, wird es als "Stable" klassifiziert.

Module, die Spezifikationsentwürfe implementieren, werden als "Draft" klassifiziert. Bei diesen Modulen gibt es i.d.R. noch Abweichungen vom erwarteten Verhalten oder von der in den aktuellen Entwürfen beschriebenen Spezifikation.

Darüber hinaus sind weitere Module mit experimentellem Charakter als Community-Module verfügbar. Die Community-Module sind kein Bestandteil der ldproxy Docker-Container.

Grundsätzliche Regeln, die für alle API-Module gelten, finden Sie [hier](general-rules.md).

<a name="api-module-overview"></a>

## Übersicht

|API-Modul |Identifikator |Status |Standardmäßig aktiviert? |Beschreibung
| --- | --- | --- | --- | ---
|[ldproxy Foundation](foundation.md) |FOUNDATION |stable |Ja |Basisklassen für ldproxy
|[Common Core](common.md) |COMMON |stable |Ja |Ressourcen "Landing Page", "Conformance Declaration" und "API Definition"
|[HTML](html.md) |HTML |stable |Ja |Aktiviert die HTML-Ausgabe bei allgemeinen API-Ressourcen ohne spezifische Formate
|[JSON](json.md) |JSON |stable |Ja |Aktiviert die JSON-Ausgabe bei allgemeinen API-Ressourcen ohne spezifische Formate
|[XML](xml.md) |XML |stable |Nein |Aktiviert die XML-Ausgabe bei allgemeinen API-Ressourcen ohne spezifische Formate (sofern implementiert)
|[OpenAPI 3.0](oas30.md) |OAS30 |stable |Ja |Aktiviert die Unterstützung für die API-Definition in OpenAPI 3.0
|[Feature Collections](collections.md) |COLLECTIONS |stable |Ja |Ressourcen "Feature Collections" und "Feature Collection"
|[Features Core](features-core.md) |FEATURES_CORE |stable |Ja |Ressourcen "Features" und "Feature"
|[Features HTML](features-html.md) |FEATURES_HTML |stable |Ja |Aktiviert die HTML-Ausgabe für die Ressourcen "Features" und "Feature"
|[Features GeoJSON](geojson.md) |GEO_JSON |stable |Ja |Aktiviert die GeoJSON-Ausgabe für die Ressourcen "Features" und "Feature"
|[Features GML](gml.md) |GML |stable |Nein |Aktiviert die GML-Ausgabe für die Ressourcen "Features" und "Feature" (nur bei WFS-Providern)
|[Coordinate Reference Systems](crs.md) |CRS |stable |Ja |Aktiviert die Unterstützung für Koordinatenreferenzsysteme neben dem Standardsystem CRS84.
|[Collections Queryables](queryables.md) |QUERYABLES |draft |Nein |Aktiviert die Ressource "Queryables" für Feature Collections
|[Collections Schema](schema.md) |SCHEMA |draft |Nein |Aktiviert die Ressource "Schema" für Feature Collections
|[Features GeoJSON-LD](geojson-ld.md) |GEO_JSON_LD |draft |Nein |Aktiviert JSON-LD-Erweiterungen in der GeoJSON-Ausgabe
|[Filter / CQL](filter.md) |FILTER |draft |Nein |Aktiviert die Angabe von CQL-Filtern für die Ressourcen "Features" und "Vector Tiles"
|[Geometry Simplification](geometry-simplification.md) |GEOMETRY_SIMPLIFICATION |draft |Nein |Aktiviert die Option zur Vereinfachung von Geometrien nach Douglas-Peucker bei den Ressourcen "Features" und "Feature"
|[Projections](projections.md) |PROJECTIONS |draft |Nein |Aktiviert die Option zur Begrenzung der zurückgelieferten Feature-Eigenschaften bei den Ressourcen "Features", "Feature" und "Vector Tiles"
|[Styles](styles.md) |STYLES |draft |Nein |Aktiviert die Unterstützung für die Bereitstellung und Verwaltung von Styles (Mapbox Style oder SLD) und zugehöriger Ressourcen (Synmbole, Sprites) in der API
|[Vector Tiles](tiles.md) |TILES |draft |Nein |Aktivitert die Unterstützung für die Bereitstellung von Vector Tiles im Format Mapbox Vector Tiles für den gesamten Datensatz und/oder einzelne Collections
|[Simple Transactions](transactional.md) |TRANSACTIONAL |draft |Nein |Aktiviert die Unterstützung für die Veränderung von Features unter Verwendung der Standardlogik der HTTP-Methoden POST/PUT/DELETE/PATCH
