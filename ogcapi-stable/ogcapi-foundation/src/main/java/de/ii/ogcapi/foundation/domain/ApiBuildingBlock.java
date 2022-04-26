/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a
 * copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocFileTemplate;
import de.ii.xtraplatform.docs.DocTemplate;

/**
 * # Overview
 * @langEn The OGC API functionality is split up into modules based on the OGC API standards.
 * The modules are classified according to the state of the implemented specification:
 *
 * - For approved standards or drafts in the final voting stage, related modules are classified as `stable`.
 * - For drafts in earlier stages, related modules are classified as `draft` (due to the dynamic nature
 * of draft specifications, the implementation might not represent the current state at any time).
 * - Furthermore there are external community modules classified as `experimental` which are not within
 * the scope of this documentation.
 *
 * There are some [general rules](general-rules.md) that apply to all modules.
 * @langDe
 * Die API-Funktionalität ist in Module, die sich an den OGC API Standards orientieren, aufgeteilt.
 * Jedes Modul ist ein [OSGi](https://de.wikipedia.org/wiki/OSGi)-Bundle.
 * Module können damit grundsätzlich zur Laufzeit hinzugefügt und wieder entfernt werden.
 *
 * Die ldproxy-Module werden nach der Stabilität der zugrundeliegenden Spezifikation unterschieden.
 * Implementiert ein Modul einen verabschiedeten Standard oder einen Entwurf,
 * der sich in der Schlussabstimmung befindet, wird es als "Stable" klassifiziert.
 *
 * Module, die Spezifikationsentwürfe oder eigene Erweiterungen implementieren,
 * werden als "Draft" klassifiziert. Bei diesen Modulen gibt es i.d.R. noch Abweichungen
 * vom erwarteten Verhalten oder von der in den aktuellen Entwürfen beschriebenen Spezifikation.
 *
 * Darüber hinaus sind weitere Module mit experimentellem Charakter als Community-Module verfügbar.
 * Die Community-Module sind kein Bestandteil der ldproxy Docker-Container.
 *
 * Grundsätzliche Regeln, die für alle API-Module gelten, finden Sie [hier](general-rules.md).
 *
 * @langAll ## Option `enabled`
 * @langEn Every module can be enabled or disabled in the configuration using `enabled`. The default
 * value differs between modules, see the [overview](#api-module-overview)).
 * @langDe Jedes API-Modul hat eine Konfigurationsoption `enabled`, die steuert, ob das Modul in der
 * jeweiligen API aktiviert ist. Einige Module sind standardmäßig aktiviert, andere deaktiviert
 * (siehe die nachfolgende [Übersicht](#api-module-overview)).
 */
@DocFile(path = "configuration/services/building-blocks/README.md")
@DocFileTemplate(
    path = "configuration/services/building-blocks",
    stripSuffix = "BuildingBlock",
    template = {
        @DocTemplate(language = "en", template = "{@body}\n\n## Scope\n\n###  Resources\n\n{@endpointTable}\n\n###  Query Parameters\n\n{@queryParameterTable}\n\n## Configuration\n\n{@propertyTable}\n\n### Example\n\n{@example}\n"),
        @DocTemplate(language = "de", template = "{@body}\n\n## Umfang\n\n###  Ressourcen\n\n{@endpointTable}\n\n###  Query Parameter\n\n{@queryParameterTable}\n\n## Konfiguration\n\n{@propertyTable}\n\n### Beispiel\n\n{@example}\n")
    }
)
@AutoMultiBind
public interface ApiBuildingBlock extends ApiExtension {

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return true;
  }

  ExtensionConfiguration getDefaultConfiguration();
}
