/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.DatasetView;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class FeatureCollectionView extends DatasetView {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCollectionView.class);

    private URI uri;
    public List<NavigationDTO> pagination;
    public List<NavigationDTO> metaPagination;
    public List<ObjectDTO> features;
    public boolean hideMap = true; // set to "hide"; change to "false" when we see a geometry
    public PropertyDTO links;
    public Set<Map.Entry<String, String>> filterFields;
    public Map<String, String> bbox2;
    public FeatureTypeConfigurationOgcApi.TemporalExtent temporalExtent;
    public URICustomizer uriBuilder;
    public URICustomizer uriBuilderWithFOnly;
    public boolean bare;
    // TODO this belongs to the nearby module in the community repo
    // public List<PropertyDTO> additionalFeatures;
    public boolean isCollection;
    public String persistentUri;
    public boolean spatialSearch;
    public boolean classic;
    public boolean complexObjects;

    public FeatureCollectionView(String template, URI uri, String name, String title, String description,
                                 String urlPrefix, HtmlConfiguration htmlConfig, String persistentUri, boolean noIndex,
                                 I18n i18n, Locale language, FeaturesHtmlConfiguration.LAYOUT layout) {
        super(template, uri, name, title, description, urlPrefix, htmlConfig, noIndex);
        this.features = new ArrayList<>();
        this.isCollection = !"featureDetails".equals(template);
        this.uri = uri; // TODO need to overload getPath() as it currently forces trailing slashes while OGC API uses no trailing slashes
        this.persistentUri = persistentUri;
        this.classic = layout == FeaturesHtmlConfiguration.LAYOUT.CLASSIC;
        this.complexObjects = layout == FeaturesHtmlConfiguration.LAYOUT.COMPLEX_OBJECTS;
    }

    @Override
    public String getPath() {
        String path = uri.getPath();
        return path;
    }

    public Optional<String> getCanonicalUrl() throws URISyntaxException {
        if (!isCollection && persistentUri!=null)
            return Optional.of(persistentUri);

        URICustomizer canonicalUri = uriBuilder.copy()
                                               .ensureNoTrailingSlash()
                                               .clearParameters();

        boolean hasOtherParams = !canonicalUri.isQueryEmpty();
        boolean hasPrevLink = Objects.nonNull(metaPagination) && metaPagination.stream()
                                                                               .anyMatch(navigationDTO -> "prev".equals(navigationDTO.label));

        return !hasOtherParams && (!isCollection || !hasPrevLink)
                ? Optional.of(canonicalUri.toString())
                : Optional.empty();
    }

    public Optional<String> getPersistentUri() throws URISyntaxException {
        if (!isCollection && persistentUri!=null)
            return Optional.of(persistentUri);

        return Optional.empty();
    }

    public String getQueryWithoutPage() {
        List<NameValuePair> query = URLEncodedUtils.parse(getQuery().substring(1), Charset.forName("utf-8"))
                                                   .stream()
                                                   .filter(kvp -> !kvp.getName().equals("offset") &&
                                                                  !kvp.getName().equals("limit"))
                                                   .collect(Collectors.toList());

        return '?' + URLEncodedUtils.format(query, '&', Charset.forName("utf-8")) + '&';
    }

    private String urlencode(String segment) {
        try {
            return URLEncoder.encode(segment, Charsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn(String.format("Exception while encoding feature id '%s' for use in a URI. Trying with the unencoded id.",segment));
            return segment;
        }
    }

    public Function<String, String> getCurrentUrlWithSegment() {
        return segment -> uriBuilderWithFOnly.copy()
                                    .ensureLastPathSegment(urlencode(segment))
                                    .ensureNoTrailingSlash()
                                    .toString();
    }

    public Function<String, String> getCurrentUrlWithSegmentClearParams() {
        return segment -> uriBuilder.copy()
                                    .ensureLastPathSegment(urlencode(segment))
                                    .ensureNoTrailingSlash()
                                    .clearParameters()
                                    .toString();
    }
}
