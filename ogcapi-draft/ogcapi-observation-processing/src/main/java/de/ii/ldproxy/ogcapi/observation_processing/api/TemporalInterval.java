package de.ii.ldproxy.ogcapi.observation_processing.api;

import java.time.temporal.Temporal;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface TemporalInterval extends Iterable<Temporal> {

    default Stream<Temporal> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default Stream<Temporal> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    @Override
    default Spliterator<Temporal> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }

    Temporal getBegin();
    Temporal getEnd();
    Temporal getTime(String value);
    int getSteps();
}
