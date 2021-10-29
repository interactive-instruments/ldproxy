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

Based on the INSPIRE codelist [EnvironmentalDomain](https://inspire.ec.europa.eu/codeList/EnvironmentalDomain), maps values like `soil` to a markdown link pointing to the INSPIRE codelist registry.

```yaml
---
id: environmental-domain
label: Umweltbereich, für den Umweltziele festgelegt werden können.
sourceType: TEMPLATES
entries:
  soil: "[Boden](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  noise: "[Lärm](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  naturalResources: "[natürliche Ressourcen](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  climateAndClimateChange: "[Klima und Klimawandel](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  healthProtection: "[Gesundheitsschutz](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  air: "[Luft](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  water: "[Wasser](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  waste: "[Abfall](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  natureAndBiodiversity: "[Natur und biologische Vielfalt](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  sustainableDevelopment: "[nachhaltige Entwicklung](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
  landUse: "[Bodennutzung](https://inspire.ec.europa.eu/codelist/EnvironmentalDomain/{{value}})"
fallback: "{{value}} (unbekannter Wert)"
```

### Storage 

Codelists reside under the relative path `store/entities/codelists/{codelistId}.yml` in the data directory.
