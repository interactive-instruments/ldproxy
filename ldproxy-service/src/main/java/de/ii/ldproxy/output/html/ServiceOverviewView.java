/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        this.title = "ldproxy Service Overview";
        this.description = "ldproxy Service Overview";
        this.keywords = new ImmutableList.Builder<String>().add("ldproxy", "service", "overview").build();
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Services", true))
                .build();
    }
}
