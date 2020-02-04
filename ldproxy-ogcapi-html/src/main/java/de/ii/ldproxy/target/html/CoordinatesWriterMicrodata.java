package de.ii.ldproxy.target.html;

import de.ii.xtraplatform.geometries.domain.CoordinatesWriter;
import org.immutables.value.Value;

import java.io.IOException;
import java.io.Writer;

@Value.Immutable
public abstract class CoordinatesWriterMicrodata implements CoordinatesWriter<Writer> {

    private final String[] values = new String[2];

    @Override
    public void onStart() throws IOException {
    }

    @Override
    public void onSeparator() throws IOException {
        getDelegate().write(values[1]);
        getDelegate().append(' ');
        getDelegate().write(values[0]);
        getDelegate().append(' ');
    }

    @Override
    public void onX(char[] chars, int offset, int length) throws IOException {
        values[0] = String.copyValueOf(chars,offset,length);
    }

    @Override
    public void onY(char[] chars, int offset, int length) throws IOException {
        values[1] = String.copyValueOf(chars,offset,length);
    }

    @Override
    public void onZ(char[] chars, int offset, int length) throws IOException {
    }

    @Override
    public void onEnd() throws IOException {
        getDelegate().write(values[1]);
        getDelegate().append(' ');
        getDelegate().write(values[0]);
    }
}
