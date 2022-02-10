/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.manager;

import de.ii.xtraplatform.dropwizard.domain.StaticResourceServlet;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.nio.charset.StandardCharsets;

@Component
@Provides(
        properties = {
                @StaticServiceProperty(name = HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, type = "java.lang.String", value = "/manager"),
                @StaticServiceProperty(name = HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, type = "java.lang.String", value = "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=org.osgi.service.http)")
        })
@Instantiate
public class ManagerAppEndpoint extends StaticResourceServlet implements Servlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerAppEndpoint.class);

    @ServiceController(value = false)
    private boolean publish;

    private final boolean isEnabled;

    public ManagerAppEndpoint(@Context BundleContext bundleContext, @Requires XtraPlatform xtraPlatform) {
        super("/manager", "/manager", StandardCharsets.UTF_8, bundleContext.getBundle());

        this.isEnabled = xtraPlatform.getConfiguration().manager.enabled;
    }

    @Validate
    public void init() {
        if (isEnabled) {
            this.publish = true;
        }
    }
}
