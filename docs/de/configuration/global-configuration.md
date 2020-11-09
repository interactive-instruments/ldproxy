# Globale Konfiguration

Die Konfigurationsdatei `cfg.yml` befindet sich im Daten-Verzeichnis.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`store` |object | |Konfiguration des [Store](#store])
|`server` |object | |Konfiguration des [Webserver](#webserver)
|`httpClient` |object | |Konfiguration des [HTTP-Client](#http-client)
|`logging` |object | |Konfiguration des [Logging](#logging)

<a name="store"></a>

## Store

Der Store enthält Konfigurationsobjekte.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`mode` |enum |`READ_WRITE` |`READ_WRITE` oder `READ_ONLY`. Bestimmt ob die Software Änderungen am Store vornehmen darf.
|`additionalLocations` |array |`[]` | Liste von Pfaden mit [zusätzlichen Verzeichnissnen](#additional-locations).

<a name="entities-defaults-overrides"></a>

### Struktur des Store

Jedes Konfigurationsobjekt hat einen Typ, einen optionalen Sub-Typ sowie eine für den Typ eindeutige Id (siehe [Konfigurationsobjekt-Typen](README.md#configuration-object-types)). Der Pfad zur entsprechenden Konfigurationsdatei sieht dann so aus:

```text
store/entities/{typ}/{id}.yml
```

Defaults für alle Konfigurationsobjekte eines Typs oder Sub-Typs können optional in solchen Dateien gesetzt werden:

```text
store/defaults/{typ}.yml
store/defaults/{typ}.{sub-typ}.yml
```

Außerdem kann man noch Overrides für Konfigurationsobjekte anlegen. Diese haben z.B. den Zweck, umgebungsspezifische Anpassungen vorzunehmen, ohne die Haupt-Konfigurationsdateien ändern zu müssen. Der Pfad zu den Overrides sieht so aus:

```text
store/overrides/{typ}/{id}.yml
```

Defaults, Konfigurationsobjekte und Overrides werden in dieser Reihenfolge eingelesen und zusammengeführt. Das heißt Angaben im Konfigurationsobjekt überschreiben Angaben in Defaults und Angaben in Overrides überschreiben sowohl Angaben in Defaults als auch im Konfigurationsobjekt.

Das Zusammenführen funktioniert auch für verschachtelte Strukturen, d.h. man muss in den verschiedenen Dateien keine Angaben wiederholen, sondern kann z.B. in Overrides nur gezielt die Angaben setzen, die man überschreiben will.

<a name="merge-exceptions"></a>

Ausnahmen, bei denen das Zusammenführen für verschachtelte Strukturen nicht funktioniert und ganze Objekte wiederholt werden müssen, sind ensprechend in der Beschreibung der [Konfigurationsobjekt-Typen](README.md#configuration-object-types) im Abschnitt ["Besonderheiten"](README.md#special-cases) genannt.

Die zusammengeführten Konfigurationsobjekte müssen dann alle Pflichtangaben enthalten, ansonsten kommt es beim Start zu einem Fehler.

<a name="additional-locations"></a>

### Zusätzliche Verzeichnisse

Bei fest vordefinierten oder standardisierten Konfigurationsobjekten kann es Sinn machen, umgebungsspezifische Anpassungen in einem separaten Verzeichnis vorzunehmen. Ein oder mehrere solche Verzeichnisse können mit `additionalLocations` konfiguriert werden. Die anzugebenden Pfade können entweder absolut oder relativ zum Daten-Verzeichnis sein, also z.B.:

```yml
store:
  additionalLocations:
    - env/test
```

Ein solches Verzeichnis kann dann wiederum Defaults und Overrides enthalten, also z.B.:

```text
env/test/defaults/{typ}.yml
env/test/overrides/{typ}/{id}.yml
```

Die Reihenfolge der Zusammenführung für alle aufgeführten Pfade sähe dann so aus:

```text
store/defaults/{typ}.yml
store/defaults/{typ}.{sub-typ}.yml
env/test/defaults/{typ}.yml
store/entities/{typ}/{id}.yml
store/overrides/{typ}/{id}.yml
env/test/overrides/{typ}/{id}.yml
```

<a name="split-defaults-overrides"></a>

### Aufsplitten von Defaults und Overrides

Defaults und Overrides können in kleinere Dateien aufgesplittet werden, z.B. um die Übersichtlichkeit zu erhöhen. Die Aufsplittung folgt dabei der Objektstruktur in den Konfigurationsobjekten.

```yml
key1:
  key2:
    key3: value1
```

Um ein Default oder Override für diesen Wert zu setzen, könnten die oben beschriebenen Dateien verwendet werden:

```text
store/defaults/{typ}.{sub-typ}.yml
store/overrides/{typ}/{id}.yml
```

Es können aber auch separate Dateien für das Objekt `key1` oder das Objekt `key2` angelegt werden, z.B.:

```text
store/defaults/{typ}/{sub-typ}/key1.yml
```

```yml
key2:
  key3: value2
```

```text
store/overrides/{typ}/{id}/key1/key2.yml
```

```yml
key3: value3
```

Der Pfad des Objekts kann also sozusagen aus dem YAML ins Dateisystem verlagert werden.

Die Reihenfolge der Zusammenführung folgt der Spezifität des Pfads. Für alle aufgeführten Pfade sähe dann so aus:

```text
store/defaults/{typ}.{sub-typ}.yml
store/defaults/{typ}/{sub-typ}/key1.yml
store/entities/{typ}/{id}.yml
store/overrides/{typ}/{id}.yml
store/overrides/{typ}/{id}/key1/key2.yml
```

<a name="array-exceptions"></a>

Es gibt einige Sonderfälle, bei denen das Aufsplitten nicht nur anhand der Objektpfade erlaubt ist, sondern z.B. auch für eindeutig referenzierbare Array-Element. Auf diese Sonderfälle wird in der Beschreibung der [Konfigurationsobjekt-Typen](README.md#configuration-object-types) im Abschnitt ["Besonderheiten"](README.md#special-cases) eingegangen.

<a name="environment-variables"></a>

### Umgebungsvariablen

Sowohl in der `cfg.yml` als auch in Konfigurationsobjekten, Defaults und Overrides können Ersetzungen durch Umgebungsvariablen vorgenommen werden.

Ein solcher Ausdruck `${NAME}` in einer dieser Dateien wird durch den Wert der Umgebungsvariable `NAME` ersetzt. Ist die Variable nicht gesetzt, wird `null` eingesetzt. Man kann auch einen Default-Wert angeben, für den Fall, dass die Variable nicht gesetzt ist. Das sähe dann so `${NAME:-WERT}` aus.

In der `cfg.yml` kann man diesen Mechanismus z.B. verwenden, um ein zusätzliches Verzeichnis anhängig von einer Umgebungsvariable zu definieren:

```yml
store:
  additionalLocations:
    - env/${DEPLOYMENT_ENV:-production}
```

Um die Dateien aus obigem Beispiel in `env/test` zu laden, müsste man dann die Umgebunsvariable `DEPLOYMENT_ENV=test` setzen. Wenn diese nicht gesetzt ist würde das Verzeichnis `env/production` geladen.

## Webserver

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`externalUrl` |string | |Die [externe URL](#external-url) des Webservers
|`requestLog` |object | |Konfiguration des [Request-Logging](#request-logging)
|`applicationConnectors` |object | |Konfiguration des [Ports](#port)

<a name="external-url"></a>

### Externe URL

Wenn die Applikation hinter einem weiteren Webserver betrieben wird, z.B. für HTTPS oder um den Pfad zu ändern, unter dem die Dienste erreichbar sind (`/rest/services`), muss die externe URL konfiguriert werden.

Ein verbreiteter Anwendungsfall wäre mittels *Apache HTTP Server* ein *ProxyPass* von `https://example.org/ldproxy` nach `http://ldproxy-host:7080/rest/services` einzurichten. Dann müsste folgendes konfiguriert werden:

```yaml
server:
  externalUrl: https://example.org/ldproxy/
```

<a name="request-logging"></a>

### Request-Logging

Request-Logging ist standardmäßig deaktiviert. Dieses Beispiel würde das Schreiben von Request-Logs nach `data/log/requests.log` aktivieren. Es aktiviert auch die tägliche Log-Rotation und verwahrt alte Logs gezippt für eine Woche.

```yaml
server:
  requestLog:
    type: classic
    timeZone: Europe/Berlin
    appenders:
      - type: file
        currentLogFilename: data/log/requests.log
        archive: true
        archivedLogFilenamePattern: data/log/requests-%d.zip
        archivedFileCount: 7
```

<a name="port"></a>

### Port

Der Standard-Port des Webservers ist `7080`. Dieser kann geändert werden, z.B. wenn es einen Konflikt mit einer anderen Anwendung gibt.

```yaml
server:
  applicationConnectors:
    - type: http
      port: 8080
```

<a name="http-client"></a>

## HTTP-Client

Einige Konfigurationsobjekte verwenden einen HTTP-Client zum Zugriff auf externe Ressourcen. Ein ensprechender Hinweis findet sich in der Beschreibung der [Konfigurationsobjekt-Typen](README.md#configuration-object-types) im Abschnitt ["Besonderheiten"](README.md#special-cases).

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`proxy` |object | |Konfiguration eines [HTTP-Proxy](#http-proxy)
|`timeout` |string |`30s` |Konfiguration des [Idle-Timeout](#idle-timout)

<a name="http-proxy"></a>

### HTTP-Proxy

Falls die Applikation einen HTTP-Proxy verwenden muss, um auf externe Ressourcen zuzugreifen, kann dieser wie folgt konfiguriert werden.

In diesem Beispiel ist die HTTP-Proxy-URL `http://localhost:8888`. Verbindungen zu Hosts die unter `nonProxyHosts` augelistet sind werden direkt und nicht durch den HTTP-Proxy hergestellt. In diesem Beispiel wären das `localhost`, jede IP-Addresse die mit `192.168.` anfängt und jede Subdomain von `example.org`.

```yaml
httpClient:
  proxy:
    host: localhost
    port: 8888
    scheme : http
    nonProxyHosts:
      - localhost
      - '192.168.*'
      - '*.example.org'
```

<a name="idle-timeout"></a>

### Idle-Timeout

Diese Einstellung sollte nur angepasst werden, falls Nutzer von anhaltenden Problemen mit langlaufenden Requests berichten. In den meisten Fällen wird die Standard-Einstellung von 30 Sekunden empfohlen.

```yaml
httpClient:
  timeout: 120s
```

<a name="logging"></a>

## Logging

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`level` |string |`OFF` |Log-Level für Third-Party-Libraries. Sollte nur für Fehlerberichte an den Hersteller auf `WARN` gesetzt werden.
|`appenders` |object | |Konfiguration der [Log-Ausgabe](#log-appenders)
|`loggers` |object | |Konfiguration des [Log-Level](#log-level)

<a name="log-appenders"></a>

### Log-Ausgabe

Standardmäßig werden Applikations-Logs nach `data/log/xtraplatform.log` geschrieben. Die tägliche Log-Rotation ist aktiviert und alte Logs werden gezippt und für eine Woche verwahrt. Die Log-Datei oder die Rotations-Einstellungen können geändert werden:

```yaml
logging:
  appenders:
    - type: file
      currentLogFilename: /var/log/ldproxy.log
      archive: true
      archivedLogFilenamePattern: /var/log/ldproxy-%d.zip
      archivedFileCount: 30
      timeZone: Europe/Berlin
```

<a name="log-level"></a>

### Log-Level

Der Log-Level für die Applikation ist standardmäßig `INFO`. Für die Fehlersuche kann er zum Beispiel auf `DEBUG` gesetzt werden:

```yaml
logging:
  loggers:
    de.ii: DEBUG
```

Weitere mögliche Werte sind `OFF`, `ERROR` und `WARN`.
