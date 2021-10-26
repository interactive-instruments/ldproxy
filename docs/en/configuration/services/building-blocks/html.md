# HTML (HTML)

The module *HTML* may be enabled for every API. It is enabled by default. Provides HTML encoding for every supported resource that does not have more specific rules (like [Features](features-html.md)).

## Configuration

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`noIndexEnabled` |boolean |`true` |Set `noIndex` for all sites to prevent search engines from indexing.
|`schemaOrgEnabled` |boolean |`true` |Enable [schema.org](https://schema.org) annotations for all sites, which are used e.g. by search engines. The annotations are embedded as JSON-LD.
|`collectionDescriptionsInOverview`  |boolean |`true` |Show collection descriptions in *Feature Collections* resource for HTML.
|`legalName` |string |"Legal notice" |Label for optional legal notice link on every site.
|`legalUrl` |string |"" |URL for optional legal notice link on every site.
|`privacyName` |string |"Privacy notice" |Label for optional privacy notice link on every site.
|`privacyUrl` |string |"" |URL for optional privacy notice link on every site.
|`leafletUrl` |string |"https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" |URL template for Leaflet background map tiles.
|`leafletAttribution` |string |"&copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors" |Source attribution for Leaflet background map.
|`openLayersUrl` |string |"https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png" |URL template for OpenLayers background map tiles.
|`openLayersAttribution` |string |"&copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors" |Source attribution for OpenLayers background map.
|`footerText` |string |"" |Additional text shown in footer of every site.
|`defaultStyle` |string |`NONE` |A default style in the style repository that is used in maps in the HTML representation of the features and tiles resources. If `NONE`, a simple wireframe style will be used with OpenStreetMap as a basemap. If the value is not `NONE`, the API landing page (or the collection page) will also contain a link to a web map with the style for the dataset (or the collection).

### Example

```yaml
- buildingBlock: HTML
  noIndexEnabled: true
  schemaOrgEnabled: true
  collectionDescriptionsInOverview: true
  legalName: Impressum
  legalUrl: 'https://example.org/impressum/'
  privacyName: Datenschutzerklärung
  privacyUrl: 'https://example.org/datenschutzerklarung/'
  leafletUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
  openLayersUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
  leafletAttribution: '&copy; <a href="https://www.bkg.bund.de" class="link0" target="_new">Bundesamt
    f&uuml;r Kartographie und Geod&auml;sie</a> 2017, <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf"
    class="link0" target="_new">Datenquellen</a>'
  openLayersAttribution: '&copy; <a href="https://www.bkg.bund.de" class="link0" target="_new">Bundesamt
    f&uuml;r Kartographie und Geod&auml;sie</a> 2017, <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf"
    class="link0" target="_new">Datenquellen</a>'
  footerText: 'Warnung: Bei den APIs auf diesem Server handelt es sich um Test-APIs während der Entwicklung.'
```

## Customization

The HTML encoding is implemented using [Mustache templates](https://mustache.github.io/). Custom templates are supported, they have to reside in the data directory under the relative path `templates/html/{templateName}.mustache`, where `{templateName}` equals the name of a default template (see [source code on GitHub](https://github.com/search?q=repo%3Ainteractive-instruments%2Fldproxy+extension%3Amustache&type=Code)).
