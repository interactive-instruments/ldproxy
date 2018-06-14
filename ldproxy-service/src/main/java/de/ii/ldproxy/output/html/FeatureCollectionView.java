/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;

import de.ii.ldproxy.wfs3.URICustomizer;
import de.ii.xtraplatform.feature.transformer.api.TemporalExtent;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public Set<Map.Entry<String,String>> filterFields;
    public Map<String,String> bbox2;
    public TemporalExtent temporalExtent;
    public URICustomizer uriBuilder;

    public FeatureCollectionView(String template, URI uri, String name, String title, String urlPrefix, HtmlConfig htmlConfig) {
        super(template, uri, name, title, urlPrefix, htmlConfig);
        this.features = new ArrayList<>();
    }

    public String getQueryWithoutPage() {
        List<NameValuePair> query = URLEncodedUtils.parse(getQuery().substring(1), Charset.forName("utf-8")).stream()
                .filter(kvp -> !kvp.getName().equals("page") && !kvp.getName().equals("startIndex"))
                .collect(Collectors.toList());

        return '?' + URLEncodedUtils.format(query, '&', Charset.forName("utf-8")) + '&';
    }

    public Function<String, String> getCurrentUrlWithSegment() {
        return segment -> uriBuilder.copy().ensureLastPathSegment(segment).toString();
    }
}
