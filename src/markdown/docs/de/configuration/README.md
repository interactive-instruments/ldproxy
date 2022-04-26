# Übersicht

<a name="manager"></a>

## Manager

Die Konfiguration von einfachen APIs kann  über den [Manager](./manager/README.md) im Webbrowser erfolgen. 

## Konfigurationsdateien

Alle deployment-spezifischen Konfigurationseinstellungen und -dateien liegen im [Daten-Verzeichnis](../data-folder.md) (typischerweise "data"). Die Konfiguration von ldproxy und jeder API wird in YAML-Dateien gespeichert (`cfg.yml` und Dateien in `store`).

### Globale Konfiguration

Die Datei `cfg.yml` im Daten-Verzeichnis enthält die [globale Konfiguration](global-configuration.md) für alle APIs in dem Deployment.

Hier eine einfache Beispielkonfiguration, die Änderungen durch ldproxy an den Konfigurationsdateien verbietet, eine externe URL konfiguriert, das Logging nur für die ldproxy/xtraplatform-Klassen auf der Konsole aktiviert (auf dem Level `INFO`) und die Zeitzone für die Zeitstempel vorgibt:

```yaml
store:
  mode: READ_ONLY
server:
  externalUrl: http://example.com/apis
logging:
  level: 'OFF'
  appenders:
    - type: console
      timeZone: Europe/Berlin
  loggers:
    de.ii: INFO
```

<a name="configuration-object-types"></a>

### Konfigurationsobjekte

Die Struktur des `store` mit dem Untervereichnis `entities` und den optionalen Unterverzeichnissen `defaults` und `overrides` ist bei der [globalen Konfiguration](global-configuration.md#entities-defaults-overrides) beschrieben. Angaben in `defaults` setzen deploymentweite Standardwerte und dienen dazu, dass für die meisten APIs gültige Vorgaben nur einmal konfiguriert werden müssen. Die Angaben in `overrides` überschreiben die Werte in `entities` und sind für lokale Anpassungen von standardisierten Konfigurationen gedacht, z.B. in Test- und Entwicklungsumgebungen.

Es gibt in ldproxy drei Typen von [Konfigurationsobjekten](global-configuration.md#entities-defaults-overrides): Daten-Provider, APIs und Codelisten.

#### Provider

Alle Provider in dieser ldproxy-Version sind [Feature-Provider](providers/README.md).

Dieser Konfigurationsobjekt-Typ hat die folgenden Eigenschaften:

|Parameter |Wert
|--- |---
|`{typ}` |`providers`
|`{sub-typ}` |n/a
|`{id}` |die Id des Feature-Providers

#### APIs

Siehe [API-Konfigurationen](services/README.md).

Dieser Konfigurationsobjekt-Typ hat die folgenden Eigenschaften:

|Parameter |Wert
|--- |---
|`{typ}` |`services`
|`{sub-typ}` |`ogc_api`
|`{id}` |die Id der API

#### Codelisten

Siehe [Codelisten](codelists/README.md).

Dieser Konfigurationsobjekt-Typ hat die folgenden Eigenschaften:

|Parameter |Wert
|--- |---
|`{typ}` |`codelists`
|`{sub-typ}` |n/a
|`{id}` |die Id der Codelist

<a name="special-cases"></a>

#### Besonderheiten

Bei der Verwendung von `defaults` und `overrides` sind die folgenden in Besonderheiten zu beachten:

* Beim [Aufsplitten von Konfigurationsobjekten](global-configuration.md#split-defaults-overrides) ist bei `buildingBlock`-Objekten im `api`-Array der Wert von `buildingBlock` in Kleinbuchstaben als Dateiname zu verwenden, nicht [der Schlüssel des JSON-Elements](global-configuration.md#array-exceptions)). Beispiel: `store/defaults/services/ogc_api/features-core.yml`.

Der [HTTP-Client](global-configuration.md#http-client) wird nur bei der Verwendung von WFS-Feature-Providern benötigt.

## Beispiele

Konfigurationsbeispiele in dieser Dokumentation sind i.d.R. Auszüge aus den Provider- und API-Konfigurationen der [ldproxy-Demo-APIs](../demos.md).
