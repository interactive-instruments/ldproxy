/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import javax.annotation.Nullable;

/**
 * # HTTP header for caching
 * @langEn ldproxy sets the following HTTP headers for HTTP caching in responses - as far as they can
 * be determined for the respective resource:
 *
 * * `Last-Modified`: The timestamp of the last modification is determined - if possible - from the representation
 * of the resource to be returned, e.g. from the modification date of a file. It can be overridden
 * via a configuration setting (see below).
 * * `ETag`: The tag is determined - if possible - from the representation of the resource to be returned.
 * * `Cache-Control`: The header is only set if it has been configured for the module's resources (see below).
 * * `Expires`: The header is set only if it has been configured for the module's resources (see below).
 *
 * In any module that provides resources and does not just implement query parameters or output formats,
 * there is a `caching` configuration option whose value is an object with the following optional entries:
 * Translated with www.DeepL.com/Translator (free version)
 * @langDe ldproxy setzt in Antworten die folgenden HTTP-Header für HTTP-Caching - soweit diese für die jeweilige
 * Ressource bestimmt werden können:
 *
 * * `Last-Modified`: Der Zeitstempel der letzten Änderung wird - sofern möglich - aus der zurückzugebenden
 * Repräsentation der Ressource bestimmt, z.B. aus dem Änderungsdatum einer Datei. Er kann über eine
 * Konfigurationseinstellung überschrieben werden (siehe unten).
 * * `ETag`: Der Tag wird - sofern möglich - aus der zurückzugebenden Repräsentation der Ressource bestimmt.
 * * `Cache-Control`: Der Header wird nur gesetzt, wenn er für die Ressourcen des Moduls konfiguriert wurde (siehe unten).
 * * `Expires`: Der Header wird nur gesetzt, wenn er für die Ressourcen des Moduls konfiguriert wurde (siehe unten).
 *
 * In jedem Modul, das Ressourcen bereitstellt und nicht nur Query-Parameter oder Ausgabeformate realisiert,
 * ist eine Konfigurationsoption `caching`, deren Wert ein Objekt mit den folgenden, optionalen Einträgen ist:
 */
public interface CachingConfiguration {

    /**
     * @langEn Sets fixed values for [HTTP-Caching-Header](general-rules.md#caching) for the resources.
     * @langDe Setzt feste Werte für [HTTP-Caching-Header](general-rules.md#caching) für die Ressourcen.
     * @default `{}`
     */
    @Nullable
    Caching getCaching();

}
