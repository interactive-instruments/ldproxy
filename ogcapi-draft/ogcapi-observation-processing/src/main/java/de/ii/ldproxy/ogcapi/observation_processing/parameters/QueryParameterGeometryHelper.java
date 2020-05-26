package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class QueryParameterGeometryHelper {

    static final String NUMBER_REGEX_NOGROUP = "[+-]?\\d+\\.?\\d*";
    static final String NUMBER_REGEX = "([+-]?\\d+\\.?\\d*)";
    static final String POSITION_REGEX_NOGROUP = NUMBER_REGEX_NOGROUP +"(?:\\s+"+ NUMBER_REGEX_NOGROUP +")+";
    static final String POSITION_REGEX = "("+ NUMBER_REGEX_NOGROUP +")(?:\\s+("+ NUMBER_REGEX_NOGROUP +"))+";
    static final String POINT_REGEX = "\\(\\s*"+POSITION_REGEX+"\\s*\\)";
    static final String RING_REGEX_NOGROUP = "\\(\\s*"+ POSITION_REGEX_NOGROUP +"(?:\\s*\\,\\s*"+ POSITION_REGEX_NOGROUP +"\\s*)+\\)";
    static final String RING_REGEX = "\\(\\s*("+ POSITION_REGEX_NOGROUP +")(?:\\s*\\,\\s*("+ POSITION_REGEX_NOGROUP +")\\s*)+\\)";
    static final String POLYGON_REGEX_NOGROUP = "\\(\\s*"+ RING_REGEX_NOGROUP +"\\s*(?:\\s*\\,\\s*"+ RING_REGEX_NOGROUP +"\\s*)*\\)";
    static final String POLYGON_REGEX = "\\(\\s*("+ RING_REGEX_NOGROUP +")\\s*(?:\\s*\\,\\s*("+ RING_REGEX_NOGROUP +")\\s*)*\\)";
    static final String MULTIPOLYGON_REGEX = "\\(\\s*("+ POLYGON_REGEX_NOGROUP +")\\s*(?:\\s*\\,\\s*("+ POLYGON_REGEX_NOGROUP +")\\s*)*\\)";
    static final Pattern numberPattern = Pattern.compile(NUMBER_REGEX);
    static final Pattern positionPattern = Pattern.compile(POSITION_REGEX);
    static final Pattern ringPattern = Pattern.compile(RING_REGEX);
    static final Pattern polygonPattern = Pattern.compile(POLYGON_REGEX);

    Collection<FeatureProperty> getProperties(OgcApiApiDataV2 apiData, String collectionId, OgcApiFeatureCoreProviders providers) {
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, apiData.getCollections().get(collectionId));
        FeatureType featureType = featureProvider.getData()
                .getTypes()
                .get(collectionId);
        return featureType.getProperties().values();
    }

    List<Float> extractPosition(String text) {
        List<Float> vector = new Vector<>();
        Matcher matcher = numberPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(Float.valueOf(subText));
        }
        return vector;
    }

    List<List<Float>> extractRing(String text) {
        List<List<Float>> vector = new Vector<>();
        Matcher matcher = positionPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(extractPosition(subText));
        }
        return vector;
    }

    List<List<List<Float>>> extractPolygon(String text) {
        List<List<List<Float>>> vector = new Vector<>();
        Matcher matcher = ringPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(extractRing(subText));
        }
        return vector;
    }

    List<List<List<List<Float>>>> extractMultiPolygon(String text) {
        List<List<List<List<Float>>>> vector = new Vector<>();
        Matcher matcher = polygonPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(extractPolygon(subText));
        }
        return vector;
    }

    List<List<List<Float>>> convertBboxToPolygon(String bbox) {
        List<String> ords = Splitter.on(",")
                .trimResults()
                .splitToList(bbox);
        String lowerLeft = ords.get(0)+" "+ords.get(1);
        String lowerRight = ords.get(2)+" "+ords.get(1);
        String upperLeft = ords.get(0)+" "+ords.get(3);
        String upperRight = ords.get(2)+" "+ords.get(3);
        String polygon = "(("+lowerLeft+","+lowerRight+","+upperRight+","+upperLeft+","+lowerLeft+"))";
        return extractPolygon(polygon);
    }
}
