/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceStatus;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class Wfs3ServiceStatus implements ServiceData {

    // TODO what is the purpose?

    public abstract ServiceStatus.STATUS getStatus();

    @Value.Default
    public boolean getHasBackgroundTask() {
        return false;
    }

    @Value.Default
    public int getProgress() {
        return 0;
    }

    @Nullable
    public abstract String getMessage();
}
