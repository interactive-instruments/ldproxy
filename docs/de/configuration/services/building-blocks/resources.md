# Modul "Resources" (RESOURCES)

Das Modul "Resources" kann für jede über ldproxy bereitgestellte API aktiviert werden. Es ergänzt Ressourcen für die Bereitstellung und Verwaltung von Datei-Ressourcen, vor allem für Styles (Symbole, Sprites).

|Ressource |Pfad |HTTP-Methode |Unterstützte Ein- und Ausgabeformate
| --- | --- | --- | ---
|Resources |`/{apiId}/resources` |GET |HTML, JSON
|Resource |`/{apiId}/resources/{resourceId}` |GET<br>PUT<br>DELETE |\*<br>\*<br>n/a

Erlaubte Zeichen für `{resourceId}` sind alle Zeichen bis auf den Querstrich ("/").

In der Konfiguration können die folgenden Optionen gewählt werden:

|Option |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`managerEnabled` |boolean |`false` |Steuert, ob die Ressourcen über PUT und DELETE über die API erzeugt und gelöscht werden können sollen.
|`caching` |object |`{}` |Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.

Die Ressourcen liegen als Dateien im ldproxy-Datenverzeichnis und dem relativen Pfad `api-resource/resources/{apiId}/{resourceId}`.

Beispiel für die Angaben in der Konfigurationsdatei:

```yaml
- buildingBlock: RESOURCES
  enabled: true
  managerEnabled: false
```
