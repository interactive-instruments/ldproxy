/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.codelists.Codelist)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class CodelistRegistryImpl implements CodelistRegistry {


    private final BundleContext bundleContext;

    private final Map<String, Codelist> codelists;

    CodelistRegistryImpl(@Context BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.codelists = new HashMap<>();
    }

    @Override
    public Map<String, Codelist> getCodelists() {
        return ImmutableMap.copyOf(codelists);
    }

    @Override
    public Optional<Codelist> getCodelist(String id) {
        return Optional.ofNullable(codelists.get(id));
    }

    private synchronized void onArrival(ServiceReference<Codelist> ref) {
        try {
            final Codelist codelist = bundleContext.getService(ref);

            codelists.put(codelist.getId(), codelist);
        } catch (Throwable e) {
            //ignore
        }
    }

    private synchronized void onDeparture(ServiceReference<Codelist> ref) {
        final Codelist codelist = bundleContext.getService(ref);

        if (Objects.nonNull(codelist)) {
            codelists.remove(codelist.getId());
        }
    }
}
