/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ldproxy.ogcapi.html.domain.DatasetView;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.features.domain.Feature;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    public List<FeatureHtml> features;
    public boolean hideMap = true; // set to "hide"; change to "false" when we see a geometry
    public Set<Map.Entry<String, String>> filterFields;
    public Map<String, String> bbox;
    public TemporalExtent temporalExtent;
    public URICustomizer uriBuilder;
    public URICustomizer uriBuilderWithFOnly;
    public boolean bare;
    // TODO this belongs to the nearby module in the community repo
    // public List<PropertyDTO> additionalFeatures;
    public boolean isCollection;
    public String persistentUri;
    public boolean spatialSearch;
    public boolean schemaOrgFeatures;
    public FeaturesHtmlConfiguration.POSITION mapPosition;

    public FeatureCollectionView(String template, URI uri, String name, String title, String description,
                                 String urlPrefix, HtmlConfiguration htmlConfig, String persistentUri, boolean noIndex,
                                 I18n i18n, Locale language, FeaturesHtmlConfiguration.POSITION mapPosition) {
        super(template, uri, name, title, description, urlPrefix, htmlConfig, noIndex);
        this.features = new ArrayList<>();
        this.isCollection = !"featureDetails".equals(template);
        this.uri = uri; // TODO need to overload getPath() as it currently forces trailing slashes while OGC API uses no trailing slashes
        this.persistentUri = persistentUri;
        this.schemaOrgFeatures = Objects.nonNull(htmlConfig) && Objects.equals(htmlConfig.getSchemaOrgEnabled(), true);
        this.mapPosition = mapPosition;
    }

    @Override
    public String getPath() {
        String path = uri.getPath();
        return path;
    }

    public boolean isMapTop() {
        return mapPosition == POSITION.TOP || (mapPosition == POSITION.AUTO && (features.isEmpty() || features.stream().anyMatch(FeatureHtml::hasObjects)));
    }

    public boolean isMapRight() {
        return mapPosition == POSITION.RIGHT || (mapPosition == POSITION.AUTO && !features.isEmpty() && features.stream().noneMatch(FeatureHtml::hasObjects));
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
