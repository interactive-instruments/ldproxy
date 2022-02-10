# Grundsätzliche Regeln für alle API-Module

## Auswahl des Antwortformats

Bei Operationen, die eine Antwort zurückliefern, wird das Format nach den Standard-HTTP-Regeln standardmäßig über Content-Negotiation und den `Accept`-Header ermittelt.

Alle GET-Operationen unterstützen zusätzlich den Query-Parameter `f`. Über diesen Parameter kann das Ausgabeformat der Antwort auch direkt ausgewählt werden. Wenn kein Wert angegeben wird, gelten die Standard-HTTP-Regeln, d.h. der `Accept`-Header wird zur Bestimmung des Formats verwendet. Die unterstützten Formate hängen von der Ressource und von der API-Konfiguration ab.

## Auswahl des Antwortsprache

Bei Operationen, die eine Antwort zurückliefern, wird die verwendete Sprache bei linguistischen Texten nach den Standard-HTTP-Regeln standardmäßig über Content-Negotiation und den `Accept-Language`-Header ermittelt.

Sofern die entsprechende Option im Modul "Common Core" aktiviert ist, unterstützen alle GET-Operationen zusätzlich den Query-Parameter `lang`. Über diesen Parameter kann die Sprache auch direkt ausgewählt werden. Wenn kein Wert angegeben wird, gelten die Standard-HTTP-Regeln, wie oben beschrieben. Die erlaubten Werte hängen von der Ressource und von der API-Konfiguration ab. Die Unterstüzung für Mehrsprachigkeit ist derzeit begrenzt. Es gibt vier Arten von Quellen für Texte:

* Texte zu festen Elementen der API: Diese werden von ldproxy erzeugt, z.B. die Texte der Titel von Links oder feste Textbausteine in der HTML-Ausgabe. Derzeit werden die Sprachen "Deutsch" (de) und "Englisch" (en) unterstützt.
* Texte aus Attributen in den Daten: Hier gibt es noch keine Unterstützung, wie die Rückgabe bei mehrsprachigen Daten in Abhängigkeit von der Ausgabesprache gesteuert werden kann.
* Texte aus der API-Konfiguration, insbesondere zum Datenschema: Hier gibt es noch keine Unterstützung, wie die Rückgabe bei mehrsprachigen Daten in Abhängigkeit von der Ausgabesprache gesteuert werden kann.
* Fehlermeldungen der API: Diese sind immer in Englisch, die Meldungen sind aktuell Bestandteil des Codes.

## Option `enabled`

Jedes API-Modul hat eine Konfigurationsoption `enabled`, die steuert, ob das Modul in der jeweiligen API aktiviert ist. Einige Module sind standardmäßig aktiviert, andere deaktiviert (siehe die nachfolgende [Übersicht](#api-module-overview)).

## Pfadangaben

Alle Pfadangaben in dieser Dokumentation sind relativ zur Basis-URI des Deployments. Ist dies zum Beispiel `https://example.com/pfad/zu/apis` und lautet der Pfad einer Ressource `/{apiId}/collections` dann ist die URI der Ressource `https://example.com/pfad/zu/apis/{apiId}/collections`.

<a name="transformations"></a>

## Transformationen

In den API-Modulen, die Features verarbeiten ([Core](features-core.md), [GeoJSON](geojson.md), [JSON-FG](json-fg.md), [HTML](features-html.md), [Tiles](tiles.md) mit dem Features-Tile-Provider), können die Feature-Eigenschaften über Transformationen an die Anforderungen der Ausgabe angepasst werden.

Die Transformation der Werte erfolgt bei der Aufbereitung der Daten für die Rückgabe über die API. Die Datenhaltung selbst bleibt unverändert.

Alle Filterausdrücke (siehe `queryables` im [Modul "Features Core"](features-core.md)) wirken unabhängig von etwaigen Transformationen bei der Ausgabe und müssen auf der Basis der Werte in der Datenhaltung formuliert sein - die Transformationen sind i.A. nicht umkehrbar und eine Berücksichtigung der inversen Transformationen bei Filterausdrücken wäre kompliziert und nur unvollständig möglich. Insofern sollten Eigenschaften, die queryable sein sollen, möglichst bereits in der Datenquelle transformiert sein. Eine Ausnahme sind typischerweise Transformationen in der HTML-Ausgabe, wo direkte Lesbarkeit i.d.R. wichtiger ist als die Filtermöglichkeit.

Siehe [Transformations](../../providers/transformations.md) für unterstützte Transformationen.

<a name="caching"></a>

## HTTP-Header für Caching

ldproxy setzt in Antworten die folgenden HTTP-Header für HTTP-Caching - soweit diese für die jeweilige Ressource bestimmt werden können:

* `Last-Modified`: Der Zeitstempel der letzten Änderung wird - sofern möglich - aus der zurückzugebenden Repräsentation der Ressource bestimmt, z.B. aus dem Änderungsdatum einer Datei. Er kann über eine Konfigurationseinstellung überschrieben werden (siehe unten).
* `ETag`: Der Tag wird - sofern möglich - aus der zurückzugebenden Repräsentation der Ressource bestimmt.
* `Cache-Control`: Der Header wird nur gesetzt, wenn er für die Ressourcen des Moduls konfiguriert wurde (siehe unten).
* `Expires`: Der Header wird nur gesetzt, wenn er für die Ressourcen des Moduls konfiguriert wurde (siehe unten).

In jedem Modul, das Ressourcen bereitstellt und nicht nur Query-Parameter oder Ausgabeformate realisiert, ist eine Konfigurationsoption `caching`, deren Wert ein Objekt mit den folgenden, optionalen Einträgen ist:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`lastModified` |string |`null` |Für die Ressourcen in dem Modul wird der `Last-Modified` Header auf den konfigurierten Wert gesetzt. Der Wert überschreibt einen ggf. aus der Ressource bestimmten Änderungszeitpunkt.
|`cacheControl` |string |`null` |Für die Ressourcen in dem Modul wird der `Expires` Header auf den konfigurierten Wert gesetzt. Ausnahme sind die "Features" und "Feature"-Ressourcen, bei denen `cacheControlItems` zu verwenden ist.
|`cacheControlItems` |string |`null` |Für die "Features" und "Feature"-Ressourcen wird der `Cache-Control` Header auf den konfigurierten Wert gesetzt.
|`expires` |string |`null` |Für die Ressourcen in dem Modul wird der `Expires` Header auf den konfigurierten Wert gesetzt.

In der API-Konfiguration können über eine Konfigurationsoption `defaultCaching` Standardwerte für die gesamte API gesetzt werden.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
defaultCaching:
  cacheControl: 'max-age=3600'
- buildingBlock: FEATURES_CORE
  caching:
    lastModified: '2021-07-01T00:00:00+02:00'
    expires: '2021-12-31T23:59:59+01:00'
```
