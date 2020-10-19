/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.app;

import de.ii.ldproxy.ogcapi.domain.*;
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
import java.util.stream.Collectors;


@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.ogcapi.domain.ApiExtension)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class ExtensionRegistryImpl implements ExtensionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionRegistryImpl.class);

    @Context
    private BundleContext bundleContext;

    private final List<ApiExtension> apiExtensions;

    ExtensionRegistryImpl() {
        this.apiExtensions = new ArrayList<>();;
    }

    @Override
    public List<ApiExtension> getExtensions() {
        return apiExtensions;
    }

    private synchronized void onArrival(ServiceReference<ApiExtension> ref) {
        try {
            final ApiExtension apiExtension = bundleContext.getService(ref);

            if (apiExtension ==null) {
                LOGGER.error("An apiExtension could not be registered: {}.", ref.getClass());
                return;
            }

            apiExtensions.add(apiExtension);

            if (apiExtension instanceof ConformanceClass) {
                final ConformanceClass conformanceClass = (ConformanceClass) apiExtension;

                LOGGER.debug("CONFORMANCE CLASS {}", conformanceClass.getConformanceClassUris().toString());
            }

            if (apiExtension instanceof FormatExtension) {
                final FormatExtension outputFormat = (FormatExtension) apiExtension;

                LOGGER.debug("OUTPUT FORMAT {} {}", outputFormat.getMediaType(), outputFormat.getPathPattern());
            }

            if (apiExtension instanceof ContentExtension) {
                final ContentExtension contentExtension = (ContentExtension) apiExtension;

                LOGGER.debug("RESOURCE CONTENT {} (class {})", contentExtension.getResourceName(), contentExtension.getClass().getName());
            }

            if (apiExtension instanceof ApiBuildingBlock) {
                final ApiBuildingBlock capabilityExtension = (ApiBuildingBlock) apiExtension;

                LOGGER.debug("CAPABILITY {}", capabilityExtension.getClass().getSimpleName());
            }

            if (apiExtension instanceof EndpointExtension) {
                final EndpointExtension endpoint = (EndpointExtension) apiExtension;

                LOGGER.debug("ENDPOINT {}", endpoint.getClass().getSimpleName());
            }

            if (apiExtension instanceof OgcApiBackgroundTask) {
                final OgcApiBackgroundTask ogcApiBackgroundTask = (OgcApiBackgroundTask) apiExtension;

                LOGGER.debug("STARTUP TASK {}", apiExtension.getClass().getSimpleName());
           }

            if (apiExtension instanceof ParameterExtension) {
                final ParameterExtension parameter = (ParameterExtension) apiExtension;

                LOGGER.debug("PARAMETER {}", apiExtension.getClass().getSimpleName());
            }

            if (apiExtension instanceof ProcessExtension) {
                final ProcessExtension process = (ProcessExtension) apiExtension;

                LOGGER.debug("PROCESS {}", process.getName());
            }

        } catch (Throwable e) {
            LOGGER.error("Error during registration of an extension: {}.", ref.getClass());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", e);
            }
        }
    }

    private synchronized void onDeparture(ServiceReference<ApiExtension> ref) {
        final ApiExtension apiExtension = bundleContext.getService(ref);

        if (Objects.nonNull(apiExtension)) {

            apiExtensions.remove(apiExtension);
        }
    }

    @Override
    public <T extends ApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
            return getExtensions().stream()
                    .filter(extension -> extension!=null && extensionType.isAssignableFrom(extension.getClass()))
                    .map(extensionType::cast)
                    .collect(Collectors.toList());
    }
}
