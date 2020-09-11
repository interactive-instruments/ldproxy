# Die API-Konfiguration

Jede API-Konfiguration wird in einer Konfigurationsdatei in einem Objekt mit den folgenden Eigenschaften beschrieben. Werte ohne Defaultwert sind in diesem Fall Pflichtangaben.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`id` |string | |Eindeutiger Identifikator der API. Typischerweise identisch mit dem Identifikator des Feature-Providers.
|`apiVersion` |integer |`null` |Ist ein Wert angegeben, dann wird eine Version im Pfad der API-URIs ergänzt. Ohne Angabe ist der Pfad zur Landing Page `/{id}`, wobei `{id}` der Identifikator der API ist. Ist der Wert angegeben, dann ist der Pfad zur Landing Page `/{id}/v{apiVersion}`, also z.B. `/{id}/v1` beim Wert 1.
|`createdAt` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei erzeugt wurde. Der Wert wird automatisch vom Manager bei der Erzeugung gesetzt und besitzt nur informativen Charakter.
|`lastModified` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei zuletzt geändert wurde. Der Wert wird automatisch vom Manager bei jeder Änderung gesetzt und besitzt nur informativen Charakter.
|`entityStorageVersion` |integer | |Bezeichnet die Version des API-Definition-Konfigurationsdatei-Schemas. Diese Dokumentation bezieht sich auf die Version 2 und alle Dateien nach diesem Schema müssen den Wert 2 haben. Konfigurationen zu Version 1 werden automatisch auf Version 2 aktualisiert.
|`label` |string |der Wert von `id` |Eine Bezeichnung der API, z.B. für die Präsentation zu Nutzern.
|`description` |string |`null` |Eine Beschreibung des Schemaobjekts, z.B. für die Präsentation zu Nutzern.
|`serviceType` |enum | |Stets `OGC_API`.
|`shouldStart` |boolean |`true` |Steuert, ob die API mit dem Start von ldproxy aktiviert wird.
|`metadata` |object |`{}` |Über dieses Objekt können grundlegende Metadaten zur API (Version, Kontaktdaten, Lizenzinformationen) festgelegt werden. Erlaubt sind die folgenden Elemente (in Klammern werden die Ressourcen genannt, in denen die Angabe verwendet wird): `version` (API-Definition), `contactName` (API-Definition, HTML-Landing-Page), `contactUrl` (API-Definition, HTML-Landing-Page), `contactEmail` (API-Definition, HTML-Landing-Page), `contactPhone` (HTML-Landing-Page), `licenseName` (API-Definition, HTML-Landing-Page, Feature-Collections, Feature-Collection), `licenseUrl` (API-Definition, HTML-Landing-Page, Feature-Collections, Feature-Collection) und `keywords` (HTML-Landing-Page). Alle Angaben sind Strings, bis auf die Keywords, die als Array von Strings angegeben werden.
|`extenalDocs` |object |`{}` |Es kann externes Dokument mit weiteren Informationen angegeben werden, auf das aus der API verlinkt wird. Anzugeben sind die Eigenschaften `url` und `description`.
|`defaultExtent` |object |`{}` |Es kann ein Standardwert für die räumliche (`spatial`) und/oder zeitliche (`temporal`) Ausdehnung der Daten angeben werden, die bei den Objektarten verwendet wird, wenn dort keine anderslautende Ausdehnung spezifiziert wird. Für die räumliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Für die zeitliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in Millisekunden seit dem 1.1.1970): `start`, `end`. Hinweis: Es handelt sich hierbei nicht um die Ausdehnung des Datensatzes insgesamt, dieser wird stets automatisch aus den Ausdehnungen der einzelnen Objektarten ermittelt.
|`api` |array |`[]` |Ein Array mit der Konfiguration der [API-Module](building-blocks/README.md) für die API.
|`collections` |object |`{}` |Ein Objekt mit der spezifischen Konfiguration zu jeder Objektart, der Name der Objektart ist der Schlüssel, der Wert ein [Collection-Objekt](#collection).
|`auto` |boolean |false |Steuert, ob die Informationen zu `collections` beim Start automatisch aus dem Feature-Provider bestimmt werden sollen (Auto-Modus). In diesem Fall sollte `collections` nicht angegeben sein.
|`autoPersist` |boolean |false |Steuert, ob die im Auto-Modus (`auto: true`) bestimmten Schemainformationen in die Konfigurationsdatei übernommen werden sollen. In diesem Fall werden `auto` und `autoPersist` beim nächsten Start automatisch aus der Datei entfernt. Liegt die Konfigurationsdatei in einem anderen Verzeichnis als unter `store/entities/services` (siehe `additionalLocations`), so wird eine neue Datei in `store/entities/services` erstellt. `autoPersist: true` setzt voraus, dass `store` sich nicht im `READ_ONLY`-Modus befindet.

<a name="collection"></a>

## Das Collection-Objekt für Objektarten aus einem Feature-Provider

Jedes Collection-Objekt beschreibt eine Objektart (derzeit werden nur Feature Collections von ldproxy unterstützt). Es setzt sich aus den folgenden Eigenschaften zusammen:

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`id` |string | |Eindeutiger Identifikator der API. Typischerweise identisch mit dem Identifikator des Feature-Providers.
|`label` |string |der Wert von `id` |Eine Bezeichnung der API, z.B. für die Präsentation zu Nutzern.
|`description` |string |`null` |Eine Beschreibung des Schemaobjekts, z.B. für die Präsentation zu Nutzern.
|`persistentUriTemplate` |string |`null` |Über die Feature-Ressource hat jedes Feature zwar eine feste URI, die für Links verwendet werden kann, allerdings ist die URI nur so lange stabil, wie die API stabil bleibt. Um von Veränderungen in der URI unabhängig zu sein, kann es sinnvoll oder gewünscht sein, API-unabhängige URIs für die Features zu definieren und von diesen URIs auf die jeweils gültige API-URI weiterzuleiten. Diese kananosche URI kann auch in ldproxy Konfiguriert und bei den Features kodiert werden. Hierfür ist ein Muster der Feature-URI anzugeben, wobei `{{value}}` als Ersetzungspunkt für den lokalen Identifikator des Features in der API angegeben werden kann.
|`extent` |object |`{}` |Es kann die räumliche (`spatial`) und/oder zeitliche (`temporal`) Ausdehnung der Features von der Objektart angeben werden. Für die räumliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in `CRS84`): `xmin`, `ymin`, `xmax`, `ymax`. Für die zeitliche Ausdehnung sind die folgenden Eigenschaften anzugeben (alle Angaben in Millisekunden seit dem 1.1.1970): `start`, `end`. Soll die räumliche Ausdehnung aus den Daten automatisch beim Start von ldproxy ermittelt werden, kann `computeSpatialExtent` mit dem Wert `true` angegeben werden. Bei großen Datenmengen verzögert diese Option allerdings die Zeitdauer, bis die API verfügbar ist.
|`additionalLinks` |array |`[]` |Erlaubt es, zusätzliche Links bei jeder Objektart zu ergänzen. Der Wert ist ein Array von Link-Objekten. Anzugeben sind jeweils mindestens die URI (`href`), der anzuzeigende Text (`label`) und die Link-Relation (`rel`).
|`api` |array |`[]` |Ein Array mit der Konfiguration der [API-Module](building-blocks/README.md) für die Objektart.

## Die Objekte für die Konfiguration von ldproxy-API-Modulen

Ein Array dieser Modul-Konfigurationen steht auf der Ebene der gesamten API und für jede Collection zur Verfügung. Die jeweils gültige Konfiguration ergibt sich aus der Priorisierung:

* Ist nichts angegeben, dann gelten die im ldproxy-Code vordefinierten Standardwerte. Diese sind bei den jeweiligen [API-Modulen](building-blocks/README.md) spezifiziert.
* Diese systemseitigen Standardwerte können von den Angaben im Verzeichnis "defaults" überschrieben werden.
* Diese deploymentweiten Standardwerte können von den Angaben in der API-Definition auf Ebene der API überschrieben werden.
* Diese API-weiten Standardwerte können bei den Collection-Ressourcen und untergeordneten Ressourcen von den Angaben in der API-Definition auf Ebene der Collection überschrieben werden.
* Diese Werte können durch Angaben im Verzeichnis "overrides" überschrieben werden.

## Eine API-Beispielkonfiguration

```yaml
id: kita
createdAt: 1598603585258
lastModified: 1598603585258
entityStorageVersion: 2
label: Kindertageseinrichtungen
description: Hier steht eine Beschreibung der API und seiner Inhalte, die einem Nutzer erläutert, was ihm die API bietet.
shouldStart: true
secured: false
serviceType: OGC_API
apiVersion: 1
externalDocs:
  url: "https://example.com/pfad/zum/dokument"
  description: Weitere Informationen zu den Kita-Daten
defaultExtent:
  spatial:
    xmin: 5.8663153
    ymin: 47.2701114
    xmax: 15.0419319
    ymax: 55.099161
metadata:
  keywords:
  - Kinderbetreuung
  - Kindertageseinrichtungen
  - Kindertagesstätten
  - Kindergarten
  - Spielgruppen
  - Kinder
  - Kita
  - INSPIRE
api:
- buildingBlock: COLLECTIONS
  additionalLinks:
  - rel: describedby
    type: application/xml
    title: INSPIRE-Metadaten zum Datensatz
    href: 'https://example.org/pfad/zu/metadaten'
    hreflang: de
  - rel: enclosure
    type: text/csv
    title: Download der Daten als CSV
    href: 'https://example.org/pfad/zu/datei.csv'
    hreflang: de
collections:
  governmentalservice:
    id: governmentalservice
    label: Staatlicher Dienst
    description: 'Staatliche Verwaltungs- und Sozialdienste wie öffentliche Verwaltung, Katastrophenschutz, Schulen und Krankenhäuser, die von öffentlichen oder privaten Einrichtungen erbracht werden, soweit sie in den Anwendungsbereich der Richtlinie 2007/2/EG fallen. Dieser Datensatz enthält Informationen zu Diensten der Kinderbetreuung.'
    persistentUriTemplate: '{{value | prepend:''https://example.com/id/soziales/kindergarten/''}}'
    extent:
      spatialComputed: true
    api:
    - buildingBlock: FEATURES_CORE
      featureType: governmentalservice
      queryables:
        spatial:
        - geometry
        other:
        - name
        - pointOfContact.address.postCode
        - pointOfContact.address.adminUnit
        - pointOfContact.telephoneVoice
        - occupancy.typeOfOccupant
        - occupancy.numberOfOccupants
      transformations:
        pointOfContact.telephoneVoice:
          null: 'bitte ausf(ue|ü)llen'
        occupancy[].anzahl:
          null: '0'
        inspireId:
          stringFormat: 'https://example.com/id/soziales/kindergarten/{{value}}'
    - buildingBlock: FEATURES_HTML
      itemLabelFormat: '{{name}}'
      transformations:
        geometry:
          remove: OVERVIEW
        occupancy[].typeOfOccupant:
          remove: OVERVIEW
        occupancy[].numberOfOccupants:
          remove: OVERVIEW
```
