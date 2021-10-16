# Modul "Sorting" (SORTING)

Das Modul "Sorting" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider, der Sortierung unterstützt, aktiviert werden. Es ergänzt die folgenden Query-Parameter:

* `sortby` (Ressource "Features"): Ist der Parameter angegeben, werden die Features sortiert zurückgegeben. Sortiert wird nach den in einer kommaseparierten Liste angegebenen Attributen. Dem Attributnamen kann ein `+` (aufsteigend, das Standardverhalten) oder ein `-` (absteigend) vorangestellt werden. Beispiel: `sortby=type,-name`.

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`sortables` |object |`{}` |Steuert, welche der Attribute in Queries für die Sortierung von Daten verwendet werden können. Erlaubt sind nur direkte Attribute (keine Attribute aus Arrays oder eingebetteten Objekten) der Datentypen `STRING`, `DATETIME`, `INTEGER` und `FLOAT`. Aktuell besteht die Einschränkung, dass alle verwendeten Attribute eindeutige Werte haben, siehe [Issue 488](https://github.com/interactive-instruments/ldproxy/issues/488).
