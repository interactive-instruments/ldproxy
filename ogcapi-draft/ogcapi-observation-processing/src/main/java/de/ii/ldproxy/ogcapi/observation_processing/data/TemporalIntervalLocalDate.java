/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.data;

import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Iterator;

import static java.time.temporal.ChronoUnit.DAYS;

public class TemporalIntervalLocalDate implements TemporalInterval {
    private final LocalDate begin;
    private final LocalDate end;
    private final int step;

    public TemporalIntervalLocalDate(LocalDate begin, LocalDate end, int stepDays) {
        this.begin = begin;
        this.end = end;
        this.step = stepDays;
    }

    @Override
    public Iterator<Temporal> iterator() {
        return new LocalDateIterator(this);
    }

    public LocalDate getBegin() {
        return begin;
    }

    public LocalDate getEnd() {
        return end;
    }

    public int getStep() {
        return step;
    }

    @Override
    public Temporal getTime(String value) {
        return LocalDate.from(DateTimeFormatter.ISO_DATE.parse(value.substring(0,10)));
    }

    @Override
    public int getSteps() {
        return (int) (DAYS.between(begin, end)+1) / step;
    }

    public class LocalDateIterator implements Iterator {

        private LocalDate current;
        private final TemporalIntervalLocalDate interval;

        public LocalDateIterator(TemporalIntervalLocalDate interval) {
            this.interval = interval;
            current = interval.begin;
        }

        @Override
        public boolean hasNext() {
            return !current.isAfter(interval.end);
        }

        @Override
        public LocalDate next() {
            LocalDate next = current;
            current = current.plusDays(interval.step);
            return next;
        }
    }

}
