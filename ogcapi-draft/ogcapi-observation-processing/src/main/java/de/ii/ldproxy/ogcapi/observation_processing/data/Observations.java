package de.ii.ldproxy.ogcapi.observation_processing.data;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import edu.mines.jtk.interp.CubicInterpolator;
import edu.mines.jtk.interp.SibsonInterpolator3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterDatetime.ANI;
import static java.lang.Float.NaN;

public class Observations {

    private static final Logger LOGGER = LoggerFactory.getLogger(Observations.class);

    static double THRESHOLD = 0.000001;
    static float NULL = Float.MIN_VALUE;
    private static final OffsetTime MIDNIGHT_UTC = OffsetTime.of(LocalTime.MIDNIGHT, ZoneOffset.UTC);
    private static LocalDate REFERENCE_DATE = LocalDate.of(1970, Month.JANUARY, 1);
    private static OffsetDateTime REFERENCE_DATETIME = REFERENCE_DATE.atTime(MIDNIGHT_UTC);
    private static OffsetDateTime toOffsetDateTime(Temporal temp) {
        return temp instanceof LocalDate ?
                ((LocalDate) temp).atTime(MIDNIGHT_UTC) :
                (OffsetDateTime) temp;
    }
    static LocalDate date(double time) {return REFERENCE_DATE.plusDays(Math.round(time / ANI));}
    static OffsetDateTime datetime(double time) {return REFERENCE_DATETIME.plusSeconds((long)(time/ANI*60*60*24));}
    private static double temporalToDouble(Temporal temp) {
        return temp instanceof LocalDate ?
                Duration.between(REFERENCE_DATETIME, ((LocalDate) temp).atTime(MIDNIGHT_UTC)).toDays()*ANI :
                Duration.between(REFERENCE_DATETIME, temp).getSeconds()*ANI/(60*60*24);
    }
    float[][] cells;
    int[] variableIndex;
    int[] stationIndex;
    String variable;
    ConcurrentMap<String, Integer> variable2index;
    ConcurrentMap<String, Integer> stationId2index;
    ConcurrentMap<Integer, String> index2variable;
    ConcurrentMap<Integer, String> index2stationId;
    ConcurrentMap<Integer, String> index2stationName;
    CubicInterpolator tInterpolator;
    SibsonInterpolator3 xytInterpolator;
    Set<Long> hashes;
    int count;

    public Observations(int count) {
        cells = new float[4][count];
        variableIndex = new int[count];
        stationIndex = new int[count];
        variable = null;
        hashes = new TreeSet<>();
        count = 0;
        variable2index = new ConcurrentHashMap<>();
        index2variable = new ConcurrentHashMap<>();
        stationId2index = new ConcurrentHashMap<>();
        index2stationId = new ConcurrentHashMap<>();
        index2stationName = new ConcurrentHashMap<>();
        tInterpolator = null;
        xytInterpolator = null;
    }

    public int getOrAddVariable(String varName) {
        if (variable2index.containsKey(varName))
            return variable2index.get(varName);

        int idx = variable2index.size();
        variable2index.put(varName, idx);
        index2variable.put(idx, varName);
        return idx;
    }

    public boolean addValue(String currentId, double lon, double lat, Temporal time, int varIdx, float result, String locationCode, String locationName) {
        double convertedTime = temporalToDouble(time);
        long hash = hash(lon, lat, convertedTime, varIdx);
        if (hashes.contains(hash)) {
            LOGGER.debug("Duplicate observation detected and skipped. Coordinates '{}'/'{}', time '{}', variable '{}', result {}.", lon, lat, time, index2variable.get(varIdx), result);
            return false;
        }

        cells[0][count] = (float) lon;
        cells[1][count] = (float) lat;
        cells[2][count] = (float) convertedTime;
        cells[3][count] = result;
        variableIndex[count] = varIdx;

        if (Objects.nonNull(locationCode)) {
            if (stationId2index.containsKey(locationCode)) {
                stationIndex[count] = stationId2index.get(locationCode);
            } else {
                stationId2index.put(locationCode, count);
                index2stationId.put(count, locationCode);
                index2stationName.put(count, locationName);
                stationIndex[count] = count;
            }
        } else {
            stationIndex[count] = -1;
        }

        hashes.add(hash);
        count++;
        return true;
    }

    private long hash(double lon, double lat, double time, int varIdx) {
        long r1 = ((long) (lon * 100000))<<40;
        long r2 = ((long) (lat * 100000))<<24;
        long r3 = ((long) (time * 100))<<3;
        long r4 = varIdx;
        return r1+r2+r3+r4;
    }

    Observations getObservations(int varIdx) {
        int count = 0;
        for (int i = 0; i < this.count; i++) {
            if (variableIndex[i] == varIdx) {
                count++;
            }
        }

        Observations newObs = new Observations(count);
        newObs.count = count;
        newObs.variable = variable;
        newObs.index2stationId = index2stationId;
        newObs.index2stationName = index2stationName;
        newObs.stationId2index = stationId2index;
        int k = 0;
        for (int i = 0; i < this.count; i++) {
            if (variableIndex[i] == varIdx) {
                newObs.cells[0][k] = this.cells[0][i];
                newObs.cells[1][k] = this.cells[1][i];
                newObs.cells[2][k] = this.cells[2][i];
                newObs.cells[3][k] = this.cells[3][i];
                newObs.variableIndex[k] = this.variableIndex[i];
                newObs.stationIndex[k] = this.stationIndex[i];
                k++;
            }
        }

        return newObs;
    }

    Observations getObservations(ObservationCollectionPointTimeSeries pos) {
        GeometryPoint loc = pos.getGeometry();
        Double lon = loc.getLon();
        Double lat = loc.getLat();
        int posCount = (int) IntStream.range(0, count).parallel()
                .filter(i -> Math.abs(cells[0][i] - lon) < THRESHOLD && Math.abs(cells[1][i] - lat) < THRESHOLD)
                .count();
        Observations newObs = new Observations(posCount);
        newObs.count = posCount;
        newObs.index2stationId = index2stationId;
        newObs.index2stationName = index2stationName;
        newObs.stationId2index = stationId2index;
        if (Objects.nonNull(this.variable))
            newObs.variable = this.variable;

        int k = 0;
        for (int i = 0; i < count; i++) {
            if (Math.abs(this.cells[0][i] - lon) < THRESHOLD && Math.abs(this.cells[1][i] - lat) < THRESHOLD) {
                newObs.cells[0][k] = this.cells[0][i];
                newObs.cells[1][k] = this.cells[1][i];
                newObs.cells[2][k] = this.cells[2][i];
                newObs.cells[3][k] = this.cells[3][i];
                newObs.variableIndex[k] = this.variableIndex[i];
                newObs.stationIndex[k] = this.stationIndex[i];
                k++;
            }
        }

        return newObs;
    }

    ObservationCollectionPointTimeSeriesList findUniquePositions() {
        ObservationCollectionPointTimeSeriesList positions = new ObservationCollectionPointTimeSeriesList();
        stationId2index.values().stream()
                .forEachOrdered(i -> positions.add(new ObservationCollectionPointTimeSeries(new GeometryPoint(ImmutableList.of((double)cells[0][i], (double)cells[1][i])), index2stationId.get(stationIndex[i]), index2stationName.get(stationIndex[i]))));
        return positions;
    }

    void createXytInterpolator() {
        if (Objects.isNull(xytInterpolator)) {
            xytInterpolator = new SibsonInterpolator3(cells[3], cells[0], cells[1], cells[2]);
            xytInterpolator.setNullValue(NULL);
        }
    }

    void createTInterpolator() {
        if (Objects.isNull(tInterpolator)) {
            tInterpolator = new CubicInterpolator(cells[2], cells[3]);
        }
    }

    public ObservationCollectionPointTimeSeries interpolate(GeometryPoint point, TemporalInterval interval) {
        ObservationCollectionPointTimeSeries timeSeriesPoint = new ObservationCollectionPointTimeSeries(point, null, null);
        interval.parallelStream()
                .forEach(time -> timeSeriesPoint.addTimeStep(time));
        IntStream.range(0, variable2index.size()).parallel()
                .forEach(var -> {
                    Observations obsVar = getObservations(var);
                    obsVar.createXytInterpolator();
                    if (obsVar.count > 0) {
                        interval.stream()
                                .forEach(time -> {
                                    float val = obsVar.interpolateAll(point.getLon(), point.getLat(), temporalToDouble(time));
                                    if (val != NULL) {
                                        timeSeriesPoint.addValue(time, index2variable.get(var), val);
                                    }
                                });
                    }
                });
        return timeSeriesPoint;
    }

    public ObservationCollectionPointTimeSeriesList interpolate(TemporalInterval interval) {
        ObservationCollectionPointTimeSeriesList positions = findUniquePositions();
        positions.parallelStream()
                .forEach(pos -> {
                    Observations obsPos = getObservations(pos);
                    interval.parallelStream()
                            .forEach(time -> pos.addTimeStep(time));
                    IntStream.range(0, variable2index.size()).parallel()
                            .forEach(var -> {
                                Observations obsPosVar = obsPos.getObservations(var);
                                if (obsPosVar.variableIndex.length > 0) {
                                    obsPosVar.createTInterpolator();
                                    interval.parallelStream()
                                            .forEach(time -> {
                                                double val = obsPosVar.interpolateTime(temporalToDouble(time));
                                                if (val != NULL) {
                                                    pos.addValue(time, index2variable.get(var), val);
                                                }
                                            });
                                }
                            });
                });
        return positions;
    }

    private double interpolateTime(double ttime) {
        int count = this.count;
        if (count==1)
            return cells[3][0];

        for (int i = 0; i < count; i++) {
            if (Math.abs(this.cells[2][i] - ttime) < THRESHOLD) {
                return cells[3][i];
            }
        }

        return ttime < cells[2][0] ? cells[3][0] : ttime > cells[2][count-1] ? cells[3][count-1] : tInterpolator.interpolate((float) ttime);
    }

    float interpolateAll(double tlon, double tlat, double ttime) {
        for (int i = 0; i < cells[0].length; i++) {
            if (Math.abs(cells[0][i] - tlon) < THRESHOLD &&
                Math.abs(cells[1][i] - tlat) < THRESHOLD &&
                Math.abs(cells[2][i] - ttime) < THRESHOLD) {
                return cells[3][i];
            }
        }

        return xytInterpolate(tlon, tlat, ttime);
    }

    float xytInterpolate(double tlon, double tlat, double ttime) {
        return xytInterpolator.interpolate((float) tlon, (float) tlat, (float) ttime);
    }

    public DataArrayXyt resampleToGrid(double[] bbox, TemporalInterval interval, OptionalInt gridWidth, OptionalInt gridHeight, OptionalInt gridSteps) {
        double widthLon = bbox[2] - bbox[0];
        double heightLat = bbox[3] - bbox[1];
        long width = gridWidth.orElse(0);
        long height = gridHeight.orElse(0);
        if (width>0 && height==0)
            height = Math.round(width * heightLat / widthLon);
        else if (width==0 && height>0)
            width = Math.round(height * widthLon / heightLat);
        else if (width==0 && height==0) {
            width = 200;
            height = Math.round(width * heightLat / widthLon);
        }
        double tbegin = temporalToDouble(interval.getBegin());
        double tend = temporalToDouble(interval.getEnd());
        long tsteps = gridSteps.orElse(0);
        if (tsteps==0)
            tsteps = interval.getSteps();
        double diffx = (bbox[2] - bbox[0])/width;
        double diffy = (bbox[3] - bbox[1])/height;
        double difft = tsteps==1 ? 0f : (tend - tbegin)/(tsteps-1);
        List<Double> lons = new Vector<>();
        List<Double> lats = new Vector<>();
        List<Double> times = new Vector<>();
        for (int i=0; i<width; i++)
            lons.add(bbox[0]+i*diffx);
        for (int i=0; i<height; i++)
            lats.add(bbox[3]-i*diffy);
        for (int i=0; i<tsteps; i++)
            times.add(tbegin+i*difft);

        ConcurrentMap<Integer, Observations> obsMap = new ConcurrentHashMap<>();
        CopyOnWriteArrayList<String> vars = new CopyOnWriteArrayList<>();
        IntStream.range(0, variable2index.size()).parallel()
                .forEach(var -> {
                    Observations obsVar = getObservations(var);
                    if (obsVar.variableIndex.length > 0) {
                        vars.add(index2variable.get(var));
                        obsMap.put(var,obsVar);
                        obsVar.createXytInterpolator();
                    }
                });

        DataArrayXyt array = new DataArrayXyt(lons.size(), lats.size(), times.size(), new Vector<>(vars), bbox[0], bbox[1], tbegin, bbox[2], bbox[3], tend);
        new ArrayList<>(obsMap.keySet()).parallelStream()
                .forEach(var -> {
                    Observations obsVar = obsMap.get(var);
                    LOGGER.debug("Resampling variable {}.", index2variable.get(var));

                    final int i3 = vars.indexOf(index2variable.get(var));
                    int i0 = 0;
                    for (double lon : lons) {
                        int i1 = 0;
                        for (double lat : lats) {
                            int i2 = 0;
                            for (double time : times) {
                                final float val = obsVar.xytInterpolate(lon+diffx/2, lat-diffy/2, time);
                                if (val == Observations.NULL)
                                    array.array[i2][i1][i0][i3] = NaN;
                                else
                                    array.array[i2][i1][i0][i3] = val;
                                i2++;
                            }
                            i1++;
                        }
                        i0++;
                    }

                    LOGGER.debug("Variable {} finished.", index2variable.get(var));
                });

        return array;
    }
}
