# Der Feature-Provider

Jeder Feature-Provider wird in einer Konfigurationsdatei in einem Objekt mit den folgenden Eigenschaften beschrieben. Werte ohne Defaultwert sind in diesem Fall Pflichtangaben.

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`id` |string | |Eindeutiger Identifikator des Feature-Providers.
|`createdAt` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei erzeugt wurde. Der Wert wird automatisch vom Manager bei der Erzeugung gesetzt und besitzt nur informativen Charakter.
|`lastModified` |integer | |Zeitpunkt in Millisekunden seit dem 1.1.1970, an dem die Datei zuletzt geändert wurde. Der Wert wird automatisch vom Manager bei jeder Änderung gesetzt und besitzt nur informativen Charakter.
|`entityStorageVersion` |integer | |Bezeichnet die Version des Feature-Provider-Konfigurationsdatei-Schemas. Diese Dokumentation bezieht sich auf die Version 2 und alle Dateien nach diesem Schema müssen den Wert 2 haben. Konfigurationen zu Version 1 werden automatisch auf Version 2 aktualisiert.
|`providerType` |enum | |Stets `FEATURE`.
|`featureProviderType` |enum | |`SQL` für ein SQL-DBMS als Datenquelle, `WFS` für einen OGC Web Feature Service als Datenquelle.
|`connectionInfo` |object | |Ein Objekt mit Angaben zur Datenquelle. Der Inhalt hängt von `featureProviderType` ab ([SQL](sql.md#connection-info) und [WFS](wfs.md#connection-info)).
|`nativeCrs` |object | |Das Koordinatenreferenzsystem, in dem Geometrien in dem Datensatz geführt werden. Der EPSG-Code des Koordinatenreferenzsystems wird als Integer in `code` angegeben. Mit `forceAxisOrder` kann die Koordinatenreihenfolge geändert werden: `NONE` verwendet die Reihenfolge des Koordinatenreferenzsystems, `LON_LAT` verwendet stets Länge/Ostwert als ersten und Breite/Nordwert als zweiten Wert, `LAT_LON` entsprechend umgekehrt. Beispiel: Das Default-Koordinatenreferenzsystem `CRS84` entspricht `code: 4326` und `forceAxisOrder: LON_LAT`.
|`types` |object |`{}` |Ein Objekt mit der Spezifikation zu jeder Objektart. Siehe unten.
|`auto` |boolean |`false` |Steuert, ob die Informationen zu `types` beim Start automatisch aus der Datenquelle bestimmt werden sollen (Auto-Modus). In diesem Fall sollte `types` nicht angegeben sein.
|`autoPersist` |boolean |`false` |Steuert, ob die im Auto-Modus (`auto: true`) bestimmten Schemainformationen in die Konfigurationsdatei übernommen werden sollen. In diesem Fall werden `auto` und `autoPersist` beim nächsten Start automatisch aus der Datei entfernt. Liegt die Konfigurationsdatei in einem anderen Verzeichnis als unter `store/entities/providers` (siehe `additionalLocations`), so wird eine neue Datei in `store/entities/providers` erstellt. `autoPersist: true` setzt voraus, dass `store` sich nicht im `READ_ONLY`-Modus befindet.

<a name="feature-provider-types"></a>

## Das Types-Objekt

Das Types-Objekt hat für jede Objektart einen Eintrag mit dem Identifikator der Objektart als Schlüssel. Jede Objektart ist durch ein Schema-Objekt mit `type: OBJECT` beschrieben:

|Eigenschaft |Datentyp |Default |Beschreibung
| --- | --- | --- | ---
|`type` |enum |`STRING` / `OBJECT` |Der Datentyp des Schemaobjekts. Der Standardwert ist `STRING`, sofern nicht auch die Eigenschaft `properties` angegeben ist, dann ist es `OBJECT`. Erlaubt sind:<ul><li>`FLOAT`, `INTEGER`, `STRING`, `BOOLEAN`, `DATETIME` für einen einfachen Wert des entsprechenden Datentyps. `DATETIME` kann dabei sowohl ein Datum als auch ein Zeitstempel sein.</li><li>`GEOMETRY` für eine Geometrie.</li><li>`OBJECT` für ein Objekt.</li><li>`OBJECT_ARRAY` für eine Liste von Objekten.</li><li>`VALUE_ARRAY` für eine Liste von einfachen Werten.</li></ul>
|`sourcePath` |string | |Der relative Pfad zu diesem Schemaobjekt. Die Pfadsyntax ist je nach Provider-Typ unterschiedlich ([SQL](sql.md#path-syntax) und [WFS](wfs.md#path-syntax)).
|`constantValue` |string / number / boolean |`null` |Alternativ zu `sourcePath` kann diese Eigenschaft verwendet werden, um im Feature-Provider eine Eigenschaft mit einem festen Wert zu belegen.
|`label` |string | |Eine Bezeichnung des Schemaobjekts, z.B. für die Angabe in der HTML-Ausgabe.
|`description` |string | |Eine Beschreibung des Schemaobjekts, z.B. für die HTML-Ausgabe oder das JSON-Schema.
|`properties` |object | |Nur bei `OBJECT` und `OBJECT_ARRAY`. Ein Objekt mit einer Eigenschaft pro Objekteigenschaft. Der Schüssel ist der Name der Objekteigenschaft, der Wert das Schema-Objekt zu der Objekteigenschaft.
|`role` |enum |`null` |`ID` ist bei der Eigenschaft eines Objekts anzugeben, die für die `featureId` in der API zu verwenden ist. Diese Eigenschaft muss die erste Eigenschaft im `properties`-Objekt sein.
|`objectType` |string | |Optional kann ein Name für den Typ spezifiziert werden. Der Name hat i.d.R. nur informativen Charakter und wird z.B. bei der Erzeugung von JSON-Schemas verwendet. Bei Eigenschaften, die als Web-Links nach RFC 8288 abgebildet werden sollen, ist immer "Link" anzugeben.
|`geometryType` |enum | |Mit der Angabe kann der Geometrietype spezifiziert werden. Die Angabe ist nur bei Geometrieeigenschaften (`type: GEOMETRY`) relevant. Erlaubt sind die Simple-Feature-Geometrietypen, d.h. `POINT`, `MULTI_POINT`, `LINE_STRING`, `MULTI_LINE_STRING`, `POLYGON`, `MULTI_POLYGON`, `GEOMETRY_COLLECTION` und `ANY`.

## Die ConnectionInfo-Objekte

Informationen zu den Datenquellen finden Sie auf separaten Seiten: [SQL](sql.md#connection-info) und [WFS](wfs.md#connection-info).

## Eine Feature-Provider-Beispielkonfiguration (SQL)

```yaml
id: kita
createdAt: 1598603585258
lastModified: 1598603585258
entityStorageVersion: 2
providerType: FEATURE
featureProviderType: SQL
connectionInfo:
  connectorType: SLICK
  host: db
  database: kita
  user: postgres
  password: cGxlYXNlY2hhbmdlbWUK
  dialect: PGIS
  computeNumberMatched: true
  pathSyntax:
    defaultPrimaryKey: oid
    defaultSortKey: oid
  schemas:
  - public
nativeCrs:
  code: 25832
  forceAxisOrder: NONE
types:
  governmentalservice:
    sourcePath: /kita
    type: OBJECT
    label: Staatlicher Dienst
    description: 'Staatliche Verwaltungs- und Sozialdienste wie öffentliche Verwaltung, Katastrophenschutz, Schulen und Krankenhäuser, die von öffentlichen oder privaten Einrichtungen erbracht werden, soweit sie in den Anwendungsbereich der Richtlinie 2007/2/EG fallen. Dieser Datensatz enthält Informationen zu Diensten der Kinderbetreuung (KiTas).'
    properties:
      thematicId:
        sourcePath: kitaid
        type: STRING
        role: ID
        label: Fachidentifikator
        description: Beschreibender eindeutiger Identifikator für Geo-Objekte in einem bestimmten Datenthema.
      oid:
        sourcePath: oid
        type: INTEGER
        label: Interner Identifikator
      inspireId:
        sourcePath: kitaid
        type: STRING
        label: INSPIRE-Objektidentifikator
        description: Externer Objektidentifikator des Geo-Objekts.
      name:
        sourcePath: name
        type: STRING
        label: Name
        description: Der Name des staatlichen Verwaltungsdienstes.
      serviceType:
        type: OBJECT
        objectType: Link
        label: Art des Dienstes
        description: Art des staatlichen Verwaltungsdienstes.
        properties:
          title:
            constantValue: Kinderbetreuung
            type: STRING
            label: Beschreibung
          href:
            constantValue: 'http://inspire.ec.europa.eu/codelist/ServiceTypeValue/childCareService'
            type: STRING
            label: Verweis
      pointOfContact:
        type: OBJECT
        objectType: Contact
        label: Kontaktdaten
        description: Enthält notwendige Informationen über den Zugang zu einem Dienst und/oder erste Informationen über einen Dienst an sich.
        properties:
          address:
            type: OBJECT
            objectType: AddressRepresentation
            label: Adresse
            properties:
              thoroughfare:
                sourcePath: strasse
                type: STRING
                label: Straße
                description: Die Bezeichnung(en) eines Durchgangs oder eines Verkehrswegs von einem Standort zu einem anderen – etwa einer Straße oder einer Wasserstraße.
              locatorDesignator:
                sourcePath: hausnummer
                type: STRING
                label: Hausnummer
                description: Eine Anzahl oder Abfolge von Zeichen, die es dem Nutzer oder einer Anwendung erlaubt, den Locator innerhalb des jeweiligen Geltungsbereichs zu interpretieren, zu analysieren und zu formatieren. Ein Locator kann mehrere Locator-Bezeichner enthalten.
              postCode:
                sourcePath: plz
                type: STRING
                label: Postleitzahl
                description: Ein zu postalischen Zwecken geschaffener und zur Untergliederung von Adressen und Zustellungspunkten verwendeter Code.
              adminUnit:
                sourcePath: ort
                type: STRING
                label: Ort
                description: Die Bezeichnung(en) einer Verwaltungseinheit, in der ein Mitgliedstaat Hoheitsbefugnisse für die lokale, regionale und nationale Verwaltung hat und/oder ausübt.
          telephoneVoice:
            sourcePath: telefon
            type: STRING
            label: Telefon
            description: Telefonnummer der Organisation oder Person.
      occupancy:
        type: OBJECT_ARRAY
        objectType: Occupancy
        sourcePath: [oid=kita_fk]plaetze
        label: Belegungskapazität
        description: Belegungskapazität pro Altersklasse
        properties:
          typeOfOccupant:
            sourcePath: art
            type: STRING
            label: Altersklasse
          numberOfOccupants:
            sourcePath: anzahl
            type: INTEGER
            label: Kapazität
            description: Belegungskapazität für die Altersklasse
      geometry:
        sourcePath: geometry
        type: GEOMETRY
        geometryType: POINT
        label: Geometrie
```
