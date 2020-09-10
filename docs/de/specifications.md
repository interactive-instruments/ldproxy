# Unterstützte Spezifikationen und Technologien

## Web-API-Spezificationen

Unterstützt werden derzeit die folgenden Standards bzw. aktuelle Entwürfe von zukünftigen Standards:

* [OGC API - Features - Part 1: Core, Version 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0)
* [OGC API - Features - Part 2: Coordinate Reference Systems by Reference, Entwurf](http://docs.opengeospatial.org/DRAFTS/18-058.html)
* [OGC API - Features - Part 3: Common Query Language, Entwurf](http://docs.opengeospatial.org/DRAFTS/19-079.html)
* [OGC API - Features - Part 4: Simple Transactions, Entwurf](http://docs.opengeospatial.org/DRAFTS/20-002.html)
* [OGC API - Tiles - Part 1: Core, Entwurf](https://htmlpreview.github.io/?https://github.com/opengeospatial/OGC-API-Tiles/blob/master/core/standard/OAPI_Tiles.html)
* [OGC API - Styles, Entwurf](http://docs.opengeospatial.org/DRAFTS/20-009.html)

Die in ldproxy implementierte Funktionalität weicht bei Entwürfen aufgrund der dynamischen Entwicklung teilweise von der in den aktuellen Entwürfen beschriebenen Spezifikation ab.

Zusätzlich werden verschiedene Erweiterungen, für die noch keine offiziellen Entwürfe vorliegen, unterstützt.

ldproxy ist als [OGC-Referenzimplementierung für "OGC API - Features - Part 1: Core" zertifiziert](http://www.ogc.org/resource/products/details/?pid=1598).

<div style='text-align:center; border:1px solid #777; background-color: #FFF; padding:12px; width:190px;'><span style='font-weight:bold; color:#000;font-size:106.8%;'>interactive instruments GmbH</span><br/><img src='https://portal.ogc.org/public_ogc/compliance/Certified_OGC_Compliant_Logo_Web.gif' alt='Certified OGC Compliant Logo' height='74' style='padding:0;margin:0;border:0;'/><br/><br/><span style='font-weight:bold; font-size:89%;'>ldproxy 2.0</span><br/><a href='http://www.ogc.org/resource/products/details/?pid=1598'><img src='https://portal.ogc.org/public_ogc/compliance/badge.php?s=ogcapi-features-1 1.0' height='45px' style='padding:0;margin:0;border:0;' /></a><br/><span style='color:#BBB;font-size:62.3%;'><br/>Valid Until: 2020-09-28</span></div>

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
  * HTML
  * [GML Simple Features Profile] (http://portal.opengeospatial.org/files/?artifact_id=42729) (nur für WFS-Feature-Provider)
* Vektor-Tiles
  * [Mapbox Vector Tiles 2.1](https://github.com/mapbox/vector-tile-spec/tree/master/2.1)
* Tileset-Beschreibungen
  * [TileJSON](https://github.com/mapbox/tilejson-spec)
* Styles
  * [Mapbox Style] (https://www.mapbox.com/mapbox-gl-js/style-spec/)

HTML kann mit [schema.org](https://schema.org/)-Markup ([schema:Place](https://schema.org/Place), [schema:Dataset](https://schema.org/Dataset) und [schema:DataCatalog](https://schema.org/DataCatalog)) angereichert werden.

## Datenquellen

Alle Feature-Daten in ldproxy werden über Feature-Provider bereitgestellt, wobei jede API genau einen Provider hat.

Es werden zwei Arten von Providern unterstützt:

* PostgreSQL-Datenbanken ab Version 9.6 mit PostGIS ab Version 2.4
  * Alle Tabellen eines Providers müssen in einer Datenbank und in einem Schema liegen. Jede Tabelle, außer Zwischentabellen, muss eine eindeutige Identifikator-Spalte besitzen (Primary Key bzw. mit Unique Index). Es wird empfohlen, Integer für die Identifikatoren zu verwenden.
* OGC Web Feature Services (WFS)
  * Damit ein WFS angebunden werden kann, sollte er beim Datenzugriff über die GetFeature-Operation Paging über die Query-Parameter `COUNT` und `STARTINDEX` unterstützen.
