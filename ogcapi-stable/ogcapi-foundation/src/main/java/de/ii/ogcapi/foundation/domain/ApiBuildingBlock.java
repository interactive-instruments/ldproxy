/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.docs.DocColumn;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocFilesTemplate;
import de.ii.xtraplatform.docs.DocFilesTemplate.ForEach;
import de.ii.xtraplatform.docs.DocI18n;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.docs.DocVar;

/**
 * @langEn # Building Blocks
 *     <p>The OGC API functionality is split up into modules based on [OGC API standards and other
 *     specifications](../../advanced/specifications.html).
 *     <p>The modules are classified according to:
 *     <p><code>
 * - The state of the **specification**
 *   - **stable**: related to approved standards or drafts in the final voting stage
 *   - **draft**: related to drafts in earlier stages (due to the dynamic nature of draft
 *     specifications, the implementation might not represent the current state at any time)
 *   - **custom**: no related standard or draft
 * - The state of the **implementation**
 *   - **mature**: no known limitations regarding generally supported use cases,
 *     adheres to code quality and testing standards
 *   - **candidate**: no known limitations regarding generally supported use cases,
 *      might not adhere to code quality and testing standards
 *   - **proposal**: stable core functionality, but might have limitations regarding generally
 *     supported use cases, might not adhere to code quality and testing standards
 *     </code>
 *     <p>## Overview
 *     <p>{@docTable:overview}
 * @langDe # Module
 *     <p>Die API-Funktionalität ist in Module aufgeteilt, die sich an den [OGC API Standards und
 *     weiteren Spezifikationen](../../advanced/specifications.html) orientieren.
 *     <p>Die Module sind klassifiziert nach:
 *     <p><code>
 * - Dem Status der **Spezifikation**
 *   - **stable**: zugehörig zu einem verabschiedeten Standard oder einen Entwurf, der sich in der
 *     Schlussabstimmung befindet
 *   - **draft**: zugehörig zu Spezifikationsentwürfen in früheren Stadien (bei diesen Modulen
 *     gibt es i.d.R. noch Abweichungen vom erwarteten Verhalten oder von der in den aktuellen
 *     Entwürfen beschriebenen Spezifikation)
 *   - **custom**: nicht zugehörig zu einem Standard oder Entwurf
 * - Dem Status der **Implementierung**
 *   - **mature**: keine Limitierungen bezogen auf allgemein unterstützte Anwendungsfälle,
 *     hält alle Code-Quality und Testing Standards ein
 *   - **candidate**: keine Limitierungen bezogen auf allgemein unterstützte Anwendungsfälle,
 *      hält eventuell nicht alle Code-Quality und Testing Standards ein
 *   - **proposal**: stabile Kernfunktionalität, aber kann Limitierungen bezogen auf allgemein
 *     unterstützte Anwendungsfälle enthalten, hält eventuell nicht alle Code-Quality und
 *     Testing Standards ein
 *     </code>
 *     <p>## Übersicht
 *     <p>{@docTable:overview}
 */
@DocFile(
    path = "services/building-blocks",
    name = "README.md",
    tables = {
      @DocTable(
          name = "overview",
          rows = {
            @DocStep(type = Step.IMPLEMENTATIONS),
            @DocStep(type = Step.SORTED, params = "{@title}")
          },
          columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "[{@title}]({@docFile:name})"),
                header = {
                  @DocI18n(language = "en", value = "Building Block"),
                  @DocI18n(language = "de", value = "Modul")
                }),
            @DocColumn(
                value =
                    @DocStep(
                        type = Step.TAG,
                        params =
                            "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"center\" style=\"margin-bottom: 5px; margin-right: 5px;\" /><span/><SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"center\" />"),
                header = {
                  @DocI18n(language = "en", value = "Classification"),
                  @DocI18n(language = "de", value = "Klassifizierung")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@body}"),
                header = {
                  @DocI18n(language = "en", value = "Description"),
                  @DocI18n(language = "de", value = "Beschreibung")
                })
          })
    })
@DocFilesTemplate(
    files = ForEach.IMPLEMENTATION,
    path = "services/building-blocks",
    stripSuffix = "BuildingBlock",
    template = {
      @DocI18n(
          language = "en",
          value =
              "# {@title}\n\n"
                  + "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"super\" />"
                  + "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"super\" />\n\n"
                  + "{@body}\n\n"
                  + "## Scope\n\n"
                  + "{@scopeEn |||}\n\n"
                  + "{@limitationsEn ### Limitations\n\n|||}\n\n"
                  + "{@conformanceEn ### Conformance Classes\n\n|||}\n\n"
                  + "{@docTable:endpoints ### Operations\n\n|||}\n\n"
                  + "{@docTable:pathParams ### Path Parameters\n\n|||}\n\n"
                  + "{@docTable:queryParams ### Query Parameters\n\n|||}\n\n"
                  + "## Configuration\n\n"
                  + "{@docVar:cfgBody |||}\n\n"
                  + "{@docTable:cfgProperties ### Options\n\n||| This building block has no configuration options.}\n\n"
                  + "{@cfgPropertiesAdditionalEn |||}\n\n"
                  + "{@docVar:cfgExamples ### Examples\n\n|||}\n"),
      @DocI18n(
          language = "de",
          value =
              "# {@title}\n\n"
                  + "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"super\" />"
                  + "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"super\" />\n\n"
                  + "{@body}\n\n"
                  + "## Umfang\n\n"
                  + "{@scopeDe |||}\n\n"
                  + "{@limitationsDe ### Limitierungen\n\n|||}\n\n"
                  + "{@conformanceDe ### Konformitätsklassen\n\n|||}\n\n"
                  + "{@docTable:endpoints ### Operationen\n\n|||}\n\n"
                  + "{@docTable:pathParams ### Pfad-Parameter\n\n|||}\n\n"
                  + "{@docTable:queryParams ### Query Parameter\n\n|||}\n\n"
                  + "## Konfiguration\n\n"
                  + "{@docVar:cfgBody |||}\n\n"
                  + "{@docTable:cfgProperties ### Optionen\n\n||| Dieses Modul hat keine Konfigurationsoptionen.}\n\n"
                  + "{@cfgPropertiesAdditionalDe |||}\n\n"
                  + "{@docVar:cfgExamples ### Beispiele\n\n|||}\n")
    },
    tables = {
      @DocTable(
          name = "queryParams",
          rows = {@DocStep(type = Step.TAG_REFS, params = "{@ref:queryParameters}")},
          columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "`{@title}`"),
                header = {
                  @DocI18n(language = "en", value = "Name"),
                  @DocI18n(language = "de", value = "Name")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@endpoints}"),
                header = {
                  @DocI18n(language = "en", value = "Resources"),
                  @DocI18n(language = "de", value = "Ressourcen")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@body}"),
                header = {
                  @DocI18n(language = "en", value = "Description"),
                  @DocI18n(language = "de", value = "Beschreibung")
                })
          }),
      @DocTable(
          name = "pathParams",
          rows = {@DocStep(type = Step.TAG_REFS, params = "{@ref:pathParameters}")},
          columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "`{@title}`"),
                header = {
                  @DocI18n(language = "en", value = "Name"),
                  @DocI18n(language = "de", value = "Name")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@endpoints}"),
                header = {
                  @DocI18n(language = "en", value = "Resources"),
                  @DocI18n(language = "de", value = "Ressourcen")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@body}"),
                header = {
                  @DocI18n(language = "en", value = "Description"),
                  @DocI18n(language = "de", value = "Beschreibung")
                })
          }),
      @DocTable(
          name = "endpoints",
          rows = {@DocStep(type = Step.TAG_REFS, params = "{@ref:endpoints}")},
          columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@title}"),
                header = {
                  @DocI18n(language = "en", value = "Resource"),
                  @DocI18n(language = "de", value = "Ressource")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "`{@path}`"),
                header = {
                  @DocI18n(language = "en", value = "Path"),
                  @DocI18n(language = "de", value = "Pfad")
                }),
            @DocColumn(
                value = {
                  @DocStep(type = Step.METHODS),
                  @DocStep(type = Step.ANNOTATIONS),
                  @DocStep(type = Step.FILTER, params = "ISIN:GET,POST,PUT,DELETE,PATCH"),
                  @DocStep(
                      type = Step.COLLECT,
                      params = {"DISTINCT", "SORTED", "SEPARATED:, "})
                },
                header = {
                  @DocI18n(language = "en", value = "Methods"),
                  @DocI18n(language = "de", value = "Methoden")
                }),
            @DocColumn(
                value = {
                  @DocStep(type = Step.TAG_REFS, params = "{@ref:formats}"),
                  @DocStep(type = Step.IMPLEMENTATIONS),
                  @DocStep(type = Step.TAG, params = "{@title |||}"),
                  @DocStep(
                      type = Step.COLLECT,
                      params = {"DISTINCT", "SORTED", "SEPARATED:, "})
                },
                header = {
                  @DocI18n(language = "en", value = "Media Types"),
                  @DocI18n(language = "de", value = "Formate")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@body}"),
                header = {
                  @DocI18n(language = "en", value = "Description"),
                  @DocI18n(language = "de", value = "Beschreibung")
                })
          }),
      @DocTable(
          name = "cfgProperties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES),
            @DocStep(type = Step.UNMARKED)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "cfgPropertiesCollection",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES),
            @DocStep(type = Step.MARKED, params = "collectionOnly")
          },
          columnSet = ColumnSet.JSON_PROPERTIES)
    },
    vars = {
      @DocVar(
          name = "cfgBody",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfg}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "cfgExamples",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfg}"),
            @DocStep(type = Step.TAG, params = "{@examples}")
          }),
    })
@AutoMultiBind
public interface ApiBuildingBlock extends ApiExtension {

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return true;
  }

  @Override
  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return getDefaultConfiguration().getClass();
  }

  ExtensionConfiguration getDefaultConfiguration();

  default <T extends ExtensionConfiguration> T hydrateConfiguration(T cfg) {
    return cfg;
  }
}
