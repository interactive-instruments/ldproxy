/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.service;


import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.filter.OGCBBOXFilterExpression;
import de.ii.xtraplatform.ogc.api.filter.OGCFilter;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterAnd;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterExpression;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterLiteral;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterPropertyIsEqualTo;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterPropertyIsLike;
import de.ii.xtraplatform.ogc.api.filter.OGCFilterValueReference;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperationGetFeature;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSQuery;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import javax.ws.rs.BadRequestException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class GetFeatureFiltered extends WFSOperationGetFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetFeatureFiltered.class);
    private static final String BBOX_PARAMETER = "bbox";
    private static final String TIME_PARAMETER = "time";

    private String namespaceUri;
    private String featureTypeName;
    private Map<String, String> filterValues;
    private Map<String, String> filterPaths;

    public GetFeatureFiltered(String namespaceUri, String featureTypeName, Map<String, String> filterValues, Map<String, String> filterPaths) {
        this.namespaceUri = namespaceUri;
        this.featureTypeName = featureTypeName;
        this.filterValues = filterValues;
        this.filterPaths = filterPaths;
    }

    @Override
    protected void initialize(XMLNamespaceNormalizer nsStore) {

        String nsPrefix = nsStore.getNamespacePrefix(namespaceUri);

        WFSQuery wfsQuery = new WFSQuery();

        String qualifiedFeatureTypeName = nsPrefix + ":" + featureTypeName;

        wfsQuery.addTypename(qualifiedFeatureTypeName);

        this.addNamespace(nsPrefix, namespaceUri);

        if (filterValues != null) {
            OGCFilter filter = new OGCFilter();
            OGCFilterAnd and = filterValues.keySet().size() > 1 ? new OGCFilterAnd() : null;
            if (and != null) {
                filter.addExpression(and);
            }

            filterValues.keySet().forEach(key -> {
                if (filterPaths.get(key) != null) {
                    final String[] propertyPath = {filterPaths.get(key)};
                    String propertyValue = filterValues.get(key);

                    nsStore.getNamespaces().forEach((prefix, uri) -> {
                        if (key.contains(uri)) {
                            this.addNamespace(prefix, uri);
                        }
                        propertyPath[0] = propertyPath[0].replaceAll(uri, prefix);
                    });

                    OGCFilterValueReference valueReference = new OGCFilterValueReference(propertyPath[0]);
                    OGCFilterLiteral literal = new OGCFilterLiteral(propertyValue);

                    OGCFilterExpression filterExpression;

                    if (key.equals(BBOX_PARAMETER)) {
                        double[] bbox = Arrays.stream(propertyValue.split(",")).mapToDouble(Double::parseDouble).toArray();
                        double[] bbox2 = {bbox[1], bbox[0], bbox[3], bbox[2]};
                        filterExpression = new OGCBBOXFilterExpression(new BoundingBox(bbox2, new EpsgCrs(4326)), propertyPath[0]);
                    } else if (key.equals(TIME_PARAMETER)) {
                        try {
                            Interval fromIso8601Period = Interval.parse(propertyValue);
                            // During
                            filterExpression = new OGCFilterPropertyIsEqualTo(valueReference, literal);
                        } catch ( DateTimeParseException ignore ) {
                            try {
                                Instant fromIso8601 = Instant.parse(propertyValue);
                                // either this or TEquals
                                filterExpression = new OGCFilterPropertyIsEqualTo(valueReference, literal);
                            } catch ( DateTimeParseException e ) {
                                LOGGER.debug("TIME PARSER ERROR", e);
                                throw new BadRequestException();
                            }
                        }
                    } else if (propertyValue.contains("*")) {
                        filterExpression = new OGCFilterPropertyIsLike(valueReference, literal);
                        ((OGCFilterPropertyIsLike) filterExpression).setWildCard("*");
                    } else {
                        filterExpression = new OGCFilterPropertyIsEqualTo(valueReference, literal);
                    }

                    if (and != null) {
                        and.addOperand(filterExpression);
                    } else {
                        filter.addExpression(filterExpression);
                    }
                }
            });

            wfsQuery.addFilter(filter);
        }

        this.addQuery(wfsQuery);
    }
}
