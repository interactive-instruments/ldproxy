package de.ii.ldproxy.ogcapi.observation_processing.data;

import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Iterator;

public class TemporalIntervalOffsetDateTime implements TemporalInterval {
    private final OffsetDateTime begin;
    private final OffsetDateTime end;
    private final int step;

    public TemporalIntervalOffsetDateTime(OffsetDateTime begin, OffsetDateTime end, int stepSeconds) {
        this.begin = begin;
        this.end = end;
        this.step = stepSeconds;
    }

    @Override
    public Iterator<Temporal> iterator() {
        return new OffsetDateTimeIterator(this);
    }

    public OffsetDateTime getBegin() {
        return begin;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public int getStep() {
        return step;
    }

    @Override
    public int getSteps() {
        return (int) ((Duration.between(begin, end).getSeconds()+1) / step);
    }

    @Override
    public Temporal getTime(String value) {
        return OffsetDateTime.from(DateTimeFormatter.ISO_DATE.parse(value));
    }

    public class OffsetDateTimeIterator implements Iterator {

        private OffsetDateTime current;
        private final TemporalIntervalOffsetDateTime interval;

        public OffsetDateTimeIterator(TemporalIntervalOffsetDateTime interval) {
            this.interval = interval;
            current = interval.begin;
        }

        @Override
        public boolean hasNext() {
            return !current.isAfter(interval.end);
        }

        @Override
        public OffsetDateTime next() {
            OffsetDateTime next = current;
            current = current.plusSeconds(interval.step);
            return next;
        }
    }

}
