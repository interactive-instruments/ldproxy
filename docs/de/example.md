# Beispiel-Konfiguration

Die Beispiele in dieser Dokumentation verwenden eine hypothetische API für einen Datensatz für Kindergärten, der nach dem INSPIRE-Datenmodell für staatliche Dienste in einer flachen Datenstruktur abgegeben werden soll, die von gängigen Clients verarbeitet werden kann.

Das Datenbankschema des Quelldatensatzes besteht aus zwei Tabellen:

```sql
CREATE TABLE public.kita (
  oid integer NOT NULL,
  kitaid character varying(255) NOT NULL,
  name character varying(255) NOT NULL,
  strasse character varying(255),
  hausnummer character varying(255),
  plz character varying(255),
  ort character varying(255) NOT NULL,
  telefon character varying(255),
  geometry public.geometry(Point,25832) NOT NULL,
  PRIMARY KEY (oid)
);

CREATE TABLE public.plaetze (
  oid integer NOT NULL,
  kita_fk integer NOT NULL,
  art character varying(255) NOT NULL,
  anzahl integer NOT NULL,
  PRIMARY KEY (oid)
);

ALTER TABLE public.plaetze
  ADD CONSTRAINT fk_plaetze
  FOREIGN KEY (kita_fk) REFERENCES public.kita;
```

Das Zielschema basiert auf der Objektart [`GovernmentalService`](https://inspire.ec.europa.eu/featureconcept/GovernmentalService) aus dem INSPIRE-Anwendungsschema für staatliche Dienste, erweitert mit dem Attribut `occupancy` aus dem erweiterten Anwendungsschema. Damit ergibt sich folgendes Profil des Anwendungsschemas:

* Objektart `GovernmentalService`
  * `inspireId : URI`
  * `thematicId : CharacterString`
  * `geometry : GM_Point`
  * `serviceType : ServiceTypeValue = ServiceTypeValue::childCareService {frozen}`
  * `pointOfContact : Contact [0..1]`
  * `name : CharacterString`
  * `occupancy : OccupancyType [0..*]`
* Datentyp `Contact`
  * `address : AddressRepresentation [0..1]`
  * `telephoneVoice : CharacterString [0..1]`
* Datentyp `AddressRepresentation`
  * `thoroughfare : CharacterString [0..1]`
  * `locatorDesignator : CharacterString [0..1]`
  * `postCode : CharacterString [0..1]`
  * `adminUnit : CharacterString`
* Datentyp `OccupancyType`
  * `typeOfOccupant : CharacterString`
  * `numberOfOccupants : Integer`
