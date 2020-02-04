package de.ii.ldproxy.target.gml;

import de.ii.xtraplatform.geometries.domain.CoordinatesWriter;
import org.immutables.value.Value;

import java.io.IOException;
import java.io.Writer;

@Value.Immutable
public abstract class CoordinatesWriterGml implements CoordinatesWriter<Writer> {

    @Override
    public void onStart() throws IOException {
    }

    @Override
    public void onSeparator() throws IOException {
        getDelegate().append(' ');
    }

    @Override
    public void onX(char[] chars, int offset, int length) throws IOException {
        onValue(chars, offset, length, true);
    }

    @Override
    public void onY(char[] chars, int offset, int length) throws IOException {
        onValue(chars, offset, length, getDimension() == 3);
    }

    @Override
    public void onZ(char[] chars, int offset, int length) throws IOException {
        onValue(chars, offset, length, false);
    }

    @Override
    public void onEnd() throws IOException {
    }

    private void onValue(char[] chars, int offset, int length, boolean separator) throws IOException {
        getDelegate().write(chars, offset, length);

        if (separator) {
            getDelegate().append(' ');
        }
    }
}
