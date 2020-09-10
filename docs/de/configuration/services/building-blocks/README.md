# API-Module

Die API-Funktionalität ist in Module, die sich an den OGC API Standards orientieren, aufgeteilt. Jedes Modul ist ein [OSGi](https://de.wikipedia.org/wiki/OSGi)-Bundle. Module können damit grundsätzlich zur Laufzeit hinzugefügt und wieder entfernt werden.

Die ldproxy-Module werden nach der Stabilität der zugrundeliegenden Spezifikation unterschieden. Implementiert ein Modul einen verabschiedeten Standard oder einen Entwurf, der sich in der Schlußabstimmung befindet, wird es als "Stable" klassifiziert.

Module, die Spezifikationsentwürfe implementieren, werden als "Draft" klassifiziert. Bei diesen Modulen gibt es i.d.R. noch Abweichungen vom erwarteten Verhalten oder von der in den aktuellen Entwürfen beschriebenen Spezifikation.

Darüber hinaus sind weitere Module mit experimentellem Charakter als Community-Module verfügbar. Die Community-Module sind kein Bestandteil der ldproxy Docker-Container.

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

## Grundsätzliche Regeln für alle API-Module

### Auswahl des Antwortformats

Bei Operationen, die eine Antwort zurückliefern, wird das Format nach den Standard-HTTP-Regeln standardmäßig über Content-Negotiation und den `Accept`-Header ermittelt.

Alle GET-Operationen unterstützen zusätzlich den Query-Parameter `f`. Über diesen Parameter kann das Ausgabeformat der Antwort auch direkt ausgewählt werden. Wenn kein Wert angegeben wird, gelten die Standard-HTTP-Regeln, d.h. der `Accept`-Header wird zur Bestimmung des Formats verwendet. Die unterstützten Formate hängen von der Ressource und von der API-Konfiguration ab.

### Auswahl des Antwortsprache

Bei Operationen, die eine Antwort zurückliefern, wird die verwendete Sprache bei linguistischen Texten nach den Standard-HTTP-Regeln standardmäßig über Content-Negotiation und den `Accept-Language`-Header ermittelt.

Sofern die entsprechende Option im Modul "Common Core" aktiviert ist, unterstützen alle GET-Operationen zusätzlich den Query-Parameter `lang`. Über diesen Parameter kann die Sprache auch direkt ausgewählt werden. Wenn kein Wert angegeben wird, gelten die Standard-HTTP-Regeln, wie oben beschrieben. Die erlaubten Werte hängen von der Ressource und von der API-Konfiguration ab. Die Unterstüzung für Mehrsprachigkeit ist derzeit begrenzt. Es gibt vier Arten von Quellen für Texte:

* Texte zu festen Elementen der API: Diese werden von ldproxy erzeugt, z.B. die Texte der Titel von Links oder feste Textbausteine in der HTML-Ausgabe. Derzeit werden die Sprachen "Deutsch" (de) und "Englisch" (en) unterstützt.
* Texte aus Attributen in den Daten: Hier gibt es noch keine Unterstützung, wie die Rückgabe bei mehrsprachigen Daten in Abhängigkeit von der Ausgabesprache gesteuert werden kann.
* Texte aus der API-Konfiguration, insbesondere zum Datenschema: Hier gibt es noch keine Unterstützung, wie die Rückgabe bei mehrsprachigen Daten in Abhängigkeit von der Ausgabesprache gesteuert werden kann.
* Fehlermeldungen der API: Diese sind immer in Englisch, die Meldungen sind aktuell Bestandteil des Codes.

### Option `enabled`

Jedes API-Modul hat eine Konfigurationsoption `enabled`, die steuert, ob das Modul in der jeweiligen API aktiviert ist. Einige Module sind standardmäßig aktiviert, andere deaktiviert (siehe die nachfolgende [Übersicht](#api-module-overview)).

### Pfadangaben

Alle Pfadangaben in dieser Dokumentation sind relativ zur Basis-URI des Deployments. Ist dies zum Beispiel `https://example.com/pfad/zu/apis` und lautet der Pfad einer Ressource `/{apiId}/collections` dann ist die URI der Ressource `https://example.com/pfad/zu/apis/{apiId}/collections`.

<a name="transformations"></a>

### Transformationen

In den API-Modulen, die Features verarbeiten ([Core](features-core.md), [GeoJSON](geojson.md), [HTML](features-html.md), [Vector Tiles](tiles.md)), können die Feature-Eigenschaften über Transformationen an die Anforderungen der Ausgabe angepasst werden:

|Transformation |Datentyp |Beschreibung
| --- | --- | ---
|`rename` |string |Benennt die Eigenschaft auf den angegebenen Namen um.
|`remove` |enum |`OVERVIEW` unterdrückt die Objekteigenschaft bei der Features-Ressource (vor allem für die HTML-Ausgabe relevant), `ALWAYS` unterdrückt sie immer, `NEVER` nie.
|`null` |regex |Bildet alle Werte, die dem regulären Ausdruck entsprechen, auf `null` ab. Diese Transformation ist nicht bei objektwertigen Eigenschaften anwendbar.
|`stringFormat` |string |Der Wert wird in den angegebenen neuen Wert transformiert. `{{value}}` wird dabei durch den aktuellen Wert und `{{serviceUr}}` durch die Landing-Page-URI der API ersetzt. Bei `{{value}}` können noch weitere Filter ausgeführt werden, diese werden durch "\|" getrennt. Diese Transformation ist nur bei STRING-wertigen Eigenschaften anwendbar. Ist der transformierte Wert für die HTML-Ausgabe gedacht, dann kann auch Markdown-Markup verwendet werden, dieser wird bei der HTML-Ausgabe aufbereitet.
|`dateFormat` |string |Der Wert wird unter Anwendung des angegebenen [Patterns](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns) transformiert. `dd.MM.yyyy` bildet den Wert zum Beispiel auf ein Datum nach deutscher Schreibweise ab. Diese Transformation ist nur bei DATETIME-wertigen Eigenschaften anwendbar.
|`codelist`|string |Bildet den Wert anhand der genannten [Codelist](../../codelists/README.md) ab. Falls der Wert nicht in der Codelist enthalten ist oder die Codelist nicht gefunden wird, bleibt der Wert unverändert. Diese Transformation ist nicht bei objektwertigen Eigenschaften anwendbar.

Einige Beispiele zu `stringFormat`-Transformationen:

* `https://example.com/id/kinder/kita/{{value}}` setzt den Wert in einer Vorlage für URIs ein.
* `{{value | replace:'\\s*[0-9].*$':''}}` entfernt alle Leerzeichen und Ziffern am Ende des Werts (z.B. zum Entfernen von Hausnummern)
* `{{value | replace:'^[^0-9]*':''}}` entfernt alle führenden Zeichen bis zur ersten Ziffer
* `{{value | toUpper}}` wandelt den Text in Großbuchstaben um
* `{{value | toLower}}` wandelt den Text in Kleinbuchstaben um
* `{{value | urlEncode}}` kodiert Sonderzeichen im Text für die Nutzung in einer URI
* `[{{value}}](https://de.wikipedia.org/wiki/{{value | replace:' ':'_' | urlencode}})` wandelt einen Gemeindenamen in einen Markdown-Link zum Wikipedia-Eintrag der Gemeinde

Die Transformation der Werte erfolgt bei der Aufbereitung der Daten für die Rückgabe über die API. Die Datenhaltung selbst bleibt unverändert.

Alle Filterausdrücke (siehe `queryables` im [Modul "Features Core"](features-core.md)) wirken unabhängig von etwaigen Transformationen bei der Ausgabe und müssen auf der Basis der Werte in der Datenhaltung formuliert sein - die Transformationen sind i.A. nicht umkehrbar und eine Berücksichtigung der inversen Transformationen bei Filterausdrücken wäre kompliziert und nur unvollständig möglich. Insofern sollten Eigenschaften, die queryable sein sollen, möglichst bereits in der Datenquelle transformiert sein. Eine Ausnahme sind typischerweise Transformationen in der HTML-Ausgabe, wo direkte Lesbarkeit i.d.R. wichtiger ist als die Filtermöglichkeit.
