/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.filter.domain.FilterConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class QueryParameterFilter extends ApiExtensionCache implements OgcApiQueryParameter, ConformanceClass {

    @Override
    public String getName() {
        return "filter";
    }

    @Override
    public String getDescription() {
        return "Filter features in the collection using the query expression in the parameter value. Filter expressions " +
            "are written in the Common Query Language (CQL), which is a candidate OGC standard. This API implements " +
            "[version 1.0.0-draft.2 from January 2021](https://github.com/opengeospatial/ogcapi-features/releases/download/part3-1.0.0-draft.2/19-079.html). " +
            "The recommended language for this query parameter is CQL Text (`filter-lang=cql-text`).\n\n" +
            "CQL Text expressions are similar to SQL expressions and also support spatial, temporal and array predicates. " +
            "All property references must be queryables of the collection and must be declared in the Queryables sub-resource " +
            "of the collection.\n\n" +
            "The following are examples of CQL Text expressions:\n\n" +
            "* Logical operators (`AND`, `OR`, `NOT`) are supported\n" +
            "* Simple comparison predicates (`=`, `<>`, `<`, `>`, `<=`, `>=`):\n" +
            "  * `address.LocalityName = 'Bonn'`\n" +
            "  * `measuredHeight > 10`\n" +
            "  * `storeysAboveGround <= 4`\n" +
            "  * `creationDate > '2017-12-31'`\n" +
            "  * `creationDate < '2018-01-01'`\n" +
            "  * `creationDate >= '2018-01-01' AND creationDate <= '2018-12-31'`\n" +
            "* Advanced comparison operators (`LIKE`, `BETWEEN`, `IN`, `IS NULL`):\n" +
            "  * `name LIKE '%Kirche%'`\n" +
            "  * `measuredHeight BETWEEN 10 AND 20`\n" +
            "  * `address.LocalityName IN ('Bonn', 'Köln', 'Düren')`\n" +
            "  * `address.LocalityName NOT IN ('Bonn', 'Köln', 'Düren')`\n" +
            "  * `name IS NULL`\n" +
            "  * `name IS NOT NULL`\n" +
            "* Spatial operators (the standard Simple Feature operators, e.g., `INTERSECTS`, `WITHIN`):\n" +
            "  * `INTERSECTS( bbox, POLYGON((8 52, 9 52, 9 53, 8 53, 8 52)) )`\n" +
            "* Temporal operators (`AFTER`, `BEFORE`, `DURING`)\n" +
            "  * `creationDate AFTER 2018-01-01`\n" +
            "  * `creationDate BEFORE 2018-01-01`\n" +
            "  * `creationDate DURING 2018-01-01/2018-12-31`\n\n" +
            "Warning: The final version of the Common Query Language standard will include a number of changes to the " +
            "CQL Text language.";
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        if (apiValidation== MODE.NONE)
            return ValidationResult.of();

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .mode(apiValidation);

        for (String collectionWithoutQueryables : apiData.getCollections()
                                                         .entrySet()
                                                         .stream()
                                                         .filter(entry ->  entry.getValue()
                                                                                .getExtension(getBuildingBlockConfigurationType())
                                                                                .map(ExtensionConfiguration::getEnabled)
                                                                                .orElse(false) &&
                                                                 !entry.getValue()
                                                                       .getExtension(QueryablesConfiguration.class)
                                                                       .map(ExtensionConfiguration::getEnabled)
                                                                       .orElse(false))
                                                         .map(entry -> entry.getKey())
                                                         .collect(Collectors.toUnmodifiableList())) {
            builder.addStrictErrors(MessageFormat.format("The FILTER module is enabled for collection ''{0}'', but the QUERYABLES module is not enabled.", collectionWithoutQueryables));
        }

        return builder.build();
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                 definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
    }

    private final Schema schema = new StringSchema();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/filter",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/features-filter",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/simple-cql",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/enhanced-spatial-operators",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/arrays",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/cql-text",
                "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/cql-json");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FilterConfiguration.class;
    }

    @Override
    public Set<String> getFilterParameters(Set<String> filterParameters, OgcApiDataV2 apiData, String collectionId) {
        if (!isEnabledForApi(apiData))
            return filterParameters;

        return new ImmutableSet.Builder<String>()
                .addAll(filterParameters)
                .add("filter")
                .build();
    }
}
