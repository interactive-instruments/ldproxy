# Der SQL-Feature-Provider

Hier werden die Besonderheiten des SQL-Feature-Providers beschrieben.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`connectionInfo` |object | |Siehe [Das Connection-Info-Objekt für SQL-Datenbanken](#connection-info).
|`sourcePathDefaults` |object |siehe unten |Defaults für die Pfad-Ausdrücke in `sourcePath`, für Details siehe [SQL-Pfad-Defaults](#source-path-defaults). 
|`queryGeneration` |object |siehe unten |Einstellungen für die Query-Generierung, für Details siehe [Query-Generierung](#query-generation). 

<a name="connection-info"></a>

## Das Connection-Info-Objekt für SQL-Datenbanken

Das Connection-Info-Objekt für SQL-Datenbanken wird wie folgt beschrieben:

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`dialect` |enum |`PGIS` |`PGIS` für PostgreSQL/PostGIS, `GPKG` für GeoPackage oder SQLite/SpatiaLite.
|`database` |string | |Der Name der Datenbank. Für `GPKG` der Pfad zur Datei, entweder absolut oder relativ zum [Daten-Verzeichnis](../../data-folder.md).
|`host` |string | |Der Datenbankhost. Wird ein anderer Port als der Standardport verwendet, ist dieser durch einen Doppelpunkt getrennt anzugeben, z.B. `db:30305`. Nicht relevant für `GPKG`. 
|`user` |string | |Der Benutzername. Nicht relevant für `GPKG`. 
|`password` |string | |Das mit base64 kodierte Passwort des Benutzers. Nicht relevant für `GPKG`. 
|`schemas` |array |`[]` |Die Namen der Schemas in der Datenbank, auf die zugegriffen werden soll. Nicht relevant für `GPKG`. 
|`pool` |object |siehe unten |Einstellungen für den Connection-Pool, für Details siehe [Pool](#connection-pool). 
|`driverOptions` |object |`{}` |Einstellungen für den JDBC-Treiber. Für `PGIS` werden `gssEncMode`, `ssl`, `sslmode`, `sslcert`, `sslkey`, `sslrootcert` und `sslpassword` durchgereicht. Für Details siehe die [Dokumentation des Treibers](https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters).
|`initFailFast` |boolean |`true` |*Deprecated* Siehe `pool.initFailFast`.
|`maxConnections` |integer |dynamic |*Deprecated* Siehe `pool.maxConnections`.
|`minConnections` |integer |`maxConnections` |*Deprecated* Siehe `pool.minConnections`.
|`maxThreads` |integer |dynamic |*Deprecated* Siehe `pool.maxConnections`.
|`pathSyntax` |object |`{ 'defaultPrimaryKey': 'id', 'defaultSortKey': 'id' }` | *Deprecated* Siehe [SQL-Pfad-Defaults](#source-path-defaults). 
|`computeNumberMatched` |boolean |`true` |*Deprecated* Siehe [Query-Generierung](#query-generation). 

<a name="connection-pool"></a>

### Pool

Einstellungen für den Connection-Pool.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`maxConnections` |integer |dynamisch |Steuert die maximale Anzahl von Verbindungen zur Datenbank. Der Default-Wert ist abhängig von der Anzahl der Prozessorkerne und der Anzahl der Joins in der [Types-Konfiguration](README.md#feature-provider-types). Der Default-Wert wird für optimale Performanz unter Last empfohlen. Der kleinstmögliche Wert ist ebenfalls von der Anzahl der Joins abhängig, kleinere Werte werden zurückgewiesen. 
|`minConnections` |integer |`maxConnections` |Steuert die minimale Anzahl von Verbindungen zur Datenbank, die jederzeit offen gehalten werden.
|`idleTimeout` |string |`10m` |Die maximale Zeit die eine Connection unbeschäftigt im Pool verbleibt. Bezieht sich nur auf Connections über der `minConnections` Grenze. Ein Wert von `0` bedeutet, dass unbeschäftigte Connections niemals aus dem Pool entfernt werden.
|`initFailFast` |boolean |`true` |Steuert, ob das Starten des Feature-Providers abgebrochen werden soll, wenn der Aufbau der ersten Connection länger dauert. Hat keinen Effekt bei `minConnections: 0`. Diese Option sollte in der Regel nur auf Entwicklungssystemen deaktiviert werden.
|`shared` |boolean |`false` |Wenn `shared` für mehrere Provider mit übereinstimmenden `host`, `database` und `user` aktiviert ist, teilen sich diese Provider einen Connection-Pool. Wenn eine der anderen Optionen in `connectionInfo` nicht übereinstimmt, schlägt der Start des Providers fehl.

<a name="source-path-defaults"></a>

## SQL-Pfad-Defaults

Defaults für die Pfad-Ausdrücke in `sourcePath`, siehe auch [SQL-Pfad-Syntax](#path-syntax).

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`sortKey` |string |`id` |Die Standard-Spalte die zur Sortierung von Reihen verwendet wird, wenn keine abweichende Spalte in `sourcePath` gesetzt wird. Es wird empfohlen, dass als Datentyp eine Ganzzahl verwendet wird.
|`primaryKey` |string |`id` |Die Standard-Spalte die zur Analyse von Joins verwendet wird, wenn keine abweichende Spalte in `sourcePath` gesetzt wird.

<a name="query-generation"></a>

## Query-Generierung

Optionen für die Query-Generierung.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`computeNumberMatched` |boolean |`true` |Steuert, ob bei Abfragen die Anzahl der selektierten Features berechnet und in `numberMatched` zurückgegeben werden soll oder ob dies aus Performancegründen unterbleiben soll. Bei großen Datensätzen empfiehlt es sich in der Regel, die Option zu deaktivieren.

<a name="path-syntax"></a>

## SQL-Pfad-Syntax

In dem Beispiel oben sind die wesentlichen Elemente der Pfadsyntax in der Datenbank bereits erkennbar. Der Pfad zu einer Eigenschaft ergibt sich immer als Konkatenation der relativen Pfadangaben (`sourcePath`), jeweils ergänzt um ein "/". Die Eigenschaft `sourcePath` ist beim ersten Objekt, das die Objektart repräsentiert, angegeben und bei allen untergeordneten Schemaobjekten, außer es handelt sich um einen festen Wert.

Auf der obersten Ebene entspricht der Pfad einem "/" gefolgt vom Namen der Tabelle zur Objektart. Jede Zeile in der Tabelle entsprich einem Feature. Beispiel: `/kita`.

Bei nachgeordneten relativen Pfadangaben zu einem Feld in derselben Tabelle wird einfach der Spaltenname angeben, z.B. `name`. Daraus ergibt sich der Gesamtpfad `/kita/name`.

Ein Join wird nach dem Muster `[id=fk]tab` angegeben, wobei `id` der Primärschlüssel der Tabelle aus dem übergeordneten Schemaobjekt ist, `fk` der Fremdschlüssel aus der über den Join angebundenen Tabelle und `tab` der Tabellenname. Siehe `[oid=kita_fk]plaetze` in dem Beispiel oben. Bei der Verwendung einer Zwischentabelle werden zwei dieser Joins aneinandergehängt, z.B. `[id=fka]a_2_b/[fkb=id]tab_b`.

Auf einer Tabelle (der Haupttabelle eines Features oder einer über Join-angebundenen Tabelle) kann zusätzlich ein einschränkender Filter durch den Zusatz `{filter=ausdruck}` angegeben werden, wobei `ausdruck` das Selektionskriertium in [CQL Text](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) spezifiziert. Für Details siehe das Modul [Filter / CQL](../services/filter.md), welches die Implementierung bereitstellt, aber nicht aktiviert sein muss.

Wenn z.B. in dem Beispiel oben nur Angaben zur Belegungskapazität selektiert werden sollen, deren Wert nicht NULL und gleichzeitig größer als Null ist, dann könnte man schreiben: `[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}`.

Ein vom Standard abweichender `sortKey` kann durch den Zusatz von `{sortKey=Spaltenname}` nach dem Tabellennamen angegeben werden.

Ein vom Standard abweichender `primaryKey` kann durch den Zusatz von `{primaryKey=Spaltenname}` nach dem Tabellennamen angegeben werden.
