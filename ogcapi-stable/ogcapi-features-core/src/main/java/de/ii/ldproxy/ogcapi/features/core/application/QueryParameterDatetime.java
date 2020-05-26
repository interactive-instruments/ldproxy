package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiContext;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class QueryParameterDatetime implements OgcApiQueryParameter {

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

    public QueryParameterDatetime() {
        baseSchema = new StringSchema().pattern(DATETIME_OPEN_REGEX);
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
    public boolean isApplicable(OgcApiApiDataV2 apiData, String definitionPath, OgcApiContext.HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method==OgcApiContext.HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items");
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }
}
