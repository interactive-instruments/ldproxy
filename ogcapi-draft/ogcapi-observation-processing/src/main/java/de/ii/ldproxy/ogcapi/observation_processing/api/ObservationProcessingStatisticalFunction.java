/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiProcessExtension;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ObservationProcessingStatisticalFunction extends OgcApiProcessExtension {
    Number getValue(CopyOnWriteArrayList<Number> values);
    Class getType();
    default boolean isDefault() { return true; }

}
