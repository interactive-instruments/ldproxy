/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Value.Style(builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableHtmlConfiguration.Builder.class)
public interface HtmlConfiguration extends ExtensionConfiguration {
    Logger LOGGER = LoggerFactory.getLogger(HtmlConfiguration.class);

    abstract class Builder extends ExtensionConfiguration.Builder {
    }

    @Nullable
    Boolean getNoIndexEnabled();

    @JsonAlias(value = "microdataEnabled")
    @Nullable
    Boolean getSchemaOrgEnabled();

    @Nullable
    Boolean getCollectionDescriptionsInOverview();

    @Nullable
    String getLegalName();

    @Nullable
    String getLegalUrl();

    @Nullable
    String getPrivacyName();

    @Nullable
    String getPrivacyUrl();

    @Nullable
    String getDefaultStyle();

    @Nullable
    String getMapBackgroundUrl();

    @Nullable
    String getMapAttribution();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getLeafletUrl();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getLeafletAttribution();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getOpenLayersUrl();

    @Deprecated(since = "3.1.0")
    @Nullable
    String getOpenLayersAttribution();

    @Nullable
    String getFooterText();

    @Override
    default Builder getBuilder() {
        return new ImmutableHtmlConfiguration.Builder();
    }

    default String getStyle(Optional<String> requestedStyle, Optional<String> collectionId, String serviceUrl) {
        String styleUrl = requestedStyle
                .map(s -> s.equals("DEFAULT") ? Objects.requireNonNullElse(getDefaultStyle(), "NONE") : s )
                .filter(s -> !s.equals("NONE"))
                .map(s -> collectionId.isEmpty()
                        ? String.format("%s/styles/%s?f=mbs", serviceUrl, s)
                        : String.format("%s/collections/%s/styles/%s?f=mbs", serviceUrl, collectionId.get(), s))
                .orElse(null);

        // Check that the style exists
        if (Objects.nonNull(styleUrl)) {
            // TODO we currently test for the availability of the style using a HTTP request to
            //      avoid a dependency to STYLES. Once OGC API Styles is stable, we should consider to
            //      separate the StyleRepository from the endpoints. The StyleRepository could be part
            //      of FOUNDATION or its own module
            try {
                URL url = new URL(styleUrl);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestMethod("HEAD");
                if (http.getResponseCode()==404 && collectionId.isPresent()) {
                    // Try fallback to the dataset style, if we have a collection style
                    return getStyle(requestedStyle, Optional.empty(), serviceUrl);
                } else if (http.getResponseCode()!=200) {
                    LOGGER.error("Could not access style '{}', falling back to style 'NONE'. Response code: '{}'. Message: {}", styleUrl, http.getResponseCode(), http.getResponseMessage());
                    return null;
                }
                http.disconnect();
            } catch (Exception e) {
                LOGGER.error("Could not access style '{}', falling back to style 'NONE'. Reason: {}", styleUrl, e.getMessage());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Stacktrace: ", e);
                }
                return null;
            }
        }

        return styleUrl;
    }
}
