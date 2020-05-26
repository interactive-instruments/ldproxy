package de.ii.ldproxy.ogcapi.observation_processing.data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Vector;

public class DataArrayXyt {
    public float[][][][] array;
    private final float minLon;
    private final float minLat;
    private final float minTime;
    private final float maxLon;
    private final float maxLat;
    private final float maxTime;
    private final int width;
    private final int height;
    private final int steps;
    private final Vector<String> vars;

    DataArrayXyt(int width, int height, int steps, Vector<String> vars,
                 float minLon, float minLat, float minTime,
                 float maxLon, float maxLat, float maxTime) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.minTime = minTime;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        this.maxTime = maxTime;
        this.width = width;
        this.height = height;
        this.steps = steps;
        this.vars = vars;
        this.array = new float[steps][height][width][vars.size()];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSteps() {
        return steps;
    }

    public Vector<String> getVars() {
        return vars;
    }

    public float lon(int i) {
        return width>1 ? minLon + (maxLon-minLon)*i/(width-1) : minLon;
    }

    public float lat(int i) {
        return height>1 ? minLat + (maxLat-minLat)*i/height : minLat;
    }

    public float time(int i) {
        return steps>1 ? minTime + (maxTime-minTime)*i/steps : minTime;
    }

    public LocalDate date(int i) {
        return Observations.date(time(i));
    }

    public OffsetDateTime datetime(int i) {
        return Observations.datetime(time(i));
    }
}
