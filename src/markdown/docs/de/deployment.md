# Bereitstellung von ldproxy

Der empfohlene Weg zur Bereitstellung von ldproxy ist die Verwendung von Docker, einer Open-Source-Containerplattform. Docker-Images für ldproxy sind auf [Docker Hub](https://hub.docker.com/r/iide/ldproxy/) verfügbar.

## Vorraussetzungen

Um ldproxy einzusetzen, benötigen Sie eine Installation von Docker. Docker ist für Linux, Windows und Mac verfügbar. Detaillierte Installationsanleitungen für jede Plattform finden Sie [hier](https://docs.docker.com/).

## Installieren und Starten von ldproxy

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

## Aktualisierung von ldproxy

Um ldproxy zu aktualisieren, entfernen Sie einfach den Container und erstellen Sie einen neuen mit dem Befehl run wie oben beschrieben. Zum Beispiel:

```bash
docker stop ldproxy
docker rm ldproxy
docker run --name ldproxy -d -p 7080:7080 -v ~/docker/ldproxy_data:/ldproxy/data iide/ldproxy:latest
```

Ihre Daten werden in einem Volume gespeichert, nicht im Container, so dass Ihre Konfigurationen, API-Ressourcen und Caches auch nach der Aktualisierung noch vorhanden sind.
