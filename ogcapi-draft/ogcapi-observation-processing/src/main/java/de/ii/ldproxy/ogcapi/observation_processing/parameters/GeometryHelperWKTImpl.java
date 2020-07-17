package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import com.google.common.base.Splitter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class GeometryHelperWKTImpl implements GeometryHelperWKT {

    static final String NUMBER_REGEX_NOGROUP = "[+-]?\\d+\\.?\\d*";
    static final String NUMBER_REGEX = "([+-]?\\d+\\.?\\d*)";
    static final String POSITION_REGEX_NOGROUP = NUMBER_REGEX_NOGROUP +"(?:\\s+"+ NUMBER_REGEX_NOGROUP +")+";
    static final String POSITION_REGEX = "("+ NUMBER_REGEX_NOGROUP +")(?:\\s+("+ NUMBER_REGEX_NOGROUP +"))+";
    static final String POINT_REGEX = "\\(\\s*"+POSITION_REGEX+"\\s*\\)";
    static final String LINE_STRING_REGEX_NOGROUP = "\\(\\s*"+ POSITION_REGEX_NOGROUP +"(?:\\s*\\,\\s*"+ POSITION_REGEX_NOGROUP +"\\s*)+\\)";
    static final String LINE_STRING_REGEX = "\\(\\s*("+ POSITION_REGEX_NOGROUP +")(?:\\s*\\,\\s*("+ POSITION_REGEX_NOGROUP +")\\s*)+\\)";
    static final String POLYGON_REGEX_NOGROUP = "\\(\\s*"+ LINE_STRING_REGEX_NOGROUP +"\\s*(?:\\s*\\,\\s*"+ LINE_STRING_REGEX_NOGROUP +"\\s*)*\\)";
    static final String POLYGON_REGEX = "\\(\\s*("+ LINE_STRING_REGEX_NOGROUP +")\\s*(?:\\s*\\,\\s*("+ LINE_STRING_REGEX_NOGROUP +")\\s*)*\\)";
    static final String MULTIPOLYGON_REGEX = "\\(\\s*("+ POLYGON_REGEX_NOGROUP +")\\s*(?:\\s*\\,\\s*("+ POLYGON_REGEX_NOGROUP +")\\s*)*\\)";
    static final Pattern numberPattern = Pattern.compile(NUMBER_REGEX);
    static final Pattern positionPattern = Pattern.compile(POSITION_REGEX);
    static final Pattern lineStringPattern = Pattern.compile(LINE_STRING_REGEX);
    static final Pattern polygonPattern = Pattern.compile(POLYGON_REGEX);

    @Override
    public String getPointRegex() {
        return "^\\s*POINT\\s*" + POINT_REGEX + "\\s*$";
    }

    @Override
    public String getLineStringRegex() {
        return "^\\s*LINESTRING\\s*"+LINE_STRING_REGEX+"\\s*$";
    }

    @Override
    public String getPolygonRegex() {
        return "^\\s*POLYGON\\s*"+POLYGON_REGEX+"\\s*$";
    }

    @Override
    public String getMultiPolygonRegex() {
        return "^\\s*MULTIPOLYGON\\s*"+MULTIPOLYGON_REGEX+"\\s*$";
    }

    @Override
    public List<Double> extractPosition(String text) {
        List<Double> vector = new Vector<>();
        Matcher matcher = numberPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(Double.valueOf(subText));
        }
        return vector;
    }

    @Override
    public List<List<Double>> extractLineString(String text) {
        List<List<Double>> vector = new Vector<>();
        Matcher matcher = positionPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(extractPosition(subText));
        }
        return vector;
    }

    @Override
    public List<List<List<Double>>> extractPolygon(String text) {
        List<List<List<Double>>> vector = new Vector<>();
        Matcher matcher = lineStringPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(extractLineString(subText));
        }
        return vector;
    }

    @Override
    public List<List<List<List<Double>>>> extractMultiPolygon(String text) {
        List<List<List<List<Double>>>> vector = new Vector<>();
        Matcher matcher = polygonPattern.matcher(text);
        while (matcher.find()) {
            String subText = text.substring(matcher.start(),matcher.end());
            vector.add(extractPolygon(subText));
        }
        return vector;
    }

    @Override
    public List<List<List<Double>>> convertBboxToPolygon(String bbox) {
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

    @Override
    public List<List<List<Double>>> convertBboxToPolygon(List<Double> bbox) {
        String lowerLeft = bbox.get(0)+" "+bbox.get(1);
        String lowerRight = bbox.get(2)+" "+bbox.get(1);
        String upperLeft = bbox.get(0)+" "+bbox.get(3);
        String upperRight = bbox.get(2)+" "+bbox.get(3);
        String polygon = "(("+lowerLeft+","+lowerRight+","+upperRight+","+upperLeft+","+lowerLeft+"))";
        return extractPolygon(polygon);
    }

    @Override
    public String convertMultiPolygonToWkt(List<List<List<List<Double>>>> multiPolygon) {
        return "MULTIPOLYGON"+convertMultiPolygonToWktCoordinates(multiPolygon);
    }

    @Override
    public String convertPolygonToWkt(List<List<List<Double>>> polygon) {
        return "POLYGON"+convertPolygonToWktCoordinates(polygon);
    }

    @Override
    public String convertLineStringToWkt(List<List<Double>> lineString) {
        return "LINESTRING"+convertLineStringToWktCoordinates(lineString);
    }

    @Override
    public String convertPointToWkt(List<Double> point) {
        return "POINT"+convertPointToWktCoordinates(point);
    }

    private String convertMultiPolygonToWktCoordinates(List<List<List<List<Double>>>> multiPolygon) {
        List<String> polygons = multiPolygon.stream()
                .map(polygon -> convertPolygonToWktCoordinates(polygon))
                .collect(Collectors.toList());
        return "(" + String.join(",", polygons) + ")";
    }

    private String convertPolygonToWktCoordinates(List<List<List<Double>>> polygon) {
        List<String> rings = polygon.stream()
                .map(ring -> convertLineStringToWktCoordinates(ring))
                .collect(Collectors.toList());
        return "(" + String.join(",", rings) + ")";
    }

    private String convertLineStringToWktCoordinates(List<List<Double>> lineString) {
        List<String> coordinates = lineString.stream()
                .map(coordinate -> convertPointToWktCoordinates(coordinate))
                .collect(Collectors.toList());
        return "(" + String.join(",", coordinates) + ")";
    }

    private String convertPointToWktCoordinates(List<Double> point) {
        List<String> ordinates = point.stream()
                .map(ordinate -> String.valueOf(ordinate))
                .collect(Collectors.toList());
        return String.join(" ", ordinates);
    }}
