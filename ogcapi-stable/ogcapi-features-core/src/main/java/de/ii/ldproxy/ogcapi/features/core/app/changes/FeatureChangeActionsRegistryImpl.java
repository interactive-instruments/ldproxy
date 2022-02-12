/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app.changes;

import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction;
import de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeActionsRegistry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.ogcapi.features.core.domain.changes.FeatureChangeAction)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class FeatureChangeActionsRegistryImpl implements FeatureChangeActionsRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangeActionsRegistryImpl.class);

    private final BundleContext bundleContext;
    private final List<FeatureChangeAction> featureChangeActions;

    public FeatureChangeActionsRegistryImpl(@Context BundleContext bundleContext) {
        this.featureChangeActions = new ArrayList<>();
        this.bundleContext = bundleContext;
    }

    @Override
    public List<FeatureChangeAction> getFeatureChangeActions() {
        return featureChangeActions;
    }

    private synchronized void onArrival(ServiceReference<FeatureChangeAction> ref) {
        try {
            final FeatureChangeAction action = bundleContext.getService(ref);

            if (Objects.nonNull(action)) {
                featureChangeActions.add(action);
            }
        } catch (Throwable e) {
            LOGGER.error("Could not add a FeatureChangeAction: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace: ", e);
            }
        }
    }

    private synchronized void onDeparture(ServiceReference<FeatureChangeAction> ref) {
        try {
            final FeatureChangeAction action = bundleContext.getService(ref);

            if (Objects.nonNull(action)) {
                featureChangeActions.remove(action);
            }
        } catch (Throwable e) {
            LOGGER.error("Could not add a FeatureChangeAction: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace: ", e);
            }
        }
    }
}
