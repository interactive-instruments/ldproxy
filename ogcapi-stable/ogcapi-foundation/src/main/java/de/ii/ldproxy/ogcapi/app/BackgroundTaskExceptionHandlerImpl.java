/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.app;

import de.ii.ldproxy.ogcapi.domain.BackgroundTaskExceptionHandler;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


@Component
@Provides
@Instantiate
public class BackgroundTaskExceptionHandlerImpl implements BackgroundTaskExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundTaskExceptionHandlerImpl.class);

    @Validate
    void onStart() {
        LOGGER.debug("OGC API BACKGROUND TASK EXCEPTION WRITER");
    }

    @Override
    public void uncaughtException(Thread t, Throwable exception) {
        LOGGER.error("Server Error during background task: {} {}", exception.getMessage(), Objects.nonNull(exception.getCause()) ? exception.getCause().getMessage() : "" );
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stacktrace:", exception);
        }
    }
}
