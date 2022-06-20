# Einführung

ldproxy macht Geodaten auf einfache Weise über moderne Web APIs zugänglich.

Die Software zeichnet sich durch folgende Eigenschaften aus:

* **Einfach nutzbar**: Die APIs unterstützen für die meisten Inhalte mindestens JSON und HTML. JSON, da es derzeit das gängigste Format in Web APIs ist. HTML, damit die APIs auch direkt im Webbrowser genutzt werden können. Bei den HTML-Ausgaben wird Wert auf intuitive Verständlichkeit gelegt.
* **Durchsuchbar**: Da alle Inhalte über Links verknüpft sind, kann sich ein Nutzer in jedem Webbrowser durch eine API navigieren und so schnell einen Eindruck der angebotenen Daten und Funktionen bekommen. Suchmaschinen können die Inhalte indizieren.
* **Verlinkbar**: Da jede Ressource in den APIs einen feste URI besitzt, können z.B. auch einzelne Features verlinkt werden.
* **Standardkonform**: ldproxy ist eine umfangreiche Implementierung der neuen [OGC API Standards](https://ogcapi.ogc.org/), sodass zunehmend Clients die APIs direkt nutzen können. Das gilt auch für die unterstützten Formate, z.B. GeoJSON, Mapbox Vector Tiles, Mapbox Styles oder TileJSON. Die APIs selbst werden über [OpenAPI-Definitionen](https://www.openapis.org/) entwicklerfreundlich dokumentiert.
* **Zertifiziert**: ldproxy ist als OGC-Referenzimplementierung für [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0) und [OGC API - Features - Part 2: Coordinate Reference Systems by Reference 1.0](http://www.opengis.net/doc/IS/ogcapi-features-2/1.0) zertifiziert.
* **Open Source**: Der Quellcode ist unter der [Mozilla Public License 2.0](http://mozilla.org/MPL/2.0/) auf [GitHub](https://github.com/interactive-instruments/ldproxy) verfügbar.
* **Optimiert**: Es werden Reactive Streams verwendet, um Hardwareressourcen zu schonen und so schnell wie möglich mit der Datenübertragung beginnen zu können.
* **Variabel**: Die Unterstützung mehrerer APIs in einem einzigen Deployment wird unterstützt, zum Beispiel für mehrere Datensätze.
* **Erweiterbar**: ldproxy ist modular aufgebaut, in Java geschrieben (aktuell unterstützte Version: Java 11) und auf Erweiterbarkeit ausgelegt.

Um eine Vorstellung davon zu bekommen, wie die APIs aussehen, werfen Sie einen Blick auf die [Demo APIs](https://demo.ldproxy.net).

Weitere Informationen über die unterstützten Spezifikationen und Technologien finden Sie [hier](advanced/specifications.md).

