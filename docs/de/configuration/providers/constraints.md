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

Als Beispiel hier die Eigenschaften der [Abschnitte/Äste-Features](https://demo.ldproxy.net/strassen/collections/abschnitteaeste/items) in der API [Straßennetz und Unfälle in NRW](https://demo.ldproxy.net/strassen) mit Constraints:

```yaml
types:
  abschnitteaeste:
    label: Abschnitte und Äste 
    description: 'Als Abschnitt wird ein gerichteter Teil des Straßennetzes bezeichnet, der zwischen zwei aufeinander folgenden Netzknoten liegt. Er wird durch die in den Netzknoten festgelegten Nullpunkte begrenzt.<br>Als Ast wird der Teil des Straßennetzes bezeichnet, der die Abschnitte untereinander verkehrlich verknüpft und deshalb Teil des Netzknotens ist. Er wird durch die im Netzknoten festgelegten Nullpunkte begrenzt. Eine Festlegung von Ästen erfolgt nur, wenn sie Bestandteil des aufzunehmenden Straßennetzes sind.<br>Weitere Begriffsdefinition siehe: <a href="http://www.bast.de/BASt_2017/DE/Verkehrstechnik/Publikationen/Regelwerke/V-asbkernsystem.pdf?__blob=publicationFile&v=3" target="_blank">Anweisung Straßeninformationsbank - ASB Version 2.03, Segment: Kernsystem</a>'
    sourcePath: /abschnitteaeste
    type: OBJECT
    properties:
      kennung:
        label: Kennung
        description: 16-stellige Kennung des Abschnittes oder Astes
        sourcePath: abs
        type: STRING
        role: ID
        constraints:
          regex: '^[0-9]{7}[A-Z][0-9]{7}[A-Z]$'
      strasse:
        label: Straße
        type: OBJECT
        objectType: Strasse
        properties:
          bez:
            label: Straßenbezeichnung
            sourcePath: strbez
            type: STRING
          klasse:
            label: Straßenklasse
            sourcePath: strkl
            type: STRING
            constraints:
              enum:
              - A
              - B
              - L
              - K
          nummer:
            label: Straßennummer
            sourcePath: strnr
            type: INTEGER
            constraints:
              min: 1
              max: 9999
          zusatz:
            label: Buchstabenzusatz
            description: Buchstabenzusatz zur Straßennummer
            sourcePath: strzus
            type: STRING
            constraints:
              regex: '^[A-Z]$'
      ...
      absast:
        label: Art
        description: Art des Abschnittes oder Astes
        sourcePath: absast
        type: STRING
        constraints:
          enum:
          - Abschnitt
          - Ast
      ...
      laenge_m:
        label: Länge [m]
        description: Länge des Abschnittes oder Astes (m)
        sourcePath: laenge
        type: INTEGER
        constraints:
          min: 0
```
