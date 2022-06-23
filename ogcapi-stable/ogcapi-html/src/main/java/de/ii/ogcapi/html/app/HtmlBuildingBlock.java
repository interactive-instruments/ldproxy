/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.html.domain.ImmutableHtmlConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * @title HTML
 * @langEn The module *HTML* may be enabled for every API. It is enabled by default. Provides HTML
 *     encoding for every supported resource that does not have more specific rules (like
 *     [Features](features_html.md)).
 *     <p>## Customization
 *     <p>The HTML encoding is implemented using [Mustache templates](https://mustache.github.io/).
 *     Custom templates are supported, they have to reside in the data directory under the relative
 *     path `templates/html/{templateName}.mustache`, where `{templateName}` equals the name of a
 *     default template (see [source code on
 *     GitHub](https://github.com/search?q=repo%3Ainteractive-instruments%2Fldproxy+extension%3Amustache&type=Code)).
 * @langDe Das Modul *HTML* kann für jede über ldproxy bereitgestellte API aktiviert werden und ist
 *     standardmäßig aktiviert. Soweit für eine Ressource keine speziellen Regelungen für die
 *     Ausgabeformate bestehen (wie zum Beispiel für [Features](features_html.md)) und die Ressource
 *     HTML unterstützt, können Clients das Ausgabeformat HTML anfordern.
 *     <p>## Anpassung
 *     <p>ldproxy verwendet für die HTML-Ausgabe [Mustache-Templates](https://mustache.github.io/).
 *     Anstelle der Standardtemplates von ldproxy können auch benutzerspezifische Templates
 *     verwendet werden. Die eigenen Templates müssen als Dateien im ldproxy-Datenverzeichnis unter
 *     dem relativen Pfad `templates/html/{templateName}.mustache` liegen, wobei `{templateName}`
 *     der Name des ldproxy-Templates ist. Die Standardtemplates liegen jeweils in den
 *     Resource-Verzeichnissen der Module, die sie verwenden ([Link zur Suche in
 *     GitHub](https://github.com/search?q=repo%3Ainteractive-instruments%2Fldproxy+extension%3Amustache&type=Code)).
 * @example {@link de.ii.ogcapi.html.domain.HtmlConfiguration}
 * @properyTable {@link de.ii.ogcapi.html.domain.ImmutableHtmlConfiguration}
 */
@Singleton
@AutoBind
public class HtmlBuildingBlock implements ApiBuildingBlock {

  @Inject
  public HtmlBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableHtmlConfiguration.Builder()
        .enabled(true)
        .noIndexEnabled(true)
        .schemaOrgEnabled(true)
        .collectionDescriptionsInOverview(false)
        .sendEtags(false)
        .legalName("Legal notice")
        .legalUrl("")
        .privacyName("Privacy notice")
        .privacyUrl("")
        .basemapUrl("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
        .basemapAttribution(
            "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors")
        .defaultStyle("NONE")
        .footerText("")
        .build();
  }
}
