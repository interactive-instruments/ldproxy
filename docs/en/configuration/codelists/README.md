# Codelists

Codelists allow to map property values to a different value. This is useful especially for HTML representations. 

## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`id` |string | |Codelist identifier. Is referenced by `codelist` transformations and has to match the file name.
|`label` |string | |Human readable label.
|`sourceType` |enum | |Always `TEMPLATES`.
|`entries` |object |`{}` |Map with the original value as key and the new value as value. Values might use [`stringFormat` transformations](../providers/transformations.md).
|`fallback` |string |der Wert |Optional default value. Might use [`stringFormat` transformations](../providers/transformations.md).

### Example

Based on the INSPIRE codelist [EnvironmentalDomain](https://inspire.ec.europa.eu/codeList/EnvironmentalDomain), maps values like `soil` to the German label of the entry in the INSPIRE codelist registry.

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

## Storage

Codelists reside under the relative path `store/entities/codelists/{codelistId}.yml` in the data directory.
