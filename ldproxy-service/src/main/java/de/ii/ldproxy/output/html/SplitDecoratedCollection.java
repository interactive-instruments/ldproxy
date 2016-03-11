package de.ii.ldproxy.output.html;

import com.github.mustachejava.util.DecoratedCollection;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author zahnen
 */
public class SplitDecoratedCollection<T> extends AbstractCollection<Element<T>> {

    private static final int DEFAULT_LIMIT = 5;
    private final Collection<T> c;
    private final int limit;

    public SplitDecoratedCollection(Collection<T> c, int limit) {
        this.c = c;
        this.limit = limit;
    }

    public SplitDecoratedCollection(Collection<T> c) {
        this(c, DEFAULT_LIMIT);
    }

    @Override
    public Iterator<Element<T>> iterator() {
        final Iterator<T> iterator = c.iterator();
        return new Iterator<Element<T>>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Element<T> next() {
                T next = iterator.next();
                int current = index++;
                return new Element<>(current, current == 0, !iterator.hasNext(), current == limit-1 && iterator.hasNext(), next);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return c.size();
    }

    public boolean hasTooMuch() {
        return size() > limit;
    }
}

class Element<T> {
    public final int index;
    public final boolean first;
    public final boolean last;
    public final boolean tooMuch;
    public final T value;

    public Element(int index, boolean first, boolean last, boolean tooMuch, T value) {
        this.index = index;
        this.first = first;
        this.last = last;
        this.tooMuch = tooMuch;
        this.value = value;
    }
}