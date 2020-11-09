/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.domain;

import com.github.mustachejava.util.DecoratedCollection;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.services.domain.GenericView;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author zahnen
 */
public class DatasetView extends GenericView {

    public String name;
    public String title;
    public String description;
    public List<String> keywords;
    public String version;
    public String license;
    public String bbox;
    public String url;
    public String metadataUrl;
    public boolean noIndex;
    public List<DatasetView> featureTypes;
    public List<NavigationDTO> breadCrumbs;
    public List<NavigationDTO> formats;
    public String urlPrefix;
    public HtmlConfiguration htmlConfig;

    public DatasetView(String template, URI uri, String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex) {
        this(template, uri, null, urlPrefix, htmlConfig, noIndex);
    }

    public DatasetView(String template, URI uri, Object data, String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex) {
        super(String.format("/templates/%s", template), uri, data);
        this.keywords = new ArrayList<>();
        this.featureTypes = new ArrayList<>();
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;
        this.noIndex = noIndex;
    }

    public DatasetView(String template, URI uri, String name, String title, String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex) {
        this(template, uri, urlPrefix, htmlConfig, noIndex);
        this.name = name;
        this.title = title;
    }

    public DatasetView(String template, URI uri, String name, String title, String description, String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex) {
        this(template, uri, urlPrefix, htmlConfig, noIndex);
        this.name = name;
        this.title = title;
        this.description = description;
    }

    public DecoratedCollection<String> getKeywordsDecorated() {
        return new DecoratedCollection<>(keywords);
    }

    public Function<String, String> getQueryWithout() {
        return without -> {
            List<String> ignore = Splitter.on(',')
                                          .trimResults()
                                          .omitEmptyStrings()
                                          .splitToList(without);

            List<NameValuePair> query = URLEncodedUtils.parse(getQuery().substring(1), Consts.ISO_8859_1)
                                                       .stream()
                                                       .filter(kvp -> !ignore.contains(kvp.getName()
                                                                                          .toLowerCase()))
                                                       .collect(Collectors.toList());

            return '?' + URLEncodedUtils.format(query, '&', Consts.UTF_8) + (!query.isEmpty() ? '&' : "");
        };
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }
}
