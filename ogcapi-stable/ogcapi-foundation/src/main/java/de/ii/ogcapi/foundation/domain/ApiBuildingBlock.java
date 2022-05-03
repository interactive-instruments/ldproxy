/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a
 * copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
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
 * # Overview
 *
 * @langEn The OGC API functionality is split up into modules based on the OGC API standards. The
 * modules are classified according to the state of the implemented specification:
 * <p>
 * - For approved standards or drafts in the final voting stage, related modules are classified as
 * `stable`. - For drafts in earlier stages, related modules are classified as `draft` (due to the
 * dynamic nature of draft specifications, the implementation might not represent the current state
 * at any time). - Furthermore there are external community modules classified as `experimental`
 * which are not within the scope of this documentation.
 * <p>
 * ## Building Blocks
 * <p>
 * {@docTable:overview}
 * <p>
 * There are some [general rules](general-rules.md) that apply to all modules.
 * @langDe Die API-Funktionalität ist in Module, die sich an den OGC API Standards orientieren,
 * aufgeteilt. Jedes Modul ist ein [OSGi](https://de.wikipedia.org/wiki/OSGi)-Bundle. Module können
 * damit grundsätzlich zur Laufzeit hinzugefügt und wieder entfernt werden.
 * <p>
 * Die ldproxy-Module werden nach der Stabilität der zugrundeliegenden Spezifikation unterschieden.
 * Implementiert ein Modul einen verabschiedeten Standard oder einen Entwurf, der sich in der
 * Schlussabstimmung befindet, wird es als "Stable" klassifiziert.
 * <p>
 * Module, die Spezifikationsentwürfe oder eigene Erweiterungen implementieren, werden als "Draft"
 * klassifiziert. Bei diesen Modulen gibt es i.d.R. noch Abweichungen vom erwarteten Verhalten oder
 * von der in den aktuellen Entwürfen beschriebenen Spezifikation.
 * <p>
 * Darüber hinaus sind weitere Module mit experimentellem Charakter als Community-Module verfügbar.
 * Die Community-Module sind kein Bestandteil der ldproxy Docker-Container.
 * <p>
 * ## Liste der Module
 * <p>
 * {@docTable:overview}
 * <p>
 * Grundsätzliche Regeln, die für alle API-Module gelten, finden Sie [hier](general-rules.md).
 * @langAll ## Option `enabled`
 * @langEn Every module can be enabled or disabled in the configuration using `enabled`. The default
 * value differs between modules, see the [overview](#api-module-overview)).
 * @langDe Jedes API-Modul hat eine Konfigurationsoption `enabled`, die steuert, ob das Modul in der
 * jeweiligen API aktiviert ist. Einige Module sind standardmäßig aktiviert, andere deaktiviert
 * (siehe die nachfolgende [Übersicht](#api-module-overview)).
 */
@DocFile(
    path = "configuration/services/building-blocks",
    name = "README.md",
    tables = {
        @DocTable(
            name = "overview",
            rows = {
                @DocStep(type = Step.IMPLEMENTATIONS)
                //@DocStep(type = Step.SORTED, params = "{@sortPriority}")
            },
            columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "[{@title}]({@docFile:name})"),
                header = {
                    @DocI18n(language = "en", value = "Building Block"),
                    @DocI18n(language = "de", value = "Modul")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"center\" /><span/><SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"center\" />"),
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
    }
)
@DocFilesTemplate(
    files = ForEach.IMPLEMENTATION,
    path = "configuration/services/building-blocks",
    stripSuffix = "BuildingBlock",
    template = {
        @DocI18n(language = "en", value =
            "# {@title}\n\n" +
                "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"super\" />"
                +
                "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"super\" />\n\n"
                +
                "{@body}\n\n" +
                "## Scope\n\n" +
                "{@conformanceEn ### Conformance Classes\n\n|||}\n\n" +
                "{@docTable:endpoints ### Resources\n\n|||}\n\n" +
                "{@docTable:queryParams ### Query Parameters\n\n|||}\n\n" +
                "## Configuration\n\n" +
                "{@docTable:properties ### Options\n\n||| This building block has no configuration options.}\n\n" +
                "{@docVar:example ### Example\n\n|||}\n"),
        @DocI18n(language = "de", value =
            "# {@title}\n\n" +
                "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"super\" />"
                +
                "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"super\" />\n\n"
                +
                "{@body}\n\n" +
                "## Umfang\n\n" +
                "{@conformanceEn ### Konformitätsklassen\n\n|||}\n\n" +
                "{@docTable:endpoints ### Ressourcen\n\n|||}\n\n" +
                "{@docTable:queryParams ### Query Parameter\n\n|||}\n\n" +
                "## Konfiguration\n\n" +
                "{@docTable:properties ### Optionen\n\n||| Dieses Modul hat keine Konfigurationsoptionen.}\n\n" +
                "{@docVar:example ### Beispiel\n\n|||}\n")
    },
    tables = {
        @DocTable(name = "queryParams", rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@queryParameterTable}")}, columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "`{@name}`"),
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
        @DocTable(name = "endpoints", rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@endpointTable}")}, columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@name}"),
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
                    @DocStep(type = Step.FILTER, params = "ISIN:GET,POST"),
                    @DocStep(type = Step.COLLECT, params = {"DISTINCT", "SORTED", "SEPARATED:, "})
                },
                header = {
                    @DocI18n(language = "en", value = "Methods"),
                    @DocI18n(language = "de", value = "Methoden")
                }),
            @DocColumn(
                value = {
                    @DocStep(type = Step.TAG_REFS, params = "{@formats}"),
                    @DocStep(type = Step.IMPLEMENTATIONS),
                    @DocStep(type = Step.TAG, params = "{@format |||}"),
                    @DocStep(type = Step.COLLECT, params = {"DISTINCT", "SORTED", "SEPARATED:, "})
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
            name = "properties",
            rows = {
                @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
                @DocStep(type = Step.JSON_PROPERTIES),
                @DocStep(type = Step.UNMARKED)
            },
            columnSet = ColumnSet.JSON_PROPERTIES
        ),
        @DocTable(
            name = "collectionProperties",
            rows = {
                @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
                @DocStep(type = Step.JSON_PROPERTIES),
                @DocStep(type = Step.MARKED, params = "collectionOnly")
            },
            columnSet = ColumnSet.JSON_PROPERTIES
        )
    },
    vars = {
        @DocVar(
            name = "example",
            value = {
                @DocStep(type = Step.TAG_REFS, params = "{@example}"),
                @DocStep(type = Step.TAG, params = "{@example}")
            }
        )
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
