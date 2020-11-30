/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables.app;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.ImmutableQueryable;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.Queryable;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class QueryablesView extends OgcApiView {
    public List<Queryable> queryables;
    public String typeTitle;
    public String enumTitle;
    public boolean hasEnum;
    public String none;
    // sum must be 12 for bootstrap
    public Integer idCols = 3;
    public Integer descCols = 9;

    public QueryablesView(OgcApiDataV2 apiData,
                          JsonSchemaObject schemaQueryables,
                          List<Link> links,
                          List<NavigationDTO> breadCrumbs,
                          String staticUrlPrefix,
                          HtmlConfiguration htmlConfig,
                          boolean noIndex,
                          URICustomizer uriCustomizer,
                          I18n i18n,
                          Optional<Locale> language) {
        super("queryables.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                links,
                i18n.get("queryablesTitle", language),
                i18n.get("queryablesDescription", language));

        Map<String, JsonSchema> properties = schemaQueryables.getProperties();
        ImmutableList.Builder<Queryable> builder = ImmutableList.builder();
        properties.forEach((key, value) -> {
            Map<String, Object> values = (Map<String, Object>) value;
            ImmutableQueryable.Builder builder2 = ImmutableQueryable.builder()
                                                                    .id(key);
            if (values.containsKey("title"))
                builder2.title((String) values.get("title"));
            if (values.containsKey("description"))
                builder2.description((String) values.get("description"));
            if (values.containsKey("$ref")) {
                builder2.type(((String)values.get("$ref"))
                                      .replace("https://geojson.org/schema/", "")
                                      .replace(".json", ""));
                builder2.isArray(false);
            } else if (!values.containsKey("type")) {
                builder2.type("string");
                builder2.isArray(false);
            } else {
                String type = (String) values.get("type");
                if (type.equals("array")) {
                    builder2.isArray(true);
                    if (values.containsKey("items")) {
                        Map<String, String> items = (Map<String, String>) values.get("items");
                        type = items.getOrDefault("type", "string");
                    } else {
                        type = "string";
                    }
                } else {
                    builder2.isArray(false);
                }
                if (values.containsKey("format")) {
                    if (values.get("format").equals("date-time"))
                        type = "date-time";
                }
                builder2.type(type);
            }
            if (values.containsKey("enum")) {
                ImmutableList.Builder<String> enumBuilder = ImmutableList.builder();
                for (Object anEnum : ((Iterable) values.get("enum"))) {
                    enumBuilder.add(String.valueOf(anEnum));
                }
                builder2.values(enumBuilder.build());
            }
            builder.add(builder2.build());
        });
        this.queryables = builder.build();
        Integer maxIdLength = this.queryables
                .stream()
                .map(Queryable::getId)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(0);
        idCols = Math.min(Math.max(2, 1 + maxIdLength/10),6);
        descCols = 12 - idCols;
        this.typeTitle = i18n.get("typeTitle", language);
        this.enumTitle = i18n.get("enumTitle", language);
        this.none = i18n.get ("none", language);
    }
}
