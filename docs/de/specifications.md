# Unterstützte Spezifikationen und Technologien

## Web-API-Spezifikationen

Unterstützt werden derzeit die folgenden OGC-Standards bzw. aktuellen Entwürfen von zukünftigen OGC-Standards:

* [OGC API - Features - Part 1: Core, Version 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0)
* [OGC API - Features - Part 2: Coordinate Reference Systems by Reference 1.0](http://www.opengis.net/doc/IS/ogcapi-features-2/1.0)
* [OGC API - Features - Part 3: Filtering, Entwurf](https://docs.ogc.org/DRAFTS/19-079r1.html)
* [OGC API - Features - Part 4: Create, Replace, Update and Delete, Entwurf](https://docs.ogc.org/DRAFTS/20-002.html)
* [OGC API - Tiles - Part 1: Core, Entwurf](https://docs.ogc.org/DRAFTS/20-057.html)
* [OGC API - Styles, Entwurf](https://docs.ogc.org/DRAFTS/20-009.html)
* [Common Query Language (CQL2), Entwurf](https://docs.ogc.org/DRAFTS/21-0065.html)

Die in ldproxy implementierte Funktionalität weicht bei Entwürfen aufgrund der dynamischen Entwicklung i.d.R. von der in den aktuellen Entwürfen beschriebenen Spezifikation ab.

Zusätzlich werden verschiedene Erweiterungen, für die noch keine offiziellen Entwürfe vorliegen, unterstützt.

ldproxy ist als [OGC-Referenzimplementierung für "OGC API - Features - Part 1: Core" und "OGC API - Features - Part 2: Coordinate Reference Systems by Reference" zertifiziert](http://www.ogc.org/resource/products/details/?pid=1705).

<img src='https://cite.opengeospatial.org/teamengine/site/certification-logo.gif' alt='Certified OGC Compliant Logo' height='74' style='padding:0;margin:0;border:0;'/>

## Formate

ldproxy unterstützt die folgenden Formate, die von verschiedenen Tools und Bibliotheken unterstützt werden:

* Allgemeine Ressourcen
  * JSON
  * HTML
  * XML (nur für Ressourcen, die in OGC API Features spezifiziert sind)
* API-Definitionen
  * [OpenAPI 3.0](http://spec.openapis.org/oas/v3.0.3) als JSON, YAML und HTML
* Features
  * [GeoJSON](http://tools.ietf.org/rfc/rfc7946.txt)
  * [JSON-FG](https://github.com/opengeospatial/ogc-feat-geo-json)
  * HTML
  * [GML Simple Features Profile](http://portal.opengeospatial.org/files/?artifact_id=42729) (nur für WFS-Feature-Provider)
* Tiles
  * [Mapbox Vector Tiles 2.1](https://github.com/mapbox/vector-tile-spec/tree/master/2.1)
  * PNG (nur für MBTiles-Tile-Provider oder Map Tiles)
  * JPEG (nur für MBTiles-Tile-Provider oder Map Tiles)
  * WebP (nur für MBTiles-Tile-Provider oder Map Tiles)
  * TIFF (nur für MBTiles-Tile-Provider)
* Tileset-Beschreibungen
  * [TileJSON](https://github.com/mapbox/tilejson-spec)
* Styles
  * [Mapbox Style] (https://www.mapbox.com/mapbox-gl-js/style-spec/)

HTML kann mit [schema.org](https://schema.org/)-Markup ([schema:Place](https://schema.org/Place), [schema:Dataset](https://schema.org/Dataset) und [schema:DataCatalog](https://schema.org/DataCatalog)) angereichert werden.

## Datenquellen

### Features

Alle Feature-Daten in ldproxy werden über Feature-Provider bereitgestellt, wobei jede API maximal einen Provider hat.

Es werden drei Arten von Feature-Providern unterstützt:

* PostgreSQL-Datenbanken ab Version 9.6 mit PostGIS ab Version 2.4
  * Alle Tabellen eines Providers müssen in einer Datenbank und in einem Schema liegen. Jede Tabelle, außer Zwischentabellen, muss eine eindeutige Identifikator-Spalte besitzen (Primary Key bzw. mit Unique Index). Es wird empfohlen, Integer für die Identifikatoren zu verwenden.
* GeoPackage
* OGC Web Feature Services (WFS)
  * Damit ein WFS angebunden werden kann, sollte er beim Datenzugriff über die GetFeature-Operation Paging über die Query-Parameter `COUNT` und `STARTINDEX` unterstützen.

### Tiles

Alle Tiles in ldproxy werden über Tile-Provider bereitgestellt.

Es werden drei Arten von Tile-Providern unterstützt:

* Features, die von derselben API bereitgestellt werden
* MBTiles
* TileServer GL (nur für Map Tiles)
