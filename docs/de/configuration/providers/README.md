# Der Feature-Provider

Jeder Feature-Provider wird in einer Konfigurationsdatei in einem Objekt mit den folgenden Eigenschaften beschrieben. Werte ohne Defaultwert sind in diesem Fall Pflichtangaben.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`id` |string | |Eindeutiger Identifikator des Feature-Providers. Erlaubt sind Buchstaben (A-Z, a-z), Ziffern (0-9), der Unterstrich und der Bindestrich.
|`createdAt` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei erzeugt wurde. Der Wert wird automatisch vom Manager bei der Erzeugung gesetzt und besitzt nur informativen Charakter.
|`lastModified` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei zuletzt geändert wurde. Der Wert wird automatisch vom Manager bei jeder Änderung gesetzt und besitzt nur informativen Charakter.
|`entityStorageVersion` |integer | |Bezeichnet die Version des Feature-Provider-Konfigurationsdatei-Schemas. Diese Dokumentation bezieht sich auf die Version 2 und alle Dateien nach diesem Schema müssen den Wert 2 haben. Konfigurationen zu Version 1 werden automatisch auf Version 2 aktualisiert.
|`providerType` |enum | |Stets `FEATURE`.
|`featureProviderType` |enum | |`SQL` für ein SQL-DBMS als Datenquelle, `WFS` für einen OGC Web Feature Service als Datenquelle.
|`connectionInfo` |object | |Ein Objekt mit Angaben zur Datenquelle. Der Inhalt hängt von `featureProviderType` ab ([SQL](sql.md#connection-info) und [WFS](wfs.md#connection-info)).
|`nativeCrs` |object | |Das Koordinatenreferenzsystem, in dem Geometrien in dem Datensatz geführt werden. Der EPSG-Code des Koordinatenreferenzsystems wird als Integer in `code` angegeben. Mit `forceAxisOrder` kann die Koordinatenreihenfolge geändert werden: `NONE` verwendet die Reihenfolge des Koordinatenreferenzsystems, `LON_LAT` verwendet stets Länge/Ostwert als ersten und Breite/Nordwert als zweiten Wert, `LAT_LON` entsprechend umgekehrt. Beispiel: Das Default-Koordinatenreferenzsystem `CRS84` entspricht `code: 4326` und `forceAxisOrder: LON_LAT`.
|`nativeTimeZone` |string | `UTC` |Eine Zeitzonen-ID, z.B. `Europe/Berlin`. Wird auf temporale Werte ohne Zeitzone im Datensatz angewendet.
|`types` |object |`{}` |Ein Objekt mit der Spezifikation zu jeder Objektart. Siehe unten.
|`typeValidation` |enum |`NONE` |Steuert ob die Spezifikationen der Objektarten daraufhin geprüft werden, ob sie zur Datenquelle passen (nur für SQL). `NONE` heißt keine Prüfung. Bei `LAX` schlägt die Prüfung fehl und der Start des Providers wird verhindert, wenn Probleme festgestellt werden, die in jedem Fall zu Laufzeitfehlern führen würden. Probleme die abhängig von den tatsächlichen Daten zu Laufzeitfehlern führen könnten, werden als Warnung geloggt. Bei `STRICT` führen alle festgestellten Probleme zu einem Fehlstart. Der Provider wird also nur gestartet, wenn keine Risiken für Laufzeitfehler im Zusammenhang mit der Datenquelle identifiziert werden.
|`auto` |boolean |`false` |Steuert, ob die Informationen zu `types` beim Start automatisch aus der Datenquelle bestimmt werden sollen (Auto-Modus). In diesem Fall sollte `types` nicht angegeben sein.
|`autoPersist` |boolean |`false` |Steuert, ob die im Auto-Modus (`auto: true`) bestimmten Schemainformationen in die Konfigurationsdatei übernommen werden sollen. In diesem Fall werden `auto` und `autoPersist` beim nächsten Start automatisch aus der Datei entfernt. Liegt die Konfigurationsdatei in einem anderen Verzeichnis als unter `store/entities/providers` (siehe `additionalLocations`), so wird eine neue Datei in `store/entities/providers` erstellt. `autoPersist: true` setzt voraus, dass `store` sich nicht im `READ_ONLY`-Modus befindet.
|`autoTypes` |boolean |`[]` |Liste von Quelltypen, die für die Ableitung der `types` Definitionen im Auto-Modus berücksichtigt werden sollen. Funktioniert aktuell nur für [SQL](sql.md).

<a name="feature-provider-types"></a>

## Das Types-Objekt

Das Types-Objekt hat für jede Objektart einen Eintrag mit dem Identifikator der Objektart als Schlüssel. Jede Objektart ist durch ein Schema-Objekt mit `type: OBJECT` beschrieben:

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`type` |enum |`STRING` / `OBJECT` |Der Datentyp des Schemaobjekts. Der Standardwert ist `STRING`, sofern nicht auch die Eigenschaft `properties` angegeben ist, dann ist es `OBJECT`. Erlaubt sind:<ul><li>`FLOAT`, `INTEGER`, `STRING`, `BOOLEAN`, `DATETIME`, `DATE` für einen einfachen Wert des entsprechenden Datentyps.</li><li>`GEOMETRY` für eine Geometrie.</li><li>`OBJECT` für ein Objekt.</li><li>`OBJECT_ARRAY` für eine Liste von Objekten.</li><li>`VALUE_ARRAY` für eine Liste von einfachen Werten.</li></ul>
|`sourcePath` |string | |Der relative Pfad zu diesem Schemaobjekt. Die Pfadsyntax ist je nach Provider-Typ unterschiedlich ([SQL](sql.md#path-syntax) und [WFS](wfs.md#path-syntax)).
|`constantValue` |string / number / boolean |`null` |Alternativ zu `sourcePath` kann diese Eigenschaft verwendet werden, um im Feature-Provider eine Eigenschaft mit einem festen Wert zu belegen.
|`label` |string | |Eine Bezeichnung des Schemaobjekts, z.B. für die Angabe in der HTML-Ausgabe.
|`description` |string | |Eine Beschreibung des Schemaobjekts, z.B. für die HTML-Ausgabe oder das JSON-Schema.
|`properties` |object | |Nur bei `OBJECT` und `OBJECT_ARRAY`. Ein Objekt mit einer Eigenschaft pro Objekteigenschaft. Der Schüssel ist der Name der Objekteigenschaft, der Wert das Schema-Objekt zu der Objekteigenschaft.
|`role` |enum |`null` |Kennzeichnet besondere Bedeutungen der Eigenschaft.<ul><li><code>ID</code> ist bei der Eigenschaft eines Objekts anzugeben, die für die <code>featureId</code> in der API zu verwenden ist. Diese Eigenschaft ist typischerweise die erste Eigenschaft im <code>properties</code>-Objekt. Erlaubte Zeichen in diesen Eigenschaften sind alle Zeichen bis auf das Leerzeichen (" ") und der Querstrich ("/").</li><li><code>TYPE</code> ist optional bei der Eigenschaft eines Objekts anzugeben, die den Namen einer Unterobjektart enthält.</li><li>Hat eine Objektart mehrere Geometrieeigenschaften, dann ist <code>PRIMARY_GEOMETRY</code> bei der Eigenschaft anzugeben, die für <code>bbox</code>-Abfragen verwendet werden soll und die in GeoJSON in <code>geometry</code> oder in JSON-FG in <code>where</code> kodiert werden soll.</li><li>Hat eine Objektart mehrere zeitliche Eigenschaften, dann sollte <code>PRIMARY_INSTANT</code> bei der Eigenschaft angegeben werden, die für <code>datetime</code>-Abfragen verwendet werden soll, sofern ein Zeitpunkt die zeitliche Ausdehnung der Features beschreibt.</li><li>Ist die zeitliche Ausdehnung hingegen ein Zeitintervall, dann sind <code>PRIMARY_INTERVAL_START</code> und <code>PRIMARY_INTERVAL_END</code> bei den jeweiligen zeitlichen Eigenschaften anzugeben.</li></ul>
|`objectType` |string | |Optional kann ein Name für den Typ spezifiziert werden. Der Name hat i.d.R. nur informativen Charakter und wird z.B. bei der Erzeugung von JSON-Schemas verwendet. Bei Eigenschaften, die als Web-Links nach RFC 8288 abgebildet werden sollen, ist immer "Link" anzugeben.
|`geometryType` |enum | |Mit der Angabe kann der Geometrietype spezifiziert werden. Die Angabe ist nur bei Geometrieeigenschaften (`type: GEOMETRY`) relevant. Erlaubt sind die Simple-Feature-Geometrietypen, d.h. `POINT`, `MULTI_POINT`, `LINE_STRING`, `MULTI_LINE_STRING`, `POLYGON`, `MULTI_POLYGON`, `GEOMETRY_COLLECTION` und `ANY`.
|`forcePolygonCCW` |boolean |`true` |Option zum Erzwingen der Orientierung von Polygonen, gegen den Uhrzeigersinn für äußere Ringe und mit dem Uhrzeigersinn für innere Ringe (nur für SQL).
|`constraints` |object |`{}` |Optionale Beschreibung von Schema-Einschränkungen, vor allem für die Erzeugung von JSON-Schemas. Siehe [Constraints](constraints.md).
|`transformations` |object |`{}` |Optionale Transformationen für die Eigenschaft, siehe [Transformationen](transformations.md).

## Die ConnectionInfo-Objekte

Informationen zu den Datenquellen finden Sie auf separaten Seiten: [SQL](sql.md#connection-info) und [WFS](wfs.md#connection-info).

## Eine Feature-Provider-Beispielkonfiguration (SQL)

Als Beispiel siehe die [Provider-Konfiguration](https://github.com/interactive-instruments/ldproxy/blob/master/demo/vineyards/store/entities/providers/vineyards.yml) der API [Weinlagen in Rheinland-Pfalz](https://demo.ldproxy.net/vineyards).
