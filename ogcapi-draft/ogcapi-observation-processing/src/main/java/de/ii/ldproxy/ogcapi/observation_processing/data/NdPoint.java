package de.ii.ldproxy.ogcapi.observation_processing.data;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.ldproxy.ogcapi.observation_processing.parameters.QueryParameterCoordPosition.R;
import static java.lang.Math.*;

public class NdPoint implements Comparable<NdPoint> {

    public final static float getValueInKm(int axis, NdPoint point) {
        double h;
        switch (axis) {
            case 0:
                h = pow(cos(toRadians(point.val[1])),2) * pow(sin(toRadians(point.val[0]) / 2), 2);
                return (float) (2 * asin(sqrt(h)) * R);

            case 1:
                h = pow(sin(toRadians(point.val[1]) / 2), 2);
                return (float) (2 * asin(sqrt(h)) * R);

            case 2:
                return (float) (point.val[2] * R * PI / 180.0);
        }
        return 0.0f;
    }

    protected final float[] val;
    protected final int k;

    public NdPoint(float x, float y, float t, float v) {
        this.k = 3;
        val = new float[k + 1];
        val[0] = x;
        val[1] = y;
        val[2] = t;
        val[3] = v;
    }

    public float distance(NdPoint o1) {
        // Haversine formula for the planar part
        double h = pow(sin((toRadians(this.val[1]) - toRadians(o1.val[1])) / 2), 2)
                + cos(toRadians(o1.val[1])) * cos(toRadians(this.val[1]))
                * pow(sin((toRadians(this.val[0]) - toRadians(o1.val[0])) / 2), 2);
        double d = 2 * asin(sqrt(h)) * R;

        // scale temporal "degrees" as well
        double t = (this.val[2] - o1.val[2]) * R * PI / 180.0;

        return (float) sqrt(d * d + t * t);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NdPoint))
            return false;

        return compareTo((NdPoint) other) == 0;
    }

    @Override
    public int compareTo(NdPoint other) {
        int comp = Float.compare(this.val[0], other.val[0]);
        if (comp != 0)
            return comp;
        comp = Float.compare(this.val[1], other.val[1]);
        if (comp != 0)
            return comp;
        return Float.compare(this.val[2], other.val[2]);
    }

    @Override
    public String toString() {
        return "(" + Stream.of(val).map(String::valueOf).collect(Collectors.joining(", ")) + ")";
    }
}
