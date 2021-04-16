/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

public abstract class AbstractQueryParameterDatetime extends ApiExtensionCache implements OgcApiQueryParameter {

    private static final String OPEN_REGEX = "(?:\\.\\.)?";
    private static final String LOCAL_DATE_REGEX = "(?:\\d{4})-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12][0-9]|3[01])";
    private static final String LOCAL_DATE_OR_OPEN_REGEX = "(?:"+LOCAL_DATE_REGEX+"|"+OPEN_REGEX+")";
    private static final String LOCAL_DATE_OPEN_INTERVAL_REGEX = LOCAL_DATE_OR_OPEN_REGEX+"/"+LOCAL_DATE_OR_OPEN_REGEX;
    private static final String LOCAL_DATE_CLOSED_INTERVAL_REGEX = LOCAL_DATE_REGEX+"/"+LOCAL_DATE_REGEX;
    private static final String OFFSET_DATE_TIME_REGEX = LOCAL_DATE_REGEX + "T(?:[01][0-9]|2[0-3]):(?:[0-5][0-9]):(?:[0-5][0-9]|60)(?:\\.[0-9]+)?(Z|(\\+|-)(?:[01][0-9]|2[0-3]):(?:[0-5][0-9]))";
    private static final String OFFSET_DATE_TIME_OR_OPEN_REGEX = "(?:"+OFFSET_DATE_TIME_REGEX+"|"+OPEN_REGEX+")";
    private static final String OFFSET_DATE_TIME_OPEN_INTERVAL_REGEX = OFFSET_DATE_TIME_OR_OPEN_REGEX+"/"+OFFSET_DATE_TIME_OR_OPEN_REGEX;
    private static final String OFFSET_DATE_TIME_CLOSED_INTERVAL_REGEX = OFFSET_DATE_TIME_REGEX+"/"+OFFSET_DATE_TIME_REGEX;
    private static final String DATETIME_OPEN_REGEX = "^"+LOCAL_DATE_REGEX + "$|^" + LOCAL_DATE_OPEN_INTERVAL_REGEX + "$|^" + OFFSET_DATE_TIME_REGEX + "$|^" + OFFSET_DATE_TIME_OPEN_INTERVAL_REGEX + "$";
    private static final String DATETIME_CLOSED_REGEX = "^"+LOCAL_DATE_REGEX + "$|^" + LOCAL_DATE_CLOSED_INTERVAL_REGEX + "$|^" + OFFSET_DATE_TIME_REGEX + "$|^" + OFFSET_DATE_TIME_CLOSED_INTERVAL_REGEX + "$";

    private final Schema baseSchema;

    public AbstractQueryParameterDatetime() {
        this.baseSchema = new StringSchema().pattern(DATETIME_OPEN_REGEX);
    }

    @Override
    public String getName() {
        return "datetime";
    }

    @Override
    public String getDescription() {
        return "Either a local date, a date-time with offsets or an open or closed interval. Date and time expressions adhere to RFC 3339. \n"+
                "Examples:\n\n" +
                "* A date-time: '2018-02-12T23:20:50Z'\n" +
                "* A closed interval: '2018-02-12T00:00:00Z/2018-03-18T12:31:12Z'\n" +
                "* Open intervals: '2018-02-12T00:00:00Z/..' or '../2018-03-18T12:31:12Z'\n\n" +
                "Selects features that have a temporal property that intersects the value of the parameter.";
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
