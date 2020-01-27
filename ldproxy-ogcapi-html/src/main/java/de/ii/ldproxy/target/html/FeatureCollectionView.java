/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class FeatureCollectionView extends DatasetView {

    // TODO check use of all properties

    private URI uri;
    // TODO public String requestUrl;
    public List<NavigationDTO> pagination;
    public List<NavigationDTO> metaPagination;
    public List<ObjectDTO> features;
    public List<NavigationDTO> indices;
    // TODO public String index;
    // TODO public String indexValue;
    public boolean hideMap = true; // set to "hide"; change to "false" when we see a geometry
    // TODO public boolean hideMetadata;
    // TODO public boolean showFooterText = true;
    public PropertyDTO links;
    public Set<Map.Entry<String, String>> filterFields;
    public Map<String, String> bbox2;
    public FeatureTypeConfigurationOgcApi.TemporalExtent temporalExtent;
    public URICustomizer uriBuilder;
    public URICustomizer uriBuilderWithFOnly;
    public boolean bare;
    public List<PropertyDTO> additionalFeatures;
    public boolean isCollection;
    public String persistentUri;
    public boolean spatialSearch;
    public boolean classic;
    public boolean complexObjects;

    public FeatureCollectionView(String template, URI uri, String name, String title, String description,
                                 String urlPrefix, HtmlConfig htmlConfig, String persistentUri, boolean noIndex,
                                 I18n i18n, Locale language, HtmlConfiguration.LAYOUT layout) {
        super(template, uri, name, title, description, urlPrefix, htmlConfig, noIndex);
        this.features = new ArrayList<>();
        this.isCollection = !"featureDetails".equals(template);
        this.uri = uri; // TODO need to overload getPath() as it currently forces trailing slashes while OGC API uses no trailing slashes
        this.persistentUri = persistentUri;
        this.classic = layout == HtmlConfiguration.LAYOUT.CLASSIC;
        this.complexObjects = layout == HtmlConfiguration.LAYOUT.COMPLEX_OBJECTS;
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

    public String getQueryWithoutPage() {
        List<NameValuePair> query = URLEncodedUtils.parse(getQuery().substring(1), Charset.forName("utf-8"))
                                                   .stream()
                                                   .filter(kvp -> !kvp.getName().equals("offset") &&
                                                                  !kvp.getName().equals("limit"))
                                                   .collect(Collectors.toList());

        return '?' + URLEncodedUtils.format(query, '&', Charset.forName("utf-8")) + '&';
    }

    public Function<String, String> getCurrentUrlWithSegment() {
        return segment -> uriBuilderWithFOnly.copy()
                                    .ensureLastPathSegment(segment)
                                    .ensureNoTrailingSlash()
                                    .toString();
    }

    public Function<String, String> getCurrentUrlWithSegmentClearParams() {
        return segment -> uriBuilder.copy()
                                    .ensureLastPathSegment(segment)
                                    .ensureNoTrailingSlash()
                                    .clearParameters()
                                    .toString();
    }
}
