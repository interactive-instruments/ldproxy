/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.functions;

import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcessingStatisticalFunction;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Provides
@Instantiate
public class StatisticalFunctionMin implements ObservationProcessingStatisticalFunction {

    private final ExtensionRegistry extensionRegistry;

    public StatisticalFunctionMin(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public String getName() {
        return "min";
    }

    @Override
    public Number getValue(CopyOnWriteArrayList<Number> values) {
        return Collections.min(values, Comparator.comparing(v -> v.floatValue()));
    }

    @Override
    public Class getType() { return Float.class; }
}
