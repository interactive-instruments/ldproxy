/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
     * determine date of last change to any stylesheet of the style; this includes root stylesheets from
     * which a collection stylesheet will be derived
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @return the date or {@code null}, if no stylesheet is found
     */
    Date getStyleLastModified(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId);

    /**
     * determine date of last change to a stylesheet of the style; this includes root stylesheets from
     * which a collection stylesheet will be derived
     * @param apiData information about the API
     * @param collectionId the optional collection, or empty for a style collection at root level
     * @param styleId the identifier of the style in the style collection
     * @param styleFormat the style encoding
     * @param includeDerived controls, if styles/stylesheets are included that are derived on-the-fly
     * @return the date or {@code null}, if no stylesheet is found
     */
    Date getStylesheetLastModified(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension styleFormat, boolean includeDerived);

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
    Optional<JsonMergePatch> getStyleMetadataPatch(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId);
    byte[] updateStyleMetadataPatch(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, byte[] additionalPatch, boolean strict);

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

    Set<String> getStyleIds(OgcApiDataV2 apiData, Optional<String> collectionId, boolean includeDerived);

    String getNewStyleId(OgcApiDataV2 apiData, Optional<String> collectionId);

    void writeStyleDocument(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, StyleFormatExtension format, byte[] requestBody) throws IOException;

    void deleteStyle(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId) throws IOException;

    void writeStyleMetadataDocument(OgcApiDataV2 apiData, Optional<String> collectionId, String styleId, byte[] requestBody) throws IOException;
}
