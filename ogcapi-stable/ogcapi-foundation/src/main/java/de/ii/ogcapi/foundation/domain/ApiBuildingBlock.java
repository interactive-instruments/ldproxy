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
 * @langEn # Overview
 *     <p>The OGC API functionality is split up into modules based on the OGC API standards. The
 *     modules are classified according to the state of the implemented specification:
 *     <p>- For approved standards or drafts in the final voting stage, related modules are
 *     classified as `stable`. - For drafts in earlier stages, related modules are classified as
 *     `draft` (due to the dynamic nature of draft specifications, the implementation might not
 *     represent the current state at any time). - Furthermore there are external community modules
 *     classified as `experimental` which are not within the scope of this documentation.
 *     <p>## List of building blocks
 *     <p>{@docTable:overview}
 *     <p>
 * @langDe # Übersicht
 *     <p>Die API-Funktionalität ist in Module, die sich an den OGC API Standards orientieren,
 *     aufgeteilt. Jedes Modul ist ein [OSGi](https://de.wikipedia.org/wiki/OSGi)-Bundle. Module
 *     können damit grundsätzlich zur Laufzeit hinzugefügt und wieder entfernt werden.
 *     <p>Die ldproxy-Module werden nach der Stabilität der zugrundeliegenden Spezifikation
 *     unterschieden. Implementiert ein Modul einen verabschiedeten Standard oder einen Entwurf, der
 *     sich in der Schlussabstimmung befindet, wird es als "Stable" klassifiziert.
 *     <p>Module, die Spezifikationsentwürfe oder eigene Erweiterungen implementieren, werden als
 *     "Draft" klassifiziert. Bei diesen Modulen gibt es i.d.R. noch Abweichungen vom erwarteten
 *     Verhalten oder von der in den aktuellen Entwürfen beschriebenen Spezifikation.
 *     <p>Darüber hinaus sind weitere Module mit experimentellem Charakter als Community-Module
 *     verfügbar. Die Community-Module sind kein Bestandteil der ldproxy Docker-Container.
 *     <p>## Liste der Bausteine
 *     <p>{@docTable:overview}
 *     <p>
 * @langEn ## General rules
 *     <p>### Response encoding
 *     <p>For operations that return a response, the encoding is chosen using standard HTTP content
 *     negotiation with `Accept` headers.
 *     <p>GET operations additionally support the query parameter `f`, which allows to explicitely
 *     choose the encoding and override the result of the content negotiation. The supported
 *     encodings depend on the affected resource and the configuration.
 *     <p>### Response language
 *     <p>For operations that return a response, the language for linguistic texts is chosen using
 *     standard HTTP content negiotiation with `Accept-Language` headers.
 *     <p>If enabled in [Common Core](common.md), GET operations additionally support the quer
 *     parameter `lang`, which allows to explicitely choose the language and override the result of
 *     the content negotiation. The supported languages depend on the affected resource and the
 *     configuration. Support for multilingualism is currently limited. There are four possible
 *     sources for linguistic texts:
 *     <p><code>
 * - Static texts: For example link labels or static texts in HTML represenations. Currently the languages English (`en`) and German (`de`) are supported.
 * - Texts contained in the data: Currently not supported.
 * - Texts set in the configuration: Currently not supported.
 * - Error messages: These are always in english, the messages are currently hard-coded.
 * </code>
 *     <p>### Option `enabled`
 *     <p>Every module can be enabled or disabled in the configuration using `enabled`. The default
 *     value differs between modules.
 *     <p>### Resource paths
 *     <p>All resource paths in this documentation are relative to the base URI of the deployment.
 *     For example given the base URI `https://example.com/pfad/zu/apis` and the resource path
 *     `/{apiId}/collections`, the full path would be
 *     `https://example.com/pfad/zu/apis/{apiId}/collections`.
 *     <p><a name="transformations"></a>
 *     <p>### Property transformations
 *     <p>Modules related to feature encoding ([Core](features_core.md),
 *     [GeoJSON](features_geojson.md), [HTML](features_html.md), [Vector Tiles](vector_tiles.md))
 *     support transforming feature properties for all or only for specific encodings.
 *     <p>Transformations do not affect data sources, they are applied on-the-fly as part of the
 *     encoding.
 *     <p>Filter expressions do not take transformations into account, they have to be based on the
 *     source values. That means queryable properties (see `queryables` in [Features
 *     Core](features_core.md)) should not use transformations in most cases. The exception to the
 *     rule is the HTML encoding, where readability might be more important than filter support.
 *     <p>See [Transformations](../../providers/details/transformations.md) for supported
 *     transformations.
 * @langDe ## Grundsätzliche Regeln für alle API-Module
 *     <p>### Auswahl des Antwortformats
 *     <p>Bei Operationen, die eine Antwort zurückliefern, wird das Format nach den
 *     Standard-HTTP-Regeln standardmäßig über Content-Negotiation und den `Accept`-Header
 *     ermittelt.
 *     <p>Alle GET-Operationen unterstützen zusätzlich den Query-Parameter `f`. Über diesen
 *     Parameter kann das Ausgabeformat der Antwort auch direkt ausgewählt werden. Wenn kein Wert
 *     angegeben wird, gelten die Standard-HTTP-Regeln, d.h. der `Accept`-Header wird zur Bestimmung
 *     des Formats verwendet. Die unterstützten Formate hängen von der Ressource und von der
 *     API-Konfiguration ab.
 *     <p>### Auswahl des Antwortsprache
 *     <p>Bei Operationen, die eine Antwort zurückliefern, wird die verwendete Sprache bei
 *     linguistischen Texten nach den Standard-HTTP-Regeln standardmäßig über Content-Negotiation
 *     und den `Accept-Language`-Header ermittelt.
 *     <p>Sofern die entsprechende Option im Modul "Common Core" aktiviert ist, unterstützen alle
 *     GET-Operationen zusätzlich den Query-Parameter `lang`. Über diesen Parameter kann die Sprache
 *     auch direkt ausgewählt werden. Wenn kein Wert angegeben wird, gelten die
 *     Standard-HTTP-Regeln, wie oben beschrieben. Die erlaubten Werte hängen von der Ressource und
 *     von der API-Konfiguration ab. Die Unterstüzung für Mehrsprachigkeit ist derzeit begrenzt. Es
 *     gibt vier Arten von Quellen für Texte:
 *     <p><code>
 * - Texte zu festen Elementen der API: Diese werden von ldproxy erzeugt, z.B. die Texte der Titel von Links oder feste Textbausteine in der HTML-Ausgabe. Derzeit werden die Sprachen "Deutsch" (de) und "Englisch" (en) unterstützt.
 * - Texte aus Attributen in den Daten: Hier gibt es noch keine Unterstützung, wie die Rückgabe bei mehrsprachigen Daten in Abhängigkeit von der Ausgabesprache gesteuert werden kann.
 * - Texte aus der API-Konfiguration, insbesondere zum Datenschema: Hier gibt es noch keine Unterstützung, wie die Rückgabe bei mehrsprachigen Daten in Abhängigkeit von der Ausgabesprache gesteuert werden kann.
 * - Fehlermeldungen der API: Diese sind immer in Englisch, die Meldungen sind aktuell Bestandteil des Codes.
 * </code>
 *     <p>### Option `enabled` </code> Jedes API-Modul hat eine Konfigurationsoption `enabled`, die
 *     steuert, ob das Modul in der jeweiligen API aktiviert ist. Einige Module sind standardmäßig
 *     aktiviert, andere deaktiviert.
 *     <p>### Pfadangaben
 *     <p>Alle Pfadangaben in dieser Dokumentation sind relativ zur Basis-URI des Deployments. Ist
 *     dies zum Beispiel `https://example.com/pfad/zu/apis` und lautet der Pfad einer Ressource
 *     `/{apiId}/collections` dann ist die URI der Ressource
 *     `https://example.com/pfad/zu/apis/{apiId}/collections`.
 *     <p><a name="transformations"></a>
 *     <p>### Transformationen
 *     <p>In den API-Modulen, die Features verarbeiten ([Core](features_core.md),
 *     [GeoJSON](features_geojson.md), [JSON-FG](features_json-fg.md), [HTML](features_html.md),
 *     [Tiles](vector_tiles.md) mit dem Features-Tile-Provider), können die Feature-Eigenschaften
 *     über Transformationen an die Anforderungen der Ausgabe angepasst werden.
 *     <p>Die Transformation der Werte erfolgt bei der Aufbereitung der Daten für die Rückgabe über
 *     die API. Die Datenhaltung selbst bleibt unverändert.
 *     <p>Alle Filterausdrücke (siehe `queryables` im [Modul "Features Core"](features_core.md))
 *     wirken unabhängig von etwaigen Transformationen bei der Ausgabe und müssen auf der Basis der
 *     Werte in der Datenhaltung formuliert sein - die Transformationen sind i.A. nicht umkehrbar
 *     und eine Berücksichtigung der inversen Transformationen bei Filterausdrücken wäre kompliziert
 *     und nur unvollständig möglich. Insofern sollten Eigenschaften, die queryable sein sollen,
 *     möglichst bereits in der Datenquelle transformiert sein. Eine Ausnahme sind typischerweise
 *     Transformationen in der HTML-Ausgabe, wo direkte Lesbarkeit i.d.R. wichtiger ist als die
 *     Filtermöglichkeit.
 *     <p>Siehe [Transformations](../../providers/details/transformations.md) für unterstützte
 *     Transformationen.
 *     <p><a name="caching"></a>
 *     <p>### HTTP-Header für Caching
 *     <p>ldproxy setzt in Antworten die folgenden HTTP-Header für HTTP-Caching - soweit diese für
 *     die jeweilige Ressource bestimmt werden können:
 *     <p><code>
 * - `Last-Modified`: Der Zeitstempel der letzten Änderung wird - sofern möglich - aus der zurückzugebenden Repräsentation der Ressource bestimmt, z.B. aus dem Änderungsdatum einer Datei. Er kann über eine Konfigurationseinstellung überschrieben werden (siehe unten).
 * - `ETag`: Der Tag wird - sofern möglich - aus der zurückzugebenden Repräsentation der Ressource bestimmt.
 * - `Cache-Control`: Der Header wird nur gesetzt, wenn er für die Ressourcen des Moduls konfiguriert wurde (siehe unten).
 * - `Expires`: Der Header wird nur gesetzt, wenn er für die Ressourcen des Moduls konfiguriert wurde (siehe unten).
 * </code>
 *     <p>In jedem Modul, das Ressourcen bereitstellt und nicht nur Query-Parameter oder
 *     Ausgabeformate realisiert, ist eine Konfigurationsoption `caching`, deren Wert ein Objekt mit
 *     den folgenden, optionalen Einträgen ist:
 *     <p><code>
 * |Option |Datentyp |Default |Beschreibung
 * | --- | --- | --- | ---
 * |`lastModified` |string |`null` |Für die Ressourcen in dem Modul wird der `Last-Modified` Header auf den konfigurierten Wert gesetzt. Der Wert überschreibt einen ggf. aus der Ressource bestimmten Änderungszeitpunkt.
 * |`cacheControl` |string |`null` |Für die Ressourcen in dem Modul wird der `Expires` Header auf den konfigurierten Wert gesetzt. Ausnahme sind die "Features" und "Feature"-Ressourcen, bei denen `cacheControlItems` zu verwenden ist.
 * |`cacheControlItems` |string |`null` |Für die "Features" und "Feature"-Ressourcen wird der `Cache-Control` Header auf den konfigurierten Wert gesetzt.
 * |`expires` |string |`null` |Für die Ressourcen in dem Modul wird der `Expires` Header auf den konfigurierten Wert gesetzt.
 * </code>
 *     <p>In der API-Konfiguration können über eine Konfigurationsoption `defaultCaching`
 *     Standardwerte für die gesamte API gesetzt werden.
 *     <p>Beispiel für die Angaben in der Konfigurationsdatei:
 *     <p><code>
 * ```yaml
 * defaultCaching:
 *   cacheControl: 'max-age=3600'
 * - buildingBlock: FEATURES_CORE
 *   caching:
 *     lastModified: '2021-07-01T00:00:00+02:00'
 *     expires: '2021-12-31T23:59:59+01:00'
 * ```
 * </code>
 *     <p>
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
                            "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"center\" style=\"margin-bottom: 5px;\" /><span/><SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"center\" />"),
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
                  + "{@conformanceEn ### Conformance Classes\n\n|||}\n\n"
                  + "{@docTable:endpoints ### Resources\n\n|||}\n\n"
                  + "{@docTable:queryParams ### Query Parameters\n\n|||}\n\n"
                  + "## Configuration\n\n"
                  + "{@configurationEn |||}\n\n"
                  + "{@docTable:properties ### Options\n\n||| This building block has no configuration options.}\n\n"
                  + "{@propertiesEn |||}\n\n"
                  + "{@docVar:example ### Example\n\n|||}\n"),
      @DocI18n(
          language = "de",
          value =
              "# {@title}\n\n"
                  + "<SplitBadge type=\"{@layer.nameSuffix}\" left=\"spec\" right=\"{@layer.nameSuffix}\" vertical=\"super\" />"
                  + "<SplitBadge type=\"{@module.maturity}\" left=\"impl\" right=\"{@module.maturity}\" vertical=\"super\" />\n\n"
                  + "{@body}\n\n"
                  + "## Umfang\n\n"
                  + "{@scopeDe |||}\n\n"
                  + "{@conformanceDe ### Konformitätsklassen\n\n|||}\n\n"
                  + "{@docTable:endpoints ### Ressourcen\n\n|||}\n\n"
                  + "{@docTable:queryParams ### Query Parameter\n\n|||}\n\n"
                  + "## Konfiguration\n\n"
                  + "{@configurationDe |||}\n\n"
                  + "{@docTable:properties ### Optionen\n\n||| Dieses Modul hat keine Konfigurationsoptionen.}\n\n"
                  + "{@propertiesDe |||}\n\n"
                  + "{@docVar:example ### Beispiel\n\n|||}\n")
    },
    tables = {
      @DocTable(
          name = "queryParams",
          rows = {@DocStep(type = Step.TAG_REFS, params = "{@queryParameterTable}")},
          columns = {
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
      @DocTable(
          name = "endpoints",
          rows = {@DocStep(type = Step.TAG_REFS, params = "{@endpointTable}")},
          columns = {
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
                  @DocStep(type = Step.TAG_REFS, params = "{@formats}"),
                  @DocStep(type = Step.IMPLEMENTATIONS),
                  @DocStep(type = Step.TAG, params = "{@format |||}"),
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
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
            @DocStep(type = Step.JSON_PROPERTIES),
            @DocStep(type = Step.UNMARKED)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "collectionProperties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
            @DocStep(type = Step.JSON_PROPERTIES),
            @DocStep(type = Step.MARKED, params = "collectionOnly")
          },
          columnSet = ColumnSet.JSON_PROPERTIES)
    },
    vars = {
      @DocVar(
          name = "example",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@example}"),
            @DocStep(type = Step.TAG, params = "{@example}")
          })
    })
@AutoMultiBind
public interface ApiBuildingBlock extends ApiExtension {

  @Override
  default boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return true;
  }

  ExtensionConfiguration getDefaultConfiguration();
}
