package de.ii.ldproxy.ogcapi.observation_processing.data;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterDatetime;
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

import static java.lang.Float.NaN;

public class Observations {

    private static final Logger LOGGER = LoggerFactory.getLogger(Observations.class);

    static float NULL = Float.MIN_VALUE;
    private static final OffsetTime MIDNIGHT_UTC = OffsetTime.of(LocalTime.MIDNIGHT, ZoneOffset.UTC);
    private static final float ANI = QueryParameterDatetime.ANI; // TODO
    private static LocalDate REFERENCE_DATE = LocalDate.of(1970, Month.JANUARY, 1);
    private static OffsetDateTime REFERENCE_DATETIME = REFERENCE_DATE.atTime(MIDNIGHT_UTC);
    private static OffsetDateTime toOffsetDateTime(Temporal temp) {
        return temp instanceof LocalDate ?
                ((LocalDate) temp).atTime(MIDNIGHT_UTC) :
                (OffsetDateTime) temp;
    }
    static LocalDate date(float time) {return REFERENCE_DATE.plusDays(Math.round(time / ANI));}
    static OffsetDateTime datetime(float time) {return REFERENCE_DATETIME.plusSeconds((long)(time/ANI*60*60*24));}
    private static float temporalToFloat(Temporal temp) {
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

    public boolean addValue(float lon, float lat, Temporal time, int varIdx, float result, String locationCode, String locationName) {
        float convertedTime = temporalToFloat(time);
        long hash = hash(lon, lat, convertedTime, varIdx);
        if (hashes.contains(hash)) {
            LOGGER.warn("Duplicate observation detected. Location code '{}', coordinates '{}'/'{}', time '{}', variable '{}'.", locationCode, lon, lat, time, index2variable.get(varIdx));
            return false;
        }

        cells[0][count] = lon;
        cells[1][count] = lat;
        cells[2][count] = convertedTime;
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

    private long hash(float lon, float lat, float time, int varIdx) {
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
        float lon = loc.getLon();
        float lat = loc.getLat();
        int posCount = (int) IntStream.range(0, count).parallel()
                .filter(i -> cells[0][i] == lon && cells[1][i] == lat)
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
            if (this.cells[0][i] == lon && this.cells[1][i] == lat) {
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
                .forEachOrdered(i -> positions.add(new ObservationCollectionPointTimeSeries(new GeometryPoint(ImmutableList.of(cells[0][i], cells[1][i])), index2stationId.get(stationIndex[i]), index2stationName.get(stationIndex[i]))));
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
                        interval.parallelStream()
                                .forEach(time -> {
                                    float val = obsVar.interpolateAll(point.getLon(), point.getLat(), temporalToFloat(time));
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
                                                float val = obsPosVar.interpolateTime(temporalToFloat(time));
                                                if (val != NULL) {
                                                    pos.addValue(time, index2variable.get(var), val);
                                                }
                                            });
                                }
                            });
                });
        return positions;
    }

    private float interpolateTime(float ttime) {
        int count = this.count;
        if (count==1)
            return cells[3][0];

        for (int i = 0; i < count; i++) {
            if (cells[2][i] == ttime) {
                return cells[3][i];
            }
        }

        return ttime < cells[2][0] ? cells[3][0] : ttime > cells[2][count-1] ? cells[3][count-1] : tInterpolator.interpolate(ttime);
    }

    float interpolateAll(float tlon, float tlat, float ttime) {
        for (int i = 0; i < cells[0].length; i++) {
            if (cells[0][i]==tlon && cells[1][i]==tlat && cells[2][i]==ttime) {
                return cells[3][i];
            }
        }

        return xytInterpolate(tlon, tlat, ttime);
    }

    float xytInterpolate(float tlon, float tlat, float ttime) {
        return xytInterpolator.interpolate(tlon, tlat, ttime);
    }

    public DataArrayXyt resampleToGrid(float[] bbox, TemporalInterval interval, OptionalInt gridWidth, OptionalInt gridHeight, OptionalInt gridSteps) {
        float widthLon = bbox[2] - bbox[0];
        float heightLat = bbox[3] - bbox[1];
        int width = gridWidth.orElse(0);
        int height = gridHeight.orElse(0);
        if (width>0 && height==0)
            height = Math.round(width * heightLat / widthLon);
        else if (width==0 && height>0)
            width = Math.round(height * widthLon / heightLat);
        else if (width==0 && height==0) {
            width = 200;
            height = Math.round(width * heightLat / widthLon);
        }
        float tbegin = temporalToFloat(interval.getBegin());
        float tend = temporalToFloat(interval.getEnd());
        int tsteps = gridSteps.orElse(0);
        if (tsteps==0)
            tsteps = interval.getSteps();
        float diffx = (bbox[2] - bbox[0])/(width-1);
        float diffy = (bbox[3] - bbox[1])/(height-1);
        float difft = tsteps==1 ? 0f : (tend - tbegin)/(tsteps-1);
        List<Float> lons = new Vector<>();
        List<Float> lats = new Vector<>();
        List<Float> times = new Vector<>();
        for (int i=0; i<width; i++)
            lons.add(bbox[0]+i*diffx);
        for (int i=0; i<height; i++)
            lats.add(bbox[1]+i*diffy);
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
        obsMap.entrySet().parallelStream()
                .forEach(entry -> {
                    int var = entry.getKey();
                    Observations obsVar = entry.getValue();
                    int i3 = vars.indexOf(index2variable.get(var));
                    int i0 = 0;
                    for (float lon : lons) {
                        int i1 = 0;
                        for (float lat : lats) {
                            int i2 = 0;
                            for (float time : times) {
                                float val = obsVar.xytInterpolate(lon, lat, time);
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
                });
        return array;
    }
}
