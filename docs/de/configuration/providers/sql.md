# Der SQL-Feature-Provider

Hier werden die Besonderheiten des SQL-Feature-Providers beschrieben.

<a name="connection-info"></a>

## Das Connection-Info-Objekt für SQL-Datenbanken

Das Connection-Info-Objekt für PostgreSQL/PostGIS-Datenbanken wird wie folgt beschrieben:

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`connectorType` |enum | |Stets `SLICK`.
|`dialect` |enum | |Stets `PGIS`.
|`host` |string | |Der Datenbankhost. Wird ein anderer Port als der Standardport verwendet, ist dieser durch einen Doppelpunkt getrennt anzugeben, z.B. `db:30305`.
|`database` |string | |Der Name der Datenbank.
|`schemas` |array |`[ 'public' ]` |Die Namen der Schemas in der Datenbank, auf die zugegriffen werden soll.
|`user` |string | |Der Benutzername.
|`password` |string | |Das mit base64 kodierte Passwort des Benutzers.
|`initFailFast` |boolean |`true` |Steuert, ob das Starten des Feature-Providers abgebrochen werden soll, wenn der Verbindungsaufbau etwas länger dauert. Diese Option sollte in der Regel nur auf Entwicklungssystemen deaktiviert werden.
|`maxThreads` |integer |16 |Steuert die maximale Anzahl von parallelen Threads für den Zugriff auf die Datenbank.
|`pathSyntax` |object |`{ 'defaultPrimaryKey': 'id', 'defaultSortKey': 'id', 'junctionTablePattern': '.+_2_.+' }` |ldproxy erwartet für die Ausführung von Queries auf der Datenbank in jeder Tabelle, mit Ausnahme von Zwischentabellen, eine Spalte mit einen eindeutigen Primärschlüssel. Der Name ist in `defaultPrimaryKey` anzugeben, Default ist "id". Es wird empfohlen, dass als Datentyp eine Ganzzahl verwendet wird.<br>Um das Paging effizient unterstützen zu können, wird bei jeder dieser Tabellen eine Spalte zum Sortieren der selektierten Objekte verwendet. Standardmäßig ist dies ebenso die Spalte "id".<br>ldproxy muss Zwischentabellen als solche über den Namen erkennen können. Sofern diese in der Datenbank vorkommen, muss der reguläre Ausdruck zur Erkennung der Zwischentabellen in `junctionTablePattern` angegeben werden, der Default erkennt z.B. eine Tabelle mit Namen "featurea_2_featureb" als Zwischentabelle.
|`computeNumberMatched` |boolean |`true` |Steuert, ob bei Abfragen auf der Features-Ressource die Anzahl der selektierten Features berechnet und in `numberMatched` zurückgegeben werden soll oder ob dies aus Performancegründen unterbleiben soll. Bei großen Datensätzen empfiehlt es sich in der Regel, die Option zu deaktivieren.

Bei `connectionInfo`-Objekten muss stets [das ganze Objekt angegeben/wiederholt werden](../global-configuration.md#merge-exceptions). Ein `connectionInfo`-Objekt in den Overrides ersetzt ein `connectionInfo`-Objekt in den Defaults oder den regulären Konfigurationsobjekten. 

Wird ein `connectionInfo`-Objekt immer in den Overrides gesetzt, dann ist ein minimales Objekt mit den Pflichtangaben im regulären Provider anzugeben, z.B.:

```yaml
connectionInfo:
  host: ''
  database: ''
  user: ''
  password: ''
```

<a name="path-syntax"></a>

## SQL-Feature-Provider-Pfadsyntax

In dem Beispiel oben sind die wesentlichen Elemente der Pfadsyntax in der Datenbank bereits erkennbar. Der Pfad zu einer Eigenschaft ergibt sich immer als Konkatenation der relativen Pfadangaben (`sourcePath`), jeweils ergänzt um ein "/". Die Eigenschaft `sourcePath` ist beim ersten Objekt, das die Objektart repräsentiert, angegeben und bei allen untergeordneten Schemaobjekten, außer es handelt sich um einen festen Wert.

Auf der obersten Ebene entspricht der Pfad einem "/" gefolgt vom Namen der Tabelle zur Objektart. Jede Zeile in der Tabelle entsprich einem Feature. Beispiel: "/kita".

Bei nachgeordneten relativen Pfadangaben zu einem Feld in derselben Tabelle wird einfach der Spaltenname angeben, z.B. "name". Daraus ergibt sich der Gesamtpfad "/kita/name".

Ein Join wird nach dem Muster "[id=fk]tab" angegeben, wobei "id" der Primärschlüssel der Tabelle aus dem übergeordneten Schemaobjekt ist, "fk" der Fremdschlüssel aus der über den Join angebundenen Tabelle und "tab" der Tabellenname. Siehe "[oid=kita_fk]plaetze" in dem Beispiel oben. Bei der Verwendung einer Zwischentabelle werden zwei dieser Joins aneinandergehängt, z.B. "[id=fka]a_2_b/[fkb=id]tab_b".

Auf einer Tabelle (der Haupttabelle eines Features oder einer über Join-angebundenen Tabelle) kann zusätzlich ein einschränkender Filter durch den Zusatz "{filter=ausdruck}" angegeben werden, wobei "ausdruck" das Selektionskriertium in ["CQL Text"](http://docs.opengeospatial.org/DRAFTS/19-079.html#cql-text) spezifiziert, in dem Umfang in dem das Modul ["Filter / CQL"](../services/filter.md) Ausdrücke in "CQL Text" auf Basis des im Provider spezifizierten Schemas unterstützt. Das Modul "Filter / CQL" muss nicht aktiviert sein.

Wenn z.B. in dem Beispiel oben nur Angaben zur Belegungskapazität selektiert werden sollen, deren Wert nicht NULL und gleichzeitig größer als Null ist, dann könnte man schreiben: "[oid=kita_fk]plaetze{filter=anzahl IS NOT NULL AND anzahl>0}".
