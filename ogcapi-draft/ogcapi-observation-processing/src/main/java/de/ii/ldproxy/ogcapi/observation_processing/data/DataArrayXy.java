package de.ii.ldproxy.ogcapi.observation_processing.data;

import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;

import java.util.Vector;

public class DataArrayXy {
    public float[][][] array;
    private final float minLon;
    private final float minLat;
    private final float maxLon;
    private final float maxLat;
    private final int width;
    private final int height;
    private final float diffLon;
    private final float diffLat;
    private final Vector<String> vars;
    private final TemporalInterval interval;

    public DataArrayXy(int width, int height, Vector<String> vars,
                       float minLon, float minLat,
                       float maxLon, float maxLat,
                       TemporalInterval interval) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        this.width = width;
        this.height = height;
        this.diffLon = width>0 ? (maxLon-minLon)/width : 0.0f;
        this.diffLat = height>0 ? (maxLat-minLat)/height : 0.0f;
        this.vars = vars;
        this.interval = interval;
        this.array = new float[height][width][vars.size()];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Vector<String> getVars() {
        return vars;
    }

    public TemporalInterval getInterval() { return interval; }

    public float lon(int i) { return minLon + diffLon*i; }

    public float lat(int i) { return maxLat - diffLat*i; }
}
