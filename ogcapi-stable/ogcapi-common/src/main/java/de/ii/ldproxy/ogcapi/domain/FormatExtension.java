/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

public interface FormatExtension extends OgcApiExtension {

    /**
     *
     * @return the media type of this format
     */
    OgcApiMediaType getMediaType();

    /**
     *
     * @return the path pattern (regex) for this format
     */
    String getPathPattern();

    /**
     * By default, encodings with the labels that are enabled in Common Core are enabled. These defaults
     * should be overloaded for specific resources like features or stylesheets.
     *
     * @param apiData information about the API
     * @return {@code true}, if this format has been enabled in the configuration for this API
     */
    default boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return apiData.getExtension(OgcApiCommonConfiguration.class)
                      .filter(config -> config.getEncodings().contains(this.getMediaType().label()))
                      .isPresent();
    }

    /**
     * By default, encodings with the labels that are enabled in Common Core are enabled. These defaults
     * should be overloaded for specific resources like features or stylesheets.
     *
     * @param apiData information about the API
     * @param collectionId identifier of the collection
     * @return {@code true}, if this format has been enabled in the configuration for this API and collection
     */
    default boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(OgcApiCommonConfiguration.class)
                      .filter(config -> config.getEncodings().contains(this.getMediaType().label()))
                      .isPresent();
    }

    /**
     *
     * @param apiData information about the API
     * @param path the resource path
     * @return the Schema and an optional example object for this format and a GET operation on this resource in this API
     */
    OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path);

    /**
     *
     * @param apiData information about the API
     * @param path the resource path
     * @param method the HTTP method of the operation
     * @return the Schema and an optional example object for this format and the resource in this API
     */
    default OgcApiMediaTypeContent getContent(OgcApiApiDataV2 apiData, String path, OgcApiContext.HttpMethods method) {
        if (method==OgcApiContext.HttpMethods.GET)
            return getContent(apiData, path);

        return null;
    }

    /**
     *
     * @param apiData information about the API
     * @param path the resource path
     * @param method the HTTP method of the operation
     * @return the Schema and an optional example object for a request body in this format and the resource in this API
     */
    default OgcApiMediaTypeContent getRequestContent(OgcApiApiDataV2 apiData, String path, OgcApiContext.HttpMethods method) {
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

