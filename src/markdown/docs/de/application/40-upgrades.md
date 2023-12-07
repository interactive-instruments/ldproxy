# Aktualisierung

## Software

Wie genau die Software aktualisiert wird, hängt von der Container-Runtime ab. Der erste Schritt ist das Idntifizieren und Setzen des gewünschten [Image-Tags](https://hub.docker.com/r/iide/ldproxy/tags), gefolgt vom Auslösen des tatsächlichen Updates.

Die Releases verwenden Semantic Versioning, was bedeutet das für Minor- und Patch-Versionen keine weiteren Aktionen nötig sind und das Update nahtlos vonstatten gehen sollte. Für Major-Versionen sollte sichergestellt werden, dass die Konfigurationsdateien auf dem neuesten Stand sind (siehe unten) und die [Release Notes](https://github.com/interactive-instruments/ldproxy/releases) sollten auf Breaking Changes geprüft werden.

## Konfiguration

Die Syntax und Struktur der Konfigurationsdateien wird sich im Laufe der Zeit ändern. In Minor-Versionen werden Optionen als deprecated eingestuft, in Major-Versionen werden alle deprecated Optionen entfernt und möglicherweise Breaking Changes vorgenommen.

Um Ihre Konfigurationen auf dem neuesten Stand zu halten, können Sie das CLI-Tool [xtracfg](../cli/xtracfg) verwenden. Es erlaubt Ihnen, Ihre Konfigurationen auf veraltete Optionen und Fehler zu überprüfen und kann Ihre Konfigurationen auch automatisch auf die neueste Version aktualisieren.
