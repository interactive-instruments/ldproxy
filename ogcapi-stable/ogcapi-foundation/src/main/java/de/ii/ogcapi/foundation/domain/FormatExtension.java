/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;

@AutoMultiBind
public interface FormatExtension extends ApiExtension {

    /**
     *
     * @return the media type of this format
     */
    ApiMediaType getMediaType();

    /**
     *
     * @return the path pattern (regex) for this format
     */
    String getPathPattern();

    /**
     *
     * @param apiData information about the API
     * @param path the resource path
     * @return the Schema and an optional example object for this format and a GET operation on this resource in this API
     */
    ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path);

    /**
     *
     * @param apiData information about the API
     * @param path the resource path
     * @param method the HTTP method of the operation
     * @return the Schema and an optional example object for this format and the resource in this API
     */
    default ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        if (method== HttpMethods.GET) {
            return getContent(apiData, path);
        }

        return null;
    }

    /**
     *
     * @param apiData information about the API
     * @param path the resource path
     * @param method the HTTP method of the operation
     * @return the Schema and an optional example object for a request body in this format and the resource in this API
     */
    default ApiMediaTypeContent getRequestContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
        return null;
    }

    /**
     *
     * @return {@code true}, if the response content varies from API to API
     */
    default boolean contentPerApi() { return false; }

    /**
     *
     * @return {@code true}, if the response content varies from resource to resource
     */
    default boolean contentPerResource() { return false; }

    /**
     *
     * @return {@code true}, if the format can be used in POST, PUT or PATCH requests
     */
    default boolean canSupportTransactions() { return false; }

    /**
     * @return {@code true}, if the format should be enabled by default
     */
    default boolean isEnabledByDefault() { return true; }
}

