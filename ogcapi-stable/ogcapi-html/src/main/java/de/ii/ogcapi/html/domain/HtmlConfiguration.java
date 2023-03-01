/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.xtraplatform.base.domain.LogContext;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @buildingBlock HTML
 * @langEn ### Custom Templates
 *     <p>The HTML encoding is implemented using [Mustache templates](https://mustache.github.io/).
 *     Custom templates are supported, they have to reside in the data directory under the relative
 *     path `templates/html/{templateName}.mustache`, where `{templateName}` equals the name of a
 *     default template (see [source code on
 *     GitHub](https://github.com/search?q=repo%3Ainteractive-instruments%2Fldproxy+extension%3Amustache&type=Code)).
 * @langDe ### Benutzerdefinierte Templates
 *     <p>Die HTML-Ausgabe ist mittels [Mustache-Templates](https://mustache.github.io/)
 *     implementiert. Anstelle der Standardtemplates können auch benutzerspezifische Templates
 *     verwendet werden. Die eigenen Templates müssen als Dateien im Datenverzeichnis unter dem
 *     relativen Pfad `templates/html/{templateName}.mustache` liegen, wobei `{templateName}` der
 *     Name des Default-Templates ist. Die Standardtemplates liegen jeweils in den
 *     Resource-Verzeichnissen der Module, die sie verwenden ([Link zur Suche in
 *     GitHub](https://github.com/search?q=repo%3Ainteractive-instruments%2Fldproxy+extension%3Amustache&type=Code)).
 * @examplesEn Example of the specifications in the configuration file (from the API for
 *     [Topographic data in Daraa, Syria](https://demo.ldproxy.net/daraa)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: HTML
 *   enabled: true
 *   noIndexEnabled: true
 *   schemaOrgEnabled: true
 *   defaultStyle: topographic
 * ```
 *     </code>
 *     <p>Example of the specifications in the configuration file (from the API for Vineyards in
 *     Rhineland-Palatinate](https://demo.ldproxy.net/vineyards)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: HTML
 *   enabled: true
 *   noIndexEnabled: false
 *   schemaOrgEnabled: true
 *   collectionDescriptionsInOverview: true
 *   legalName: Legal notice
 *   legalUrl: https://www.interactive-instruments.de/en/about/impressum/
 *   privacyName: Privacy notice
 *   privacyUrl: https://www.interactive-instruments.de/en/about/datenschutzerklarung/
 *   basemapUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
 *   basemapAttribution: '&copy; <a href="https://www.bkg.bund.de" target="_new">Bundesamt f&uuml;r Kartographie und Geod&auml;sie</a> (2020), <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf" target="_new">Datenquellen</a>'
 *   defaultStyle: default
 * ```
 *     </code>
 * @examplesDe Beispiel für die Angaben in der Konfigurationsdatei (aus der API für [Topographische
 *     Daten in Daraa, Syrien](https://demo.ldproxy.net/daraa)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: HTML
 *   enabled: true
 *   noIndexEnabled: true
 *   schemaOrgEnabled: true
 *   defaultStyle: topographic
 * ```
 *     </code>
 *     <p>Beispiel für die Angaben in der Konfigurationsdatei (aus der API für [Weinlagen in
 *     Rheinland-Pfalz](https://demo.ldproxy.net/vineyards)):
 *     <p><code>
 * ```yaml
 * - buildingBlock: HTML
 *   enabled: true
 *   noIndexEnabled: false
 *   schemaOrgEnabled: true
 *   collectionDescriptionsInOverview: true
 *   legalName: Legal notice
 *   legalUrl: https://www.interactive-instruments.de/en/about/impressum/
 *   privacyName: Privacy notice
 *   privacyUrl: https://www.interactive-instruments.de/en/about/datenschutzerklarung/
 *   basemapUrl: https://sg.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web_grau/default/WEBMERCATOR/{z}/{y}/{x}.png
 *   basemapAttribution: '&copy; <a href="https://www.bkg.bund.de" target="_new">Bundesamt f&uuml;r Kartographie und Geod&auml;sie</a> (2020), <a href="https://sg.geodatenzentrum.de/web_public/Datenquellen_TopPlus_Open.pdf" target="_new">Datenquellen</a>'
 *   defaultStyle: default
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableHtmlConfiguration.Builder.class)
public interface HtmlConfiguration extends ExtensionConfiguration {
  Logger LOGGER = LoggerFactory.getLogger(HtmlConfiguration.class);

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @default true
   */
  @Nullable
  @Override
  Boolean getEnabled();

  /**
   * @langEn Set `noIndex` for all sites to prevent search engines from indexing.
   * @langDe Steuert, ob in allen Seiten "noIndex" gesetzt wird und Suchmaschinen angezeigt wird,
   *     dass sie die Seiten nicht indizieren sollen.
   * @default true
   */
  @Nullable
  Boolean getNoIndexEnabled();

  /**
   * @langEn Enable [schema.org](https://schema.org) annotations for all sites, which are used e.g.
   *     by search engines. The annotations are embedded as JSON-LD.
   * @langDe Steuert, ob in die HTML-Ausgabe schema.org-Annotationen, z.B. für Suchmaschinen,
   *     eingebettet sein sollen, sofern. Die Annotationen werden im Format JSON-LD eingebettet.
   * @default true
   */
  @JsonAlias(value = "microdataEnabled")
  @Nullable
  Boolean getSchemaOrgEnabled();

  /**
   * @langEn Show collection descriptions in *Feature Collections* resource for HTML.
   * @langDe Steuert, ob in der HTML-Ausgabe der Feature-Collections-Ressource für jede Collection
   *     die Beschreibung ausgegeben werden soll.
   * @default false
   */
  @Nullable
  Boolean getCollectionDescriptionsInOverview();

  @Nullable
  Boolean getSendEtags();

  /**
   * @langEn Label for optional legal notice link on every site.
   * @langDe Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einem Impressum
   *     angezeigt werden. Diese Eigenschaft spezfiziert den anzuzeigenden Text.
   * @default Legal notice
   */
  @Nullable
  String getLegalName();

  /**
   * @langEn URL for optional legal notice link on every site.
   * @langDe Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einem Impressum
   *     angezeigt werden. Diese Eigenschaft spezfiziert die URL des Links.
   * @default null
   */
  @Nullable
  String getLegalUrl();

  /**
   * @langEn Label for optional privacy notice link on every site.
   * @langDe Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einer
   *     Datenschutzerklärung angezeigt werden. Diese Eigenschaft spezfiziert den anzuzeigenden
   *     Text.
   * @default Privacy notice
   */
  @Nullable
  String getPrivacyName();

  /**
   * @langEn URL for optional privacy notice link on every site.
   * @langDe Auf jeder HTML-Seite kann ein ggf. rechtlich erforderlicher Link zu einer
   *     Datenschutzerklärung angezeigt werden. Diese Eigenschaft spezfiziert die URL des Links.
   * @default null
   */
  @Nullable
  String getPrivacyUrl();

  /**
   * @langEn A default style in the style repository that is used in maps in the HTML representation
   *     of the features and tiles resources. If `NONE`, a simple wireframe style will be used with
   *     OpenStreetMap as a basemap. If the value is not `NONE`, the API landing page (or the
   *     collection page) will also contain a link to a web map with the style for the dataset (or
   *     the collection).
   * @langDe Ein Style im Style-Repository, der standardmäßig in Karten mit Feature- und
   *     Tile-Ressourcen verwendet werden soll. Bei `NONE` wird ein einfacher Style mit
   *     OpenStreetMap als Basiskarte verwendet. Wenn der Wert nicht `NONE` ist, enthält die
   *     "Landing Page" bzw. die "Feature Collection" auch einen Link zu einer Webkarte mit dem Stil
   *     für den Datensatz bzw. die Feature Collection. Der Style sollte alle Daten abdecken und
   *     muss im Format Mapbox Style verfügbar sein.
   * @default NONE
   */
  @Nullable
  String getDefaultStyle();

  /**
   * @langEn URL template for background map tiles.
   * @langDe Das URL-Template für die Kacheln einer Hintergrundkarte.
   * @default https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png
   */
  @Nullable
  String getBasemapUrl();

  /**
   * @langEn Source attribution for background map.
   * @langDe Die Quellenangabe für die Hintergrundkarte.
   * @default &copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors
   */
  @Nullable
  String getBasemapAttribution();

  /**
   * @langEn *Deprecated* See `mapBackgroundUrl`.
   * @langDe *Deprecated* Siehe `basemapUrl`.
   * @default https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png
   */
  @Deprecated(since = "3.1.0")
  @Nullable
  String getLeafletUrl();

  /**
   * @langEn *Deprecated* See `mapAttribution`.
   * @langDe *Deprecated* Siehe `basemapAttribution`.
   * @default &copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors
   */
  @Deprecated(since = "3.1.0")
  @Nullable
  String getLeafletAttribution();

  /**
   * @langEn *Deprecated* See `mapBackgroundUrl`.
   * @langDe *Deprecated* Siehe `basemapUrl`.
   * @default https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png
   */
  @Deprecated(since = "3.1.0")
  @Nullable
  String getOpenLayersUrl();

  /**
   * @langEn *Deprecated* See `mapAttribution`
   * @langDe *Deprecated* Siehe `basemapAttribution`.
   * @default &copy; <a href='http://osm.org/copyright'>OpenStreetMap</a> contributors
   */
  @Deprecated(since = "3.1.0")
  @Nullable
  String getOpenLayersAttribution();

  /**
   * @langEn Additional text shown in footer of every site.
   * @langDe Zusätzlicher Text, der auf jeder HTML-Seite im Footer angezeigt wird.
   * @default null
   */
  @Nullable
  String getFooterText();

  @Override
  default Builder getBuilder() {
    return new ImmutableHtmlConfiguration.Builder();
  }

  default String getStyle(
      Optional<String> requestedStyle,
      Optional<String> collectionId,
      String serviceUrl,
      MapClient.Type mapClientType) {
    String f =
        mapClientType == Type.MAP_LIBRE ? "mbs" : mapClientType == Type.CESIUM ? "3dtiles" : null;
    String styleUrl = null;
    if (Objects.nonNull(f)) {
      styleUrl =
          requestedStyle
              .map(
                  s ->
                      s.equals("DEFAULT")
                          ? Objects.requireNonNullElse(getDefaultStyle(), "NONE")
                          : s)
              .filter(s -> !s.equals("NONE"))
              .map(
                  s ->
                      collectionId.isEmpty()
                          ? String.format("%s/styles/%s?f=%s", serviceUrl, s, f)
                          : String.format(
                              "%s/collections/%s/styles/%s?f=%s",
                              serviceUrl, collectionId.get(), s, f))
              .orElse(null);
    }

    // Check that the style exists
    if (Objects.nonNull(styleUrl)) {
      // TODO we currently test for the availability of the style using a HTTP request to
      //      avoid a dependency to STYLES. Once OGC API Styles is stable, we should consider to
      //      separate the StyleRepository from the endpoints. The StyleRepository could be part
      //      of FOUNDATION or its own module
      try {
        URL url = new URL(styleUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("HEAD");
        if (http.getResponseCode() == 404 && collectionId.isPresent()) {
          // Try fallback to the dataset style, if we have a collection style
          return getStyle(requestedStyle, Optional.empty(), serviceUrl, mapClientType);
        } else if (http.getResponseCode() != 200) {
          LOGGER.error(
              "Could not access style '{}', falling back to style 'NONE'. Response code: '{}'. Message: {}",
              styleUrl,
              http.getResponseCode(),
              http.getResponseMessage());
          return null;
        }
        http.disconnect();
      } catch (Exception e) {
        LOGGER.error(
            "Could not access style '{}', falling back to style 'NONE'. Reason: {}",
            styleUrl,
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
        return null;
      }
    }

    return styleUrl;
  }
}
