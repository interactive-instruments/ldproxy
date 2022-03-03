/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesType;
import de.ii.ogcapi.features.core.domain.CollectionProperty;
import de.ii.ogcapi.features.core.domain.ImmutableCollectionProperty;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaArray;
import de.ii.ogcapi.features.core.domain.JsonSchemaBoolean;
import de.ii.ogcapi.features.core.domain.JsonSchemaInteger;
import de.ii.ogcapi.features.core.domain.JsonSchemaNumber;
import de.ii.ogcapi.features.core.domain.JsonSchemaObject;
import de.ii.ogcapi.features.core.domain.JsonSchemaRef;
import de.ii.ogcapi.features.core.domain.JsonSchemaString;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CollectionPropertiesView extends OgcApiView {
    public List<CollectionProperty> collectionProperties;
    public String typeTitle;
    public String enumTitle;
    public boolean hasEnum;
    public String none;
    // sum must be 12 for bootstrap
    public Integer idCols = 3;
    public Integer descCols = 9;

    public CollectionPropertiesView(OgcApiDataV2 apiData,
                                    JsonSchemaObject schemaCollectionProperties,
                                    CollectionPropertiesType type,
                                    List<Link> links,
                                    List<NavigationDTO> breadCrumbs,
                                    String staticUrlPrefix,
                                    HtmlConfiguration htmlConfig,
                                    boolean noIndex,
                                    URICustomizer uriCustomizer,
                                    I18n i18n,
                                    Optional<Locale> language) {
        super("collectionProperties.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                links,
                i18n.get(type+"Title", language),
                i18n.get(type+"Description", language));

        Map<String, JsonSchema> properties = schemaCollectionProperties.getProperties();
        ImmutableList.Builder<CollectionProperty> builder = ImmutableList.builder();
        properties.forEach((key, value) -> {
            ImmutableCollectionProperty.Builder builder2 = ImmutableCollectionProperty.builder()
                                                                    .id(key);
            builder2.title(value.getTitle())
                    .description(value.getDescription());

            if (value instanceof JsonSchemaArray) {
                builder2.isArray(true);
                value = ((JsonSchemaArray) value).getItems();
            } else {
                builder2.isArray(false);
            }

            if (value instanceof JsonSchemaString) {
                Optional<String> format = ((JsonSchemaString) value).getFormat();
                if (format.isPresent() && format.get().equals("date-time,date"))
                    builder2.type("date-time/date");
                else
                    builder2.type("string");
                builder2.values(((JsonSchemaString) value).getEnums());
            } else if (value instanceof JsonSchemaNumber) {
                builder2.type("number");
            } else if (value instanceof JsonSchemaInteger) {
                builder2.type("integer");
                builder2.values(((JsonSchemaInteger) value).getEnums().stream().map(val -> String.valueOf(val)).collect(Collectors.toList()));
            } else if (value instanceof JsonSchemaBoolean) {
                builder2.type("boolean");
            } else if (value instanceof JsonSchemaRef) {
                builder2.type(((JsonSchemaRef) value).getRef()
                                                     .replace("https://geojson.org/schema/", "")
                                                     .replace(".json", ""));
            } else {
                builder2.type("string");
            }
            builder.add(builder2.build());
        });
        this.collectionProperties = builder.build();
        Integer maxIdLength = this.collectionProperties
                .stream()
                .map(CollectionProperty::getId)
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
