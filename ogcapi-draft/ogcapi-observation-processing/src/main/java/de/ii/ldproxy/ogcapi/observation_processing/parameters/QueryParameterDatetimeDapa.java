package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.data.TemporalIntervalLocalDate;
import de.ii.ldproxy.ogcapi.observation_processing.data.TemporalIntervalOffsetDateTime;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.BUFFER;
import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.R;

@Component
@Provides
@Instantiate
public class QueryParameterDatetimeDapa implements OgcApiQueryParameter {

    public static final double ANI = 2; // TODO document

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
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterDatetimeDapa(@Requires FeatureProcessInfo featureProcessInfo) {
        this.featureProcessInfo = featureProcessInfo;
        baseSchema = new StringSchema().pattern(DATETIME_CLOSED_REGEX);
    }

    @Override
    public String getId() {
        return "datetimeClosed";
    }

    @Override
    public String getName() {
        return "datetime";
    }

    @Override
    public String getDescription() {
        return "Either a local date, a date-time with offsets or a closed interval. Date and time expressions adhere to RFC 3339. "+
                "\n" +
                "Examples:\n" +
                "\n" +
                "* A date-time: '2018-02-12T23:20:50Z'\n" +
                "* A closed interval: '2018-02-12T00:00:00Z/2018-03-18T12:31:12Z'\n" +
                "\n" +
                "Selects features that have a temporal property that intersects the value of the parameter.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"position", "area", "resample-to-grid");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        Optional<String> defValue = getDefault(apiData, Optional.empty());
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.get());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        Optional<String> defValue = getDefault(apiData, Optional.of(collectionId));
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.get());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public boolean getRequired(OgcApiDataV2 apiData) {
        return !getDefault(apiData, Optional.empty()).isPresent();
    }

    @Override
    public boolean getRequired(OgcApiDataV2 apiData, String collectionId) {
        return !getDefault(apiData, Optional.of(collectionId)).isPresent();
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
}

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                   Map<String, String> parameters, 
                                                   OgcApiDataV2 apiData) {
        String datetime = parameters.get(getName());
        if (datetime==null) {
            datetime = getDefault(apiData, Optional.of(featureType.getId())).orElse(null);
            if (datetime == null)
                throw new IllegalArgumentException(String.format("The required parameter '%s' has no value.", getName()));
        }

        // update datetime, with buffer for the feature query
        String newDatetime = null;
        Temporal start = null;
        Temporal end = null;
        long timeBuffer = (long)(60*60*24 * BUFFER / (R * Math.PI/180.0) * ANI); // TODO review and document
        List<String> temps = Splitter.on('/').splitToList(datetime);
        boolean localDate = !datetime.contains("T");
        try {
            switch (temps.size()) {
                case 1:
                    String s = temps.get(0);
                    start = localDate ?
                            LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)).minusDays(Math.round(timeBuffer/(60*60*24))) :
                            OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s)).minusSeconds(timeBuffer);
                    end = localDate ?
                            LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)).plusDays(Math.round(timeBuffer/(60*60*24))) :
                            OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s)).plusSeconds(timeBuffer);
                    newDatetime = start.toString() + "/" + end.toString();
                    break;
                case 2:
                    s = temps.get(0);
                    start = localDate ?
                            (s.matches("^(\\.){0,2}$") ?
                                null :
                                LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)).minusDays(Math.round(timeBuffer/(60*60*24)))) :
                            (s.matches("^(\\.){0,2}$") ?
                                null :
                                OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s)).minusSeconds(timeBuffer));
                    s = temps.get(1);
                    end = localDate ?
                            (s.matches("^(\\.){0,2}$") ?
                                null :
                                LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)).plusDays(Math.round(timeBuffer/(60*60*24)))) :
                            (s.matches("^(\\.){0,2}$") ?
                                null :
                                OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s)).plusSeconds(timeBuffer));
                    newDatetime = (start==null?"..":start.toString()) + "/" + (end==null?"..":end.toString());
                    break;
                default:
                    throw new IllegalArgumentException(String.format("The parameter '%s' has an invalid value '%s'.", "datetime", datetime));
            }            
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(String.format("The parameter '%s' has an invalid value '%s'.", "datetime", datetime), e);
        }
        parameters.put(getName(),newDatetime);
        return parameters;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiDataV2 apiData) {
        String datetime = parameters.get(getName());
        if (datetime==null) {
            datetime = getDefault(apiData, Optional.of(featureType.getId())).orElse(null);
            if (datetime == null)
                throw new IllegalArgumentException(String.format("The required parameter '%s' has no value.", getName()));
        }

        // no buffer for the transformation context
        Temporal start = null;
        Temporal end = null;
        List<String> temps = Splitter.on('/').splitToList(datetime);
        boolean localDate = !datetime.contains("T");
        try {
            switch (temps.size()) {
                case 1:
                    String s = temps.get(0);
                    start = localDate ?
                            LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)) :
                            OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s));
                    end = localDate ?
                            LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)) :
                            OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s));
                    break;
                case 2:
                    s = temps.get(0);
                    start = localDate ?
                            (s.matches("^(\\.){0,2}$") ?
                                    null :
                                    LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s))) :
                            (s.matches("^(\\.){0,2}$") ?
                                    null :
                                    OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s)));
                    s = temps.get(1);
                    end = localDate ?
                            (s.matches("^(\\.){0,2}$") ?
                                    null :
                                    LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s))) :
                            (s.matches("^(\\.){0,2}$") ?
                                    null :
                                    OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(s)));
                    break;
                default:
                    throw new IllegalArgumentException(String.format("The parameter '%s' has an invalid value '%s'.", "datetime", datetime));
            }
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(String.format("The parameter '%s' has an invalid value '%s'.", "datetime", datetime));
        }

        // TODO configure step length
        TemporalInterval interval = localDate ?
                new TemporalIntervalLocalDate( (LocalDate)start, (LocalDate)end, 1) :
                new TemporalIntervalOffsetDateTime( (OffsetDateTime)start, (OffsetDateTime)end, 24*60*60);
        context.put("interval",interval);

        return context;
    }

    private Optional<String> getDefault(OgcApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                featureType.getExtension(ObservationProcessingConfiguration.class) :
                apiData.getExtension(ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultDatetime();
        }
        return Optional.empty();
    }
}
