# Modul "Geometry Simplification" (GEOMETRY_SIMPLIFICATION)

Das Modul "Geometry Simplification" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider aktiviert werden. Es ergänzt den Query-Parameter `maxAllowableOffset` für die Ressourcen "Features" und "Feature". Ist der Parameter angegeben, werden alle Geometrien mit dem [Douglas-Peucker-Algorithmus](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm) vereinfacht. Der Wert von `maxAllowableOffset` legt den maximalen Abstand zwischen der Originalgeometrie und der vereinfachten Geometrie fest ([Hausdorff-Abstand](https://en.wikipedia.org/wiki/Hausdorff_distance)). Der Wert ist in den Einheiten des Koordinatenreferenzsystems der Ausgabe (`CRS84` bzw. der Wert des Parameters Query-Parameters `crs`) angegeben.

In der Konfiguration können keine Optionen gewählt werden.
