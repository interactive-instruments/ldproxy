<a name="transformations"></a>

# Transformationen

Transformationen werden in verschiedenen Teilen der Konfiguration unterstützt.

Die Transformation der Werte erfolgt bei der Aufbereitung der Daten für die Rückgabe. Die Datenhaltung selbst bleibt unverändert.

Alle Filterausdrücke wirken unabhängig von etwaigen Transformationen bei der Ausgabe und müssen auf der Basis der Werte in der Datenhaltung formuliert sein - die Transformationen sind i.A. nicht umkehrbar und eine Berücksichtigung der inversen Transformationen bei Filterausdrücken wäre kompliziert und nur unvollständig möglich. 

|Transformation |Datentyp |Beschreibung
| --- | --- | ---
|`rename` |string |Benennt die Eigenschaft auf den angegebenen Namen um.
|`remove` |enum |`IN_COLLECTION` (bis Version 3.0: `OVERVIEW`) unterdrückt die Objekteigenschaft bei der Features-Ressource (vor allem für die HTML-Ausgabe relevant), `ALWAYS` unterdrückt sie immer, `NEVER` nie.
|`flatten` |string |Flacht Objekt- oder Array-Properties unter Verwendung des gegebenen Trennzeichens ab. Ein Trennzeichen wird immer dann eingesetzt, wenn eine Eigenschaft multipel (ein Array) oder strukturiert (ein Objekt) ist. Im Fall eines Array ergeben sich die Namen der abgeflachten Eigenschaften aus dem Namen der Eigenschaft im Schema und der Position im Array, getrennt durch das Trennzeichen. Bei einer objektwertigen Eigenschaft ergeben sich die Namen der abgeflachten Eigenschaften aus dem Namen der objektwertigen Eigenschaft im Schema und den Namen der Eigenschaften im Datentyp des Objekts, ebenfalls getrennt durch das Trennzeichen. Erlaubt sind ".", "/", ":", oder "_". Kann nur auf der Feature-Ebene im Provider oder an anderen Stellen mittels des Wildcard Property-Names `*` angewendet werden.
|`reduceStringFormat` |string |Reduziert ein Objekt zu einem String mithilfe der `stringFormat`-Syntax aber mit zusätzlichen Ersetzungen für die Property-Names des Objekts.
|`nullify` |array |Bildet alle Werte, die einem der regulären Ausdrücke in der Liste entsprechen, auf `null` ab. Diese Transformation ist nicht bei objektwertigen Eigenschaften anwendbar.
|`null` |regex |*Deprecated* Siehe `nullify`.
|`stringFormat` |string |Der Wert wird in den angegebenen neuen Wert transformiert. `{{value}}` wird dabei durch den aktuellen Wert und `{{serviceUr}}` durch die Landing-Page-URI der API ersetzt. Bei `{{value}}` können noch weitere [Filter](#String-Template-Filter) ausgeführt werden, diese werden durch "\|" getrennt. Diese Transformation ist nur bei STRING-wertigen Eigenschaften anwendbar. Ist der transformierte Wert für die HTML-Ausgabe gedacht, dann kann auch Markdown-Markup verwendet werden, dieser wird bei der HTML-Ausgabe aufbereitet.
|`dateFormat` |string |Der Wert wird unter Anwendung des angegebenen [Patterns](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns) transformiert. `dd.MM.yyyy` bildet den Wert zum Beispiel auf ein Datum nach deutscher Schreibweise ab. Diese Transformation ist nur bei DATETIME-wertigen Eigenschaften anwendbar.
|`codelist`|string |Bildet den Wert anhand der genannten [Codelist](../../codelists/README.md) ab. Falls der Wert nicht in der Codelist enthalten ist oder die Codelist nicht gefunden wird, bleibt der Wert unverändert. Diese Transformation ist nicht bei objektwertigen Eigenschaften anwendbar.

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
