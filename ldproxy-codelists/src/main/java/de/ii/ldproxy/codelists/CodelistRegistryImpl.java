/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.codelists;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
@Provides
@Instantiate
@Wbp(
        filter = "(objectClass=de.ii.ldproxy.codelists.CodelistEntity)",
        onArrival = "onArrival",
        onDeparture = "onDeparture")
public class CodelistRegistryImpl implements CodelistRegistry {


    private final BundleContext bundleContext;

    private final Map<String, CodelistEntity> codelists;

    CodelistRegistryImpl(@Context BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.codelists = new HashMap<>();
    }

    @Override
    public Map<String,CodelistEntity> getCodelists() {
        return ImmutableMap.copyOf(codelists);
    }

    @Override
    public Optional<CodelistEntity> getCodelist(String id) {
        return Optional.ofNullable(codelists.get(id));
    }

    private synchronized void onArrival(ServiceReference<CodelistEntity> ref) {
        try {
            final CodelistEntity codelist = bundleContext.getService(ref);

            codelists.put(codelist.getId(), codelist);
        } catch (Throwable e) {
            //ignore
        }
    }

    private synchronized void onDeparture(ServiceReference<CodelistEntity> ref) {
        final CodelistEntity codelist = bundleContext.getService(ref);

        if (Objects.nonNull(codelist)) {
            codelists.remove(codelist.getId());
        }
    }
}
