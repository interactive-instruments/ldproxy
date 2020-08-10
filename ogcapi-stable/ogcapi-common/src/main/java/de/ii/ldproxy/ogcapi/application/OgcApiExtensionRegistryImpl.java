/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

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
        filter = "(objectClass=de.ii.ldproxy.ogcapi.domain.OgcApiExtension)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class OgcApiExtensionRegistryImpl implements OgcApiExtensionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiExtensionRegistryImpl.class);

    @Context
    private BundleContext bundleContext;

    private final List<OgcApiExtension> wfs3Extensions;

    OgcApiExtensionRegistryImpl() {
        this.wfs3Extensions = new ArrayList<>();;
    }

    @Override
    public List<OgcApiExtension> getExtensions() {
        return wfs3Extensions;
    }

    private synchronized void onArrival(ServiceReference<OgcApiExtension> ref) {
        try {
            final OgcApiExtension extension = bundleContext.getService(ref);

            if (extension==null) {
                LOGGER.error("An extension could not be registered");
                return;
            }

            wfs3Extensions.add(extension);

            if (extension instanceof ConformanceClass) {
                final ConformanceClass conformanceClass = (ConformanceClass) extension;

                LOGGER.debug("CONFORMANCE CLASS {}", conformanceClass.getConformanceClassUris().toString());
            }

            if (extension instanceof FormatExtension) {
                final FormatExtension outputFormat = (FormatExtension) extension;

                LOGGER.debug("OUTPUT FORMAT {} {}", outputFormat.getMediaType(), outputFormat.getPathPattern());
            }

            if (extension instanceof OgcApiContentExtension) {
                final OgcApiContentExtension contentExtension = (OgcApiContentExtension) extension;

                LOGGER.debug("RESOURCE CONTENT {} (class {})", contentExtension.getResourceName(), contentExtension.getClass().getName());
            }

            if (extension instanceof OgcApiBuildingBlock) {
                final OgcApiBuildingBlock capabilityExtension = (OgcApiBuildingBlock) extension;

                LOGGER.debug("CAPABILITY {}", capabilityExtension.getClass().getSimpleName());
            }

            if (extension instanceof OgcApiEndpointExtension) {
                final OgcApiEndpointExtension endpoint = (OgcApiEndpointExtension) extension;

                LOGGER.debug("ENDPOINT {}", endpoint.getClass().getSimpleName());
            }

            if (extension instanceof OgcApiStartupTask) {
                final OgcApiStartupTask ogcApiStartupTask = (OgcApiStartupTask) extension;

                LOGGER.debug("STARTUP TASK {}", extension.getClass().getSimpleName());
           }

            if (extension instanceof OgcApiParameter) {
                final OgcApiParameter parameter = (OgcApiParameter) extension;

                LOGGER.debug("PARAMETER {}", extension.getClass().getSimpleName());
            }

            if (extension instanceof OgcApiProcessExtension) {
                final OgcApiProcessExtension process = (OgcApiProcessExtension) extension;

                LOGGER.debug("PROCESS {}", process.getName());
            }

        } catch (Throwable e) {
            LOGGER.error("E", e);
        }
    }

    private synchronized void onDeparture(ServiceReference<OgcApiExtension> ref) {
        final OgcApiExtension extension = bundleContext.getService(ref);

        if (Objects.nonNull(extension)) {

            wfs3Extensions.remove(extension);
        }
    }

    @Override
    public <T extends OgcApiExtension> List<T> getExtensionsForType(Class<T> extensionType) {
            return getExtensions().stream()
                    .filter(extension -> extension!=null && extensionType.isAssignableFrom(extension.getClass()))
                    .map(extensionType::cast)
                    .collect(Collectors.toList());
    }
}
