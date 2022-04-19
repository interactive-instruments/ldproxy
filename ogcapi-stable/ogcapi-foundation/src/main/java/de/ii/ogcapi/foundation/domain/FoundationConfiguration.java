/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * @lang_en The schema of the API Catalog resource:
 * @lang_de Das Schema der API-Catalog-Ressource ist:
 * @example <code>
 * ```yaml
 * type: object
 * required:
 *   - apis
 * properties:
 *   title:
 *     type: string
 *   description:
 *     type: string
 *   apis:
 *     type: array
 *     items:
 *       type: object
 *       required:
 *         - title
 *         - landingPageUri
 *       properties:
 *         title:
 *           type: string
 *         description:
 *           type: string
 *         landingPageUri:
 *           type: string
 *           format: uri
 * ```
 * </code>
 */

/**
 * @lang_en Example of the specifications in the configuration file:
 * @lang_de Beispiel für die Angaben in der Konfigurationsdatei:
 * @example <code>
 * ```yaml
 * - buildingBlock: FOUNDATION
 *   includeLinkHeader: true
 *   useLangParameter: false
 *   apiCatalogLabel: 'Demonstration APIs using ldproxy'
 *   apiCatalogDescription: 'The APIs below are available as examples for Web APIs that can be set up with <a href="https://github.com/interactive-instruments/ldproxy">ldproxy</a>.'
 * ```
 * </code>
 */
@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFoundationConfiguration.Builder.class)
public interface FoundationConfiguration extends ExtensionConfiguration {

    String API_RESOURCES_DIR = "api-resources";
    String CACHE_DIR = "cache";
    String TMP_DIR = "tmp";

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    /**
     * @lang_en Support query parameter `lang` to set the desired response language.
     * @lang_de Steuert, ob die Sprache der Antwort bei allen GET-Operationen nur über den
     * `Accept-Lang`-Header oder auch über einen Parameter `lang` ausgewählt werden kann.
     * @default `false`
     */
    @Nullable
    Boolean getUseLangParameter();

    /**
     * @lang_en Return links contained in API responses also as
     * [HTTP header](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_link_headers).
     * @lang_de Steuert, ob die in Antworten der API enthaltenen Links auch als
     * [HTTP-Header](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#_link_headers)
     * zurückgegeben werden.
     * @default `true`
     */
    @Nullable
    Boolean getIncludeLinkHeader();

    /**
     * @lang_en Title for resource *API Catalog*.
     * @lang_de Titel für die API-Catalog-Ressource in diesem Deployment.
     * @default "API Overview"
     */
    @Nullable
    String getApiCatalogLabel();

    /**
     * @lang_en Description for resource *API Catalog*. May contain HTML elements.
     * @lang_de Beschreibung für die API-Catalog-Ressource
     * in diesem Deployment. HTML-Markup wird bei der HTML-Ausgabe aufbereitet.
     * @default "The following OGC APIs are available."
     */
    @Nullable
    String getApiCatalogDescription();

    /**
     * @lang_en If set, the value is embedded in the HTML page of the API catalog resource in
     * a "googleSiteVerification" meta tag (`<meta name="`google-site-verification`" content="{value}">).
     * @lang_de Sofern gesetzt, wird der Wert in die HTML-Seite des API-Catalog-Ressource in einem
     * "googleSiteVerification"-Meta-Tag eingebettet (`<meta name="`google-site-verification`"
     * content="{value}">).
     * @default `null`
     */
    @Nullable
    String getGoogleSiteVerification();

    @Override
    default Builder getBuilder() {
        return new ImmutableFoundationConfiguration.Builder();
    }
}