
# Der WFS-Feature-Provider

Hier werden die Besonderheiten des WFS-Feature-Providers beschrieben.

<a name="connection-info"></a>

## Das Connection-Info-Objekt für OGC Web Feature Services

Das Connection-Info-Objekt für OGC Web Feature Services wird wie folgt beschrieben:

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`connectorType` |enum | |Stets `HTTP`.
|`uri` |string | |Die URI der GetCapabilities-Operation des WFS.
|`version` |enum |2.0.0 |Die zu verwendende WFS-Version.
|`gmVersion` |enum | |Die zu verwendende GML-Version.
|`method` |enum |`GET` |Die bevorzugt zu verwendende HTTP-Methode, `GET` oder `POST`.
|`user` |string | |Der Benutzername.
|`password` |string | |Das mit base64 verschüsselte Passwort des Benutzers.
|`namespaces`|object | |Eine Map von zu verwendenden Namespace-Prefixen und der zugehörigen Namespace-URI.

Ein Beispiel:

```yaml
connectorType: HTTP
version: 2.0.0
gmlVersion: 3.2.1
namespaces:
  ave: http://rexample.com/ns/app/1.0
  wfs: http://www.opengis.net/wfs/2.0
  fes: http://www.opengis.net/fes/2.0
  gml: http://www.opengis.net/gml/3.2
  xsd: http://www.w3.org/2001/XMLSchema
  ows: http://www.opengis.net/ows/1.1
  xlink: http://www.w3.org/1999/xlink
  xsi: http://www.w3.org/2001/XMLSchema-instance
uri: https://example.com/pfad/zum/wfs?
method: GET
```

<a name="path-syntax"></a>

## WFS-Feature-Provider-Pfadsyntax

Die Pfade in den WFS-Feature-Providern werden von ldproxy bei der Analyse des WFS gebildet und sollten nicht verändert werden.
