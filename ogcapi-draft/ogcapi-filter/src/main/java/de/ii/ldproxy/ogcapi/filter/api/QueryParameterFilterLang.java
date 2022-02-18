/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter.api;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.filter.domain.FilterConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterFilterLang extends ApiExtensionCache implements OgcApiQueryParameter {

    private static final String FILTER_LANG_CQL2_TEXT = "cql2-text";
    private static final String FILTER_LANG_CQL2_JSON = "cql2-json";

    private final FeaturesCoreProviders providers;

    @Inject
    public QueryParameterFilterLang(FeaturesCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) && providers.getFeatureProvider(apiData).map(FeatureProvider2::supportsQueries).orElse(false);
    }

    @Override
    public String getName() {
        return "filter-lang";
    }

    @Override
    public String getDescription() {
        /* TODO support CQL2 JSON
        return "Language of the query expression in the 'filter' parameter. Supported are 'cql2-text' (default) and 'cql2-json', " +
            "specified in the OGC candidate standard 'Common Query Language (CQL2)'. 'cql2-text' is an SQL-like text encoding for " +
            "filter expressions that also supports spatial, temporal and array predicates. 'cql2-json' is a JSON encoding of " +
            "that grammar, suitable for use as part of a JSON object that represents a query. The use of 'cql2-text' is recommended " +
            "for filter expressions in the 'filter' parameter.";
         */
        return "Language of the query expression in the 'filter' parameter. Supported is currently only 'cql2-text', " +
            "specified in the OGC candidate standard 'Common Query Language (CQL2)'. 'cql2-text' is an SQL-like text encoding for " +
            "filter expressions that also supports spatial, temporal and array predicates.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                 definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
    }

    /* TODO support CQL2 JSON
    private final Schema<String> schema = new StringSchema()._enum(ImmutableList.of(FILTER_LANG_CQL2_TEXT, FILTER_LANG_CQL2_JSON))._default(FILTER_LANG_CQL2_TEXT);
     */
    private final Schema<String> schema = new StringSchema()._enum(ImmutableList.of(FILTER_LANG_CQL2_TEXT))._default(FILTER_LANG_CQL2_TEXT);

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FilterConfiguration.class;
    }
}
