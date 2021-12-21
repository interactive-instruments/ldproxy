# Codelisten

Codelisten können zum Übersetzen von Eigenschaftswerten in einen anderen Wert genutzt werden, meist für die HTML-Ausgabe. Die Codelisten liegen als YAML-Dateien im ldproxy-Datenverzeichnis unter dem relativen Pfad `store/entities/codelists/{codelistId}.yml`.

Die nachfolgende Tabelle beschreibt die Struktur der Codelisten-Dateien.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`id` |string | |Identifiziert die Codelist. Die id wird in `codelist`-Transformationen angegeben und muss dem Dateinamen entsprechen.
|`label` |string | |Eine lesbare Bezeichnung der Codelist, die im Manager angezeigt wird.
|`sourceType` |enum | |`TEMPLATES` für alle manuell erstellte Codelisten.
|`entries` |object |`{}` |Jeder Eintrag bildet einen Wert auf den neuen Wert ab.
|`fallback` |string |der Wert |Optional kann ein Defaultwert angegeben werden. Dabei können auch [`stringFormat`-Transformationen](../providers/transformations.md) genutzt werden.

Bei den Zielwerten in `entries` und bei `fallback` können auch [`stringFormat`-Transformationen](../providers/transformations.md) genutzt werden. Ist der transformierte Wert für die HTML-Ausgabe gedacht, dann kann auch Markdown-Markup verwendet werden, dieser wird bei der HTML-Ausgabe aufbereitet.

Ein Beispiel, basierend auf der INSPIRE-Codelist [EnvironmentalDomain](https://inspire.ec.europa.eu/codeList/EnvironmentalDomain), der Werte wie "soil" auf das deutschsprachige Label in der INSPIRE-Codelist-Registry abbildet:

```yaml
---
id: environmental-domain
label: Umweltbereich, für den Umweltziele festgelegt werden können.
sourceType: TEMPLATES
entries:
  air: Luft
  climateAndClimateChange: Klima und Klimawandel
  healthProtection: Gesundheitsschutz
  landUse: Bodennutzung
  naturalResources: natürliche Ressourcen
  natureAndBiodiversity: Natur und biologische Vielfalt
  noise: Lärm
  soil: Boden
  sustainableDevelopment: nachhaltige Entwicklung
  waste: Abfall
  water: Wasser
```
