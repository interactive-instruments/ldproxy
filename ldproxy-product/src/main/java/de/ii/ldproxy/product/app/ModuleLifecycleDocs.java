/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.product.app;

import de.ii.xtraplatform.docs.DocColumn;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocI18n;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;

/**
 * @langEn # Module Lifecycle
 *     <p>ldproxy is a completely modular application. The modules are classified according to:
 *     <p><code>
 * - The state of the **implementation**
 *   - `mature`: no known limitations regarding generally supported use cases,
 *     adheres to code quality and testing standards
 *   - `candidate`: no known limitations regarding generally supported use cases,
 *      might not adhere to code quality and testing standards
 *   - `proposal`: stable core functionality, but might have limitations regarding generally
 *     supported use cases, might not adhere to code quality and testing standards
 * - The level of **maintenance**
 *   - `full`: is regularly tested with a range of use cases, bugs are usually fixed quickly, both proactive and reactive
 *   - `low`: is tested only sporadically or only with few use cases, bugs are only fixed on demand and with low priority
 *   - `none`: is not tested at all or only for very specific use cases, bugs are likely not fixed
 *     </code>
 *     <p>A module with maintenance level `low` or `none` might also be `deprecated`, which means it
 *     will most likely be removed in the next major release.
 *     <p>## Overview
 *     <p>{@docTable:overview}
 * @langDe # Modul-Lebenszyklus
 *     <p>ldproxy ist eine komplett modulare Applikation. Die Module sind klassifiziert nach:
 *     <p><code>
 * - Dem Status der **Implementierung**
 *   - `mature`: keine Limitierungen bezogen auf allgemein unterstützte Anwendungsfälle,
 *     hält alle Code-Quality und Testing Standards ein
 *   - `candidate`: keine Limitierungen bezogen auf allgemein unterstützte Anwendungsfälle,
 *      hält eventuell nicht alle Code-Quality und Testing Standards ein
 *   - `proposal`: stabile Kernfunktionalität, aber kann Limitierungen bezogen auf allgemein
 *     unterstützte Anwendungsfälle enthalten, hält eventuell nicht alle Code-Quality und
 *     Testing Standards ein
 * - Dem Grad der **Pflege**
 *   - `full`: wird regelmäßig mit verschiedenen Anwendungsfällen getestet, Bugs
 *     werden im Normalfall schnell behoben, sowohl proaktiv als auch reaktiv
 *   - `low`: wird nur unregelmäßig oder nur mit wenigen Anwendungsfällen getestet,
 *     Bugs werden nur reaktiv und mit niedriger Priorität behoben
 *   - `none`: wird gar nicht oder nur für sehr spezielle Anwendungsfälle getestet,
 *     Bugs werden voraussichtlich nicht behoben
 *     </code>
 *     <p>Ein Modul mit Pflegegrad `low` oder `none` kann zusätzlich noch `deprecated` sein, was
 *     bedeutet, dass es wahrscheinlich im nächsten Major-Release entfernt wird.
 *     <p>## Übersicht
 *     <p>{@docTable:overview}
 */
@DocFile(
    path = "references",
    name = "modules.md",
    tables = {
      @DocTable(
          name = "overview",
          rows = {
            @DocStep(type = Step.MODULES),
            @DocStep(type = Step.SORTED, params = "{@module.name}")
          },
          columns = {
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@module.name}"),
                header = {
                  @DocI18n(language = "en", value = "Module"),
                  @DocI18n(language = "de", value = "Modul")
                }),
            @DocColumn(
                value =
                    @DocStep(
                        type = Step.TAG,
                        params =
                            "<SplitBadge type=\"{@module.maturityBadge}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"center\" style=\"margin-bottom: 5px; margin-right: 5px;\" /><span/><SplitBadge type=\"{@module.maintenanceBadge}\" left=\"main\" right=\"{@module.maintenance}\" vertical=\"center\" /><span v-if=\"'{@module.deprecated}' === 'true'\"  style=\"height: 14px;display: inline-block;margin-bottom: 5px; margin-right: 5px;\" /><Badge v-if=\"'{@module.deprecated}' === 'true'\" type=\"danger\" text=\"deprecated\" vertical=\"middle\" style=\"font-size: 12px;font-weight: normal;font-family: var(--font-family-code);color: var(--c-bg);text-shadow: 1px 1px 2px var(--c-text-lightest);\" />"),
                header = {
                  @DocI18n(language = "en", value = "Classification"),
                  @DocI18n(language = "de", value = "Klassifizierung")
                }),
            @DocColumn(
                value = @DocStep(type = Step.TAG, params = "{@module.description}"),
                header = {
                  @DocI18n(language = "en", value = "Description"),
                  @DocI18n(language = "de", value = "Beschreibung")
                })
          })
    })
public class ModuleLifecycleDocs {}
