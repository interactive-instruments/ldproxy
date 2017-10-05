/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;

import com.google.common.collect.ImmutableList;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.views.GenericView;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * @author zahnen
 */
public class ServiceOverviewView extends DatasetView {
    public ServiceOverviewView(URI uri, Object data) {
        super("services", uri, data);
        this.title = "ldproxy Dataset Overview";
        this.description = "ldproxy Dataset Overview";
        this.keywords = new ImmutableList.Builder<String>().add("ldproxy", "service", "dataset", "overview").build();
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", true))
                .build();
    }
}
