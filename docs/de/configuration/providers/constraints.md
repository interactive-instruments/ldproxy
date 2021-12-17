<a name="constraints"></a>

# Constraints

In der Konfiguration der Objektarten im Feature-Provider können Schema-Einschränkungen dokumentiert werden. Diese werden z.B. bei der Erzeugung von JSON-Schema-Dokumenten verwendet.

|Constraint |Datentyp |Beschreibung
| --- | --- | ---
|`codelist` |string |Identifiziert eine Codelist in der API-Konfiguration, die für die Eigenschaft gilt. Nur bei String- oder Integer-Eigenschaften sinnvoll.
|`enum` |array |Liste von erlaubten Werten für die Eigenschaft. Nur bei String- oder Integer-Eigenschaften sinnvoll.
|`regex` |string |Ein regulärer Ausdruck, der von allen Werten erfüllt werden muss. Nur bei String-Eigenschaften sinnvoll.
|`required` |boolean |Eine Eigenschaft kann als Pflichteigenschaft, die in allen Instanzen gesetzt sein muss, qualifiziert werden.
|`min` |number |Mindestwert für alle Instanzen. Nur bei numerischen Eigenschaften sinnvoll.
|`max` |number |Maxmialwert für alle Instanzen. Nur bei numerischen Eigenschaften sinnvoll.
|`minOccurrence` |number |Mindestanzahl von Werten für alle Instanzen. Nur bei Array-Eigenschaften sinnvoll.
|`maxOccurrence` |number |Maxmimalanzahl von Werten für alle Instanzen. Nur bei Array-Eigenschaften sinnvoll.
