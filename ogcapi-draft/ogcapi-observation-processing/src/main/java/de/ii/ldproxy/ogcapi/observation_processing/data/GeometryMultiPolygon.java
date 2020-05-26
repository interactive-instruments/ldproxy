package de.ii.ldproxy.ogcapi.observation_processing.data;

import java.util.List;

public class GeometryMultiPolygon implements Geometry {
    final List<List<List<List<Float>>>> coord;

    public GeometryMultiPolygon(List<List<List<List<Float>>>> coord) {
        this.coord = coord;
    }

    public int size() { return coord.size(); }

    public List<List<List<List<Float>>>> asList() { return coord; }

    public float[] getBbox() {
        final float[] bbox = {180f,90f,-180f,-90f};
        coord.stream().forEachOrdered(polygon -> {
            polygon.stream().forEachOrdered(ring -> {
                ring.stream().forEachOrdered(pos -> {
                    float lon = pos.size()>=1 ? pos.get(0) : Float.NaN;
                    if (lon < bbox[0])
                        bbox[0] = lon;
                    if (lon > bbox[2])
                        bbox[2] = lon;
                    float lat = pos.size()>=2 ? pos.get(1) : Float.NaN;
                    if (lat < bbox[1])
                        bbox[1] = lat;
                    if (lat > bbox[3])
                        bbox[3] = lat;
                });
            });
        });
        return bbox;
    }
}
