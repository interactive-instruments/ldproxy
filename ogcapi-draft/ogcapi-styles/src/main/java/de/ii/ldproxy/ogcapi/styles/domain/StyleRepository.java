/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public interface StyleRepository {

    /**
     * get a stream of all formats available for this style collection
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @return stream of encodings for a style collection
     */
    Stream<StylesFormatExtension> getStylesFormatStream(OgcApiDataV2 apiData, Optional<String> collectionId);

    /**
     * get a stream of all style encodings available for this styles collection
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @return stream of style encodings
     */
    Stream<StyleFormatExtension> getStyleFormatStream(OgcApiDataV2 apiData, Optional<String> collectionId);

    /**
     * get a stream of all style metadata encodings available for this styles collection
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @return stream of style metadata encodings
     */
    Stream<StyleMetadataFormatExtension> getStyleMetadataFormatStream(OgcApiDataV2 apiData, Optional<String> collectionId);

    /**
     * get the list of all style encodings available for this style
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @return list of style encodings for this style
     */
    List<ApiMediaType> getStylesheetMediaTypes(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId);

    /**
     * get an object representation for this styles collection, derived styles are ignored
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param requestContext the current API request
     * @return the list of styles in this collection and links to API resources
     */
    Styles getStyles(OgcApiDataV2 apiData, Optional<String> collectionId, ApiRequestContext requestContext);

    /**
     * get an object representation for this styles collection
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param requestContext the current API request
     * @param includeDerived controls, if styles/stylesheets are included that are derived on-the-fly
     * @return the list of styles in this collection and links to API resources
     */
    Styles getStyles(OgcApiDataV2 apiData, Optional<String> collectionId, ApiRequestContext requestContext, boolean includeDerived);

    /**
     * determine, of a stylesheet is available, derived styles are ignored
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @param styleFormat the style encoding
     * @return {@code true}, if the stylesheet exists
     */
    boolean stylesheetExists(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat);

    /**
     * determine, of a stylesheet is available
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @param styleFormat the style encoding
     * @param includeDerived controls, if styles/stylesheets are included that are derived on-the-fly
     * @return {@code true}, if the stylesheet exists
     */
    boolean stylesheetExists(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, boolean includeDerived);

    /**
     * fetches a stylesheet
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @param styleFormat the style encoding
     * @param requestContext the current API request
     * @return the stylesheet, or throws an exception, if not available
     */
    StylesheetContent getStylesheet(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, ApiRequestContext requestContext);

    /**
     * fetches a stylesheet
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @param styleFormat the style encoding
     * @param requestContext the current API request
     * @param includeDerived controls, if styles/stylesheets are included that are derived on-the-fly
     * @return the stylesheet, or throws an exception, if not available
     */
    StylesheetContent getStylesheet(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, ApiRequestContext requestContext, boolean includeDerived);

    /**
     * fetches metadata of a style, including of derived styles
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @param requestContext the current API request
     * @return the style metadata, or throws an exception, if the style is not available
     */
    StyleMetadata getStyleMetadata(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, ApiRequestContext requestContext);

    /**
     * validate the style configuration during startup
     * @param builder the startup validation result builder
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @return the updated validation result builder
     */
    ImmutableValidationResult.Builder validate(ImmutableValidationResult.Builder builder,
                                               OgcApiDataV2 apiData,
                                               Optional<String> collectionId);
}
