# Das Daten-Verzeichnis

Alle deployment-spezifischen Dateien mit Ausnahme der Datenquelle für Features liegen im Daten-Verzeichnis (typischerweise "data"). Es liegt immer ausserhalb des Docker-Containers.

Das Daten-Verzeichnis enthält typischerweise folgende Dateien und Verzeichnisse:

* `cfg.yml`: Die [Konfigurationsdatei für globale Einstellungen](configuration/global-configuration.md).
* `api-resources`: Eine Ablage von Ressourcen oder Sub-Ressourcen, auf die über die API zugegriffen werden kann und die entweder vom Administrator oder über die API geändert werden können. Beispiele sind Styles, Kartensymbole, JSON-LD-Kontexte, usw. Weitere Details dazu finden Sie in den jeweiligen [API-Modulen](configuration/services/building-blocks/README.md). Ist oder war ein Modul nie aktiviert, dann fehlen auch die entsprechenden Verzeichnisse.
* `cache`: Der Cache für Ressourcen die von ldproxy aus Performanzgründen gecacht werden. Derzeit sind die nur die Vector Tiles für das [Modul "Vector Tiles"](configuration/services/building-blocks/tiles.md).
* `logs`: Die Log-Dateien gemäß der Einstellungen in `cfg.yml`.
* `store`: Die [ldproxy-Konfigurationsdateien](configuration/README.md).
* `tmp`: Ein Verzeichnis für temporäre Daten. Die Inhalte können bei Bedarf gelöscht werden, wenn ldproxy gestoppt ist. Es enthält z.B. der Cache der OSGi-Bundles.
