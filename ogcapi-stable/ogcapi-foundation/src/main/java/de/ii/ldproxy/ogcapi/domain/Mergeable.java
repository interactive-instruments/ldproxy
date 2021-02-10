package de.ii.ldproxy.ogcapi.domain;

public interface Mergeable<T> {
    T mergeInto(T source);
}
