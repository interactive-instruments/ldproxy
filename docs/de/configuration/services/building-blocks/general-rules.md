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

TODO: zu aktualisieren (flatten, nullify, reduceStringFormat)

In den API-Modulen, die Features verarbeiten ([Core](features-core.md), [GeoJSON](geojson.md), [JSON-FG](json-fg.md), [HTML](features-html.md), [Tiles](tiles.md)), können die Feature-Eigenschaften über Transformationen an die Anforderungen der Ausgabe angepasst werden:

|Transformation |Datentyp |Beschreibung
| --- | --- | ---
|`rename` |string |Benennt die Eigenschaft auf den angegebenen Namen um.
|`remove` |enum |`IN_COLLECTION` (bis Version 3.0: `OVERVIEW`) unterdrückt die Objekteigenschaft bei der Features-Ressource (vor allem für die HTML-Ausgabe relevant), `ALWAYS` unterdrückt sie immer, `NEVER` nie.
|`null` |regex |Bildet alle Werte, die dem regulären Ausdruck entsprechen, auf `null` ab. Diese Transformation ist nicht bei objektwertigen Eigenschaften anwendbar.
|`stringFormat` |string |Der Wert wird in den angegebenen neuen Wert transformiert. `{{value}}` wird dabei durch den aktuellen Wert und `{{serviceUr}}` durch die Landing-Page-URI der API ersetzt. Bei `{{value}}` können noch weitere [Filter](#String-Template-Filter) ausgeführt werden, diese werden durch "\|" getrennt. Diese Transformation ist nur bei STRING-wertigen Eigenschaften anwendbar. Ist der transformierte Wert für die HTML-Ausgabe gedacht, dann kann auch Markdown-Markup verwendet werden, dieser wird bei der HTML-Ausgabe aufbereitet.
|`dateFormat` |string |Der Wert wird unter Anwendung des angegebenen [Patterns](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns) transformiert. `dd.MM.yyyy` bildet den Wert zum Beispiel auf ein Datum nach deutscher Schreibweise ab. Diese Transformation ist nur bei DATETIME-wertigen Eigenschaften anwendbar.
|`codelist`|string |Bildet den Wert anhand der genannten [Codelist](../../codelists/README.md) ab. Falls der Wert nicht in der Codelist enthalten ist oder die Codelist nicht gefunden wird, bleibt der Wert unverändert. Diese Transformation ist nicht bei objektwertigen Eigenschaften anwendbar.

Die Transformation der Werte erfolgt bei der Aufbereitung der Daten für die Rückgabe über die API. Die Datenhaltung selbst bleibt unverändert.

Alle Filterausdrücke (siehe `queryables` im [Modul "Features Core"](features-core.md)) wirken unabhängig von etwaigen Transformationen bei der Ausgabe und müssen auf der Basis der Werte in der Datenhaltung formuliert sein - die Transformationen sind i.A. nicht umkehrbar und eine Berücksichtigung der inversen Transformationen bei Filterausdrücken wäre kompliziert und nur unvollständig möglich. Insofern sollten Eigenschaften, die queryable sein sollen, möglichst bereits in der Datenquelle transformiert sein. Eine Ausnahme sind typischerweise Transformationen in der HTML-Ausgabe, wo direkte Lesbarkeit i.d.R. wichtiger ist als die Filtermöglichkeit.

## String-Template-Filter

Mit den Filtern können Strings nachprozessiert werden. Es können mehrere Filter nacheinander ausgeführt werden, jeweils durch ein '\|' getrennt. 

Einige Beispiele:

* `{{value | replace:'\\s*[0-9].*$':''}}` entfernt alle Leerzeichen und Ziffern am Ende des Werts (z.B. zum Entfernen von Hausnummern)
* `{{value | replace:'^[^0-9]*':''}}` entfernt alle führenden Zeichen bis zur ersten Ziffer
* `{{value | prepend:'(' | append:')'}}` ergänzt Klammern um den Text
* `{{value | toUpper}}` wandelt den Text in Großbuchstaben um
* `{{value | toLower}}` wandelt den Text in Kleinbuchstaben um
* `{{value | urlEncode}}` kodiert Sonderzeichen im Text für die Nutzung in einer URI
* `{{value | unHtml}}` entfernt HTML-Tags (z.B. zum Reduzieren eines HTML-Links auf den Link-Text)
* `[{{value}}](https://de.wikipedia.org/wiki/{{value | replace:' ':'_' | urlencode}})` wandelt einen Gemeindenamen in einen Markdown-Link zum Wikipedia-Eintrag der Gemeinde

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
