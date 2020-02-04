package de.ii.ldproxy.wfs3.aroundrelations;

import de.ii.xtraplatform.geometries.domain.CoordinatesWriter;
import org.immutables.value.Value;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Value.Immutable
public abstract class CoordinatesWriterJts implements CoordinatesWriter<List<Coordinate>> {

    private final double[] values = new double[2];

    @Override
    public void onStart() throws IOException {
    }

    @Override
    public void onSeparator() throws IOException {
        getDelegate().add(new Coordinate(values[0], values[1]));
    }

    @Override
    public void onX(char[] chars, int offset, int length) throws IOException {
        values[0] = Double.parseDouble(String.copyValueOf(chars,offset,length));
    }

    @Override
    public void onY(char[] chars, int offset, int length) throws IOException {
        values[1] = Double.parseDouble(String.copyValueOf(chars,offset,length));
    }

    @Override
    public void onZ(char[] chars, int offset, int length) throws IOException {
    }

    @Override
    public void onEnd() throws IOException {
        getDelegate().add(new Coordinate(values[0], values[1]));
    }
}
