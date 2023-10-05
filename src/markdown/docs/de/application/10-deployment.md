# Bereitstellung

ldproxy wird als OCI-Image auf [Docker Hub](https://hub.docker.com/r/iide/ldproxy/) veröffentlicht. Um ldproxy zu deployen, kann theoretisch jede OCI-kompatible Laufzeitumgebung verwendet werden. Empfohlen und regelmäßig getestet werden Docker und Kubernetes mit containerd.

## Data Volume

Das Verzeichnis `/ldproxy/data` im Container ist der Ort, von dem ldproxy standardmäßig Dateien liest und in den er sie schreibt.

Es ist als Volume im Image deklariert, was bedeutet, dass es außerhalb des Container-Dateisystems existiert. Wenn kein Mount für `/ldproxy/data` angegeben ist, wird die Container-Laufzeit normalerweise ein anonymes Volume erstellen.

### Inhalt (alt)

::: info
Dies beschreibt das Layout des Datenverzeichnisses bis `v3.5`. Es wird ab `v4.0` nicht mehr unterstützt. Siehe unten für das neue Layout.
:::

Das Datenverzeichnis enthält normalerweise die folgenden Dateien und Verzeichnisse:

* `cfg.yml`: Die [Konfigurationsdatei für globale Einstellungen](30-configuration.md).
* `api-resources`: Ein Repository von Ressourcen oder Sub-Ressourcen, auf die über die API zugegriffen werden kann und die entweder vom Administrator oder über die API geändert werden können. Beispiele sind Stile, Kartensymbole, JSON-LD Kontexte, etc. Weitere Einzelheiten finden Sie in der Datei [API-Module](../services/building-blocks/README.md). Wenn ein Modul nicht aktiviert ist oder war, dann fehlen auch die entsprechenden Verzeichnisse.
* `cache`: Der Cache für Ressourcen, die von ldproxy aus Leistungsgründen zwischengespeichert werden. Derzeit sind dies nur die Vector Tiles für das [Modul "Tiles"](../services/building-blocks/tiles.md).
* `Logs`: Die Log-Dateien gemäß den Einstellungen in `cfg.yml`.
* `store`: Die [ldproxy-Konfigurationsdateien](40-store.md).
* `templates`: Mustache-Vorlagen für die HTML-Seiten, die die Standardvorlagen von ldproxy überschreiben.
* `tmp`: Ein Verzeichnis für temporäre Daten. Der Inhalt kann bei Bedarf gelöscht werden, wenn ldproxy gestoppt wird. Es enthält z.B. den Cache der OSGi-Bundles.

### Inhalt (neu)

Ein minimales Datenverzeichnis enthält nur ein `tmp`-Verzeichnis für temporäre Dateien, die bei Bedarf gelöscht werden können, wenn ldproxy gestoppt wird.

Standardmäßig dient das Datenverzeichnis auch als Haupt-[Store Source](41-store-new.md). In diesem Fall kann es eine `cfg.yml` mit [globalen Konfigurationseinstellungen](30-configuration.md) sowie die Verzeichnisse `entities` und `resources` enthalten.

## Docker

Um ldproxy einzusetzen, benötigen Sie eine Installation von Docker. Docker ist für Linux, Windows und Mac verfügbar. Detaillierte Installationsanleitungen für jede Plattform finden Sie [hier](https://docs.docker.com/).

### Installieren und Starten von ldproxy

Um ldproxy zu installieren, führen Sie einfach den folgenden Befehl auf einem Rechner mit installiertem Docker aus:

```bash
docker run -d -p 7080:7080 -v ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

Dadurch wird das neueste stabile ldproxy-Image heruntergeladen, als neuer Container bereitgestellt, die Webanwendung auf Port 7080 verfügbar gemacht und Ihre Anwendungsdaten in einem vom Docker bereitgestellten Verzeichnis außerhalb des Containers gespeichert.

Anstatt ein vom Docker bereitgestelltes Verzeichnis zu verwenden, in dem ldproxy seine Daten speichert (d.h. "ldproxy_data"), können Sie z.B. einen absoluten Pfad angeben:

```bash
docker run --name ldproxy -d -p 7080:7080 -v ~/docker/ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

Wir haben zusätzlich `--name ldproxy` hinzugefügt, um den Namen des Docker-Containers von einem zufälligen Namen in "ldproxy" zu ändern.

Sie können auch den Host-Port oder andere Parameter nach Ihren Bedürfnissen ändern, indem Sie die auf dieser Seite gezeigten Befehle anpassen.

Um zu überprüfen, ob der Docker-Prozess läuft, verwenden Sie

```bash
docker ps
```

was etwas Ähnliches zurückgeben sollte wie

```bash
CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                    NAMES
62db022d9bee        iide/ldproxy:latest   "/ldproxy/bin/ldproxy"   16 minutes ago      Up 16 minutes       0.0.0.0:7080->7080/tcp   ldproxy
```

Prüfen Sie, ob ldproxy läuft, indem Sie die URI http://localhost:7080/ in einem Webbrowser öffnen. Da der ldproxy-Manager erst in einer zukünftigen Version wieder verfügbar ist, sollten Sie einen `404`-Fehler erhalten.

Falls ldproxy nicht antwortet, konsultieren Sie das Protokoll mit `docker logs ldproxy`.

### Aktualisierung von ldproxy

Um ldproxy zu aktualisieren, entfernen Sie einfach den Container und erstellen Sie einen neuen mit dem Befehl run wie oben beschrieben. Zum Beispiel:

```bash
docker stop ldproxy
docker rm ldproxy
docker run --name ldproxy -d -p 7080:7080 -v ~/docker/ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

Ihre Daten werden in einem Volume gespeichert, nicht im Container, so dass Ihre Konfigurationen, API-Ressourcen und Caches auch nach der Aktualisierung noch vorhanden sind.

## Kubernetes

Coming soon.