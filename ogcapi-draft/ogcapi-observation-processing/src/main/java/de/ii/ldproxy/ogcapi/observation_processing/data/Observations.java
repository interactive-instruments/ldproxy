package de.ii.ldproxy.ogcapi.observation_processing.data;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Constant;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterDatetimeDapa.ANI;
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
    UnivariateFunction tInterpolator;
    XytInterpolator xytInterpolator;
    int count;

    public Observations(int count) {
        cells = new float[4][count];
        variableIndex = new int[count];
        stationIndex = new int[count];
        variable = null;
        this.count = 0;
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

        count++;
        return true;
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
        newObs.index2variable = index2variable;
        newObs.variable = index2variable.get(varIdx);
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
        newObs.index2variable = index2variable;
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

    private class XytInterpolator {
        final KdTree kdtree;
        final float p;
        final int n;
        final float maxDistance;

        public XytInterpolator(float[][] cells, int idwCount, double idwDistanceKm, double idwPower) {
            kdtree = new KdTree(3);
            for (int i = 0; i < cells[0].length; i++) {
                kdtree.add(new NdPoint(cells[0][i], cells[1][i], cells[2][i], cells[3][i]));
            }
            this.p = (float) idwPower;
            this.n = idwCount;
            this.maxDistance = (float) idwDistanceKm;
        }

        float interpolate(float x, float y, float t) {
            NdPoint location = new NdPoint(x, y, t, 0);
            Collection<NdPoint> observations = kdtree.nearestNeighbourSearch(n, maxDistance, location);

            // compute IDW
            float a = 0;
            float b = 0;
            for (NdPoint obs : observations) {
                double distance = obs.distance(location);
                double c = Math.pow(1 / distance, p);
                b += c;
                a += c * obs.val[3];
            }
            return a/b;
        }
    }

    void createXytInterpolator(int idwCount, double idwDistanceKm, double idwPower) {
        if (Objects.isNull(xytInterpolator)) {
            xytInterpolator = new XytInterpolator(cells, idwCount, idwDistanceKm, idwPower);
        }
    }

    void createTInterpolator() {
        if (Objects.isNull(tInterpolator)) {
            int len = cells[2].length;
            if (len<2) {
                tInterpolator = len==1 ? new Constant(cells[3][0]) : new Constant(NULL);
                return;
            }
            double[] times = new double[len];
            IntStream.range(0, len)
                    .parallel()
                    .forEach(i -> times[i] = (double) cells[2][i]);
            double[] values = new double[len];
            IntStream.range(0, len)
                    .parallel()
                    .forEach(i -> values[i] = (double) cells[3][i]);

            tInterpolator = len==2 ? new LinearInterpolator().interpolate(times, values) : new SplineInterpolator().interpolate(times, values);
        }
    }

    public ObservationCollectionPointTimeSeries interpolate(GeometryPoint point, TemporalInterval interval, int idwCount, double idwDistanceKm, double idwPower) {
        ObservationCollectionPointTimeSeries timeSeriesPoint = new ObservationCollectionPointTimeSeries(point, null, null);
        interval.parallelStream()
                .forEach(time -> timeSeriesPoint.addTimeStep(time));
        IntStream.range(0, variable2index.size()).parallel()
                .forEach(var -> {
                    Observations obsVar = getObservations(var);
                    obsVar.createXytInterpolator(idwCount, idwDistanceKm, idwPower);
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

        return ttime < cells[2][0] ? cells[3][0] : ttime > cells[2][count-1] ? cells[3][count-1] : tInterpolator.value(ttime);
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

    public DataArrayXyt resampleToGrid(double[] bbox, TemporalInterval interval, OptionalInt gridWidth, OptionalInt gridHeight, OptionalInt gridSteps, int idwCount, double idwDistanceKm, double idwPower) {
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
                        obsVar.createXytInterpolator(idwCount, idwDistanceKm, idwPower);
                    }
                });

        DataArrayXyt array = new DataArrayXyt(lons.size(), lats.size(), times.size(), new Vector<>(vars), bbox[0], bbox[1], tbegin, bbox[2], bbox[3], tend);

        new ArrayList<>(obsMap.keySet()).parallelStream()
                .forEach(var -> {
                    Observations obsVar = obsMap.get(var);
                    LOGGER.debug("Resampling variable {}.", index2variable.get(var));

                    final int i3 = vars.indexOf(index2variable.get(var));

                    IntStream.range(0, lons.size())
                            .parallel()
                            .forEach(i0 -> {
                                double lon = lons.get(i0);
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
                            });

                    LOGGER.debug("Variable {} finished.", index2variable.get(var));
                });

        return array;
    }
}
