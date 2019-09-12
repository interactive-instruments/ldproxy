/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.wfs3.api.Wfs3ParameterExtension;
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

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.ogcapi.domain.OgcApiExtension)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class Wfs3ExtensionRegistryImpl implements OgcApiExtensionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ExtensionRegistryImpl.class);

    @Context
    private BundleContext bundleContext;

    private final List<OgcApiExtension> wfs3Extensions;

    Wfs3ExtensionRegistryImpl() {
        this.wfs3Extensions = new ArrayList<>();;
    }

    // TODO: temporary hack so that the ogcapi-features-1/core conformance class can be added, too. Refactoring is required so that the extension registry is not part of Wfs3Core
    @Override
    public void addExtension(OgcApiExtension extension) {
        wfs3Extensions.add(extension);
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
                final ConformanceClass wfs3ConformanceClass = (ConformanceClass) extension;

                LOGGER.debug("CONFORMANCE CLASS {}", wfs3ConformanceClass.getConformanceClass());
            }

            if (extension instanceof FormatExtension) {
                final FormatExtension outputFormat = (FormatExtension) extension;

                LOGGER.debug("OUTPUT FORMAT {} {}", outputFormat.getMediaType(), outputFormat.getPathPattern());
            }

            if (extension instanceof OgcApiEndpointExtension) {
                final OgcApiEndpointExtension wfs3Endpoint = (OgcApiEndpointExtension) extension;

                LOGGER.debug("ENDPOINT {}", wfs3Endpoint.getApiContext());
            }

            if (extension instanceof OgcApiStartupTask) {
                final OgcApiStartupTask ogcApiStartupTask = (OgcApiStartupTask) extension;

                LOGGER.debug("STARTUP TASK {}", extension.getClass().getSimpleName());
           }

            if (extension instanceof Wfs3ParameterExtension) {
                final Wfs3ParameterExtension wfs3Parameter = (Wfs3ParameterExtension) extension;

                LOGGER.debug("PARAMETER {}", extension.getClass().getSimpleName());
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
