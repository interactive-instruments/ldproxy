# Modul "Filter / CQL" (FILTER)

Das Modul "Filter / CQL" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es aktiviert die Angabe der Query-Parameter `filter` und `filter-lang` für die Ressourcen "Features" und "Vector Tile". Unterstützt werden die Filtersprachen `cql-text` und `cql-json`.

Das Modul basiert auf den Vorgaben der Konformatitätsklassen "Filter", "Features Filter", "Simple CQL", "CQL Text" und "CQL JSON" aus dem [Entwurf von OGC API - Features - Part 3: Common Query Language](http://docs.opengeospatial.org/DRAFTS/19-079.html#filter-queryables). Die Implementierung kann sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.

Die Veröffentlichung der Queryables wird über des [Modul "Collections Queryables"](queryables.md) gesteuert. Ist "Filter / CQL" aktiviert, dann muss "Collection Queryables" aktiviert sein, damit Clients die abfragbaren Objekteigenschaften bestimmen können.

In der Konfiguration können keine Optionen gewählt werden.
