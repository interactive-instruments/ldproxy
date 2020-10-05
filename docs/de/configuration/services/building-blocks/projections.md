# Modul "Projections" (PROJECTIONS)

Das Modul "Projections" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt die folgenden Query-Parameter:

* `properties` (Ressourcen "Features", "Feature" und "Vector Tile"): Ist der Parameter angegeben, werden nur die angegeben Objekteigenschaften ausgegeben. Die Angabe begrenzt nur die Eigenschaften, die in GeoJSON im `properties`-Objekt bzw. in Mapbox Vector Tiles im `tags`-Feld enthalten sind;
* `skipGeometry` (Ressourcen "Features" und "Feature"): Hat der Parameter den Wert `true`, dann werden Geometrien in der Ausgabe unterdrückt.<br>_seit Version 2.2_

In der Konfiguration können keine Optionen gewählt werden.
