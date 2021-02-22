/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.app;

import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.xtraplatform.dropwizard.domain.PerClassMustacheResolver;
import de.ii.xtraplatform.dropwizard.domain.PartialMustacheResolver;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.Reader;

/**
 *  Reuse the per class loader, but switch to the HTML module context.
 */
@Component
@Provides
@Instantiate
public class MustacheResolverFallback extends PerClassMustacheResolver implements PartialMustacheResolver {

    // TODO sortPriority is higher than PerClassMustacheResolver; in case of a name collision or an attempt to overload
    //      a template that exists in the HTML module locally, the HTML module template will always be used

    @Override
    public int getSortPriority() {
        return 100;
    }


    @Override
    public boolean canResolve(String templateName, Class<?> viewClass) {
        return super.canResolve(templateName, OgcApiView.class);
    }

    @Override
    public Reader getReader(String templateName, Class<?> viewClass) {
        return super.getReader(templateName, OgcApiView.class);
    }

}
