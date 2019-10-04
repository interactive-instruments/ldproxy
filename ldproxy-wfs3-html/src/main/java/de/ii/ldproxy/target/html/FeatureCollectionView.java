/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

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

    public String requestUrl;
    public List<NavigationDTO> pagination;
    public List<NavigationDTO> metaPagination;
    public List<FeatureDTO> features;
    public List<NavigationDTO> indices;
    public String index;
    public String indexValue;
    public boolean hideMap;
    public boolean hideMetadata;
    public boolean showFooterText = true;
    public FeaturePropertyDTO links;
    public Set<Map.Entry<String, String>> filterFields;
    public Map<String, String> bbox2;
    public FeatureTypeConfigurationOgcApi.TemporalExtent temporalExtent;
    public URICustomizer uriBuilder;
    public URICustomizer uriBuilder2;
    public boolean bare;
    public List<FeaturePropertyDTO> additionalFeatures;
    public boolean isCollection;
    public boolean spatialSearch;

    public FeatureCollectionView(String template, URI uri, String name, String title, String description,
                                 String urlPrefix, HtmlConfig htmlConfig) {
        super(template, uri, name, title, description, urlPrefix, htmlConfig);
        this.features = new ArrayList<>();
        this.isCollection = !"featureDetails".equals(template);
    }

    public Optional<String> getCanonicalUrl() throws URISyntaxException {
        String bla = uriBuilder2.copy()
                                .clearParameters()
                                .toString() + "?";
        String bla2 = uriBuilder2.copy()
                                 .removeParameters("f")
                                 .toString();

        boolean hasOtherParams = !bla.equals(bla2);
        boolean hasPrevLink = Objects.nonNull(metaPagination) && metaPagination.stream()
                                                                               .anyMatch(navigationDTO -> "prev".equals(navigationDTO.label));

        return !hasOtherParams && (!isCollection || !hasPrevLink)
                ? Optional.of(uriBuilder2.copy()
                                         .clearParameters()
                                         .ensureNoTrailingSlash()
                                         .toString())
                : Optional.empty();
    }

    public String getQueryWithoutPage() {
        List<NameValuePair> query = URLEncodedUtils.parse(getQuery().substring(1), Charset.forName("utf-8"))
                                                   .stream()
                                                   .filter(kvp -> !kvp.getName()
                                                                      .equals("page") && !kvp.getName()
                                                                                             .equals("offset") && !kvp.getName()
                                                                                                                          .equals("limit"))
                                                   .collect(Collectors.toList());

        return '?' + URLEncodedUtils.format(query, '&', Charset.forName("utf-8")) + '&';
    }

    public Function<String, String> getCurrentUrlWithSegment() {
        return segment -> uriBuilder.copy()
                                    .ensureLastPathSegment(segment)
                                    .toString();
    }

    public Function<String, String> getCurrentUrlWithSegmentClearParams() {
        return segment -> uriBuilder.copy()
                                    .ensureLastPathSegment(segment)
                                    .clearParameters()
                                    .toString();
    }
}
